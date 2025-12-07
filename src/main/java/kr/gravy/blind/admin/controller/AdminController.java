package kr.gravy.blind.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.gravy.blind.admin.dto.*;
import kr.gravy.blind.admin.entity.Admin;
import kr.gravy.blind.admin.service.AdminService;
import kr.gravy.blind.auth.annotation.CurrentAdmin;
import kr.gravy.blind.auth.dto.AuthTokenDto;
import kr.gravy.blind.auth.utils.CookieUtil;
import kr.gravy.blind.user.model.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "관리자", description = "관리자 인증 및 프로필 심사 API")
@RestController
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final CookieUtil cookieUtil;

    @Operation(summary = "관리자 회원가입", description = "새로운 관리자 계정 생성")
    @PostMapping("/api/v1/admin/admins")
    public ResponseEntity<Void> createAdmin(@Valid @RequestBody AdminSignupDto.Request request) {
        adminService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "관리자 로그인", description = "관리자 인증 및 JWT 발급")
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

    @Operation(summary = "심사 대기 목록 조회", description = "심사 상태별 사용자 목록 조회")
    @GetMapping("/api/v1/admin/reviews")
    public ResponseEntity<ReviewListDto.Response> getReviewList(
            @CurrentAdmin Admin admin,
            @RequestParam(defaultValue = "UNDER_REVIEW") UserStatus status
    ) {
        ReviewListDto.Response response = adminService.getReviewList(status);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "심사 대상 상세 조회", description = "심사 대상 사용자의 프로필 상세 정보 조회")
    @GetMapping("/api/v1/admin/reviews/{publicId}")
    public ResponseEntity<ReviewDetailDto.Response> getReviewDetail(
            @CurrentAdmin Admin admin,
            @PathVariable UUID publicId
    ) {
        ReviewDetailDto.Response response = adminService.getReviewDetail(publicId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "심사 상태 변경", description = "프로필 승인 또는 반려 처리")
    @PatchMapping("/api/v1/admin/reviews/{publicId}")
    public ResponseEntity<Void> updateReviewStatus(
            @CurrentAdmin Admin admin,
            @PathVariable UUID publicId,
            @Valid @RequestBody ReviewStatusUpdateDto.Request request
    ) {
        adminService.updateReviewStatus(publicId, request.status(), request.reason());
        return ResponseEntity.ok().build();
    }
}
