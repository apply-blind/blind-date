package kr.gravy.blind.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kr.gravy.blind.board.entity.Comment;

import java.util.UUID;

public class CreateCommentDto {

    public record Request(
            @NotBlank(message = "내용은 필수입니다")
            @Size(max = 1000, message = "댓글은 1000자를 초과할 수 없습니다")
            String content
    ) {
    }

    public record Response(
            UUID commentPublicId
    ) {

        public static Response of(Comment comment) {
            return new Response(comment.getPublicId());
        }
    }

    private CreateCommentDto() {
    }
}
