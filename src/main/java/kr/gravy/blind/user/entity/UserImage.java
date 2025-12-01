package kr.gravy.blind.user.entity;

import jakarta.persistence.*;
import kr.gravy.blind.common.utils.GeneratorUtil;
import kr.gravy.blind.user.entity.base.BaseImageEntity;
import kr.gravy.blind.user.model.ImageUploadStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 이미지 엔티티 (승인된 이미지만 저장)
 * - user_images: 승인된 이미지만 저장
 * - user_images_pending: 심사 대기 이미지 저장
 */
@Entity
@Table(name = "user_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserImage extends BaseImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    /**
     * 승인된 이미지 생성용
     */
    public static UserImage create(
            UserProfile userProfile,
            String s3Key,
            Integer displayOrder,
            ImageUploadStatus status
    ) {
        return new UserImage(userProfile, s3Key, displayOrder, status);
    }

    private UserImage(
            UserProfile userProfile,
            String s3Key,
            int displayOrder,
            ImageUploadStatus status
    ) {
        super(GeneratorUtil.generatePublicId(), s3Key, displayOrder, status);
        this.userProfile = userProfile;
    }
}
