package kr.gravy.blind.board.dto;

public class TogglePostLikeDto {

    public record LikeToggleResponse(
            boolean isLiked,
            int likeCount
    ) {
        public static LikeToggleResponse of(boolean isLiked, int likeCount) {
            return new LikeToggleResponse(isLiked, likeCount);
        }
    }

    private TogglePostLikeDto() {
    }
}
