package kr.gravy.blind.board.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import kr.gravy.blind.board.entity.Post;
import kr.gravy.blind.board.model.PostCategory;
import kr.gravy.blind.board.model.PostStatus;
import kr.gravy.blind.user.model.Gender;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

public class GetListPostDto {

    public record PageResponse(
            List<ListResponse> content,
            int pageNumber,
            int pageSize,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last
    ) {
        public static PageResponse of(Page<Post> page, List<String> imageUrls) {
            List<ListResponse> content = IntStream.range(0, page.getContent().size())
                    .mapToObj(index -> ListResponse.of(
                            page.getContent().get(index),
                            imageUrls.get(index)
                    ))
                    .toList();

            return new PageResponse(
                    content,
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages(),
                    page.isFirst(),
                    page.isLast()
            );
        }
    }

    public record ListResponse(
            UUID publicId,
            PostCategory category,
            Gender authorGender,
            String title,
            PostStatus status,
            Integer viewCount,
            Integer likeCount,
            Integer commentCount,
            Boolean isPinned,
            Boolean isHot,
            Boolean hasImage,
            String imageUrl,
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime createdAt
    ) {

        public static ListResponse of(Post post, String imageUrl) {
            return new ListResponse(
                    post.getPublicId(),
                    post.getCategory(),
                    post.getAuthorGender(),
                    post.getDisplayTitle(),
                    post.getStatus(),
                    post.getViewCount(),
                    post.getLikeCount(),
                    post.getCommentCount(),
                    post.getIsPinned(),
                    post.getIsHot(),
                    post.isDeleted() ? false : (imageUrl != null),  // 삭제된 게시글은 이미지 숨김
                    post.isDeleted() ? null : imageUrl,             // 삭제된 게시글은 imageUrl null
                    post.getCreatedAt()
            );
        }
    }

    /**
     * Effective Java Item 4: 인스턴스화 방지
     */
    private GetListPostDto() {
    }
}
