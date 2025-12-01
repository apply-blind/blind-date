package kr.gravy.blind.user.repository;

import kr.gravy.blind.user.entity.User;
import kr.gravy.blind.user.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    Optional<User> findByPublicId(UUID publicId);

    List<User> findByStatus(UserStatus status);
}
