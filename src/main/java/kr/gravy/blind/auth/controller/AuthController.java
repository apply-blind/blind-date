package kr.gravy.blind.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "인증", description = "로그인, 로그아웃, 토큰 갱신 API")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    @Operation(summary = "로그인", description = "카카오 OAuth2 인증 후 JWT 발급")
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

    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새 Access/Refresh Token 발급")
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

    @Operation(summary = "로그아웃", description = "Refresh Token 무효화 및 쿠키 삭제")
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
