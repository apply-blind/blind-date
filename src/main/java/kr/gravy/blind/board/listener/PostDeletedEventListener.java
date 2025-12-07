package kr.gravy.blind.board.listener;

import kr.gravy.blind.board.event.PostDeletedEvent;
import kr.gravy.blind.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * SSE 실시간 알림 전송 (모든 사용자에게 삭제 알림)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostDeletedEventListener {

    private final NotificationService notificationService;

    /**
     * @param event 게시글 삭제 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostDeleted(PostDeletedEvent event) {
        log.info("게시글 삭제 이벤트 수신 - postPublicId: {}, category: {}",
                event.postPublicId(), event.category());

        // SSE 브로드캐스트 알림 전송
        notificationService.sendPostDeleted(
                event.postPublicId(),
                event.category()
        );

        log.info("게시글 삭제 알림 전송 완료 - postPublicId: {}", event.postPublicId());
    }
}
