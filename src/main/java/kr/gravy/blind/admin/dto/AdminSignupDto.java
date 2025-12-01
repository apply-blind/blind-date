package kr.gravy.blind.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 관리자 회원가입 DTO
 */
public class AdminSignupDto {

    /**
     * 관리자 회원가입 요청
     */
    public record Request(
            @NotBlank(message = "아이디는 필수입니다")
            @Size(min = 4, max = 20, message = "아이디는 4-20자여야 합니다")
            String username,

            @NotBlank(message = "비밀번호는 필수입니다")
            @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
            String password
    ) {
    }
}