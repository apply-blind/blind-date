package kr.gravy.blind.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.gravy.blind.notification.dto.NotificationDto;
import kr.gravy.blind.notification.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub 알림 리스너
 * Redis에서 수신한 알림을 SSE로 전파 (다중 인스턴스 지원)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener implements MessageListener {

    private final SseEmitterService sseEmitterService;
    private final ObjectMapper objectMapper;

    /**
     * Redis 메시지 수신 및 SSE 전송
     * - userPublicId가 null이면 브로드캐스트
     * - userPublicId가 있으면 1:1 전송
     *
     * @param message Redis 메시지 (NotificationDto JSON)
     * @param pattern Redis 패턴
     */
    @Override
    public void onMessage(Message message, byte @Nullable [] pattern) {
        try {
            String json = new String(message.getBody());
            log.debug("Redis 메시지 수신: {}", json);

            // JSON → NotificationDto 역직렬화
            NotificationDto notification = objectMapper.readValue(json, NotificationDto.class);

            // userPublicId가 null이면 브로드캐스트 (연결된 모든 사용자에게)
            if (notification.userPublicId() == null) {
                sseEmitterService.broadcast(notification);
            } else {
                // 특정 사용자 알림 (1:1)
                sseEmitterService.send(notification.userPublicId(), notification);
            }

        } catch (Exception e) {
            log.error("Redis 메시지 처리 실패 - message: {}", new String(message.getBody()), e);
        }
    }
}
