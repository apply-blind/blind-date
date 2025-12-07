package kr.gravy.blind.notification.service;

import kr.gravy.blind.notification.dto.NotificationDto;
import kr.gravy.blind.notification.model.SseConstants;
import kr.gravy.blind.notification.model.SseEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE Emitter 관리 서비스
 * 실시간 알림을 위한 SSE 연결 관리 및 Heartbeat
 * <p>
 * 단일 세션 정책: 새 로그인 시 기존 연결 강제 종료
 */
@Slf4j
@Service
public class SseEmitterService {

    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID userPublicId) {
        SseEmitter newEmitter = emitters.compute(userPublicId, (key, oldEmitter) -> {
            if (oldEmitter != null) {
                try {
                    oldEmitter.complete();
                } catch (IllegalStateException e) {
                }
                log.info("기존 SSE 연결 종료 - userPublicId: {}", key);
            }
            return new SseEmitter(SseConstants.DEFAULT_TIMEOUT);
        });

        newEmitter.onCompletion(() -> removeEmitterSafely(userPublicId, "정상 종료"));
        newEmitter.onTimeout(() -> removeEmitterSafely(userPublicId, "타임아웃"));
        newEmitter.onError(e -> {
            log.error("SSE 연결 오류 - userPublicId: {}", userPublicId, e);
            removeEmitterSafely(userPublicId, "오류 발생");
        });

        log.info("SSE 연결 - userPublicId: {}, 활성 연결 수: {}", userPublicId, emitters.size());

        // Effective Java Item 79: alien method는 compute() 밖에서 호출
        try {
            newEmitter.send(SseEmitter.event()
                    .name(SseEventType.CONNECTED.getValue())
                    .data("SSE connection established")
                    .build());
        } catch (IOException | IllegalStateException e) {
            log.warn("SSE 초기 이벤트 전송 실패 - userPublicId: {}, error: {}",
                    userPublicId, e.getClass().getSimpleName());
        }

        return newEmitter;
    }

    /**
     * 브로드캐스트 알림 전송 (모든 연결된 사용자에게)
     */
    public void broadcast(NotificationDto notification) {
        if (emitters.isEmpty()) {
            log.debug("SSE 연결 없음, 브로드캐스트 전송 스킵 - type: {}", notification.type());
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (Map.Entry<UUID, SseEmitter> entry : emitters.entrySet()) {
            UUID userPublicId = entry.getKey();
            SseEmitter emitter = entry.getValue();

            try {
                emitter.send(SseEmitter.event()
                        .name(SseEventType.NOTIFICATION.getValue())
                        .data(notification));
                successCount++;
            } catch (IOException e) {
                log.warn("SSE 브로드캐스트 실패 (네트워크 오류) - userPublicId: {}, type: {}",
                        userPublicId, notification.type());
                removeEmitterSafely(userPublicId, "브로드캐스트 실패 (네트워크 오류)");
                failCount++;
            } catch (IllegalStateException e) {
                log.debug("SSE 브로드캐스트 실패 (이미 종료됨) - userPublicId: {}", userPublicId);
                removeEmitterSafely(userPublicId, "브로드캐스트 실패 (이미 종료됨)");
                failCount++;
            }
        }

        log.info("SSE 브로드캐스트 완료 - type: {}, 성공: {}, 실패: {}",
                notification.type(), successCount, failCount);
    }

    /**
     * 알림 전송
     */
    public void send(UUID userPublicId, NotificationDto notification) {
        SseEmitter emitter = emitters.get(userPublicId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(SseEventType.NOTIFICATION.getValue())
                        .data(notification));
                log.info("SSE 알림 전송 완료 - userPublicId: {}, type: {}", userPublicId, notification.type());
            } catch (IOException e) {
                log.warn("SSE 알림 전송 실패 (네트워크 오류) - userPublicId: {}, type: {}",
                        userPublicId, notification.type());
                removeEmitterSafely(userPublicId, "전송 실패 (네트워크 오류)");
            } catch (IllegalStateException e) {
                log.debug("SSE 알림 전송 실패 (이미 종료됨) - userPublicId: {}", userPublicId);
                removeEmitterSafely(userPublicId, "전송 실패 (이미 종료됨)");
            }
        } else {
            log.debug("SSE 연결 없음, 알림 전송 실패 - userPublicId: {}, type: {}", userPublicId, notification.type());
        }
    }

    /**
     * Heartbeat (15초마다)
     */
    @Scheduled(fixedRate = SseConstants.HEARTBEAT_INTERVAL_MILLIS)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) {
            return;
        }

        var failedConnections = emitters.entrySet().stream()
                .filter(entry -> !sendHeartbeatToEmitter(entry.getKey(), entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();

        failedConnections.forEach(userPublicId -> removeEmitterSafely(userPublicId, "Heartbeat 실패"));

        if (!failedConnections.isEmpty()) {
            log.debug("Heartbeat 전송 완료 - 활성 연결: {}, 제거: {}", emitters.size(), failedConnections.size());
        }
    }

    private boolean sendHeartbeatToEmitter(UUID userPublicId, SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .name(SseEventType.HEARTBEAT.getValue())
                    .data("ping")
                    .build());
            return true;
        } catch (IOException e) {
            log.warn("Heartbeat 실패 (네트워크 오류) - userPublicId: {}", userPublicId);
            removeEmitterSafely(userPublicId, "Heartbeat 실패 (네트워크 오류)");
            return false;
        } catch (IllegalStateException e) {
            log.debug("Heartbeat 실패 (이미 종료됨) - userPublicId: {}", userPublicId);
            return false;
        }
    }

    /**
     * Emitter 안전하게 제거
     */
    private void removeEmitterSafely(UUID userPublicId, String reason) {
        SseEmitter emitter = emitters.remove(userPublicId);

        if (emitter == null) {
            return;
        }

        try {
            emitter.complete();
        } catch (IllegalStateException ignored) {
        }

        log.info("SSE 연결 제거 - userPublicId: {}, reason: {}, 남은 연결: {}",
                userPublicId, reason, emitters.size());
    }
}
