package kr.gravy.blind.board.event;

import java.util.UUID;

/**
 * 댓글 삭제 이벤트
 * - 댓글/대댓글 소프트 삭제 시 발행
 * - SSE 브로드캐스트 알림용 (실시간 댓글 마스킹)
 */
public record CommentDeletedEvent(
        UUID postPublicId,
        UUID commentPublicId
) {
    /**
     * @param postPublicId    게시글 Public ID
     * @param commentPublicId 삭제된 댓글 Public ID
     * @return CommentDeletedEvent 인스턴스
     */
    public static CommentDeletedEvent of(UUID postPublicId, UUID commentPublicId) {
        return new CommentDeletedEvent(postPublicId, commentPublicId);
    }
}
