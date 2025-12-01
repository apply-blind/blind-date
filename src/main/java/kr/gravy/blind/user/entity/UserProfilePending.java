package kr.gravy.blind.user.entity;

import jakarta.persistence.*;
import kr.gravy.blind.user.entity.base.BaseProfileEntity;
import kr.gravy.blind.user.model.Personality;
import kr.gravy.blind.user.model.ProfileData;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 프로필 수정 심사 대기 엔티티
 * 사용자의 프로필 수정 요청을 저장하며, 관리자 승인 시 UserProfile에 적용
 * BaseProfileEntity 상속으로 중복 코드 제거
 * personalities만 개별 정의 (테이블명이 다름)
 */
@Entity
@Table(name = "user_profiles_pending")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfilePending extends BaseProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    // personalities는 테이블명이 다르므로 개별 정의
    @ElementCollection
    @CollectionTable(
            name = "user_profile_personalities_pending",
            joinColumns = @JoinColumn(name = "user_profile_pending_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "personality", nullable = false, length = 50)
    private List<Personality> personalities;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    /**
     * 프로필 수정 요청 생성
     * Parameter Object Pattern: 19개 파라미터를 ProfileData VO로 그룹화
     *
     * @param userId      사용자 ID
     * @param profileData 프로필 데이터 Value Object
     * @return UserProfilePending
     */
    public static UserProfilePending create(Long userId, ProfileData profileData) {
        return new UserProfilePending(userId, profileData);
    }

    private UserProfilePending(Long userId, ProfileData profileData) {
        this.userId = userId;
        setProfileFields(
                profileData.nickname(), profileData.gender(), profileData.birthday(),
                profileData.jobCategory(), profileData.jobTitle(), profileData.company(),
                profileData.school(), profileData.residenceCity(), profileData.residenceDistrict(),
                profileData.workCity(), profileData.workDistrict(), profileData.height(),
                profileData.bloodType(), profileData.bodyType(), profileData.religion(),
                profileData.drinking(), profileData.smoking(), profileData.hasCar(),
                profileData.introduction()
        );
        this.personalities = profileData.personalities();
        this.requestedAt = LocalDateTime.now();
    }

    /**
     * 프로필 수정 요청 업데이트
     *
     * @param profileData 프로필 데이터 Value Object
     */
    public void updateFrom(ProfileData profileData) {
        setProfileFields(
                profileData.nickname(), profileData.gender(), profileData.birthday(),
                profileData.jobCategory(), profileData.jobTitle(), profileData.company(),
                profileData.school(), profileData.residenceCity(), profileData.residenceDistrict(),
                profileData.workCity(), profileData.workDistrict(), profileData.height(),
                profileData.bloodType(), profileData.bodyType(), profileData.religion(),
                profileData.drinking(), profileData.smoking(), profileData.hasCar(),
                profileData.introduction()
        );
        this.personalities = profileData.personalities();
        this.requestedAt = LocalDateTime.now();
    }

}
