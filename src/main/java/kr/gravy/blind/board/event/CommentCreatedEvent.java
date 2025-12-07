package kr.gravy.blind.board.event;

import java.util.UUID;

/**
 * 댓글 생성 이벤트
 * - 게시글에 댓글 작성 시 발행
 * - 게시글 작성자에게 1:1 알림 전송용
 */
public record CommentCreatedEvent(
        UUID commentPublicId,
        UUID postPublicId,
        String postTitle,
        Long targetUserId,     // 알림 받을 사용자 (게시글 작성자)
        String commentContent
) {
    /**
     * @param commentPublicId 생성된 댓글 Public ID
     * @param postPublicId    게시글 Public ID
     * @param postTitle       게시글 제목
     * @param targetUserId    알림 받을 사용자 (게시글 작성자)
     * @param commentContent  댓글 내용
     * @return CommentCreatedEvent
     */
    public static CommentCreatedEvent of(
            UUID commentPublicId,
            UUID postPublicId,
            String postTitle,
            Long targetUserId,
            String commentContent
    ) {
        return new CommentCreatedEvent(
                commentPublicId,
                postPublicId,
                postTitle,
                targetUserId,
                commentContent
        );
    }
}
