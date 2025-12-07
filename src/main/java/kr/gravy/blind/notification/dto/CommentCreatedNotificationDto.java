package kr.gravy.blind.notification.dto;

import kr.gravy.blind.notification.model.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 댓글 생성 알림 DTO (1:1 알림)
 * - userPublicId: 게시글 작성자
 * - 게시글에 댓글이 달렸음을 알림
 */
public record CommentCreatedNotificationDto(
        NotificationType type,
        UUID userPublicId,      // 알림 받을 사용자 (게시글 작성자)
        UUID postPublicId,      // 게시글 상세 페이지 이동용
        String postTitle,       // 게시글 제목
        String commentContent,  // 댓글 내용 미리보기
        LocalDateTime timestamp
) implements NotificationDto {

    /**
     * @param userPublicId   알림 받을 사용자 Public ID (게시글 작성자)
     * @param postPublicId   게시글 Public ID
     * @param postTitle      게시글 제목
     * @param commentContent 댓글 내용 미리보기
     * @return CommentCreatedNotificationDto
     */
    public static CommentCreatedNotificationDto create(
            UUID userPublicId,
            UUID postPublicId,
            String postTitle,
            String commentContent
    ) {
        return new CommentCreatedNotificationDto(
                NotificationType.COMMENT_CREATED,
                userPublicId,
                postPublicId,
                postTitle,
                commentContent,
                LocalDateTime.now()
        );
    }
}
