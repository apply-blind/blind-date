package kr.gravy.blind.board.entity;

import jakarta.persistence.*;
import kr.gravy.blind.board.model.PostCategory;
import kr.gravy.blind.board.model.PostStatus;
import kr.gravy.blind.common.BaseEntity;
import kr.gravy.blind.common.utils.GeneratorUtil;
import kr.gravy.blind.user.model.Gender;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "anonymous_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID publicId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_gender", nullable = false, length = 10)
    private Gender authorGender;

    @Column(name = "anonymous_nickname", nullable = false, length = 50)
    private String anonymousNickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostCategory category;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount;

    @Column(name = "comment_count", nullable = false)
    private Integer commentCount;

    @Column(name = "is_pinned", nullable = false)
    private Boolean isPinned;

    @Column(name = "is_hot", nullable = false)
    private Boolean isHot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    private List<PostImage> images = new ArrayList<>();

    /**
     * @param userId       작성자 ID
     * @param authorGender 작성자 성별
     * @param category     카테고리
     * @param title        제목
     * @param content      내용
     * @return Post 엔티티
     */
    public static Post create(Long userId, Gender authorGender, PostCategory category, String title, String content) {
        Post post = new Post();
        post.publicId = GeneratorUtil.generatePublicId();
        post.userId = userId;
        post.authorGender = authorGender;
        post.anonymousNickname = GeneratorUtil.generateRandomNickname();
        post.category = category;
        post.title = title;
        post.content = content;
        post.viewCount = 0;
        post.likeCount = 0;
        post.commentCount = 0;
        post.isPinned = false;
        post.isHot = false;
        post.status = PostStatus.ACTIVE;
        return post;
    }

    /**
     * 게시글 SoftDelete
     */
    public void delete() {
        this.status = PostStatus.DELETED;
    }

    /**
     * 첫 번째 이미지 조회 (@OneToOne 보다 효율적이라 판단)
     * - 게시글은 이미지 1개만 첨부 가능
     *
     * @return 이미지 (Optional)
     */
    public Optional<PostImage> getFirstImage() {
        return images.isEmpty() ? Optional.empty() : Optional.of(images.get(0));
    }

    public boolean isDeleted() {
        return this.status == PostStatus.DELETED;
    }

    /**
     * 표시용 제목 반환
     * - 삭제된 게시글: "삭제된 게시글입니다"
     * - 활성 게시글: 원본 제목
     *
     * @return 사용자에게 표시할 제목
     */
    public String getDisplayTitle() {
        return isDeleted() ? "삭제된 게시글입니다" : this.title;
    }

    /**
     * 표시용 내용 반환
     * - 삭제된 게시글: "삭제된 게시글입니다"
     * - 활성 게시글: 원본 내용
     *
     * @return 사용자에게 표시할 내용
     */
    public String getDisplayContent() {
        return isDeleted() ? "삭제된 게시글입니다" : this.content;
    }

    public boolean isAuthor(Long userId) {
        return this.userId.equals(userId);
    }
}
