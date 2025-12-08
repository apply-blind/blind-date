package kr.gravy.blind.board.listener;

import kr.gravy.blind.board.entity.Post;
import kr.gravy.blind.board.event.PostCreatedEvent;
import kr.gravy.blind.board.event.PostDeletedEvent;
import kr.gravy.blind.board.event.PostIndexingMessage;
import kr.gravy.blind.board.repository.PostRepository;
import kr.gravy.blind.board.service.PostImageService;
import kr.gravy.blind.common.exception.BlindException;
import kr.gravy.blind.common.type.ImageSize;
import kr.gravy.blind.infrastructure.kafka.KafkaConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static kr.gravy.blind.common.exception.Status.POST_NOT_FOUND;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostIndexingEventListener {

    private final PostRepository postRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PostImageService postImageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostCreated(PostCreatedEvent event) {
        log.info("게시글 생성 이벤트 수신: publicId={}", event.postPublicId());

        Post post = postRepository.findWithImagesByPublicId(event.postPublicId())
                .orElseThrow(() -> new BlindException(POST_NOT_FOUND));

        String imageUrl = post.getFirstImage()
                .map(image -> postImageService.getCdnImageUrl(
                        image.getS3Key(),
                        ImageSize.THUMBNAIL
                ))
                .orElse(null);

        // Kafka로 인덱싱 메시지 발송
        PostIndexingMessage message = PostIndexingMessage.forIndexing(
                post.getPublicId(),
                post.getTitle(),
                post.getContent(),
                post.getCategory(),
                imageUrl
        );
        kafkaTemplate.send(
                KafkaConstants.POST_INDEXING_TOPIC,
                event.postPublicId().toString(),  // Key: publicId (파티션 할당 기준)
                message
        );

        log.info("Kafka 메시지 발송 완료: topic={}, key={}, operation=INDEX, imageUrl={}",
                KafkaConstants.POST_INDEXING_TOPIC, event.postPublicId(), imageUrl != null);
    }


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostDeleted(PostDeletedEvent event) {
        log.info("게시글 삭제 이벤트 수신: publicId={}", event.postPublicId());

        // Kafka로 삭제 메시지 발송
        PostIndexingMessage message = PostIndexingMessage.forDeletion(event.postPublicId());
        kafkaTemplate.send(
                KafkaConstants.POST_INDEXING_TOPIC,
                event.postPublicId().toString(),
                message
        );

        log.info("Kafka 메시지 발송 완료: topic={}, key={}, operation=DELETE",
                KafkaConstants.POST_INDEXING_TOPIC, event.postPublicId());
    }
}
