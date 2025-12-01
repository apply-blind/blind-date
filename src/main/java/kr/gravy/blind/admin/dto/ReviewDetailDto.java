package kr.gravy.blind.admin.dto;

import kr.gravy.blind.user.entity.User;
import kr.gravy.blind.user.entity.UserProfilePending;
import kr.gravy.blind.user.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 심사 상세 정보 DTO
 */
public class ReviewDetailDto {

    /**
     * 심사 대상 상세 정보 응답
     */
    public record Response(
            UUID publicId,
            UserStatus status,
            LocalDateTime createdAt,
            boolean isInitialReview,
            ProfileInfo profile,
            List<ImageInfo> images
    ) {
        public static Response of(User user, UserProfilePending pending, List<ImageInfo> images, boolean isInitialReview) {
            ProfileInfo profile = ProfileInfo.of(pending);

            return new Response(
                    user.getPublicId(),
                    user.getStatus(),
                    user.getCreatedAt(),
                    isInitialReview,
                    profile,
                    images
            );
        }
    }

    /**
     * 프로필 전체 정보 (Pending 데이터)
     */
    public record ProfileInfo(
            String nickname,
            Gender gender,
            LocalDate birthday,
            JobCategory jobCategory,
            String jobTitle,
            String company,
            String school,
            City residenceCity,
            String residenceDistrict,
            City workCity,
            String workDistrict,
            Integer height,
            BloodType bloodType,
            BodyType bodyType,
            List<Personality> personalities,
            Religion religion,
            Drinking drinking,
            Smoking smoking,
            Boolean hasCar,
            String introduction
    ) {
        public static ProfileInfo of(UserProfilePending pending) {
            return new ProfileInfo(
                    pending.getNickname(),
                    pending.getGender(),
                    pending.getBirthday(),
                    pending.getJobCategory(),
                    pending.getJobTitle(),
                    pending.getCompany(),
                    pending.getSchool(),
                    pending.getResidenceCity(),
                    pending.getResidenceDistrict(),
                    pending.getWorkCity(),
                    pending.getWorkDistrict(),
                    pending.getHeight(),
                    pending.getBloodType(),
                    pending.getBodyType(),
                    pending.getPersonalities(),
                    pending.getReligion(),
                    pending.getDrinking(),
                    pending.getSmoking(),
                    pending.getHasCar(),
                    pending.getIntroduction()
            );
        }
    }

    /**
     * 이미지 정보 (Presigned URL)
     */
    public record ImageInfo(
            String imageUrl,
            int displayOrder
    ) {
    }
}
