package kr.gravy.blind.notification.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import kr.gravy.blind.notification.model.NotificationType;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ReviewApprovedNotificationDto.class, name = "REVIEW_APPROVED"),
        @JsonSubTypes.Type(value = ReviewRejectedNotificationDto.class, name = "REVIEW_REJECTED"),
        @JsonSubTypes.Type(value = PostCreatedNotificationDto.class, name = "POST_CREATED"),
        @JsonSubTypes.Type(value = PostDeletedNotificationDto.class, name = "POST_DELETED"),
        @JsonSubTypes.Type(value = CommentCreatedNotificationDto.class, name = "COMMENT_CREATED"),
        @JsonSubTypes.Type(value = ReplyCreatedNotificationDto.class, name = "REPLY_CREATED"),
        @JsonSubTypes.Type(value = CommentAddedNotificationDto.class, name = "COMMENT_ADDED"),
        @JsonSubTypes.Type(value = CommentDeletedNotificationDto.class, name = "COMMENT_DELETED")
})
public sealed interface NotificationDto
        permits ReviewApprovedNotificationDto, ReviewRejectedNotificationDto, PostCreatedNotificationDto, PostDeletedNotificationDto,
        CommentCreatedNotificationDto, ReplyCreatedNotificationDto, CommentAddedNotificationDto, CommentDeletedNotificationDto {

    NotificationType type();

    /**
     * 알림 대상 사용자 Public ID
     * 브로드캐스트 알림은 null
     */
    @Nullable
    UUID userPublicId();

    LocalDateTime timestamp();
}
