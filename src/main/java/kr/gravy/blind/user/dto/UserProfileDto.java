package kr.gravy.blind.user.dto;

import jakarta.validation.constraints.*;
import kr.gravy.blind.common.validation.ValidJob;
import kr.gravy.blind.common.validation.ValidRegion;
import kr.gravy.blind.user.entity.UserProfile;
import kr.gravy.blind.user.entity.UserProfilePending;
import kr.gravy.blind.user.model.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 사용자 프로필 DTO
 */
public class UserProfileDto {

    /**
     * 프로필 완성 요청
     * 프로필 16개 필드 + 자기소개
     */
    @ValidRegion
    @ValidJob
    public record Request(
            @NotBlank(message = "닉네임은 필수입니다")
            @Size(max = 10, message = "닉네임은 최대 10자입니다")
            String nickname,

            @NotNull(message = "성별은 필수입니다")
            Gender gender,

            @NotNull(message = "생년월일은 필수입니다")
            LocalDate birthday,

            @NotNull(message = "직업 카테고리는 필수입니다")
            JobCategory jobCategory,

            @NotBlank(message = "직업명은 필수입니다")
            @Size(max = 50, message = "직업명은 최대 50자입니다")
            String jobTitle,

            @NotBlank(message = "직장은 필수입니다")
            @Size(max = 100, message = "직장은 최대 100자입니다")
            String company,

            @NotBlank(message = "학교는 필수입니다")
            @Size(max = 100, message = "학교는 최대 100자입니다")
            String school,

            @NotNull(message = "거주 시/도는 필수입니다")
            City residenceCity,

            @NotBlank(message = "거주 구/군은 필수입니다")
            @Size(max = 50, message = "거주 구/군은 최대 50자입니다")
            String residenceDistrict,

            @NotNull(message = "직장 시/도는 필수입니다")
            City workCity,

            @NotBlank(message = "직장 구/군은 필수입니다")
            @Size(max = 50, message = "직장 구/군은 최대 50자입니다")
            String workDistrict,

            @NotNull(message = "키는 필수입니다")
            @Min(value = 100, message = "키는 100cm 이상이어야 합니다")
            @Max(value = 250, message = "키는 250cm 이하여야 합니다")
            Integer height,

            @NotNull(message = "혈액형은 필수입니다")
            BloodType bloodType,

            @NotNull(message = "체형은 필수입니다")
            BodyType bodyType,

            @NotNull(message = "성격은 필수입니다")
            @Size(min = 1, max = 3, message = "성격은 1-3개 선택해야 합니다")
            List<Personality> personalities,

            @NotNull(message = "종교는 필수입니다")
            Religion religion,

            @NotNull(message = "음주여부는 필수입니다")
            Drinking drinking,

            @NotNull(message = "흡연여부는 필수입니다")
            Smoking smoking,

            @NotNull(message = "자차여부는 필수입니다")
            Boolean hasCar,

            @NotBlank(message = "자기소개는 필수입니다")
            String introduction
    ) {
    }

    /**
     * 프로필 전체 정보 응답
     * 프로필 수정 시 기존 데이터 로드용
     */
    public record Response(
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
            String introduction,
            List<ImageInfo> images
    ) {
        /**
         * UserProfile과 ImageInfo 리스트로부터 Response 생성
         */
        public static Response of(UserProfile userProfile, List<ImageInfo> images) {
            return new Response(
                    userProfile.getNickname(),
                    userProfile.getGender(),
                    userProfile.getBirthday(),
                    userProfile.getJobCategory(),
                    userProfile.getJobTitle(),
                    userProfile.getCompany(),
                    userProfile.getSchool(),
                    userProfile.getResidenceCity(),
                    userProfile.getResidenceDistrict(),
                    userProfile.getWorkCity(),
                    userProfile.getWorkDistrict(),
                    userProfile.getHeight(),
                    userProfile.getBloodType(),
                    userProfile.getBodyType(),
                    userProfile.getPersonalities(),
                    userProfile.getReligion(),
                    userProfile.getDrinking(),
                    userProfile.getSmoking(),
                    userProfile.getHasCar(),
                    userProfile.getIntroduction(),
                    images
            );
        }

        /**
         * UserProfilePending과 ImageInfo 리스트로부터 Response 생성
         * REJECTED/UNDER_REVIEW 상태에서 pending 데이터 반환용
         */
        public static Response of(UserProfilePending pending, List<ImageInfo> images) {
            return new Response(
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
                    pending.getIntroduction(),
                    images
            );
        }

        /**
         * 이미지 정보
         */
        public record ImageInfo(
                UUID imagePublicId,
                String imageUrl,
                int displayOrder
        ) {
        }
    }
}
