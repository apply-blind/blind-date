package kr.gravy.blind.user.dto;

import jakarta.validation.constraints.NotNull;
import kr.gravy.blind.user.model.ImageUpdateType;

/**
 * 이미지 메타데이터 DTO
 * 프로필 이미지 업데이트 시 기존 이미지 유지/신규 업로드 구분
 */
public record ImageMetadataDto(
        @NotNull(message = "이미지 타입은 필수입니다")
        ImageUpdateType type,

        String url,  // EXISTING 타입인 경우 필수 (기존 이미지 Presigned URL)

        Integer index  // NEW 타입인 경우 필수 (files 배열의 인덱스)
) {
}