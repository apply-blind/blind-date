package kr.gravy.blind.board.listener;

import kr.gravy.blind.board.event.PostCreatedEvent;
import kr.gravy.blind.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostCreatedEventListener {

    private final NotificationService notificationService;

    /**
     * @param event 게시글 생성 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostCreated(PostCreatedEvent event) {
        log.info("게시글 생성 이벤트 수신 - postPublicId: {}, category: {}",
                event.postPublicId(), event.category());

        // SSE 브로드캐스트 알림 전송
        notificationService.sendPostCreated(
                event.postPublicId(),
                event.category(),
                event.title()
        );

        log.info("게시글 생성 알림 전송 완료 - postPublicId: {}", event.postPublicId());
    }
}
