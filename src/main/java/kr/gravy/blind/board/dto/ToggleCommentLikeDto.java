package kr.gravy.blind.board.dto;

public class ToggleCommentLikeDto {

    public record LikeToggleResponse(
            boolean isLiked,
            int likeCount
    ) {
        /**
         * @param isLiked   좋아요 여부
         * @param likeCount 좋아요 수
         * @return LikeToggleResponse DTO
         */
        public static LikeToggleResponse of(boolean isLiked, int likeCount) {
            return new LikeToggleResponse(isLiked, likeCount);
        }
    }

    private ToggleCommentLikeDto() {
    }
}
