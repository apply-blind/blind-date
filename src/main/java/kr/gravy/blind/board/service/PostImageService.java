package kr.gravy.blind.board.service;

import kr.gravy.blind.board.dto.CreatePostDto;
import kr.gravy.blind.board.entity.Post;
import kr.gravy.blind.board.entity.PostImage;
import kr.gravy.blind.board.model.ImageUploadStatus;
import kr.gravy.blind.board.repository.PostImageRepository;
import kr.gravy.blind.common.exception.BlindException;
import kr.gravy.blind.common.type.ImageContentType;
import kr.gravy.blind.infrastructure.aws.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostImageService {

    private final PostImageRepository postImageRepository;
    private final S3Service s3Service;

    /**
     * @param post        Post 엔티티
     * @param filename    파일명
     * @param contentType MIME 타입
     * @return Presigned URL 정보 (DTO 내부 record)
     */
    public CreatePostDto.PresignedUrlInfo createPresignedUrl(Post post, String filename, ImageContentType contentType) {
        // 1. S3 Key 생성
        String s3Key = s3Service.generateS3Key(filename, contentType.getMimeType());

        // 2. PostImage 엔티티 생성
        PostImage postImage = PostImage.create(post.getId(), s3Key, contentType.getMimeType());
        postImageRepository.save(postImage);

        // 3. Presigned URL 발급
        String presignedUrl = s3Service.generatePutPresignedUrl(s3Key, contentType.getMimeType());

        log.info("게시글 이미지 Presigned URL 발급 - postId: {}, s3Key: {}", post.getId(), s3Key);

        return CreatePostDto.PresignedUrlInfo.of(presignedUrl, s3Key);
    }

    /**
     * S3 이미지 업로드 검증 및 완료 처리
     * 클라이언트가 S3 업로드 완료 후 호출
     * NOT_UPLOADED → UPLOADED 상태 변경
     *
     * @param postId 게시글 ID
     */
    public void verifyAndCompleteUpload(Long postId) {
        PostImage postImage = postImageRepository.findByPostId(postId)
                .orElseThrow(() -> new BlindException(
                        String.format("이미지가 첨부되지 않은 게시글입니다 - postId: %d", postId)
                ));

        if (!s3Service.exists(postImage.getS3Key())) {
            throw new BlindException(
                    String.format("S3 업로드 미완료 - s3Key: %s", postImage.getS3Key())
            );
        }

        // 정상 플로우: 상태 업데이트 (NOT_UPLOADED → UPLOADED)
        postImage.updateStatus(ImageUploadStatus.UPLOADED);

        log.info("게시글 이미지 업로드 완료 - postId: {}, s3Key: {}", postId, postImage.getS3Key());
    }

    /**
     * S3 Key로 Presigned URL 생성 (다운로드용)
     * User 도메인과 동일한 패턴
     *
     * @param s3Key S3 객체 키
     * @return Presigned URL (24시간 유효)
     * @deprecated CloudFront CDN URL 사용 권장 (getCdnImageUrl)
     */
    @Deprecated(since = "2025-12-07")
    public String generatePresignedUrl(String s3Key) {
        return s3Service.generatePresignedUrl(s3Key);
    }

    /**
     * S3 Key로 CloudFront CDN URL 생성
     * URL 만료 없이 영구 사용 가능
     *
     * @param s3Key     S3 객체 키
     * @param imageSize 이미지 크기 (THUMBNAIL/MEDIUM/FULL)
     * @return CloudFront CDN URL (예: https://d3rxrj4h11j0bv.cloudfront.net/{s3Key}?width=200)
     */
    public String getCdnImageUrl(String s3Key, kr.gravy.blind.common.type.ImageSize imageSize) {
        return s3Service.getCdnImageUrl(s3Key, imageSize);
    }
}
