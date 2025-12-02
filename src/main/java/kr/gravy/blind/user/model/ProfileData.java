package kr.gravy.blind.user.model;

import kr.gravy.blind.user.dto.UserProfileDto;

import java.time.LocalDate;
import java.util.List;

/**
 * 프로필 데이터 Value Object
 * Domain Layer의 Parameter Object Pattern 구현
 * 목적:
 * - Entity가 DTO에 의존하지 않도록 분리 (Clean Architecture)
 * - 20개 파라미터를 하나의 객체로 그룹화 (Clean Code - Parameter Object Pattern)
 * - 컴파일 타임 안정성 보장 (Record의 canonical constructor)
 *
 * 검증 전략:
 * - DTO 레이어에서 Bean Validation으로 검증 완료 (@NotNull, @Size, 커스텀 Validator)
 * - VO는 검증된 데이터를 받아 변환만 수행
 *
 * 아키텍처 레이어:
 * Controller (DTO + Bean Validation) → Service (DTO → ProfileData 변환) → Entity (ProfileData 사용)
 */
public record ProfileData(
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
    public ProfileData {
    }

    /**
     * @param dto UserProfileDto.Request (Application Layer)
     * @return ProfileData (Domain Layer)
     */
    public static ProfileData from(UserProfileDto.Request dto) {
        return new ProfileData(
                dto.nickname(),
                dto.gender(),
                dto.birthday(),
                dto.jobCategory(),
                dto.jobTitle(),
                dto.company(),
                dto.school(),
                dto.residenceCity(),
                dto.residenceDistrict(),
                dto.workCity(),
                dto.workDistrict(),
                dto.height(),
                dto.bloodType(),
                dto.bodyType(),
                dto.personalities(),
                dto.religion(),
                dto.drinking(),
                dto.smoking(),
                dto.hasCar(),
                dto.introduction()
        );
    }
}
