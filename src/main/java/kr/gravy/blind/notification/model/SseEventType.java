package kr.gravy.blind.notification.model;

/**
 * SSE 이벤트 타입
 */
public enum SseEventType {
    CONNECTED("connected"),
    HEARTBEAT("heartbeat"),
    NOTIFICATION("notification"),
    SESSION_EXPIRED("session-expired");

    private final String value;

    SseEventType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}