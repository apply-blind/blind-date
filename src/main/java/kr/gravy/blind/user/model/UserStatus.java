package kr.gravy.blind.user.model;

import lombok.Getter;

/**
 * 사용자 심사 상태
 */
@Getter
public enum UserStatus {
    PROFILE_WRITING("프로필 작성 중"),
    UNDER_REVIEW("심사 중"),
    APPROVED("승인"),
    REJECTED("반려"),
    BANNED("영구 정지");

    private final String description;

    UserStatus(String description) {
        this.description = description;
    }

    /**
     * 프로필 심사 요청 가능 여부 확인
     * 허용 상태:
     * - PROFILE_WRITING: 최초 제출
     * - APPROVED: 프로필 개선 요청
     * - REJECTED: 재수정 제출
     *
     * @return 심사 요청 가능 여부
     */
    public boolean canSubmitProfileRequest() {
        return this == PROFILE_WRITING || this == APPROVED || this == REJECTED;
    }
}
