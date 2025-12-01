package kr.gravy.blind.auth.controller;

import jakarta.validation.Valid;
import kr.gravy.blind.auth.dto.AuthTokenDto;
import kr.gravy.blind.auth.dto.LoginRequest;
import kr.gravy.blind.auth.service.AuthService;
import kr.gravy.blind.auth.utils.CookieUtil;
import kr.gravy.blind.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 API
 */
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    /**
     * 로그인 (토큰 생성)
     *
     * @param request Authorization Code를 담은 요청
     * @return HTTP 200 OK (쿠키에 JWT 포함)
     */
    @PostMapping("/api/v1/auth/tokens")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request) {
        // 1. 로그인 처리 및 JWT 생성 (현재는 카카오만)
        AuthTokenDto tokens = authService.kakaoLogin(request.code());

        // 2. JWT를 HttpOnly Cookie로 설정
        ResponseCookie accessCookie = cookieUtil.createAccessTokenCookie(tokens.accessToken());
        ResponseCookie refreshCookie = cookieUtil.createRefreshTokenCookie(tokens.refreshToken());

        return ResponseEntity.status(HttpStatus.OK)
                .headers(httpHeaders -> {
                    httpHeaders.add("Set-Cookie", accessCookie.toString());
                    httpHeaders.add("Set-Cookie", refreshCookie.toString());
                })
                .build();
    }

    /**
     * 토큰 갱신
     *
     * @param refreshToken 쿠키에서 추출한 Refresh Token
     * @return HTTP 200 OK (쿠키에 새로운 JWT 포함)
     */
    @PutMapping("/api/v1/auth/tokens")
    public ResponseEntity<Void> refreshTokens(
            @CookieValue(name = CookieUtil.REFRESH_COOKIE) String refreshToken) {
        AuthTokenDto tokens = authService.reissueAccessToken(refreshToken);

        ResponseCookie accessCookie = cookieUtil.createAccessTokenCookie(tokens.accessToken());
        ResponseCookie refreshCookie = cookieUtil.createRefreshTokenCookie(tokens.refreshToken());

        return ResponseEntity.status(HttpStatus.OK)
                .headers(httpHeaders -> {
                    httpHeaders.add("Set-Cookie", accessCookie.toString());
                    httpHeaders.add("Set-Cookie", refreshCookie.toString());
                })
                .build();
    }

    /**
     * 로그아웃 (토큰 삭제)
     * Access Token으로 인증된 사용자의 모든 활성 Refresh Token을 무효화하고 쿠키를 삭제
     *
     * @param user Access Token으로 인증된 사용자
     * @return HTTP 200 OK (쿠키 삭제)
     */
    @DeleteMapping("/api/v1/auth/tokens")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal User user) {
        // 1. DB에서 모든 Refresh Token 무효화
        authService.logout(user.getId());

        // 2. 쿠키 삭제
        ResponseCookie deleteAccessCookie = cookieUtil.deleteCookieOfAccessToken();
        ResponseCookie deleteRefreshCookie = cookieUtil.deleteCookieOfRefreshToken();

        return ResponseEntity.status(HttpStatus.OK)
                .headers(httpHeaders -> {
                    httpHeaders.add("Set-Cookie", deleteAccessCookie.toString());
                    httpHeaders.add("Set-Cookie", deleteRefreshCookie.toString());
                })
                .build();
    }
}
