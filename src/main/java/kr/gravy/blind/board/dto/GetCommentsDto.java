package kr.gravy.blind.board.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import kr.gravy.blind.board.entity.Comment;
import kr.gravy.blind.board.model.CommentStatus;
import kr.gravy.blind.user.model.Gender;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class GetCommentsDto {

    public record CommentResponse(
            UUID publicId,
            Gender authorGender,
            String anonymousNickname,
            String content,
            CommentStatus status,  // 삭제 여부 판단용
            Integer likeCount,
            Boolean isLikedByCurrentUser,
            Boolean isAuthor,
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime createdAt,
            List<ReplyResponse> replies  // 대댓글 리스트
    ) {
        /**
         * @param comment  Comment 엔티티
         * @param isLiked  현재 사용자 좋아요 여부
         * @param isAuthor 현재 사용자 작성 여부
         * @param replies  대댓글 리스트
         * @return CommentResponse DTO
         */
        public static CommentResponse of(Comment comment, boolean isLiked, boolean isAuthor, List<ReplyResponse> replies) {
            return new CommentResponse(
                    comment.getPublicId(),
                    comment.getAuthorGender(),
                    comment.getAnonymousNickname(),
                    comment.getDisplayContent(),
                    comment.getStatus(),
                    comment.getLikeCount(),
                    comment.isDeleted() ? false : isLiked,
                    comment.isDeleted() ? false : isAuthor,
                    comment.getCreatedAt(),
                    replies
            );
        }
    }

    public record ReplyResponse(
            UUID publicId,
            Gender authorGender,
            String anonymousNickname,
            String content,
            CommentStatus status,  // 삭제 여부 판단용
            Integer likeCount,
            Boolean isLikedByCurrentUser,
            Boolean isAuthor,
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime createdAt
    ) {
        /**
         * @param reply    Comment Entity
         * @param isLiked  현재 사용자 좋아요 여부
         * @param isAuthor 현재 사용자 작성 여부
         * @return ReplyResponse DTO
         */
        public static ReplyResponse of(Comment reply, boolean isLiked, boolean isAuthor) {
            return new ReplyResponse(
                    reply.getPublicId(),
                    reply.getAuthorGender(),
                    reply.getAnonymousNickname(),
                    reply.getDisplayContent(),
                    reply.getStatus(),
                    reply.getLikeCount(),
                    reply.isDeleted() ? false : isLiked,
                    reply.isDeleted() ? false : isAuthor,
                    reply.getCreatedAt()
            );
        }
    }

    public record PageResponse(
            List<CommentResponse> content,
            int pageNumber,
            int pageSize,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last
    ) {
        /**
         * @param commentPage
         * @param commentResponses
         * @return PageResponse DTO
         */
        public static PageResponse of(Page<Comment> commentPage, List<CommentResponse> commentResponses) {
            return new PageResponse(
                    commentResponses,
                    commentPage.getNumber(),
                    commentPage.getSize(),
                    commentPage.getTotalElements(),
                    commentPage.getTotalPages(),
                    commentPage.isFirst(),
                    commentPage.isLast()
            );
        }
    }

    private GetCommentsDto() {
    }
}
