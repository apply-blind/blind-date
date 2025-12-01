package kr.gravy.blind.user.dto;

/**
 * 닉네임 중복 확인 DTO
 */
public class NicknameCheckDto {

    /**
     * 닉네임 사용 가능 여부 응답
     */
    public record Response(
            boolean available  // true: 사용 가능, false: 중복
    ) {
        public static Response of(boolean available) {
            return new Response(available);
        }
    }
}