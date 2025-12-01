package kr.gravy.blind.user.entity;

import jakarta.persistence.*;
import kr.gravy.blind.user.entity.base.BaseProfileEntity;
import kr.gravy.blind.user.model.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile extends BaseProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // personalities는 테이블명이 다르므로 개별 정의
    @ElementCollection
    @CollectionTable(
            name = "user_profile_personalities",
            joinColumns = @JoinColumn(name = "user_profile_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "personality", nullable = false, length = 50)
    private List<Personality> personalities;

    public static UserProfile create(
            User user,
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
        return new UserProfile(
                user, nickname, gender, birthday, jobCategory, jobTitle, company, school,
                residenceCity, residenceDistrict, workCity, workDistrict,
                height, bloodType, bodyType, personalities, religion,
                drinking, smoking, hasCar, introduction
        );
    }

    private UserProfile(
            User user,
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
        this.user = user;
        setProfileFields(nickname, gender, birthday, jobCategory, jobTitle, company, school,
                residenceCity, residenceDistrict, workCity, workDistrict,
                height, bloodType, bodyType, religion, drinking, smoking, hasCar, introduction);
        this.personalities = personalities;
    }

    /**
     * UserProfilePending 기반 UserProfile 생성
     * 관리자 승인 시 Pending → Profile 변환용
     */
    public static UserProfile create(User user, UserProfilePending pending) {
        return create(
                user,
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
                new ArrayList<>(pending.getPersonalities()), // Deep copy로 컬렉션 공유 방지
                pending.getReligion(),
                pending.getDrinking(),
                pending.getSmoking(),
                pending.getHasCar(),
                pending.getIntroduction()
        );
    }

    /**
     * UserProfilePending에서 데이터를 가져와 업데이트
     */
    public void updateFrom(UserProfilePending pending) {
        setProfileFields(
                pending.getNickname(), pending.getGender(), pending.getBirthday(),
                pending.getJobCategory(), pending.getJobTitle(), pending.getCompany(),
                pending.getSchool(), pending.getResidenceCity(), pending.getResidenceDistrict(),
                pending.getWorkCity(), pending.getWorkDistrict(), pending.getHeight(),
                pending.getBloodType(), pending.getBodyType(), pending.getReligion(),
                pending.getDrinking(), pending.getSmoking(), pending.getHasCar(),
                pending.getIntroduction()
        );
        this.personalities = new ArrayList<>(pending.getPersonalities()); // Deep copy
    }

}
