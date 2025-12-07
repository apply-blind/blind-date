package kr.gravy.blind.notification.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import kr.gravy.blind.notification.model.NotificationType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 대댓글 생성 알림 엔티티
 * - 댓글에 대댓글 작성 시 멘션된 사용자에게 알림
 */
@Entity
@DiscriminatorValue("REPLY_CREATED")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReplyCreatedNotification extends Notification {

    @Override
    public NotificationType getType() {
        return NotificationType.REPLY_CREATED;
    }

    /**
     * @param userPublicId  알림 받을 사용자 Public ID (멘션된 사용자)
     * @param replyPublicId 대댓글 Public ID (중복 방지용)
     * @param postPublicId  게시글 Public ID
     * @param postTitle     게시글 제목
     * @param replyContent  대댓글 내용 미리보기
     * @return ReplyCreatedNotification
     */
    public static ReplyCreatedNotification create(
            UUID userPublicId,
            UUID replyPublicId,
            UUID postPublicId,
            String postTitle,
            String replyContent
    ) {
        ReplyCreatedNotification notification = new ReplyCreatedNotification();
        notification.userPublicId = userPublicId;
        notification.isRead = false;
        notification.commentPublicId = replyPublicId;
        notification.postPublicId = postPublicId;
        notification.postTitle = postTitle;
        notification.commentContent = replyContent;
        return notification;
    }
}
