package kr.gravy.blind.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 관리자 로그인 DTO
 */
public class AdminLoginDto {

    /**
     * 관리자 로그인 요청
     */
    public record Request(
            @NotBlank(message = "아이디는 필수입니다")
            String username,

            @NotBlank(message = "비밀번호는 필수입니다")
            String password
    ) {
    }
}