package kr.gravy.blind.infrastructure.aws.event;

import kr.gravy.blind.infrastructure.aws.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * S3 관련 이벤트 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class S3EventListener {

    private final S3Service s3Service;

    /**
     * S3 파일 삭제 이벤트 처리
     * 에러 처리:
     * - S3 삭제 실패는 로그만 남기고 진행 (고아 파일 발생 가능)
     * - 추후 배치 작업으로 cleanup 필요
     *
     * @param event S3 삭제 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleS3Delete(S3DeleteEvent event) {
        log.info("S3 삭제 이벤트 수신 - source: {}, 파일 개수: {}",
                event.getSource(), event.getFileCount());

        if (event.getS3Keys().isEmpty()) {
            log.debug("삭제할 S3 파일이 없습니다.");
            return;
        }

        try {
            s3Service.deleteMultipleFiles(event.getS3Keys());

            log.info("S3 삭제 이벤트 처리 완료 - source: {}, 성공 개수: {}",
                    event.getSource(), event.getFileCount());
        } catch (Exception e) {
            log.error("S3 삭제 이벤트 처리 중 에러 - source: {}, 에러: {}",
                    event.getSource(), e.getMessage(), e);
        }
    }
}
