package kr.gravy.blind.notification.dto;

import kr.gravy.blind.notification.model.NotificationType;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 댓글 추가 알림 DTO
 * - userPublicId: null => 브로드캐스트
 * - 해당 게시글을 보고 있는 모든 사용자에게 실시간 댓글 목록 업데이트
 */
public record CommentAddedNotificationDto(
        NotificationType type,
        UUID postPublicId,       // 게시글 Public ID (필터링용)
        UUID commentPublicId,    // 댓글 Public ID
        LocalDateTime timestamp
) implements NotificationDto {

    @Nullable
    @Override
    public UUID userPublicId() {
        return null;
    }

    /**
     * 댓글 추가 브로드캐스트 알림 생성
     *
     * @param postPublicId    게시글 Public ID
     * @param commentPublicId 댓글 Public ID
     * @return CommentAddedNotificationDto
     */
    public static CommentAddedNotificationDto create(
            UUID postPublicId,
            UUID commentPublicId
    ) {
        return new CommentAddedNotificationDto(
                NotificationType.COMMENT_ADDED,
                postPublicId,
                commentPublicId,
                LocalDateTime.now()
        );
    }
}
