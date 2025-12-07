package kr.gravy.blind.notification.dto;

public class UnreadCountDto {

    /**
     * @param count 읽지 않은 알림 개수
     */
    public record Response(int count) {

        public static Response of(int count) {
            return new Response(count);
        }
    }

    private UnreadCountDto() {
    }
}
