package kr.gravy.blind.notification.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import kr.gravy.blind.notification.model.NotificationType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 댓글 생성 알림 엔티티
 * - 게시글에 댓글 작성 시 게시글 작성자에게 알림
 */
@Entity
@DiscriminatorValue("COMMENT_CREATED")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentCreatedNotification extends Notification {

    @Override
    public NotificationType getType() {
        return NotificationType.COMMENT_CREATED;
    }

    /**
     * @param userPublicId    알림 받을 사용자 Public ID (게시글 작성자)
     * @param commentPublicId 댓글 Public ID (중복 방지용)
     * @param postPublicId    게시글 Public ID
     * @param postTitle       게시글 제목
     * @param commentContent  댓글 내용 미리보기
     * @return CommentCreatedNotification
     */
    public static CommentCreatedNotification create(
            UUID userPublicId,
            UUID commentPublicId,
            UUID postPublicId,
            String postTitle,
            String commentContent
    ) {
        CommentCreatedNotification notification = new CommentCreatedNotification();
        notification.userPublicId = userPublicId;
        notification.isRead = false;
        notification.commentPublicId = commentPublicId;
        notification.postPublicId = postPublicId;
        notification.postTitle = postTitle;
        notification.commentContent = commentContent;
        return notification;
    }
}
