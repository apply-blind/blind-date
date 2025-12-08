package kr.gravy.blind.board.service;

import kr.gravy.blind.board.entity.Post;
import kr.gravy.blind.board.entity.PostDocument;
import kr.gravy.blind.board.event.PostIndexingMessage;
import kr.gravy.blind.board.repository.PostRepository;
import kr.gravy.blind.board.repository.PostSearchRepository;
import kr.gravy.blind.common.exception.BlindException;
import kr.gravy.blind.infrastructure.kafka.KafkaConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static kr.gravy.blind.common.exception.Status.POST_NOT_FOUND;


@Slf4j
@Service
@RequiredArgsConstructor
public class PostIndexingService {

    private final PostRepository postRepository;
    private final PostSearchRepository postSearchRepository;
    private final PostImageService postImageService;

    @KafkaListener(
            topics = KafkaConstants.POST_INDEXING_TOPIC,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePostIndexing(PostIndexingMessage message) {
        log.info("Kafka 메시지 수신: publicId={}, operation={}",
                message.publicId(), message.operation());

        try {
            switch (message.operation()) {
                case INDEX -> indexPost(message.publicId());
                case DELETE -> deletePost(message.publicId());
            }
        } catch (Exception e) {
            log.error("Elasticsearch 인덱싱 실패: publicId={}, operation={}, error={}",
                    message.publicId(), message.operation(), e.getMessage(), e);
            throw e;
        }
    }

    private void indexPost(UUID publicId) {
        Post post = postRepository.findWithImagesByPublicId(publicId)
                .orElseThrow(() -> new BlindException(POST_NOT_FOUND));

        String imageUrl = post.getFirstImage()
                .map(image -> postImageService.getCdnImageUrl(
                        image.getS3Key(),
                        kr.gravy.blind.common.type.ImageSize.THUMBNAIL
                ))
                .orElse(null);

        PostDocument document = PostDocument.from(post, imageUrl);
        postSearchRepository.save(document);

        log.info("Elasticsearch 인덱싱 완료: publicId={}, title={}, imageUrl={}",
                publicId, post.getTitle(), imageUrl != null);
    }

    private void deletePost(UUID publicId) {
        String documentId = publicId.toString();
        postSearchRepository.deleteById(documentId);

        log.info("Elasticsearch 문서 삭제 완료: publicId={}", publicId);
    }
}
