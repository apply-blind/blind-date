package kr.gravy.blind.board.listener;

import kr.gravy.blind.board.event.CommentCreatedEvent;
import kr.gravy.blind.notification.event.NotificationSavedEvent;
import kr.gravy.blind.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * 댓글 생성 이벤트 리스너
 * - 게시글 작성자에게 1:1 알림 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentCreatedEventListener {

    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 댓글 생성 이벤트 처리
     * - 트랜잭션 커밋 후에만 실행 (롤백 시 알림 방지)
     *
     * @param event 댓글 생성 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentCreated(CommentCreatedEvent event) {
        log.info("댓글 생성 이벤트 수신 - commentPublicId: {}, postPublicId: {}, targetUserId: {}",
                event.commentPublicId(), event.postPublicId(), event.targetUserId());

        UUID userPublicId = notificationService.saveCommentNotification(
                event.commentPublicId(),
                event.postPublicId(),
                event.postTitle(),
                event.targetUserId(),
                event.commentContent()
        );

        if (userPublicId != null) {
            eventPublisher.publishEvent(
                    NotificationSavedEvent.commentCreated(
                            userPublicId,
                            event.postPublicId(),
                            event.postTitle(),
                            event.commentContent()
                    )
            );
            log.info("댓글 생성 알림 DB 저장 완료 - commentPublicId: {}", event.commentPublicId());
        }
    }
}
