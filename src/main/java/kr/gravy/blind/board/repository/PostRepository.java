package kr.gravy.blind.board.repository;

import kr.gravy.blind.board.entity.Post;
import kr.gravy.blind.board.model.PostCategory;
import kr.gravy.blind.board.model.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findByPublicId(UUID publicId);

    @EntityGraph(attributePaths = {"images"})
    Optional<Post> findWithImagesByPublicId(UUID publicId);

    @EntityGraph(attributePaths = {"images"})
    @Query("SELECT p FROM Post p WHERE p.status = :status")
    List<Post> findAllByStatusWithImages(@Param("status") PostStatus status);

    @EntityGraph(attributePaths = {"images"})
    @Query("SELECT p FROM Post p WHERE p.category = :category AND p.status IN :statuses ORDER BY p.isPinned DESC, p.createdAt DESC")
    Page<Post> findByCategoryAndStatusWithImages(
            @Param("category") PostCategory category,
            @Param("statuses") List<PostStatus> statuses,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"images"})
    @Query("SELECT p FROM Post p WHERE p.isHot = true AND p.status IN :statuses ORDER BY p.createdAt DESC")
    Page<Post> findByIsHotTrueAndStatusWithImages(
            @Param("statuses") List<PostStatus> statuses,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    void incrementViewCount(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
    void incrementCommentCount(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.commentCount = GREATEST(p.commentCount - 1, 0) WHERE p.id = :postId")
    void decrementCommentCount(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :postId")
    void incrementLikeCount(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.likeCount = GREATEST(p.likeCount - 1, 0) WHERE p.id = :postId")
    void decrementLikeCount(@Param("postId") Long postId);

    @EntityGraph(attributePaths = {"images"})
    @Query("SELECT p FROM Post p WHERE p.userId = :userId AND p.status IN :statuses ORDER BY p.createdAt DESC")
    Page<Post> findByUserIdWithImages(
            @Param("userId") Long userId,
            @Param("statuses") List<PostStatus> statuses,
            Pageable pageable
    );
}
