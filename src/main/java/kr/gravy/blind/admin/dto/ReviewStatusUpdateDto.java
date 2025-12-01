package kr.gravy.blind.admin.dto;

import jakarta.validation.constraints.NotNull;
import kr.gravy.blind.user.model.UserStatus;

/**
 * 심사 상태 변경 DTO
 */
public class ReviewStatusUpdateDto {

    /**
     * 심사 상태 변경 요청
     */
    public record Request(
            @NotNull(message = "상태는 필수입니다")
            UserStatus status,

            String reason  // REJECTED일 때만 필수
    ) {
    }
}
