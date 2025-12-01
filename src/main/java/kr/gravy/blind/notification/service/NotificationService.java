package kr.gravy.blind.notification.service;

import kr.gravy.blind.infrastructure.redis.RedisConstants;
import kr.gravy.blind.notification.dto.NotificationDto;
import kr.gravy.blind.notification.dto.ReviewApprovedNotificationDto;
import kr.gravy.blind.notification.dto.ReviewRejectedNotificationDto;
import kr.gravy.blind.notification.entity.ReviewApprovedNotification;
import kr.gravy.blind.notification.entity.ReviewRejectedNotification;
import kr.gravy.blind.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 알림 서비스
 * - DB 저장 (Entity - 알림 이력 관리)
 * - Redis Pub (DTO - 다중 인스턴스 SSE 전파)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final RedisTemplate<String, NotificationDto> redisTemplate;

    /**
     * 프로필 승인 알림 전송
     *
     * @param userPublicId 사용자 Public ID
     */
    @Transactional
    public void sendReviewApproval(UUID userPublicId) {
        // 1. Entity 생성 및 DB 저장
        ReviewApprovedNotification entity = ReviewApprovedNotification.create(userPublicId);
        notificationRepository.save(entity);
        log.info("알림 DB 저장 완료 - type: REVIEW_APPROVED, userPublicId: {}", userPublicId);

        // 2. DTO 생성 및 Redis Pub
        NotificationDto dto = ReviewApprovedNotificationDto.create(userPublicId);
        redisTemplate.convertAndSend(RedisConstants.NOTIFICATION_TOPIC, dto);
        log.info("Redis Pub 전송 완료 - type: REVIEW_APPROVED, userPublicId: {}", userPublicId);
    }

    /**
     * 프로필 반려 알림 전송
     *
     * @param userPublicId 사용자 Public ID
     * @param reason       반려 사유
     */
    @Transactional
    public void sendReviewRejection(UUID userPublicId, String reason) {
        // 1. Entity 생성 및 DB 저장
        ReviewRejectedNotification entity = ReviewRejectedNotification.create(userPublicId, reason);
        notificationRepository.save(entity);
        log.info("알림 DB 저장 완료 - type: REVIEW_REJECTED, userPublicId: {}", userPublicId);

        // 2. DTO 생성 및 Redis Pub
        NotificationDto dto = ReviewRejectedNotificationDto.create(userPublicId, reason);
        redisTemplate.convertAndSend(RedisConstants.NOTIFICATION_TOPIC, dto);
        log.info("Redis Pub 전송 완료 - type: REVIEW_REJECTED, userPublicId: {}", userPublicId);
    }
}