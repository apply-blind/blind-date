package kr.gravy.blind.board.entity;

import jakarta.persistence.*;
import kr.gravy.blind.board.model.CommentStatus;
import kr.gravy.blind.common.BaseEntity;
import kr.gravy.blind.common.utils.GeneratorUtil;
import kr.gravy.blind.user.model.Gender;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * - Self-Referencing FK로 댓글/대댓글 구조 표현
 * - parentCommentId가 NULL이면 최상위 댓글, NOT NULL이면 대댓글
 * - depth 1만 허용 (대댓글의 대댓글 불가)
 */
@Entity
@Table(name = "anonymous_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID publicId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_gender", nullable = false, length = 10)
    private Gender authorGender;

    @Column(name = "anonymous_nickname", nullable = false, length = 50)
    private String anonymousNickname;

    @Column(name = "parent_comment_id")  // NULL: 댓글, NOT NULL: 대댓글
    private Long parentCommentId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommentStatus status;

    // 연관관계 (cascade 없음, LAZY 로딩)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id", insertable = false, updatable = false)
    private Comment parentComment;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id", insertable = false, updatable = false)
    private List<Comment> replies = new ArrayList<>();

    /**
     * @param postId       게시글 ID
     * @param userId       작성자 ID
     * @param authorGender 작성자 성별
     * @param nickname     익명 닉네임
     * @param content      댓글 내용
     * @return Comment 엔티티
     */
    public static Comment createComment(Long postId, Long userId, Gender authorGender, String nickname, String content) {
        Comment comment = new Comment();
        comment.publicId = GeneratorUtil.generatePublicId();
        comment.postId = postId;
        comment.userId = userId;
        comment.authorGender = authorGender;
        comment.anonymousNickname = nickname;
        comment.parentCommentId = null;  // 최상위 댓글
        comment.content = content;
        comment.likeCount = 0;
        comment.status = CommentStatus.ACTIVE;
        return comment;
    }

    /**
     * @param postId          게시글 ID
     * @param parentCommentId 부모 댓글 ID
     * @param userId          작성자 ID
     * @param authorGender    작성자 성별
     * @param nickname        익명 닉네임
     * @param content         대댓글 내용
     * @return Comment 엔티티 (대댓글)
     */
    public static Comment createReply(Long postId, Long parentCommentId, Long userId, Gender authorGender, String nickname, String content) {
        Comment reply = new Comment();
        reply.publicId = GeneratorUtil.generatePublicId();
        reply.postId = postId;
        reply.userId = userId;
        reply.authorGender = authorGender;
        reply.anonymousNickname = nickname;
        reply.parentCommentId = parentCommentId;  // 대댓글
        reply.content = content;
        reply.likeCount = 0;
        reply.status = CommentStatus.ACTIVE;
        return reply;
    }

    /**
     * 댓글 SoftDelete
     */
    public void delete() {
        this.status = CommentStatus.DELETED;
    }

    public boolean isDeleted() {
        return this.status == CommentStatus.DELETED;
    }

    public boolean isReply() {
        return this.parentCommentId != null;
    }

    /**
     * 표시용 내용 반환
     * - 삭제된 댓글: "삭제된 댓글입니다"
     * - 활성 댓글: 원본 내용
     *
     * @return 사용자에게 표시할 내용
     */
    public String getDisplayContent() {
        return isDeleted() ? "삭제된 댓글입니다" : this.content;
    }
}
