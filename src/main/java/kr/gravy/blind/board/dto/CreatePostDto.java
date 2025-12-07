package kr.gravy.blind.board.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.gravy.blind.board.entity.Post;
import kr.gravy.blind.board.model.PostCategory;
import kr.gravy.blind.common.type.ImageContentType;
import org.springframework.lang.Nullable;

import java.util.UUID;

public class CreatePostDto {

    public record Request(
            @NotNull(message = "카테고리는 필수입니다")
            PostCategory category,

            @NotBlank(message = "제목은 필수입니다")
            @Size(max = 255, message = "제목은 255자를 초과할 수 없습니다")
            String title,

            @NotBlank(message = "내용은 필수입니다")
            @Size(max = 10000, message = "내용은 10000자를 초과할 수 없습니다")
            String content,

            @Valid
            @Nullable
            ImageMetadata imageMetadata
    ) {
    }

    public record ImageMetadata(
            @NotBlank(message = "파일명은 필수입니다")
            String filename,

            @NotNull(message = "Content-Type은 필수입니다")
            ImageContentType contentType
    ) {
    }

    public record Response(
            UUID postPublicId,
            PresignedUrlInfo presignedUrlInfo
    ) {
        public static Response of(Post post, PresignedUrlInfo presignedUrlInfo) {
            return new Response(post.getPublicId(), presignedUrlInfo);
        }
    }

    public record PresignedUrlInfo(
            String presignedUrl,
            String s3Key
    ) {
        public static PresignedUrlInfo of(String presignedUrl, String s3Key) {
            return new PresignedUrlInfo(presignedUrl, s3Key);
        }
    }

    private CreatePostDto() {
    }
}
