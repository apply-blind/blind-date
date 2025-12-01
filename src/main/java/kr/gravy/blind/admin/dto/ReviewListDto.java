package kr.gravy.blind.admin.dto;

import kr.gravy.blind.user.entity.User;
import kr.gravy.blind.user.model.UserStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 심사 목록 조회 DTO
 */
public class ReviewListDto {

    /**
     * 심사 목록 응답
     */
    public record Response(
            List<ReviewInfo> reviews
    ) {
        public static Response of(List<User> users) {
            List<ReviewInfo> reviews = users.stream()
                    .map(ReviewInfo::of)
                    .toList();
            return new Response(reviews);
        }
    }

    /**
     * 심사 대상 간략 정보
     */
    public record ReviewInfo(
            UUID publicId,
            UserStatus status,
            LocalDateTime createdAt
    ) {
        public static ReviewInfo of(User user) {
            return new ReviewInfo(
                    user.getPublicId(),
                    user.getStatus(),
                    user.getCreatedAt()
            );
        }
    }
}