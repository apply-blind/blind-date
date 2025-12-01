package kr.gravy.blind.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 프로필 반려 알림 엔티티
 */
@Entity
@DiscriminatorValue("REVIEW_REJECTED")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewRejectedNotification extends Notification {

    @Column(name = "reason", length = 500)
    private String reason;

    /**
     * 프로필 반려 알림 생성
     */
    public static ReviewRejectedNotification create(UUID userPublicId, String reason) {
        ReviewRejectedNotification notification = new ReviewRejectedNotification();
        notification.userPublicId = userPublicId;
        notification.reason = reason;
        return notification;
    }
}