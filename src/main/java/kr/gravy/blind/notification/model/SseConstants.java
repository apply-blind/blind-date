package kr.gravy.blind.notification.model;

/**
 * SSE 관련 상수
 */
public final class SseConstants {

    private SseConstants() {
        // 인스턴스 생성 방지
    }

    /**
     * SSE 타임아웃 (무제한)
     * Heartbeat로 연결 관리
     */
    public static final Long INFINITE_TIMEOUT = 0L;

    /**
     * Heartbeat 전송 간격 (밀리초)
     * 15초마다 전송하여 연결 유지
     */
    public static final int HEARTBEAT_INTERVAL_MILLIS = 15000;

    /**
     * 세션 만료 메시지
     */
    public static final String SESSION_EXPIRED_MESSAGE = "다른 곳에서 로그인되었습니다.";
}