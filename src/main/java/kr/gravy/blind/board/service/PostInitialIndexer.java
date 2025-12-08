package kr.gravy.blind.board.service;

import kr.gravy.blind.board.entity.Post;
import kr.gravy.blind.board.entity.PostDocument;
import kr.gravy.blind.board.model.PostStatus;
import kr.gravy.blind.board.repository.PostRepository;
import kr.gravy.blind.board.repository.PostSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Elasticsearch 초기 인덱싱 (기존 게시글 마이그레이션)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "post-indexing.initial-indexing.enabled",
        havingValue = "true"
)
public class PostInitialIndexer implements CommandLineRunner {

    private final PostRepository postRepository;
    private final PostSearchRepository postSearchRepository;
    private final PostImageService postImageService;

    /**
     * 애플리케이션 시작 시 실행
     * - ACTIVE 게시글만 인덱싱 (DELETED 제외)
     */
    @Override
    public void run(String... args) {
        log.info("=== Elasticsearch 초기 인덱싱 시작 ===");

        try {
            // 1. ACTIVE 게시글만 조회 (이미지 포함)
            List<Post> activePosts = postRepository.findAllByStatusWithImages(PostStatus.ACTIVE);

            if (activePosts.isEmpty()) {
                log.info("인덱싱할 게시글이 없습니다.");
                return;
            }

            log.info("인덱싱 대상 게시글 수: {}", activePosts.size());

            // 2. Post → PostDocument 변환 (이미지 URL 생성)
            List<PostDocument> documents = activePosts.stream()
                    .map(post -> {
                        String imageUrl = post.getFirstImage()
                                .map(image -> postImageService.getCdnImageUrl(
                                        image.getS3Key(),
                                        kr.gravy.blind.common.type.ImageSize.THUMBNAIL
                                ))
                                .orElse(null);
                        return PostDocument.from(post, imageUrl);
                    })
                    .toList();

            // 3. Elasticsearch bulk indexing
            postSearchRepository.saveAll(documents);

            log.info("=== Elasticsearch 초기 인덱싱 완료: {}건 ===", documents.size());

        } catch (Exception e) {
            log.error("Elasticsearch 초기 인덱싱 실패", e);
        }
    }
}
