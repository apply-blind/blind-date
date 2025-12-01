package kr.gravy.blind.notification.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import kr.gravy.blind.notification.model.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 알림 DTO (Sealed Interface)
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ReviewApprovedNotificationDto.class, name = "REVIEW_APPROVED"),
        @JsonSubTypes.Type(value = ReviewRejectedNotificationDto.class, name = "REVIEW_REJECTED")
})
public sealed interface NotificationDto
        permits ReviewApprovedNotificationDto, ReviewRejectedNotificationDto {

    NotificationType type();

    UUID userPublicId();

    LocalDateTime timestamp();
}
