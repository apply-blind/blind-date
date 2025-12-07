package kr.gravy.blind.board.entity;

import jakarta.persistence.*;
import kr.gravy.blind.board.model.ImageUploadStatus;
import kr.gravy.blind.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글 이미지 엔티티
 * - 게시글당 1개 이미지만 첨부 가능
 * - 최대 5MB 제한 (애플리케이션 레벨에서 검증)
 * - S3 저장 (Presigned URL 직접 업로드 방식)
 * - User 도메인과 동일한 패턴: s3Key만 저장, 필요 시 Presigned URL 생성
 */
@Entity
@Table(name = "anonymous_post_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ImageUploadStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    private Post post;

    /**
     * @param postId      게시글 ID
     * @param s3Key       S3 키
     * @param contentType MIME 타입
     * @return PostImage 엔티티
     */
    public static PostImage create(Long postId, String s3Key, String contentType) {
        PostImage image = new PostImage();
        image.postId = postId;
        image.s3Key = s3Key;
        image.contentType = contentType;
        image.status = ImageUploadStatus.NOT_UPLOADED;
        return image;
    }

    /**
     * @param status 업로드 상태
     */
    public void updateStatus(ImageUploadStatus status) {
        this.status = status;
    }
}
