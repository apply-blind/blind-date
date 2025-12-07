package kr.gravy.blind.notification.dto;

import kr.gravy.blind.board.model.PostCategory;
import kr.gravy.blind.notification.model.NotificationType;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SSE 브로드캐스트용 (모든 사용자에게 실시간 목록 갱신 알림)
 */
public record PostDeletedNotificationDto(
        NotificationType type,
        UUID postPublicId,
        PostCategory category,
        LocalDateTime timestamp
) implements NotificationDto {

    @Nullable
    @Override
    public UUID userPublicId() {
        return null;
    }

    /**
     * @param postPublicId 삭제된 게시글 Public ID
     * @param category     게시글 카테고리
     * @return PostDeletedNotificationDto
     */
    public static PostDeletedNotificationDto create(UUID postPublicId, PostCategory category) {
        return new PostDeletedNotificationDto(
                NotificationType.POST_DELETED,
                postPublicId,
                category,
                LocalDateTime.now()
        );
    }
}
