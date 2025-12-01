package kr.gravy.blind.notification.listener;

import kr.gravy.blind.notification.service.NotificationService;
import kr.gravy.blind.user.event.ReviewStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 심사 상태 변경 이벤트 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewStatusEventListener {

    private final NotificationService notificationService;

    /**
     * 심사 상태 변경 이벤트 처리
     *
     * @param event 심사 상태 변경 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewStatusChanged(ReviewStatusChangedEvent event) {
        log.info("심사 상태 변경 이벤트 수신 - userPublicId: {}, status: {}",
                event.userPublicId(), event.newStatus());

        // 상태별 알림 전송
        switch (event.newStatus()) {
            case APPROVED -> {
                notificationService.sendReviewApproval(event.userPublicId());
                log.info("승인 알림 전송 완료 - userPublicId: {}", event.userPublicId());
            }
            case REJECTED -> {
                notificationService.sendReviewRejection(event.userPublicId(), event.reason());
                log.info("반려 알림 전송 완료 - userPublicId: {}, reason: {}",
                        event.userPublicId(), event.reason());
            }
            default -> log.warn("처리되지 않는 상태 - status: {}", event.newStatus());
        }
    }
}
