package kr.gravy.blind.notification.model;

/**
 * 알림 타입
 */
public enum NotificationType {
    REVIEW_APPROVED("프로필 승인"),
    REVIEW_REJECTED("프로필 반려"),
    PROFILE_UPDATE_APPROVED("프로필 수정 승인"),
    PROFILE_UPDATE_REJECTED("프로필 수정 반려");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
