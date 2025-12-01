package kr.gravy.blind.user.model;

import lombok.Getter;

/**
 * S3 이미지 업로드 상태
 * Presigned URL 기반 업로드 플로우 추적
 *
 * NOT_UPLOADED: S3 업로드 대기 중 (Presigned URL 발급 후, 클라이언트 업로드 전)
 * UPLOADED: S3 업로드 완료 (클라이언트가 S3에 업로드 완료)
 * FAILED: S3 업로드 실패 (업로드 중 오류 발생)
 * EXISTING: 기존 이미지 참조 (프로필 수정 시 유지되는 이미지)
 */
@Getter
public enum ImageUploadStatus {
    NOT_UPLOADED("업로드 대기 중"),
    UPLOADED("업로드 완료"),
    FAILED("업로드 실패"),
    EXISTING("기존 이미지");

    private final String description;

    ImageUploadStatus(String description) {
        this.description = description;
    }
}
