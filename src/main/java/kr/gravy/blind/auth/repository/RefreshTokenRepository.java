package kr.gravy.blind.auth.repository;

import jakarta.persistence.LockModeType;
import kr.gravy.blind.auth.entity.RefreshToken;
import kr.gravy.blind.auth.model.RefreshTokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.token = :refreshToken AND rt.status = :status")
    Optional<RefreshToken> findByTokenAndStatusWithLock(@Param("refreshToken") String refreshToken, @Param("status") RefreshTokenStatus status);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshToken rt SET rt.status = 'REVOKED' WHERE rt.userId = :userId AND rt.status = 'ACTIVE'")
    void revokeActiveTokensByUserId(@Param("userId") Long userId);
}
