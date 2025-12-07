package kr.gravy.blind.board.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.gravy.blind.auth.annotation.CurrentApprovedUser;
import kr.gravy.blind.board.dto.CreateCommentDto;
import kr.gravy.blind.board.dto.CreateReplyDto;
import kr.gravy.blind.board.dto.GetCommentsDto;
import kr.gravy.blind.board.dto.ToggleCommentLikeDto;
import kr.gravy.blind.board.service.CommentService;
import kr.gravy.blind.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@Tag(name = "익명 게시판 댓글", description = "익명 게시판 댓글 API (APPROVED 사용자만 접근 가능)")
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * @param postPublicId 게시글 publicId
     * @param user         현재 로그인한 사용자
     * @param request      댓글 작성 요청
     * @return 201 Created + commentPublicId
     */
    @Operation(summary = "댓글 작성", description = "게시글에 댓글을 작성합니다")
    @PostMapping("/api/v1/posts/{postPublicId}/comments")
    public ResponseEntity<CreateCommentDto.Response> createComment(
            @PathVariable UUID postPublicId,
            @CurrentApprovedUser User user,
            @Valid @RequestBody CreateCommentDto.Request request
    ) {
        CreateCommentDto.Response response = commentService.createComment(postPublicId, user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * @param commentPublicId 부모 댓글 publicId
     * @param user            현재 로그인한 사용자
     * @param request         대댓글 작성 요청
     * @return 201 Created + replyPublicId
     */
    @Operation(summary = "대댓글 작성", description = "댓글에 대댓글을 작성합니다 (depth 1만 허용)")
    @PostMapping("/api/v1/comments/{commentPublicId}/replies")
    public ResponseEntity<CreateReplyDto.Response> createReply(
            @PathVariable UUID commentPublicId,
            @CurrentApprovedUser User user,
            @Valid @RequestBody CreateReplyDto.Request request
    ) {
        CreateReplyDto.Response response = commentService.createReply(commentPublicId, user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * @param postPublicId 게시글 publicId
     * @param user         현재 로그인한 사용자
     * @param pageable     페이징 정보 (기본: page=0, size=20, sort=createdAt,ASC)
     * @return 200 OK + 댓글 목록 (계층적 구조)
     */
    @Operation(summary = "댓글 목록 조회", description = "게시글의 댓글 목록을 조회합니다 (대댓글 포함, 페이징)")
    @GetMapping("/api/v1/posts/{postPublicId}/comments")
    public ResponseEntity<GetCommentsDto.PageResponse> getComments(
            @PathVariable UUID postPublicId,
            @CurrentApprovedUser User user,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        GetCommentsDto.PageResponse response = commentService.getCommentsByPost(postPublicId, user, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * @param commentPublicId 댓글 publicId
     * @param user            현재 로그인한 사용자
     * @return 204 No Content
     */
    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다 (작성자만 가능, 소프트 삭제)")
    @DeleteMapping("/api/v1/comments/{commentPublicId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID commentPublicId,
            @CurrentApprovedUser User user
    ) {
        commentService.deleteComment(commentPublicId, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * @param commentPublicId 댓글 publicId
     * @param user            현재 로그인한 사용자
     * @return 200 OK + isLiked, likeCount
     */
    @Operation(summary = "댓글 좋아요 토글", description = "댓글 좋아요를 토글합니다 (추가/취소)")
    @PostMapping("/api/v1/comments/{commentPublicId}/likes")
    public ResponseEntity<ToggleCommentLikeDto.LikeToggleResponse> toggleLike(
            @PathVariable UUID commentPublicId,
            @CurrentApprovedUser User user
    ) {
        ToggleCommentLikeDto.LikeToggleResponse response = commentService.toggleLike(commentPublicId, user);
        return ResponseEntity.ok(response);
    }
}
