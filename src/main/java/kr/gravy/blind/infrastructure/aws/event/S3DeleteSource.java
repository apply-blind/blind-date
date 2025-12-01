package kr.gravy.blind.infrastructure.aws.event;

/**
 * S3 파일 삭제 이벤트의 발생 소스를 나타내는 Enum
 */
public enum S3DeleteSource {
    /**
     * 프로필 승인 시 기존 이미지 업데이트
     * ProfileImageService.updateProfileImages() 에서 발생
     * 승인 시 사용되지 않는 기존 이미지 삭제
     */
    APPROVE_PROFILE_UPDATE_IMAGES("프로필 승인 - 이미지 업데이트"),

    /**
     * 프로필 반려 시 Pending 이미지 삭제
     * ProfileReviewService.rejectProfile() 에서 발생
     * 반려된 프로필의 임시 업로드 이미지 정리
     */
    REJECT_PROFILE("프로필 반려"),

    /**
     * 프로필 수정 제출 시 승인된 프로필 변경
     * ProfileSubmissionService 에서 발생
     * APPROVED 상태에서 새로운 수정 요청 제출 시 이미지 동기화
     */
    SUBMIT_PROFILE_UPDATE_APPROVED("프로필 수정 제출 - 승인 상태");

    private final String description;

    S3DeleteSource(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name(), description);
    }
}
