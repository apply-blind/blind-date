package kr.gravy.blind.board.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.gravy.blind.auth.annotation.CurrentApprovedUser;
import kr.gravy.blind.board.dto.*;
import kr.gravy.blind.board.model.PostCategory;
import kr.gravy.blind.board.service.PostSearchService;
import kr.gravy.blind.board.service.PostService;
import kr.gravy.blind.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "익명 게시판", description = "익명 게시판 API (APPROVED 사용자만 접근 가능)")
@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostSearchService postSearchService;

    @Operation(summary = "게시글 작성", description = "익명 게시판에 게시글을 작성합니다")
    @PostMapping("/api/v1/posts")
    public ResponseEntity<CreatePostDto.Response> createPost(
            @CurrentApprovedUser User user,
            @Valid @RequestBody CreatePostDto.Request request
    ) {
        return postService.createPost(user, request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.CREATED).build());
    }

    @Operation(summary = "카테고리별 게시글 목록 조회", description = "카테고리별로 게시글 목록을 페이징 조회합니다")
    @GetMapping("/api/v1/posts")
    public ResponseEntity<GetListPostDto.PageResponse> getPostsByCategory(
            @RequestParam PostCategory category,
            @CurrentApprovedUser User user,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        GetListPostDto.PageResponse response = postService.getPostsByCategory(category, user, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "게시글 상세 조회", description = "게시글 상세 정보를 조회합니다 (조회수 증가)")
    @GetMapping("/api/v1/posts/{publicId}")
    public ResponseEntity<GetDetailPostDto.Response> getPost(
            @PathVariable UUID publicId,
            @CurrentApprovedUser User user
    ) {
        GetDetailPostDto.Response response = postService.getDetailPost(publicId, user);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "인기글 목록 조회", description = "인기글 목록을 페이징 조회합니다")
    @GetMapping("/api/v1/posts/hot")
    public ResponseEntity<GetListPostDto.PageResponse> getHotPosts(
            @CurrentApprovedUser User user,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        GetListPostDto.PageResponse response = postService.getHotPosts(pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "게시글 검색", description = "Elasticsearch + Nori를 사용한 한글 형태소 분석 기반 게시글 검색 (제목 + 내용)")
    @GetMapping("/api/v1/posts/search")
    public ResponseEntity<PostSearchDto.PageResponse> searchPosts(
            @RequestParam(required = true) String keyword,
            @RequestParam(required = false) PostCategory category,
            @CurrentApprovedUser User user,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PostSearchDto.PageResponse response = postSearchService.searchPosts(keyword, category, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "게시글 삭제", description = "게시글을 삭제합니다 (작성자만 가능)")
    @DeleteMapping("/api/v1/posts/{publicId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable UUID publicId,
            @CurrentApprovedUser User user
    ) {
        postService.deletePost(publicId, user);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "공감 토글", description = "게시글 공감을 토글합니다 (추가/취소)")
    @PostMapping("/api/v1/posts/{publicId}/likes")
    public ResponseEntity<TogglePostLikeDto.LikeToggleResponse> toggleLike(
            @PathVariable UUID publicId,
            @CurrentApprovedUser User user
    ) {
        TogglePostLikeDto.LikeToggleResponse response = postService.toggleLike(publicId, user);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "이미지 업로드 완료 알림", description = "S3 이미지 업로드 완료 후 호출하여 상태를 업데이트합니다")
    @PatchMapping("/api/v1/posts/{publicId}/image")
    public ResponseEntity<Void> completeImageUpload(
            @PathVariable UUID publicId,
            @CurrentApprovedUser User user
    ) {
        postService.verifyImageUpload(publicId, user);
        return ResponseEntity.ok().build();
    }
}
