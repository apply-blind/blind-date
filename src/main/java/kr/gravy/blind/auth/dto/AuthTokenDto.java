package kr.gravy.blind.auth.dto;

/**
 * 인증 토큰 정보를 담는 DTO
 * 로그인 성공 시 클라이언트에게 반환되는 JWT 토큰들
 */
public record AuthTokenDto(
        String accessToken,
        String refreshToken
) {
}
