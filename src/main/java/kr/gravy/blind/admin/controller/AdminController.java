package kr.gravy.blind.admin.controller;

import jakarta.validation.Valid;
import kr.gravy.blind.admin.dto.*;
import kr.gravy.blind.admin.service.AdminService;
import kr.gravy.blind.auth.dto.AuthTokenDto;
import kr.gravy.blind.auth.utils.CookieUtil;
import kr.gravy.blind.user.model.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 관리자 API 컨트롤러
 */
@RestController
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final CookieUtil cookieUtil;

    /**
     * 관리자 회원가입
     */
    @PostMapping("/api/v1/admin/admins")
    public ResponseEntity<Void> createAdmin(@Valid @RequestBody AdminSignupDto.Request request) {
        adminService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 관리자 로그인
     */
    @PostMapping("/api/v1/admin/auth/tokens")
    public ResponseEntity<Void> login(@Valid @RequestBody AdminLoginDto.Request request) {
        AuthTokenDto tokens = adminService.login(request);

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
     * 심사 대기 목록 조회
     */
    @GetMapping("/api/v1/admin/reviews")
    public ResponseEntity<ReviewListDto.Response> getReviewList(
            @RequestParam(defaultValue = "UNDER_REVIEW") UserStatus status
    ) {
        ReviewListDto.Response response = adminService.getReviewList(status);
        return ResponseEntity.ok(response);
    }

    /**
     * 심사 대상 상세 정보 조회
     * GET /api/v1/admin/reviews/{publicId}
     */
    @GetMapping("/api/v1/admin/reviews/{publicId}")
    public ResponseEntity<ReviewDetailDto.Response> getReviewDetail(@PathVariable UUID publicId) {
        ReviewDetailDto.Response response = adminService.getReviewDetail(publicId);
        return ResponseEntity.ok(response);
    }

    /**
     * 심사 상태 변경 (승인/반려)
     * PATCH /api/v1/admin/reviews/{publicId}
     */
    @PatchMapping("/api/v1/admin/reviews/{publicId}")
    public ResponseEntity<Void> updateReviewStatus(
            @PathVariable UUID publicId,
            @Valid @RequestBody ReviewStatusUpdateDto.Request request
    ) {
        adminService.updateReviewStatus(publicId, request.status(), request.reason());
        return ResponseEntity.ok().build();
    }
}
