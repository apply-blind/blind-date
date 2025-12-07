package kr.gravy.blind.board.event;

import java.util.UUID;

/**
 * 대댓글 생성 이벤트
 * - 댓글에 대댓글 작성 시 발행
 * - 멘션된 사용자에게 1:1 알림 전송
 */
public record ReplyCreatedEvent(
        UUID replyPublicId,
        UUID postPublicId,
        String postTitle,
        Long targetUserId,     // 알림 받을 사용자 (멘션된 사용자)
        String replyContent
) {
    /**
     * @param replyPublicId 생성된 대댓글 Public ID
     * @param postPublicId  게시글 Public ID
     * @param postTitle     게시글 제목
     * @param targetUserId  알림 받을 사용자 (멘션된 사용자)
     * @param replyContent  대댓글 내용
     * @return ReplyCreatedEvent
     */
    public static ReplyCreatedEvent of(
            UUID replyPublicId,
            UUID postPublicId,
            String postTitle,
            Long targetUserId,
            String replyContent
    ) {
        return new ReplyCreatedEvent(
                replyPublicId,
                postPublicId,
                postTitle,
                targetUserId,
                replyContent
        );
    }
}
