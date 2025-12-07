package kr.gravy.blind.notification.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import kr.gravy.blind.notification.model.NotificationType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 프로필 승인 알림 엔티티
 */
@Entity
@DiscriminatorValue("REVIEW_APPROVED")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewApprovedNotification extends Notification {

    @Override
    public NotificationType getType() {
        return NotificationType.REVIEW_APPROVED;
    }

    /**
     * 프로필 승인 알림 생성
     */
    public static ReviewApprovedNotification create(UUID userPublicId) {
        ReviewApprovedNotification notification = new ReviewApprovedNotification();
        notification.userPublicId = userPublicId;
        notification.isRead = false;
        return notification;
    }
}