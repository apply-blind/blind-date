package kr.gravy.blind.auth.service;

import kr.gravy.blind.auth.dto.AuthTokenDto;
import kr.gravy.blind.auth.entity.RefreshToken;
import kr.gravy.blind.auth.jwt.JWTUtil;
import kr.gravy.blind.auth.model.RefreshTokenStatus;
import kr.gravy.blind.auth.oauth.OAuth2Provider;
import kr.gravy.blind.auth.oauth.kakao.dto.KakaoTokenResponse;
import kr.gravy.blind.auth.oauth.kakao.dto.KakaoUserInfoResponse;
import kr.gravy.blind.auth.oauth.kakao.service.KakaoApiService;
import kr.gravy.blind.auth.repository.RefreshTokenRepository;
import kr.gravy.blind.common.exception.BlindException;
import kr.gravy.blind.common.utils.DateTimeUtil;
import kr.gravy.blind.user.entity.User;
import kr.gravy.blind.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;

import static kr.gravy.blind.common.exception.Status.INVALID_TOKEN;
import static kr.gravy.blind.common.exception.Status.USER_NOT_FOUND;

/**
 * 인증 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoApiService kakaoApiService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JWTUtil jwtUtil;

    /**
     * 카카오 로그인 처리
     *
     * @param code 카카오 Authorization Code
     * @return JWT Access Token과 Refresh Token
     */
    @Transactional
    public AuthTokenDto kakaoLogin(String code) {
        // 1. 카카오에 토큰 요청
        KakaoTokenResponse kakaoToken = kakaoApiService.getKakaoToken(code);

        // 2. 카카오 Access Token으로 사용자 정보 조회
        KakaoUserInfoResponse userInfo = kakaoApiService.getUserInfo(kakaoToken.accessToken());

        // 3. DB에서 사용자 조회 또는 생성
        User user = getOrCreateUser(userInfo);

        // 4. JWT 토큰 생성
        String accessToken = jwtUtil.createAccessToken(user);
        String refreshToken = jwtUtil.createRefreshToken(user);

        // 5. 기존 활성 Refresh Token 무효화
        refreshTokenRepository.revokeActiveTokensByUserId(user.getId());

        // 6. 새 Refresh Token DB에 저장
        saveRefreshToken(user.getId(), refreshToken);

        return new AuthTokenDto(accessToken, refreshToken);
    }

    private User getOrCreateUser(KakaoUserInfoResponse userInfo) {
        String provider = OAuth2Provider.KAKAO.getRegistrationId();
        String providerId = userInfo.id().toString();

        return userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> createUser(provider, providerId));
    }

    private User createUser(String provider, String providerId) {
        User newUser = User.create(provider, providerId);
        return userRepository.save(newUser);
    }

    /**
     * Refresh Token 재발급
     *
     * @param requestedRefreshToken 요청된 Refresh Token
     * @return 새로운 JWT Access Token과 Refresh Token
     */
    @Transactional
    public AuthTokenDto reissueAccessToken(String requestedRefreshToken) {
        // 1. JWT 서명 및 만료 검증 (위변조 방지)
        jwtUtil.validateToken(requestedRefreshToken);

        // 2. DB에서 Refresh Token 조회 (비관적 락으로 동시성 제어)
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenAndStatusWithLock(requestedRefreshToken, RefreshTokenStatus.ACTIVE)
                .orElseThrow(() -> new BlindException(INVALID_TOKEN));

        // 3. User 조회
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new BlindException(USER_NOT_FOUND));

        // 4. 사용된 Refresh Token 즉시 무효화 (재사용 방지)
        refreshToken.updateStatus(RefreshTokenStatus.REVOKED);

        // 5. 새로운 JWT 토큰 생성
        String accessToken = jwtUtil.createAccessToken(user);
        String newRefreshToken = jwtUtil.createRefreshToken(user);

        // 6. 새 Refresh Token DB에 저장
        saveRefreshToken(user.getId(), newRefreshToken);

        return new AuthTokenDto(accessToken, newRefreshToken);
    }

    /**
     * 로그아웃 처리
     * 해당 사용자의 모든 활성 Refresh Token을 무효화하여 모든 디바이스에서 로그아웃
     *
     * @param userId 로그아웃할 사용자 ID
     */
    @Transactional
    public void logout(Long userId) {
        // 해당 사용자의 모든 활성 Refresh Token 무효화 (모든 디바이스에서 로그아웃)
        refreshTokenRepository.revokeActiveTokensByUserId(userId);
    }

    private void saveRefreshToken(Long userId, String refreshToken) {
        Date refreshTokenExpiration = jwtUtil.getExpiration(refreshToken);
        LocalDateTime expiredAt = DateTimeUtil.convertToLocalDateTime(refreshTokenExpiration);

        RefreshToken newRefreshToken = RefreshToken.create(userId, "USER", refreshToken, expiredAt);
        refreshTokenRepository.save(newRefreshToken);
    }
}
