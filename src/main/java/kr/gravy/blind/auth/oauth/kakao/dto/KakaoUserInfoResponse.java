package kr.gravy.blind.auth.oauth.kakao.dto;

/**
 * 카카오 사용자 정보 API 응답
 * GET https://kapi.kakao.com/v2/user/me
 *
 * 현재는 최소 정보만 필요 (사용자 ID만)
 * scope를 설정하지 않았으므로 id만 응답됨
 */
public record KakaoUserInfoResponse(
        Long id
) {
}
