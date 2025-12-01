package kr.gravy.blind.auth.entity;

import jakarta.persistence.*;
import kr.gravy.blind.auth.model.RefreshTokenStatus;
import kr.gravy.blind.common.BaseEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_type", nullable = false, length = 10)
    private String userType;  // "USER" or "ADMIN"

    @Column(nullable = false, length = 500)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefreshTokenStatus status;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    public static RefreshToken create(
            Long userId,
            String userType,
            String token,
            LocalDateTime expiredAt
    ) {
        return new RefreshToken(null, userId, userType, token, RefreshTokenStatus.ACTIVE, expiredAt);
    }

    public void updateStatus(RefreshTokenStatus newStatus) {
        this.status = newStatus;
    }
}
