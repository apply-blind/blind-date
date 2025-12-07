package kr.gravy.blind.notification.model;

import lombok.Getter;


@Getter
public enum NotificationType {
    REVIEW_APPROVED("프로필 승인"),
    REVIEW_REJECTED("프로필 반려"),
    POST_CREATED("새 게시글 알림"),       // 실시간 게시글 알림 (브로드캐스트)
    POST_DELETED("게시글 삭제 알림"),     // 실시간 게시글 삭제 알림 (브로드캐스트)
    COMMENT_CREATED("댓글 알림"),       // 게시글에 댓글 작성 (1:1 알림)
    REPLY_CREATED("답글 알림"),         // 댓글에 대댓글 작성 (1:1 알림)
    COMMENT_ADDED("댓글 추가 알림"),     // 실시간 댓글 추가 (브로드캐스트)
    COMMENT_DELETED("댓글 삭제 알림");   // 실시간 댓글 삭제 (브로드캐스트)

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

}
