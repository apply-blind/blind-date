package kr.gravy.blind.notification.repository;

import kr.gravy.blind.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByUserPublicIdAndCommentPublicId(UUID userPublicId, UUID commentPublicId);

    Page<Notification> findByUserPublicIdOrderByCreatedAtDesc(UUID userPublicId, Pageable pageable);

    Optional<Notification> findByIdAndUserPublicId(Long id, UUID userPublicId);

    int countByUserPublicIdAndIsReadFalse(UUID userPublicId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userPublicId = :userPublicId AND n.isRead = false")
    void markAllAsRead(@Param("userPublicId") UUID userPublicId);
}
