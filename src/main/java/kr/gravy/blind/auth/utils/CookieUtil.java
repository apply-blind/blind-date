package kr.gravy.blind.auth.utils;

import kr.gravy.blind.configuration.properties.BlindProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import static org.springframework.boot.web.server.Cookie.SameSite;

/**
 * JWT 토큰을 HttpOnly Cookie로 관리하는 유틸리티
 */
@Component
@RequiredArgsConstructor
public class CookieUtil {

    private final BlindProperties blindProperties;

    public static final String ACCESS_COOKIE = "access_token";
    public static final String REFRESH_COOKIE = "refresh_token";

    private static final String ACCESS_TOKEN_USABLE_PATH = "/api/v1";
    private static final String REFRESH_TOKEN_REISSUE_PATH = "/api/v1/auth/tokens";

    public ResponseCookie createAccessTokenCookie(String token) {
        return ResponseCookie.from(ACCESS_COOKIE, token)
                .httpOnly(true)
                .secure(blindProperties.security().cookie().secure())
                .sameSite(SameSite.LAX.attributeValue())
                .path(ACCESS_TOKEN_USABLE_PATH)
                .build();
    }

    public ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from(REFRESH_COOKIE, token)
                .httpOnly(true)
                .secure(blindProperties.security().cookie().secure())
                .sameSite(SameSite.STRICT.attributeValue())
                .path(REFRESH_TOKEN_REISSUE_PATH)
                .build();
    }

    public ResponseCookie deleteCookieOfAccessToken() {
        return deleteCookie(ACCESS_COOKIE, SameSite.LAX, ACCESS_TOKEN_USABLE_PATH);
    }

    public ResponseCookie deleteCookieOfRefreshToken() {
        return deleteCookie(REFRESH_COOKIE, SameSite.STRICT, REFRESH_TOKEN_REISSUE_PATH);
    }

    private ResponseCookie deleteCookie(String name, SameSite sameSite, String path) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(blindProperties.security().cookie().secure())
                .sameSite(sameSite.attributeValue())
                .path(path)
                .maxAge(0)  // 즉시 만료
                .build();
    }
}
