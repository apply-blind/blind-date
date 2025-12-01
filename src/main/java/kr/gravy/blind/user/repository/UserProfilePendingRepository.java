package kr.gravy.blind.user.repository;

import kr.gravy.blind.user.entity.UserProfilePending;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 프로필 수정 심사 대기 데이터 조회
 */
public interface UserProfilePendingRepository extends JpaRepository<UserProfilePending, Long> {

    /**
     * 사용자 ID로 Pending 프로필 조회
     */
    Optional<UserProfilePending> findByUserId(Long userId);
}
