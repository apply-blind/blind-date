package kr.gravy.blind.user.controller;

import jakarta.validation.Valid;
import kr.gravy.blind.auth.annotation.CurrentUser;
import kr.gravy.blind.board.dto.GetListPostDto;
import kr.gravy.blind.board.service.PostService;
import kr.gravy.blind.user.dto.MyInfoDto;
import kr.gravy.blind.user.dto.NicknameCheckDto;
import kr.gravy.blind.user.dto.ProfileUpdateDto;
import kr.gravy.blind.user.dto.UserProfileDto;
import kr.gravy.blind.user.entity.User;
import kr.gravy.blind.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PostService postService;

    /**
     * 현재 로그인한 사용자 정보 조회 API
     */
    @GetMapping("/api/v1/users/me")
    public ResponseEntity<MyInfoDto.Response> getCurrentUser(
            @CurrentUser User user) {
        MyInfoDto.Response response = userService.getMyInfo(user);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 내 프로필 전체 정보 조회 API
     * 프로필 수정 시 기존 데이터 로드용
     */
    @GetMapping("/api/v1/users/profile")
    public ResponseEntity<UserProfileDto.Response> getMyProfile(
            @CurrentUser User user) {
        UserProfileDto.Response response = userService.getMyProfile(user);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 내가 작성한 게시글 목록 조회 API
     */
    @GetMapping("/api/v1/users/me/posts")
    public ResponseEntity<GetListPostDto.PageResponse> getMyPosts(
            @CurrentUser User user,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        GetListPostDto.PageResponse response = postService.getMyPosts(user, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 닉네임 사용 가능 여부 조회 API
     */
    @GetMapping("/api/v1/users/nicknames/{nickname}/availability")
    public ResponseEntity<NicknameCheckDto.Response> checkNicknameAvailability(
            @PathVariable String nickname,
            @CurrentUser User user) {
        NicknameCheckDto.Response response = userService.checkNicknameAvailability(nickname, user.getId());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    /**
     * 프로필 심사 요청 제출 API
     */
    @PostMapping("/api/v1/users/profiles/pending")
    public ResponseEntity<ProfileUpdateDto.Response> submitProfileUpdateRequest(
            @CurrentUser User user,
            @Valid @RequestBody ProfileUpdateDto.Request request
    ) {
        ProfileUpdateDto.Response response = userService.submitProfileUpdateRequest(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * S3 이미지 업로드 완료 처리 API
     */
    @PatchMapping("/api/v1/users/profiles/pending/images")
    public ResponseEntity<Void> completeImageUploads(
            @CurrentUser User user
    ) {
        userService.verifyImageUploads(user);
        return ResponseEntity.ok().build();
    }
}
