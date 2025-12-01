package kr.gravy.blind.user.repository;

import kr.gravy.blind.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    /**
     * User ID로 프로필 조회
     */
    Optional<UserProfile> findByUserId(Long userId);

    /**
     * 닉네임 중복 확인 (특정 사용자 제외)
     * 프로필 수정 시 자기 자신 제외하고 체크
     */
    boolean existsByNicknameAndUserIdNot(String nickname, Long userId);
}
