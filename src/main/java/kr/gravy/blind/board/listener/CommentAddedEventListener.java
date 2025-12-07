package kr.gravy.blind.board.listener;

import kr.gravy.blind.board.event.CommentAddedEvent;
import kr.gravy.blind.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 댓글 추가 이벤트 리스너
 * - 트랜잭션 커밋 후 실행 (AFTER_COMMIT)
 * - SSE 브로드캐스트 알림 전송 (실시간 댓글 목록 업데이트)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentAddedEventListener {

    private final NotificationService notificationService;

    /**
     * 댓글 추가 이벤트 처리
     * - 트랜잭션 커밋 후에만 실행 (롤백 시 알림 방지)
     * - 해당 게시글을 보고 있는 모든 사용자에게 브로드캐스트
     *
     * @param event 댓글 추가 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentAdded(CommentAddedEvent event) {
        log.info("댓글 추가 이벤트 수신 - commentPublicId: {}, postPublicId: {}",
                event.commentPublicId(), event.postPublicId());

        // SSE 브로드캐스트 알림 전송
        notificationService.sendCommentAdded(
                event.postPublicId(),
                event.commentPublicId()
        );

        log.info("댓글 추가 브로드캐스트 알림 전송 완료 - commentPublicId: {}", event.commentPublicId());
    }
}
