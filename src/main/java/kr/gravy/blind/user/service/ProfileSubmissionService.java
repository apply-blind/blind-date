package kr.gravy.blind.user.service;

import kr.gravy.blind.common.exception.BlindException;
import kr.gravy.blind.infrastructure.aws.S3Service;
import kr.gravy.blind.infrastructure.aws.event.S3DeleteEvent;
import kr.gravy.blind.infrastructure.aws.event.S3DeleteSource;
import kr.gravy.blind.user.dto.ProfileUpdateDto;
import kr.gravy.blind.user.entity.User;
import kr.gravy.blind.user.entity.UserImagePending;
import kr.gravy.blind.user.entity.UserProfilePending;
import kr.gravy.blind.user.model.ImageUploadStatus;
import kr.gravy.blind.user.model.ProfileData;
import kr.gravy.blind.user.model.UserStatus;
import kr.gravy.blind.user.repository.UserImagePendingRepository;
import kr.gravy.blind.user.repository.UserProfilePendingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static kr.gravy.blind.common.exception.Status.*;

/**
 * 프로필 제출 및 검증 서비스
 * 사용자의 프로필 제출, 수정 요청 및 S3 업로드 검증 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileSubmissionService {

    private final UserProfilePendingRepository userProfilePendingRepository;
    private final UserImagePendingRepository userImagePendingRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    private final S3Service s3Service;
    private final ProfileImageService profileImageService;

    /**
     * 프로필 심사 요청 제출 (회원가입 플로우)
     * PROFILE_WRITING, REJECTED 상태에서 요청 가능
     *
     * @param user    현재 로그인한 사용자
     * @param request 프로필 심사 요청 (profile + imageMetadata)
     * @return Presigned URLs
     */
    @Transactional
    public ProfileUpdateDto.Response submitProfileUpdateRequest(User user, ProfileUpdateDto.Request request) {
        // Guard: 상태 검증
        if (!user.getStatus().canSubmitProfileRequest()) {
            throw new BlindException(INVALID_USER_STATUS);
        }

        // 1. UserProfilePending 생성 또는 업데이트 (최초(PROFILE_WRITING -> null, REJECTED -> have, APPROVED -> null or have)
        UserProfilePending pending = userProfilePendingRepository.findByUserId(user.getId())
                .orElse(null);

        if (pending == null) {
            // 최초 제출 (PROFILE_WRITING) 또는 APPROVED 사용자 신규 요청
            if (user.getStatus() != UserStatus.PROFILE_WRITING && user.getStatus() != UserStatus.APPROVED) {
                throw new BlindException(PROFILE_ALREADY_UNDER_REVIEW);
            }

            // DTO → Value Object 변환 (Service Layer 책임)
            ProfileData profileData = ProfileData.from(request.profile());
            pending = UserProfilePending.create(user.getId(), profileData);
            userProfilePendingRepository.save(pending);
        } else {
            // 재제출 (REJECTED) 또는 APPROVED 사용자 재요청
            if (user.getStatus() != UserStatus.APPROVED && user.getStatus() != UserStatus.REJECTED) {
                throw new BlindException(PROFILE_ALREADY_UNDER_REVIEW);
            }

            // APPROVED 사용자가 수정 요청 시 기존 pending 이미지 삭제
            if (user.isApproved()) {
                List<UserImagePending> oldPendingImages = userImagePendingRepository
                        .findByUserProfilePendingIdOrderByDisplayOrder(pending.getId());
                if (!oldPendingImages.isEmpty()) {
                    userImagePendingRepository.deleteAll(oldPendingImages);

                    // NEW 이미지만 S3 삭제 대상
                    List<String> s3KeysToDelete = oldPendingImages.stream()
                            .filter(image -> image.getStatus() != ImageUploadStatus.EXISTING)
                            .map(UserImagePending::getS3Key)
                            .toList();

                    if (!s3KeysToDelete.isEmpty()) {
                        applicationEventPublisher.publishEvent(
                                new S3DeleteEvent(s3KeysToDelete, S3DeleteSource.SUBMIT_PROFILE_UPDATE_APPROVED)
                        );
                    }
                    log.info("APPROVED 사용자 재수정 요청 - pending 이미지 삭제: {}개 (S3 삭제: {}개)",
                            oldPendingImages.size(), s3KeysToDelete.size());
                }
            }

            // DTO → Value Object 변환 (Service Layer 책임)
            ProfileData profileData = ProfileData.from(request.profile());
            pending.updateFrom(profileData);
        }

        // 2. 신규 이미지 생성 및 Presigned URL 발급
        List<ProfileUpdateDto.PresignedUrlInfo> presignedUrls = profileImageService.createPresignedUrlsForImages(pending, request.imageMetadata());

        user.updateStatus(UserStatus.UNDER_REVIEW);


        log.info("프로필 심사 요청 제출 완료 - userId: {}, status: {}, 이미지 개수: {}", user.getId(), user.getStatus(), presignedUrls.size());


        return new ProfileUpdateDto.Response(presignedUrls);
    }

    /**
     * S3 이미지 업로드 검증
     * 클라이언트가 S3 업로드 완료 후 호출
     * PENDING → UPLOADED 상태 변경
     * REJECTED만 UNDER_REVIEW로 상태 변경 (APPROVED는 유지)
     *
     * @param user 현재 로그인한 사용자
     */
    @Transactional
    public void verifyImageUploads(User user) {
        // 1. UserProfilePending 조회
        UserProfilePending pending = userProfilePendingRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BlindException(PENDING_PROFILE_NOT_FOUND));

        // 2. UserImagePending 조회
        List<UserImagePending> pendingImages = userImagePendingRepository
                .findByUserProfilePendingIdOrderByDisplayOrder(pending.getId());

        // 3. 전체 이미지 개수 검증 (EXISTING + NEW 합계)
        profileImageService.validateMinimumImageCount(pendingImages);

        // 4. S3 실제 업로드 검증 (NEW 이미지만 검증)
        for (UserImagePending image : pendingImages) {
            // EXISTING 이미지는 이미 업로드되어 있으므로 검증 스킵
            if (image.getStatus() == ImageUploadStatus.EXISTING) {
                continue;
            }

            // NEW 이미지 S3 업로드 검증
            if (!s3Service.exists(image.getS3Key())) {
                throw new BlindException(
                        String.format("S3 업로드 미완료 - s3Key: %s", image.getS3Key())
                );
            }

            // 정상 플로우: 상태 업데이트 (NOT_UPLOADED → UPLOADED)
            image.updateStatus(ImageUploadStatus.UPLOADED);
        }

        // 5. 상태 변경 (REJECTED, APPROVED → UNDER_REVIEW)
        // hasBaseProfile로 최초/수정 구분 가능하므로 APPROVED도 변경
        if (user.isRejected() || user.isApproved()) {
            user.updateStatus(UserStatus.UNDER_REVIEW);
        }

        log.info("S3 이미지 업로드 검증 완료 - userId: {}, 이미지 개수: {}, 최종 상태: {}",
                user.getId(), pendingImages.size(), user.getStatus());
    }

}
