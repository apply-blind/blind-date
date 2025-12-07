package kr.gravy.blind.notification.listener;

import kr.gravy.blind.infrastructure.redis.RedisConstants;
import kr.gravy.blind.notification.dto.CommentCreatedNotificationDto;
import kr.gravy.blind.notification.dto.NotificationDto;
import kr.gravy.blind.notification.dto.ReplyCreatedNotificationDto;
import kr.gravy.blind.notification.event.NotificationSavedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 알림 저장 완료 이벤트 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSavedEventListener {

    @Qualifier("notificationRedisTemplate")
    private final RedisTemplate<String, NotificationDto> redisTemplate;

    /**
     * @param event 알림 저장 완료 이벤트
     */
    @EventListener
    public void onNotificationSaved(NotificationSavedEvent event) {
        log.info("알림 저장 완료 이벤트 수신 - type: {}, userPublicId: {}",
                event.type(), event.userPublicId());

        NotificationDto dto = createNotificationDto(event);

        redisTemplate.convertAndSend(RedisConstants.NOTIFICATION_TOPIC, dto);
        log.info("Redis Pub 전송 완료 - type: {}, userPublicId: {}, postPublicId: {}",
                event.type(), event.userPublicId(), event.postPublicId());
    }

    /**
     * @param event 알림 저장 완료 이벤트
     * @return NotificationDto
     */
    private NotificationDto createNotificationDto(NotificationSavedEvent event) {
        return switch (event.type()) {
            case COMMENT_CREATED -> CommentCreatedNotificationDto.create(
                    event.userPublicId(),
                    event.postPublicId(),
                    event.postTitle(),
                    event.content()
            );
            case REPLY_CREATED -> ReplyCreatedNotificationDto.create(
                    event.userPublicId(),
                    event.postPublicId(),
                    event.postTitle(),
                    event.content()
            );
            default -> throw new IllegalArgumentException(
                    "Unsupported notification type for Redis Pub: " + event.type()
            );
        };
    }
}
