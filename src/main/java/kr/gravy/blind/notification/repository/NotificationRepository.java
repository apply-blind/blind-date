package kr.gravy.blind.notification.repository;

import kr.gravy.blind.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

}
