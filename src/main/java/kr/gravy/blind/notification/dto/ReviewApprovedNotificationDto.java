package kr.gravy.blind.notification.dto;

import kr.gravy.blind.notification.model.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 프로필 승인 알림 DTO
 */
public record ReviewApprovedNotificationDto(
        NotificationType type,
        UUID userPublicId,
        LocalDateTime timestamp
) implements NotificationDto {

    /**
     * 프로필 승인 알림 생성
     */
    public static ReviewApprovedNotificationDto create(UUID userPublicId) {
        return new ReviewApprovedNotificationDto(
                NotificationType.REVIEW_APPROVED,
                userPublicId,
                LocalDateTime.now()
        );
    }
}
