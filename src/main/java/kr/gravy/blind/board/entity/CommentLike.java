package kr.gravy.blind.board.entity;

import jakarta.persistence.*;
import kr.gravy.blind.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 익명 게시판 댓글 좋아요 엔티티
 * - 댓글/대댓글의 좋아요 가능
 * - UNIQUE 제약: 한 사용자는 한 댓글에 1번만 좋아요 가능
 */
@Entity
@Table(
        name = "anonymous_comment_likes",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "comment_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    /**
     * @param userId    사용자 ID
     * @param commentId 댓글 ID
     * @return CommentLike 엔티티
     */
    public static CommentLike create(Long userId, Long commentId) {
        CommentLike like = new CommentLike();
        like.userId = userId;
        like.commentId = commentId;
        return like;
    }
}
