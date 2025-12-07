package kr.gravy.blind.board.listener;

import kr.gravy.blind.board.event.ReplyCreatedEvent;
import kr.gravy.blind.notification.event.NotificationSavedEvent;
import kr.gravy.blind.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReplyCreatedEventListener {

    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * @param event 대댓글 생성 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReplyCreated(ReplyCreatedEvent event) {
        log.info("대댓글 생성 이벤트 수신 - replyPublicId: {}, postPublicId: {}, targetUserId: {}",
                event.replyPublicId(), event.postPublicId(), event.targetUserId());

        UUID userPublicId = notificationService.saveReplyNotification(
                event.replyPublicId(),
                event.postPublicId(),
                event.postTitle(),
                event.targetUserId(),
                event.replyContent()
        );

        // 중복 알림이 아닌 경우에만 NotificationSavedEvent 발행
        if (userPublicId != null) {
            eventPublisher.publishEvent(
                    NotificationSavedEvent.replyCreated(
                            userPublicId,
                            event.postPublicId(),
                            event.postTitle(),
                            event.replyContent()
                    )
            );
            log.info("대댓글 생성 알림 DB 저장 완료 - replyPublicId: {}", event.replyPublicId());
        }
    }
}
