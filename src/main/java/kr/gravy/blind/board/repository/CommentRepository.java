package kr.gravy.blind.board.repository;

import kr.gravy.blind.board.entity.Comment;
import kr.gravy.blind.board.model.CommentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Optional<Comment> findByPublicId(UUID publicId);

    /**
     * @param postId   게시글 ID
     * @param pageable 페이징 정보
     * @return 댓글 Page (대댓글 포함)
     */
    @EntityGraph(attributePaths = {"replies"})
    @Query("SELECT c FROM Comment c WHERE c.postId = :postId AND c.parentCommentId IS NULL ORDER BY c.createdAt ASC")
    Page<Comment> findCommentsByPostIdWithReplies(@Param("postId") Long postId, Pageable pageable);

    /**
     * @param postId   게시글 ID
     * @param nickname 익명 닉네임
     * @param status   댓글 상태
     * @return userId (Optional)
     */
    @Query("SELECT c.userId FROM Comment c " +
            "WHERE c.postId = :postId AND c.anonymousNickname = :nickname " +
            "AND c.status = :status")
    Optional<Long> findUserIdByPostIdAndNickname(
            @Param("postId") Long postId,
            @Param("nickname") String nickname,
            @Param("status") CommentStatus status
    );

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :commentId")
    void incrementLikeCount(@Param("commentId") Long commentId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Comment c SET c.likeCount = GREATEST(c.likeCount - 1, 0) WHERE c.id = :commentId")
    void decrementLikeCount(@Param("commentId") Long commentId);
}
