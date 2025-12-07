package kr.gravy.blind.board.repository;

import kr.gravy.blind.board.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    /**
     * @param userId    사용자 ID
     * @param commentId 댓글 ID
     * @return 좋아요 존재 여부
     */
    boolean existsByUserIdAndCommentId(Long userId, Long commentId);

    /**
     * @param userId    사용자 ID
     * @param commentId 댓글 ID
     */
    void deleteByUserIdAndCommentId(Long userId, Long commentId);

    @Query("SELECT cl.commentId FROM CommentLike cl WHERE cl.userId = :userId AND cl.commentId IN :commentIds")
    Set<Long> findLikedCommentIdsByUserIdAndCommentIds(@Param("userId") Long userId, @Param("commentIds") Set<Long> commentIds);
}
