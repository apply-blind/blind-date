package kr.gravy.blind.notification.dto;

import kr.gravy.blind.notification.entity.Notification;
import kr.gravy.blind.notification.entity.ReviewRejectedNotification;
import kr.gravy.blind.notification.model.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public class GetNotificationsDto {

    /**
     * 알림 목록 조회 응답
     *
     * @param id              알림 ID
     * @param type            알림 타입
     * @param isRead          읽음 여부
     * @param postPublicId    게시글 Public ID (댓글 알림용 - 게시글 링크)
     * @param postTitle       게시글 제목 (댓글 알림용)
     * @param commentContent  댓글 내용 (댓글 알림용 - 미리보기)
     * @param commentPublicId 댓글 Public ID (중복 알림 방지 + 댓글 스크롤 이동용)
     * @param reason          반려 사유 (반려 알림용)
     * @param createdAt       알림 생성 시간
     */
    public record Response(
            Long id,
            NotificationType type,
            boolean isRead,
            UUID postPublicId,
            String postTitle,
            String commentContent,
            UUID commentPublicId,
            String reason,
            LocalDateTime createdAt
    ) {
        /**
         * @param notification Notification Entity
         * @return GetNotificationsDto.Response
         */
        public static Response from(Notification notification) {
            String reason = null;
            if (notification instanceof ReviewRejectedNotification rejected) {
                reason = rejected.getReason();
            }

            return new Response(
                    notification.getId(),
                    notification.getType(),
                    notification.isRead(),
                    notification.getPostPublicId(),
                    notification.getPostTitle(),
                    notification.getCommentContent(),
                    notification.getCommentPublicId(),
                    reason,
                    notification.getCreatedAt()
            );
        }
    }

    private GetNotificationsDto() {
    }
}
