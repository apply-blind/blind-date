package kr.gravy.blind.user.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import kr.gravy.blind.common.BaseEntity;
import kr.gravy.blind.common.exception.BlindException;
import kr.gravy.blind.user.model.ImageUploadStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

import static kr.gravy.blind.common.exception.Status.INVALID_DISPLAY_ORDER;

/**
 * 이미지 공통 필드 추상 클래스
 */
@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseImageEntity extends BaseEntity {

    @Column(name = "public_id", nullable = false)
    protected UUID publicId;

    @Column(name = "s3_key", nullable = false, length = 500)
    protected String s3Key;

    @Column(name = "display_order", nullable = false)
    protected int displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    protected ImageUploadStatus status;

    /**
     * 공통 필드 생성자
     * 자식 클래스에서 super()로 호출
     */
    protected BaseImageEntity(UUID publicId, String s3Key, int displayOrder, ImageUploadStatus status) {
        this.publicId = publicId;
        this.s3Key = s3Key;
        this.displayOrder = displayOrder;
        this.status = status;
    }

    /**
     * 업로드 상태 업데이트
     */
    public void updateStatus(ImageUploadStatus status) {
        this.status = status;
    }

    public void updateDisplayOrder(int newDisplayOrder) {
        if (newDisplayOrder < 1) {
            throw new BlindException(INVALID_DISPLAY_ORDER);
        }
        this.displayOrder = newDisplayOrder;
    }
}
