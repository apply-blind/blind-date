package kr.gravy.blind.notification.service;

import kr.gravy.blind.notification.dto.NotificationDto;
import kr.gravy.blind.notification.model.SseConstants;
import kr.gravy.blind.notification.model.SseEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE Emitter 관리 서비스
 * 실시간 알림을 위한 SSE 연결 관리 및 Heartbeat
 *
 * 단일 세션 정책: 새 로그인 시 기존 연결 강제 종료
 */
@Slf4j
@Service
public class SseEmitterService {

    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * SSE 구독
     * 기존 연결이 있으면 강제 종료 후 새 연결 수립 (단일 세션 정책)
     *
     * @param userPublicId 사용자 Public ID
     * @return SseEmitter
     */
    public SseEmitter subscribe(UUID userPublicId) {
        // 기존 연결 강제 종료 (단일 세션 정책)
        SseEmitter oldEmitter = emitters.get(userPublicId);
        if (oldEmitter != null) {
            try {
                oldEmitter.send(SseEmitter.event()
                        .name(SseEventType.SESSION_EXPIRED.getValue())
                        .data(SseConstants.SESSION_EXPIRED_MESSAGE));
            } catch (IOException e) {
                log.warn("기존 세션 종료 알림 전송 실패 - userPublicId: {}", userPublicId);
            }
            oldEmitter.complete();
            emitters.remove(userPublicId);
            log.info("기존 SSE 연결 강제 종료 - userPublicId: {} (새 로그인)", userPublicId);
        }

        // 새 연결 수립
        SseEmitter emitter = new SseEmitter(SseConstants.INFINITE_TIMEOUT);
        emitters.put(userPublicId, emitter);
        log.info("SSE 연결 - userPublicId: {}, 활성 연결 수: {}", userPublicId, emitters.size());

        // 연결 즉시 connected 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                    .name(SseEventType.CONNECTED.getValue())
                    .data("SSE connection established")
                    .build());
        } catch (IOException e) {
            removeEmitter(userPublicId, "초기 연결 실패");
            log.error("SSE 연결 즉시 전송 실패 - userPublicId: {}", userPublicId, e);
        }

        // 연결 종료 시 정리
        emitter.onCompletion(() -> removeEmitter(userPublicId, "정상 종료"));
        emitter.onTimeout(() -> removeEmitter(userPublicId, "타임아웃"));
        emitter.onError(e -> {
            log.error("SSE 연결 오류 - userPublicId: {}", userPublicId, e);
            removeEmitter(userPublicId, "오류 발생");
        });

        return emitter;
    }

    /**
     * 알림 전송
     *
     * @param userPublicId 사용자 Public ID
     * @param notification 알림 DTO
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
                log.error("SSE 알림 전송 실패 - userPublicId: {}, type: {}", userPublicId, notification.type(), e);
                removeEmitter(userPublicId, "전송 실패");
            }
        } else {
            log.debug("SSE 연결 없음, 알림 전송 실패 - userPublicId: {}, type: {}", userPublicId, notification.type());
        }
    }

    /**
     * Heartbeat (15초마다)
     * 연결 유지를 위해 주기적으로 heartbeat 이벤트 전송
     */
    @Scheduled(fixedRate = SseConstants.HEARTBEAT_INTERVAL_MILLIS)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) {
            return;  // 연결 없으면 스킵
        }

        List<UUID> deadEmitters = new ArrayList<>();

        emitters.forEach((userPublicId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(SseEventType.HEARTBEAT.getValue())
                        .data("ping")
                        .build());
            } catch (IOException e) {
                deadEmitters.add(userPublicId);
                log.warn("Heartbeat 실패, 연결 제거 예정 - userPublicId: {}", userPublicId);
            }
        });

        // 끊어진 연결 제거
        deadEmitters.forEach(userPublicId -> removeEmitter(userPublicId, "Heartbeat 실패"));

        log.debug("Heartbeat 전송 완료 - 활성 연결: {}, 제거: {}", emitters.size(), deadEmitters.size());
    }

    /**
     * Emitter 제거
     *
     * @param userPublicId 사용자 Public ID
     * @param reason       제거 사유
     */
    private void removeEmitter(UUID userPublicId, String reason) {
        SseEmitter emitter = emitters.remove(userPublicId);
        if (emitter != null) {
            emitter.complete();  // 리소스 정리
        }
        log.info("SSE 연결 제거 - userPublicId: {}, reason: {}, 남은 연결: {}",
                userPublicId, reason, emitters.size());
    }
}