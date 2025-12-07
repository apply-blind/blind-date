package kr.gravy.blind.notification.dto;

import kr.gravy.blind.notification.model.NotificationType;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 댓글 삭제 알림 DTO (브로드캐스트)
 * - userPublicId: null (특정 사용자 아닌 전체 알림)
 * - 게시글 상세 페이지를 보고 있는 모든 사용자에게 실시간 댓글 마스킹 처리
 */
public record CommentDeletedNotificationDto(
        NotificationType type,
        UUID postPublicId,
        UUID commentPublicId,
        LocalDateTime timestamp
) implements NotificationDto {

    /**
     * userPublicId는 브로드캐스트 알림이므로 null 반환
     */
    @Nullable
    @Override
    public UUID userPublicId() {
        return null;
    }

    /**
     * 댓글 삭제 브로드캐스트 알림 생성
     * Effective Java Item 1: 정적 팩터리 메서드
     *
     * @param postPublicId    게시글 Public ID
     * @param commentPublicId 삭제된 댓글 Public ID
     * @return CommentDeletedNotificationDto
     */
    public static CommentDeletedNotificationDto create(UUID postPublicId, UUID commentPublicId) {
        return new CommentDeletedNotificationDto(
                NotificationType.COMMENT_DELETED,
                postPublicId,
                commentPublicId,
                LocalDateTime.now()
        );
    }
}
