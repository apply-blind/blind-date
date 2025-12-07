package kr.gravy.blind.board.event;

import java.util.UUID;

/**
 * 댓글 추가 이벤트
 * - 댓글/대댓글 작성 시 발행
 * - SSE 브로드캐스트 알림용 (실시간 댓글 목록 업데이트)
 */
public record CommentAddedEvent(
        UUID postPublicId,
        UUID commentPublicId
) {
    /**
     * @param postPublicId    게시글 Public ID
     * @param commentPublicId 댓글 Public ID
     * @return CommentAddedEvent
     */
    public static CommentAddedEvent of(
            UUID postPublicId,
            UUID commentPublicId
    ) {
        return new CommentAddedEvent(
                postPublicId,
                commentPublicId
        );
    }
}
