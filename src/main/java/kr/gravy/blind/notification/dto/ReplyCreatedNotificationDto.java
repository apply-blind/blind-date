package kr.gravy.blind.notification.dto;

import kr.gravy.blind.notification.model.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 대댓글 생성 알림 DTO (1:1 알림)
 * - userPublicId: 멘션된 사용자
 * - 댓글에 대댓글이 달렸음을 알림
 */
public record ReplyCreatedNotificationDto(
        NotificationType type,
        UUID userPublicId,      // 알림 받을 사용자 (멘션된 사용자)
        UUID postPublicId,      // 게시글 상세 페이지 이동용
        String postTitle,       // 게시글 제목
        String replyContent,    // 대댓글 내용 미리보기
        LocalDateTime timestamp
) implements NotificationDto {

    /**
     * @param userPublicId 알림 받을 사용자 Public ID (멘션된 사용자)
     * @param postPublicId 게시글 Public ID
     * @param postTitle    게시글 제목
     * @param replyContent 대댓글 내용 미리보기
     * @return ReplyCreatedNotificationDto
     */
    public static ReplyCreatedNotificationDto create(
            UUID userPublicId,
            UUID postPublicId,
            String postTitle,
            String replyContent
    ) {
        return new ReplyCreatedNotificationDto(
                NotificationType.REPLY_CREATED,
                userPublicId,
                postPublicId,
                postTitle,
                replyContent,
                LocalDateTime.now()
        );
    }
}
