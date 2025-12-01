package kr.gravy.blind.user.repository;

import kr.gravy.blind.user.entity.UserImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserImageRepository extends JpaRepository<UserImage, Long> {

    List<UserImage> findByUserProfileIdOrderByDisplayOrder(Long profileId);

    Optional<UserImage> findByPublicId(UUID publicId);
}
