package kr.gravy.blind.board.event;

import kr.gravy.blind.board.entity.Post;
import kr.gravy.blind.board.model.PostCategory;

import java.util.UUID;

/**
 * 게시글 생성 이벤트
 * - 새 게시글 작성 시 발행
 * - SSE 브로드캐스트 알림용
 */
public record PostCreatedEvent(
        UUID postPublicId,
        PostCategory category,
        String title
) {
    /**
     * @param post 생성된 게시글 엔티티
     * @return PostCreatedEvent
     */
    public static PostCreatedEvent of(Post post) {
        return new PostCreatedEvent(
                post.getPublicId(),
                post.getCategory(),
                post.getTitle()
        );
    }
}
