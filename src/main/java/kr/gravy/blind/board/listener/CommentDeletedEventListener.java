package kr.gravy.blind.board.listener;

import kr.gravy.blind.board.event.CommentDeletedEvent;
import kr.gravy.blind.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 댓글 삭제 이벤트 리스너
 * - CommentDeletedEvent 수신 시 실시간 알림 발송 (브로드캐스트)
 * - Redis Pub/Sub를 통해 모든 연결된 사용자에게 SSE 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentDeletedEventListener {

    private final NotificationService notificationService;

    /**
     * 댓글 삭제 이벤트 처리
     * - 게시글 상세 페이지를 보고 있는 모든 사용자에게 브로드캐스트
     * - 실시간 댓글 마스킹 업데이트용
     * - AFTER_COMMIT: 트랜잭션 커밋 성공 후에만 실행 (롤백 시 알림 전송 안 됨)
     *
     * @param event 댓글 삭제 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentDeleted(CommentDeletedEvent event) {
        log.info("댓글 삭제 이벤트 수신 - postPublicId: {}, commentPublicId: {}",
                event.postPublicId(), event.commentPublicId());

        // 브로드캐스트 알림 전송 (Redis Pub/Sub)
        notificationService.sendCommentDeleted(
                event.postPublicId(),
                event.commentPublicId()
        );

        log.info("댓글 삭제 알림 전송 완료 - commentPublicId: {}", event.commentPublicId());
    }
}
