package kr.gravy.blind.auth.oauth.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 카카오 토큰 API 응답
 * POST https://kauth.kakao.com/oauth/token
 */
public record KakaoTokenResponse(
        @JsonProperty("access_token")
        String accessToken
) {
}
