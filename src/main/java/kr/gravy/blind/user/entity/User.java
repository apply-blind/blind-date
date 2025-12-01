package kr.gravy.blind.user.entity;

import jakarta.persistence.*;
import kr.gravy.blind.auth.model.Grade;
import kr.gravy.blind.common.BaseEntity;
import kr.gravy.blind.common.utils.GeneratorUtil;
import kr.gravy.blind.user.model.UserStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 사용자 엔티티
 * OAuth2 소셜 로그인 정보 및 심사 상태 관리
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider", "provider_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false)
    private UUID publicId;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Grade grade;

    @Column(name = "has_base_profile", nullable = false)
    private Boolean hasBaseProfile;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    public static User create(String provider, String providerId) {
        return new User(null, GeneratorUtil.generatePublicId(), provider, providerId, UserStatus.PROFILE_WRITING, Grade.USER, false, null);
    }

    /**
     * 사용자 상태 업데이트
     * APPROVED나 UNDER_REVIEW로 변경 시 반려 사유 자동 초기화
     */
    public void updateStatus(UserStatus newStatus) {
        this.status = newStatus;

        // 승인되거나 재심사 상태로 변경되면 반려 사유 삭제
        if (newStatus == UserStatus.APPROVED || newStatus == UserStatus.UNDER_REVIEW) {
            this.rejectionReason = null;
        }
    }

    /**
     * 프로필 반려 (관리자용)
     *
     * @param reason 반려 사유
     */
    public void reject(String reason) {
        this.status = UserStatus.REJECTED;
        this.rejectionReason = reason;
    }

    /**
     * Base 프로필 생성 완료 표시
     * 최초 프로필 승인 시 호출
     */
    public void markBaseProfileCreated() {
        this.hasBaseProfile = true;
    }

    /**
     * Base 프로필 존재 여부 확인
     * 0 쿼리로 최초/수정 구분
     */
    public boolean hasBaseProfile() {
        return this.hasBaseProfile;
    }

    /**
     * 프로필 데이터 정합성 검증
     *
     * @param userProfile 조회된 UserProfile
     * @throws kr.gravy.blind.common.exception.BlindException APPROVED인데 프로필 없으면 예외
     */
    public void validateProfileExists(UserProfile userProfile) {
        if (this.status == UserStatus.APPROVED && userProfile == null) {
            throw new kr.gravy.blind.common.exception.BlindException(
                    kr.gravy.blind.common.exception.Status.PROFILE_STATE_INCONSISTENT
            );
        }
    }
}
