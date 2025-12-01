package kr.gravy.blind.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청 DTO
 * 현재는 카카오 OAuth2만 지원
 */
public record LoginRequest(
        @NotBlank(message = "Authorization code는 필수입니다")
        String code
) {
}
