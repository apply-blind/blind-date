package kr.gravy.blind.user.repository;

import kr.gravy.blind.user.entity.UserImagePending;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * UserImagePending Repository
 * 심사 대기 중인 이미지 데이터 조회
 */
public interface UserImagePendingRepository extends JpaRepository<UserImagePending, Long> {

    List<UserImagePending> findByUserProfilePendingIdOrderByDisplayOrder(Long userProfilePendingId);
}
