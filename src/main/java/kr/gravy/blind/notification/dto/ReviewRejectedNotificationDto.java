package kr.gravy.blind.notification.dto;

import jakarta.validation.constraints.NotBlank;
import kr.gravy.blind.notification.model.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 프로필 반려 알림 DTO
 */
public record ReviewRejectedNotificationDto(
        NotificationType type,
        UUID userPublicId,
        LocalDateTime timestamp,
        @NotBlank String reason  // 반려 사유 (필수)
) implements NotificationDto {

    /**
     * 프로필 반려 알림 생성
     */
    public static ReviewRejectedNotificationDto create(UUID userPublicId, String reason) {
        return new ReviewRejectedNotificationDto(
                NotificationType.REVIEW_REJECTED,
                userPublicId,
                LocalDateTime.now(),
                reason
        );
    }
}
