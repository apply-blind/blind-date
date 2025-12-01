package kr.gravy.blind.user.event;

import kr.gravy.blind.user.model.UserStatus;

import java.util.UUID;

/**
 * 심사 상태 변경 도메인 이벤트
 * AdminService가 사용자 프로필 심사 상태를 변경할 때 발행
 */
public record ReviewStatusChangedEvent(
        UUID userPublicId,
        UserStatus newStatus,
        String reason  // REJECTED 시 반려 사유 (APPROVED 시 null)
) {
    /**
     * 프로필 승인 이벤트 생성
     */
    public static ReviewStatusChangedEvent approved(UUID userPublicId) {
        return new ReviewStatusChangedEvent(userPublicId, UserStatus.APPROVED, null);
    }

    /**
     * 프로필 반려 이벤트 생성
     */
    public static ReviewStatusChangedEvent rejected(UUID userPublicId, String reason) {
        return new ReviewStatusChangedEvent(userPublicId, UserStatus.REJECTED, reason);
    }
}