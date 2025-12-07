package kr.gravy.blind.admin.service;

import kr.gravy.blind.admin.dto.AdminLoginDto;
import kr.gravy.blind.admin.dto.AdminSignupDto;
import kr.gravy.blind.admin.dto.ReviewDetailDto;
import kr.gravy.blind.admin.dto.ReviewListDto;
import kr.gravy.blind.admin.entity.Admin;
import kr.gravy.blind.admin.repository.AdminRepository;
import kr.gravy.blind.auth.dto.AuthTokenDto;
import kr.gravy.blind.auth.entity.RefreshToken;
import kr.gravy.blind.auth.jwt.JWTUtil;
import kr.gravy.blind.auth.repository.RefreshTokenRepository;
import kr.gravy.blind.common.exception.BlindException;
import kr.gravy.blind.common.utils.DateTimeUtil;
import kr.gravy.blind.infrastructure.aws.S3Service;
import kr.gravy.blind.user.entity.User;
import kr.gravy.blind.user.entity.UserImagePending;
import kr.gravy.blind.user.entity.UserProfilePending;
import kr.gravy.blind.user.model.UserStatus;
import kr.gravy.blind.user.repository.UserImagePendingRepository;
import kr.gravy.blind.user.repository.UserProfilePendingRepository;
import kr.gravy.blind.user.repository.UserRepository;
import kr.gravy.blind.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static kr.gravy.blind.common.exception.Status.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final UserProfilePendingRepository userProfilePendingRepository;
    private final UserImagePendingRepository userImagePendingRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;
    private final S3Service s3Service;
    private final JWTUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * 관리자 회원가입
     */
    @Transactional
    public void signup(AdminSignupDto.Request request) {
        String encodedPassword = passwordEncoder.encode(request.password());
        Admin admin = Admin.create(request.username(), encodedPassword);
        adminRepository.save(admin);

        log.info("관리자 생성 완료 - username: {}", request.username());
    }

    /**
     * 관리자 로그인
     */
    @Transactional
    public AuthTokenDto login(AdminLoginDto.Request request) {
        // 1. username으로 조회
        Admin admin = adminRepository.findByUsername(request.username())
                .orElseThrow(() -> new BlindException(ADMIN_NOT_FOUND));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), admin.getPassword())) {
            throw new BlindException(ADMIN_PASSWORD_MISMATCH);
        }

        // 3. JWT 토큰 생성
        String accessToken = jwtUtil.createAccessToken(admin);
        String refreshToken = jwtUtil.createRefreshToken(admin);

        // 4. 기존 Refresh Token 무효화
        refreshTokenRepository.revokeActiveTokensByUserId(admin.getId());

        // 5. 새 Refresh Token 저장
        saveRefreshToken(admin.getId(), refreshToken);

        log.info("관리자 로그인 완료 - username: {}", request.username());

        return new AuthTokenDto(accessToken, refreshToken);
    }

    private void saveRefreshToken(Long adminId, String refreshToken) {
        Date refreshTokenExpiration = jwtUtil.getExpiration(refreshToken);
        LocalDateTime expiredAt = DateTimeUtil.convertToLocalDateTime(refreshTokenExpiration);

        RefreshToken newRefreshToken = RefreshToken.create(adminId, "ADMIN", refreshToken, expiredAt);
        refreshTokenRepository.save(newRefreshToken);
    }


    /**
     * 심사 대기 목록 조회
     */
    @Transactional(readOnly = true)
    public ReviewListDto.Response getReviewList(UserStatus status) {
        List<User> users = userRepository.findByStatus(status);
        return ReviewListDto.Response.of(users);
    }

    /**
     * 심사 대상 상세 정보 조회
     * - UserProfilePending과 UserImagePending 조회
     */
    @Transactional(readOnly = true)
    public ReviewDetailDto.Response getReviewDetail(UUID publicId) {
        // 1. User 조회
        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BlindException(USER_NOT_FOUND));

        // 2. 최초/수정 구분 (hasBaseProfile로 0 쿼리 판단)
        boolean isInitialReview = !user.hasBaseProfile();

        // 3. UserProfilePending 조회 (심사 대상은 항상 Pending)
        UserProfilePending pending = userProfilePendingRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BlindException(PENDING_PROFILE_NOT_FOUND));

        // 4. UserImagePending 조회 및 Presigned URL 변환
        List<UserImagePending> pendingImages = userImagePendingRepository
                .findByUserProfilePendingIdOrderByDisplayOrder(pending.getId());

        List<ReviewDetailDto.ImageInfo> presignedImages = pendingImages.stream()
                .map(image -> new ReviewDetailDto.ImageInfo(
                        s3Service.generatePresignedUrl(image.getS3Key()),
                        image.getDisplayOrder()
                ))
                .toList();

        // 5. DTO 반환 (Pending 데이터 + isInitialReview 플래그)
        return ReviewDetailDto.Response.of(user, pending, presignedImages, isInitialReview);
    }

    /**
     * 심사 상태 변경 (승인/반려)
     */
    @Transactional
    public void updateReviewStatus(UUID publicId, UserStatus status, String reason) {
        // 1. User 조회
        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BlindException(USER_NOT_FOUND));

        // 2. 심사 대기 상태 검증
        if (!user.isUnderReview()) {
            throw new BlindException(INVALID_REVIEW_STATUS);
        }

        // 3. 상태별 처리
        switch (status) {
            case APPROVED -> {
                userService.approveProfile(user);  // 통합 메서드 사용
                log.info("프로필 승인 완료 - userId: {}", user.getId());
            }
            case REJECTED -> {
                // 반려 사유 필수 검증
                if (reason == null || reason.isBlank()) {
                    throw new BlindException(BAD_REQUEST);
                }

                userService.rejectProfile(user, reason);  // User 엔티티 전달
                log.info("프로필 반려 완료 - userId: {}, reason: {}", user.getId(), reason);
            }
            default -> throw new BlindException(BAD_REQUEST);
        }

        // 주의: UserService에서 이미 SSE 알림 발행했으므로 여기서는 안 함
    }
}
