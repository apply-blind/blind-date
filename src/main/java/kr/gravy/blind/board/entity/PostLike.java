package kr.gravy.blind.board.entity;

import jakarta.persistence.*;
import kr.gravy.blind.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글 공감(좋아요) 엔티티
 * - 사용자당 게시글당 1개만 가능 (복합 UNIQUE 제약)
 * - 토글 방식: 존재하면 삭제, 없으면 생성
 */
@Entity
@Table(
        name = "anonymous_post_likes",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "post_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    /**
     * @param userId 사용자 ID
     * @param postId 게시글 ID
     * @return PostLike 엔티티
     */
    public static PostLike create(Long userId, Long postId) {
        PostLike like = new PostLike();
        like.userId = userId;
        like.postId = postId;
        return like;
    }
}
