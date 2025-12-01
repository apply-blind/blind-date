package kr.gravy.blind.user.dto;

import kr.gravy.blind.auth.model.Grade;
import kr.gravy.blind.user.entity.User;
import kr.gravy.blind.user.entity.UserProfile;
import kr.gravy.blind.user.model.UserStatus;

import java.util.UUID;

/**
 * 나의 정보 DTO
 */
public class MyInfoDto {

    /**
     * 현재 로그인한 사용자 정보 응답
     */
    public record Response(
            UUID publicId,
            UserStatus status,
            Grade grade,
            String rejectionReason,  // 반려 사유 (REJECTED, BANNED일 때만 값 있음)
            boolean hasProfile,
            String nickname  // 프로필이 있으면 값, 없으면 null
    ) {
        /**
         * User와 UserProfile로부터 Response 생성
         */
        public static Response of(User user, UserProfile userProfile) {
            boolean hasProfile = userProfile != null;
            String nickname = hasProfile ? userProfile.getNickname() : null;

            return new Response(
                    user.getPublicId(),
                    user.getStatus(),
                    user.getGrade(),
                    user.getRejectionReason(),
                    hasProfile,
                    nickname
            );
        }
    }
}