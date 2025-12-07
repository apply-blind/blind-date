package kr.gravy.blind.board.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import kr.gravy.blind.board.entity.Post;
import kr.gravy.blind.board.model.PostCategory;
import kr.gravy.blind.board.model.PostStatus;
import kr.gravy.blind.user.model.Gender;

import java.time.LocalDateTime;
import java.util.UUID;

public class GetDetailPostDto {

    public record Response(
            UUID publicId,
            PostCategory category,
            Gender authorGender,
            String anonymousNickname,
            String title,
            String content,
            PostStatus status,
            Integer viewCount,
            Integer likeCount,
            Integer commentCount,
            Boolean isPinned,
            Boolean isHot,
            Boolean isLikedByCurrentUser,
            Boolean isAuthor,
            String imageUrl,
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime createdAt,
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime updatedAt
    ) {

        public static Response of(Post post, boolean isLikedByCurrentUser, boolean isAuthor, String imageUrl) {
            return new Response(
                    post.getPublicId(),
                    post.getCategory(),
                    post.getAuthorGender(),
                    post.getAnonymousNickname(),
                    post.getDisplayTitle(),
                    post.getDisplayContent(),
                    post.getStatus(),
                    post.getViewCount(),
                    post.getLikeCount(),
                    post.getCommentCount(),
                    post.getIsPinned(),
                    post.getIsHot(),
                    post.isDeleted() ? false : isLikedByCurrentUser,
                    post.isDeleted() ? false : isAuthor,
                    post.isDeleted() ? null : imageUrl,
                    post.getCreatedAt(),
                    post.getUpdatedAt()
            );
        }
    }

    private GetDetailPostDto() {
    }
}
