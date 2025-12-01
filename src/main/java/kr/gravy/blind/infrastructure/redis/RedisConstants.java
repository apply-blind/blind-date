package kr.gravy.blind.infrastructure.redis;

/**
 * Redis Pub/Sub 토픽 상수
 */
public final class RedisConstants {

    private RedisConstants() {
        // 인스턴스 생성 방지
    }

    /**
     * 알림 토픽
     * 모든 인스턴스가 구독하여 SSE로 전파
     */
    public static final String NOTIFICATION_TOPIC = "blind-notifications";
}
