package kr.gravy.blind.user.service;

import kr.gravy.blind.user.dto.MyInfoDto;
import kr.gravy.blind.user.dto.NicknameCheckDto;
import kr.gravy.blind.user.dto.ProfileUpdateDto;
import kr.gravy.blind.user.dto.UserProfileDto;
import kr.gravy.blind.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 사용자 도메인 서비스 (Facade Pattern)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final ProfileQueryService profileQueryService;
    private final ProfileSubmissionService profileSubmissionService;
    private final ProfileReviewService profileReviewService;

    /**
     * 현재 사용자 정보 조회
     */
    public MyInfoDto.Response getMyInfo(User user) {
        return profileQueryService.getUserBasicInfo(user);
    }

    /**
     * 내 프로필 전체 정보 조회
     * 프로필 수정 시 기존 데이터 로드용
     */
    public UserProfileDto.Response getMyProfile(User user) {
        return profileQueryService.getUserProfileDetails(user);
    }

    /**
     * 닉네임 사용 가능 여부 확인
     * 자기 자신을 제외하고 중복 체크
     */
    public NicknameCheckDto.Response checkNicknameAvailability(String nickname, Long currentUserId) {
        return profileQueryService.checkNickname(nickname, currentUserId);
    }

    /**
     * 프로필 심사 요청 제출
     * PROFILE_WRITING, REJECTED, APPROVED 상태에서 요청 가능
     */
    public ProfileUpdateDto.Response submitProfileUpdateRequest(User user, ProfileUpdateDto.Request request) {
        return profileSubmissionService.submitProfileUpdateRequest(user, request);
    }

    /**
     * S3 이미지 업로드 검증
     * 클라이언트가 S3 업로드 완료 후 호출
     */
    public void verifyImageUploads(User user) {
        profileSubmissionService.verifyImageUploads(user);
    }

    /**
     * 프로필 승인 메서드
     */
    public void approveProfile(User user) {
        profileReviewService.approveProfile(user);
    }

    /**
     * 관리자 -> 사용자 프로필 반려
     * 이미지만 삭제, 프로필 데이터는 유지 (재수정용)
     */
    public void rejectProfile(User user, String reason) {
        profileReviewService.rejectProfile(user, reason);
    }
}
