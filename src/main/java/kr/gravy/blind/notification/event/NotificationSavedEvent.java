package kr.gravy.blind.notification.event;

import kr.gravy.blind.notification.model.NotificationType;

import java.util.UUID;

public record NotificationSavedEvent(
        NotificationType type,
        UUID userPublicId,
        UUID postPublicId,
        String postTitle,
        String content
) {
    /**
     * 댓글 생성 알림 저장 완료 이벤트 생성
     *
     * @param userPublicId 알림 받을 사용자 Public ID
     * @param postPublicId 게시글 Public ID
     * @param postTitle    게시글 제목
     * @param content      댓글 내용
     * @return NotificationSavedEvent
     */
    public static NotificationSavedEvent commentCreated(
            UUID userPublicId,
            UUID postPublicId,
            String postTitle,
            String content
    ) {
        return new NotificationSavedEvent(
                NotificationType.COMMENT_CREATED,
                userPublicId,
                postPublicId,
                postTitle,
                content
        );
    }

    /**
     * 대댓글 생성 알림 저장 완료 이벤트 생성
     *
     * @param userPublicId 알림 받을 사용자 Public ID
     * @param postPublicId 게시글 Public ID
     * @param postTitle    게시글 제목
     * @param content      대댓글 내용
     * @return NotificationSavedEvent
     */
    public static NotificationSavedEvent replyCreated(
            UUID userPublicId,
            UUID postPublicId,
            String postTitle,
            String content
    ) {
        return new NotificationSavedEvent(
                NotificationType.REPLY_CREATED,
                userPublicId,
                postPublicId,
                postTitle,
                content
        );
    }
}
