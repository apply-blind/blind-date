package kr.gravy.blind.user.service;

import kr.gravy.blind.common.exception.BlindException;
import kr.gravy.blind.configuration.properties.S3Properties;
import kr.gravy.blind.infrastructure.aws.S3Service;
import kr.gravy.blind.infrastructure.aws.event.S3DeleteEvent;
import kr.gravy.blind.infrastructure.aws.event.S3DeleteSource;
import kr.gravy.blind.user.dto.ProfileUpdateDto;
import kr.gravy.blind.user.entity.UserImage;
import kr.gravy.blind.user.entity.UserImagePending;
import kr.gravy.blind.user.entity.UserProfile;
import kr.gravy.blind.user.entity.UserProfilePending;
import kr.gravy.blind.user.model.ImageUploadStatus;
import kr.gravy.blind.user.repository.UserImagePendingRepository;
import kr.gravy.blind.user.repository.UserImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static kr.gravy.blind.common.exception.Status.IMAGE_NOT_FOUND;
import static kr.gravy.blind.common.exception.Status.TOO_MANY_IMAGES;

/**
 * 프로필 이미지 처리 서비스
 * 이미지 업로드, 변환, 정리 등 이미지 관련 모든 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileImageService {

    private final UserImageRepository userImageRepository;
    private final UserImagePendingRepository userImagePendingRepository;
    private final S3Service s3Service;
    private final S3Properties s3Properties;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 이미지 메타데이터 처리 및 Presigned URL 발급
     * EXISTING: 기존 이미지 참조 (UserImagePending에 existing_image_id 저장)
     * NEW: 신규 이미지 생성 및 Presigned URL 발급
     *
     * @param pending       UserProfilePending
     * @param imageMetadata 이미지 메타데이터 리스트
     * @return Presigned URL 정보 리스트 (NEW 타입만)
     */
    public List<ProfileUpdateDto.PresignedUrlInfo> createPresignedUrlsForImages(
            UserProfilePending pending,
            List<ProfileUpdateDto.ImageUpdateMetadata> imageMetadata
    ) {
        List<ProfileUpdateDto.PresignedUrlInfo> presignedUrls = new ArrayList<>();

        // 이미지 메타데이터 처리 (EXISTING/NEW 타입별)
        for (ProfileUpdateDto.ImageUpdateMetadata metadata : imageMetadata) {
            switch (metadata.type()) {
                case EXISTING -> {
                    // EXISTING: 기존 이미지 참조
                    UserImage existingImage = userImageRepository.findByPublicId(metadata.imagePublicId())
                            .orElseThrow(() -> new BlindException(IMAGE_NOT_FOUND));

                    UserImagePending imagePending = UserImagePending.create(
                            pending,
                            existingImage.getS3Key(),
                            metadata.displayOrder(),
                            ImageUploadStatus.EXISTING,
                            existingImage.getId()
                    );
                    userImagePendingRepository.save(imagePending);
                    // EXISTING 타입은 Presigned URL 없음
                }
                case NEW -> {
                    // NEW: 신규 이미지 생성
                    String s3Key = s3Service.generateS3Key(metadata.filename(), metadata.contentType());
                    UserImagePending imagePending = UserImagePending.create(
                            pending,
                            s3Key,
                            metadata.displayOrder(),
                            ImageUploadStatus.NOT_UPLOADED,
                            null
                    );
                    userImagePendingRepository.save(imagePending);

                    String presignedUrl = s3Service.generatePutPresignedUrl(s3Key, metadata.contentType());
                    presignedUrls.add(new ProfileUpdateDto.PresignedUrlInfo(
                            imagePending.getPublicId(),
                            presignedUrl,
                            s3Key,
                            metadata.displayOrder()
                    ));
                }
            }

        }

        return presignedUrls;
    }

    /**
     * Pending 이미지를 UserImage로 변환
     * 모든 pending 이미지를 UserImage 엔티티로 변환하여 저장
     *
     * @param pendingImages pending 이미지 리스트
     * @param profile       대상 프로필
     */
    public void convertPendingToImages(List<UserImagePending> pendingImages, UserProfile profile) {
        for (UserImagePending pendingImage : pendingImages) {
            UserImage image = UserImage.create(
                    profile,
                    pendingImage.getS3Key(),
                    pendingImage.getDisplayOrder(),
                    ImageUploadStatus.UPLOADED
            );
            userImageRepository.save(image);
        }
        log.info("Pending 이미지를 UserImage로 변환 완료 - profileId: {}, 이미지 개수: {}",
                profile.getId(), pendingImages.size());
    }

    /**
     * 프로필 이미지 업데이트 (수정 승인 시)
     * 기존 이미지 중 선택되지 않은 것은 삭제, 새 이미지는 추가
     *
     * @param profile       대상 프로필
     * @param pendingImages pending 이미지 리스트
     */
    public void updateProfileImages(UserProfile profile, List<UserImagePending> pendingImages) {
        // 1. EXISTING 이미지 ID 수집 (유지할 이미지)
        Set<Long> existingImageIds = pendingImages.stream()
                .map(UserImagePending::getExistingImageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 2. 기존 이미지 중 EXISTING에 없는 것만 필터링 (삭제 대상)
        List<UserImage> oldImages = userImageRepository.findByUserProfileIdOrderByDisplayOrder(profile.getId());
        List<UserImage> imagesToDelete = oldImages.stream()
                .filter(img -> !existingImageIds.contains(img.getId()))
                .collect(Collectors.toList());

        // 3. NEW 이미지만 UserImage로 변환하여 저장
        int newImageCount = 0;
        for (UserImagePending pendingImage : pendingImages) {
            if (pendingImage.getStatus() != ImageUploadStatus.EXISTING) {
                // NEW 이미지만 저장
                UserImage image = UserImage.create(
                        profile,
                        pendingImage.getS3Key(),
                        pendingImage.getDisplayOrder(),
                        ImageUploadStatus.UPLOADED
                );
                userImageRepository.save(image);
                newImageCount++;
            }
        }

        // 4. 새 이미지 저장 후, 기존 이미지 삭제
        if (!imagesToDelete.isEmpty()) {
            userImageRepository.deleteAll(imagesToDelete);

            // 트랜잭션 커밋 후 S3 삭제 이벤트 발행
            List<String> s3Keys = imagesToDelete.stream()
                    .map(UserImage::getS3Key)
                    .toList();
            applicationEventPublisher.publishEvent(
                    new S3DeleteEvent(s3Keys, S3DeleteSource.APPROVE_PROFILE_UPDATE_IMAGES)
            );
            log.info("사용하지 않을 기존 이미지 삭제 - profileId: {}, 삭제 개수: {}", profile.getId(), imagesToDelete.size());
        }

        log.info("프로필 이미지 업데이트 완료 - profileId: {}, 신규: {}개, 삭제: {}개",
                profile.getId(), newImageCount, imagesToDelete.size());
    }

    /**
     * Pending 이미지 데이터 삭제
     * ProfileImageService는 이미지만 담당 (SRP 준수)
     *
     * @param pendingImages pending 이미지 리스트
     */
    public void deleteAllPendingImages(List<UserImagePending> pendingImages) {
        if (pendingImages.isEmpty()) {
            return;
        }
        userImagePendingRepository.deleteAll(pendingImages);
        log.info("Pending 이미지 삭제 완료 - 이미지 개수: {}", pendingImages.size());
    }

    /**
     * 이미지 개수 검증 (properties에서 설정 가능)
     */
    public void validateMinimumImageCount(List<UserImagePending> images) {
        if (images.size() < s3Properties.minImageCount()) {
            throw new BlindException(
                    String.format("프로필 이미지는 최소 %d개 이상 업로드해야 합니다", s3Properties.minImageCount())
            );
        }
        if (images.size() > s3Properties.maxImageCount()) {
            throw new BlindException(TOO_MANY_IMAGES);
        }
    }
}
