package kr.gravy.blind.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kr.gravy.blind.notification.dto.NotificationDto;
import kr.gravy.blind.notification.listener.NotificationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Pub/Sub 설정
 */
@Configuration
public class RedisConfig {

    /**
     * 알림 DTO 전용 RedisTemplate
     * @param connectionFactory Redis 연결 팩토리
     * @return RedisTemplate<String, NotificationDto>
     */
    @Bean
    public RedisTemplate<String, NotificationDto> notificationRedisTemplate(
            RedisConnectionFactory connectionFactory) {

        // ObjectMapper 설정: LocalDateTime 직렬화 지원
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Jackson2JsonRedisSerializer: 타입 안전 + 다형성 지원
        Jackson2JsonRedisSerializer<NotificationDto> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, NotificationDto.class);

        // RedisTemplate 구성
        RedisTemplate<String, NotificationDto> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);

        return template;
    }

    /**
     * Redis 메시지 리스너 컨테이너
     * 알림 토픽을 구독하여 NotificationListener로 전달
     *
     * @param connectionFactory    Redis 연결 팩토리
     * @param notificationListener 알림 리스너
     * @return RedisMessageListenerContainer
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            NotificationListener notificationListener) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(notificationListener, new ChannelTopic(RedisConstants.NOTIFICATION_TOPIC));
        return container;
    }
}
