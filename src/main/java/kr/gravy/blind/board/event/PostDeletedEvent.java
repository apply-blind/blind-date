package kr.gravy.blind.board.event;

import kr.gravy.blind.board.model.PostCategory;

import java.util.UUID;

/**
 * - 게시글 SoftDelete 시 발행
 * - SSE 브로드캐스트 알림용 (실시간 목록 갱신)
 */
public record PostDeletedEvent(
        UUID postPublicId,
        PostCategory category
) {
    /**
     * @param postPublicId 삭제된 게시글 Public ID
     * @param category     게시글 카테고리
     * @return PostDeletedEvent
     */
    public static PostDeletedEvent of(UUID postPublicId, PostCategory category) {
        return new PostDeletedEvent(postPublicId, category);
    }
}
