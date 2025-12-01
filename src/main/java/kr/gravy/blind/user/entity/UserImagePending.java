package kr.gravy.blind.user.entity;

import jakarta.persistence.*;
import kr.gravy.blind.common.utils.GeneratorUtil;
import kr.gravy.blind.user.entity.base.BaseImageEntity;
import kr.gravy.blind.user.model.ImageUploadStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 이미지 Pending 엔티티 (심사 대기 이미지만 저장)
 * - UserImage: 승인된 이미지
 * - UserImagePending: 심사 대기 이미지
 */
@Entity
@Table(name = "user_images_pending")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserImagePending extends BaseImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_pending_id", nullable = false)
    private UserProfilePending userProfilePending;

    @Column(name = "existing_image_id")
    private Long existingImageId;

    /**
     * Pending 이미지 생성용
     *
     * @param userProfilePending UserProfilePending 엔티티
     * @param s3Key              S3 객체 키
     * @param displayOrder       표시 순서 (1-based)
     * @param status             S3 업로드 상태 (NOT_UPLOADED, UPLOADED, EXISTING)
     * @param existingImageId    기존 이미지 ID (EXISTING 타입일 때만 사용)
     * @return UserImagePending
     */
    public static UserImagePending create(
            UserProfilePending userProfilePending,
            String s3Key,
            Integer displayOrder,
            ImageUploadStatus status,
            Long existingImageId
    ) {
        return new UserImagePending(userProfilePending, s3Key, displayOrder, status, existingImageId);
    }

    private UserImagePending(
            UserProfilePending userProfilePending,
            String s3Key,
            int displayOrder,
            ImageUploadStatus status,
            Long existingImageId
    ) {
        super(GeneratorUtil.generatePublicId(), s3Key, displayOrder, status);
        this.userProfilePending = userProfilePending;
        this.existingImageId = existingImageId;
    }
}
