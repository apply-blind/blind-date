package kr.gravy.blind.auth.oauth.kakao.service;

import kr.gravy.blind.auth.oauth.OAuth2GrantType;
import kr.gravy.blind.auth.oauth.kakao.dto.KakaoTokenResponse;
import kr.gravy.blind.auth.oauth.kakao.dto.KakaoUserInfoResponse;
import kr.gravy.blind.configuration.properties.KakaoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * 카카오 서버와 직접 통신하여 토큰 교환 및 사용자 정보 조회
 */
@Service
@RequiredArgsConstructor
public class KakaoApiService {

    private final KakaoProperties kakaoProperties;
    private final RestClient restClient = RestClient.create();

    /**
     * Authorization Code를 Access Token으로 교환
     * POST https://kauth.kakao.com/oauth/token
     *
     * @param code 카카오에서 받은 Authorization Code
     * @return 카카오 토큰 응답 (access_token, refresh_token 등)
     */
    public KakaoTokenResponse getKakaoToken(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", OAuth2GrantType.AUTHORIZATION_CODE.getValue());
        params.add("client_id", kakaoProperties.clientId());
        params.add("client_secret", kakaoProperties.clientSecret());
        params.add("redirect_uri", kakaoProperties.redirectUri());
        params.add("code", code);

        return restClient.post()
                .uri(kakaoProperties.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(KakaoTokenResponse.class);
    }

    /**
     * Access Token으로 사용자 정보 조회
     * GET https://kapi.kakao.com/v2/user/me
     *
     * @param accessToken 카카오 Access Token
     * @return 카카오 사용자 정보
     */
    public KakaoUserInfoResponse getUserInfo(String accessToken) {
        return restClient.get()
                .uri(kakaoProperties.userInfoUri())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(KakaoUserInfoResponse.class);
    }
}
