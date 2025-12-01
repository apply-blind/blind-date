package kr.gravy.blind.user.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import kr.gravy.blind.common.BaseEntity;
import kr.gravy.blind.user.model.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 프로필 공통 필드 추상 클래스
 * UserProfile과 UserProfilePending의 중복 필드 제거
 */
@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseProfileEntity extends BaseEntity {

    @Column(nullable = false, length = 10)
    protected String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    protected Gender gender;

    @Column(nullable = false)
    protected LocalDate birthday;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_category", nullable = false, length = 20)
    protected JobCategory jobCategory;

    @Column(name = "job_title", nullable = false, length = 50)
    protected String jobTitle;

    @Column(nullable = false, length = 100)
    protected String company;

    @Column(nullable = false, length = 100)
    protected String school;

    @Enumerated(EnumType.STRING)
    @Column(name = "residence_city", nullable = false, length = 20)
    protected City residenceCity;

    @Column(name = "residence_district", nullable = false, length = 50)
    protected String residenceDistrict;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_city", nullable = false, length = 20)
    protected City workCity;

    @Column(name = "work_district", nullable = false, length = 50)
    protected String workDistrict;

    @Column(nullable = false)
    protected Integer height;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_type", nullable = false, length = 5)
    protected BloodType bloodType;

    @Enumerated(EnumType.STRING)
    @Column(name = "body_type", nullable = false, length = 20)
    protected BodyType bodyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    protected Religion religion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    protected Drinking drinking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    protected Smoking smoking;

    @Column(name = "has_car", nullable = false)
    protected Boolean hasCar;

    @Column(nullable = false, columnDefinition = "TEXT")
    protected String introduction;

    /**
     * 공통 필드 설정 메서드
     * 자식 클래스의 생성자나 update 메서드에서 호출
     */
    protected void setProfileFields(
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
            Religion religion,
            Drinking drinking,
            Smoking smoking,
            Boolean hasCar,
            String introduction
    ) {
        this.nickname = nickname;
        this.gender = gender;
        this.birthday = birthday;
        this.jobCategory = jobCategory;
        this.jobTitle = jobTitle;
        this.company = company;
        this.school = school;
        this.residenceCity = residenceCity;
        this.residenceDistrict = residenceDistrict;
        this.workCity = workCity;
        this.workDistrict = workDistrict;
        this.height = height;
        this.bloodType = bloodType;
        this.bodyType = bodyType;
        this.religion = religion;
        this.drinking = drinking;
        this.smoking = smoking;
        this.hasCar = hasCar;
        this.introduction = introduction;
    }
}
