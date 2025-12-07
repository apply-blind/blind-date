package kr.gravy.blind.user.service;

import kr.gravy.blind.common.exception.BlindException;
import kr.gravy.blind.infrastructure.aws.event.S3DeleteEvent;
import kr.gravy.blind.infrastructure.aws.event.S3DeleteSource;
import kr.gravy.blind.user.entity.User;
import kr.gravy.blind.user.entity.UserImagePending;
import kr.gravy.blind.user.entity.UserProfile;
import kr.gravy.blind.user.entity.UserProfilePending;
import kr.gravy.blind.user.event.ReviewStatusChangedEvent;
import kr.gravy.blind.user.model.ImageUploadStatus;
import kr.gravy.blind.user.model.UserStatus;
import kr.gravy.blind.user.repository.UserImagePendingRepository;
import kr.gravy.blind.user.repository.UserProfilePendingRepository;
import kr.gravy.blind.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static kr.gravy.blind.common.exception.Status.*;

/**
 * 프로필 심사 서비스
 * 관리자의 프로필 승인/반려 처리 (AdminService에서 호출됨)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileReviewService {

    private final UserProfileRepository userProfileRepository;
    private final UserProfilePendingRepository userProfilePendingRepository;
    private final UserImagePendingRepository userImagePendingRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ProfileImageService profileImageService;

    /**
     * 프로필 승인 메서드
     * Pending 중심 아키텍처:
     * - 최초: UserProfile 생성, UserImagePending → UserImage 변환
     * - 수정: UserProfile 업데이트, 기존 이미지 선택적 삭제/추가
     * - 공통: Pending 데이터 삭제, User 상태 APPROVED로 변경
     *
     * @param user 승인할 사용자 엔티티
     */
    @Transactional
    public void approveProfile(User user) {
        // 1. Pending 데이터 조회
        UserProfilePending pending = userProfilePendingRepository
                .findByUserId(user.getId())
                .orElseThrow(() -> new BlindException(PENDING_PROFILE_NOT_FOUND));

        List<UserImagePending> pendingImages = userImagePendingRepository
                .findByUserProfilePendingIdOrderByDisplayOrder(pending.getId());

        // 이미지 개수 검증 (최소 3개 필요)
        profileImageService.validateMinimumImageCount(pendingImages);

        // 2. 최초/수정 분기 처리
        UserProfile profile;
        if (!user.hasBaseProfile()) {
            // 최초 승인: UserProfile 생성
            profile = UserProfile.create(user, pending);
            userProfileRepository.save(profile);

            // 모든 이미지를 UserImage로 변환
            profileImageService.convertPendingToImages(pendingImages, profile);

            user.markBaseProfileCreated();
            log.info("최초 프로필 승인 - userId: {}", user.getId());
        } else {
            // 수정 승인: UserProfile 업데이트
            profile = userProfileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new BlindException(PROFILE_NOT_FOUND));

            profile.updateFrom(pending);

            // 기존 이미지 선택적 삭제 & 새 이미지 추가
            profileImageService.updateProfileImages(profile, pendingImages);

            log.info("프로필 수정 승인 - userId: {}", user.getId());
        }

        // 3. 공통 처리
        cleanupPendingData(pending, pendingImages);
        user.updateStatus(UserStatus.APPROVED);

        // 4. SSE 알림
        applicationEventPublisher.publishEvent(
                ReviewStatusChangedEvent.approved(user.getPublicId())
        );
    }

    /**
     * 관리자 -> 사용자 프로필 반려
     * 이미지만 삭제, 프로필 데이터는 유지 (재수정용)
     * Pending 중심 아키텍처:
     * - UserImagePending 삭제 (DB + S3)
     * - UserProfilePending 유지 (사용자 재수정 시 기존 내용 조회용)
     * - User 상태: UNDER_REVIEW → REJECTED
     *
     * @param user   반려할 사용자 엔티티
     * @param reason 반려 사유
     */
    @Transactional
    public void rejectProfile(User user, String reason) {
        if (!user.isUnderReview()) {
            throw new BlindException(INVALID_REVIEW_STATUS);
        }

        // 1. UserProfilePending 조회
        UserProfilePending pending = userProfilePendingRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BlindException(PENDING_PROFILE_NOT_FOUND));

        // 2. Pending 이미지 삭제 (DB + S3)
        List<UserImagePending> pendingImages = userImagePendingRepository
                .findByUserProfilePendingIdOrderByDisplayOrder(pending.getId());

        userImagePendingRepository.deleteAll(pendingImages);

        // NEW 이미지만 S3 삭제 대상
        if (!pendingImages.isEmpty()) {
            List<String> s3KeysToDelete = pendingImages.stream()
                    .filter(image -> image.getStatus() != ImageUploadStatus.EXISTING)
                    .map(UserImagePending::getS3Key)
                    .toList();

            if (!s3KeysToDelete.isEmpty()) {
                applicationEventPublisher.publishEvent(
                        new S3DeleteEvent(s3KeysToDelete, S3DeleteSource.REJECT_PROFILE)
                );
            }
            log.info("프로필 반려 - Pending 이미지 삭제: {}개 (S3 삭제: {}개)",
                    pendingImages.size(), s3KeysToDelete.size());
        }

        // 3. Pending 데이터 유지 (REJECTED 재제출용)

        // 4. User 상태 업데이트
        user.reject(reason);

        log.info("프로필 반려 완료 - userId: {}, reason: {}", user.getId(), reason);

        // 5. SSE 알림 전송 (PROFILE_REJECTED)
        applicationEventPublisher.publishEvent(ReviewStatusChangedEvent.rejected(user.getPublicId(), reason));
    }

    /**
     * Pending 데이터 전체 정리 (프로필 + 이미지)
     *
     * @param pending       pending 프로필
     * @param pendingImages pending 이미지 리스트
     */
    private void cleanupPendingData(UserProfilePending pending, List<UserImagePending> pendingImages) {
        // 이미지 삭제는 ProfileImageService에 위임
        profileImageService.deleteAllPendingImages(pendingImages);

        // 프로필 삭제는 ProfileReviewService가 담당
        userProfilePendingRepository.delete(pending);

        log.info("Pending 데이터 전체 정리 완료 - userId: {}, 이미지 개수: {}",
                pending.getUserId(), pendingImages.size());
    }
}
