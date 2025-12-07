package kr.gravy.blind.notification.entity;

import jakarta.persistence.*;
import kr.gravy.blind.common.BaseEntity;
import kr.gravy.blind.notification.model.NotificationType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 알림 엔티티 (추상 클래스)
 * JPA Inheritance - SINGLE_TABLE 전략
 */
@Entity
@Table(name = "notifications")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_public_id", nullable = false)
    protected UUID userPublicId;

    @Column(name = "is_read", nullable = false)
    protected boolean isRead;

    @Column(name = "post_public_id")
    protected UUID postPublicId;

    @Column(name = "post_title", length = 200)
    protected String postTitle;

    @Column(name = "comment_content", columnDefinition = "TEXT")
    protected String commentContent;

    @Column(name = "comment_public_id")
    protected UUID commentPublicId;

    /**
     * 알림 타입 반환
     *
     * @return NotificationType
     */
    public abstract NotificationType getType();

    /**
     * 알림 읽음 처리
     */
    public void markAsRead() {
        this.isRead = true;
    }
}
