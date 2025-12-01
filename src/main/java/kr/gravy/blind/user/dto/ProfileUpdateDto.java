package kr.gravy.blind.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.gravy.blind.user.model.ImageUpdateType;

import java.util.List;
import java.util.UUID;

/**
 * 프로필 수정 DTO
 * Presigned URL 패턴: 기존 이미지 유지 + 신규 이미지 추가
 */
public class ProfileUpdateDto {

    /**
     * 프로필 수정 요청
     */
    public record Request(
            @NotNull(message = "프로필 정보는 필수입니다")
            @Valid
            UserProfileDto.Request profile,

            @NotEmpty(message = "이미지 메타데이터는 최소 3개 필수입니다")
            @Size(min = 3, max = 6, message = "이미지는 최소 3개, 최대 6개까지 가능합니다")
            @Valid
            List<ImageUpdateMetadata> imageMetadata
    ) {
    }

    /**
     * 프로필 수정 응답 (신규 이미지에 대한 Presigned URLs만 반환)
     */
    public record Response(
            List<PresignedUrlInfo> presignedUrls
    ) {
    }

    /**
     * 이미지 수정 메타데이터
     * EXISTING: 기존 이미지 유지
     * NEW: 신규 이미지 추가
     */
    public record ImageUpdateMetadata(
            @NotNull(message = "이미지 타입은 필수입니다")
            ImageUpdateType type,

            UUID imagePublicId,    // EXISTING 타입인 경우 필수
            Integer displayOrder,  // 모든 타입 필수
            String filename,       // NEW 타입인 경우 필수
            String contentType     // NEW 타입인 경우 필수
    ) {
    }

    /**
     * Presigned URL 정보 (신규 이미지만)
     */
    public record PresignedUrlInfo(
            UUID imagePublicId,    // 서버가 생성한 UUID
            String presignedUrl,
            String s3Key,
            int displayOrder
    ) {
    }
}