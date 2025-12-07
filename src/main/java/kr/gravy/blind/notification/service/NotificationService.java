package kr.gravy.blind.notification.service;

import kr.gravy.blind.board.model.PostCategory;
import kr.gravy.blind.common.exception.BlindException;
import kr.gravy.blind.infrastructure.redis.RedisConstants;
import kr.gravy.blind.notification.dto.*;
import kr.gravy.blind.notification.entity.*;
import kr.gravy.blind.notification.repository.NotificationRepository;
import kr.gravy.blind.user.entity.User;
import kr.gravy.blind.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static kr.gravy.blind.common.exception.Status.NOTIFICATION_NOT_FOUND;
import static kr.gravy.blind.common.exception.Status.USER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Qualifier("notificationRedisTemplate")
    private final RedisTemplate<String, NotificationDto> redisTemplate;

    /**
     * 프로필 승인 알림 전송
     *
     * @param userPublicId 사용자 Public ID
     */
    @Transactional
    public void sendReviewApproval(UUID userPublicId) {
        ReviewApprovedNotification entity = ReviewApprovedNotification.create(userPublicId);
        notificationRepository.save(entity);
        log.info("알림 DB 저장 완료 - type: REVIEW_APPROVED, userPublicId: {}", userPublicId);

        NotificationDto dto = ReviewApprovedNotificationDto.create(userPublicId);
        redisTemplate.convertAndSend(RedisConstants.NOTIFICATION_TOPIC, dto);
        log.info("Redis Pub 전송 완료 - type: REVIEW_APPROVED, userPublicId: {}", userPublicId);
    }

    /**
     * @param userPublicId 사용자 Public ID
     * @param reason       반려 사유
     */
    @Transactional
    public void sendReviewRejection(UUID userPublicId, String reason) {
        ReviewRejectedNotification entity = ReviewRejectedNotification.create(userPublicId, reason);
        notificationRepository.save(entity);
        log.info("알림 DB 저장 완료 - type: REVIEW_REJECTED, userPublicId: {}", userPublicId);

        NotificationDto dto = ReviewRejectedNotificationDto.create(userPublicId, reason);
        redisTemplate.convertAndSend(RedisConstants.NOTIFICATION_TOPIC, dto);
        log.info("Redis Pub 전송 완료 - type: REVIEW_REJECTED, userPublicId: {}", userPublicId);
    }

    /**
     * 새 게시글 생성 알림 전송 (브로드캐스트)
     *
     * @param postPublicId 게시글 Public ID
     * @param category     게시글 카테고리
     * @param title        게시글 제목
     */
    public void sendPostCreated(UUID postPublicId, PostCategory category, String title) {
        NotificationDto dto = PostCreatedNotificationDto.create(postPublicId, category, title);
        redisTemplate.convertAndSend(RedisConstants.NOTIFICATION_TOPIC, dto);
        log.info("Redis Pub 전송 완료 - type: POST_CREATED, postPublicId: {}, category: {}", postPublicId, category);
    }

    /**
     * 게시글 삭제 알림 전송 (브로드캐스트)
     *
     * @param postPublicId 게시글 Public ID
     * @param category     게시글 카테고리
     */
    public void sendPostDeleted(UUID postPublicId, PostCategory category) {
        NotificationDto dto = PostDeletedNotificationDto.create(postPublicId, category);
        redisTemplate.convertAndSend(RedisConstants.NOTIFICATION_TOPIC, dto);
        log.info("Redis Pub 전송 완료 - type: POST_DELETED, postPublicId: {}, category: {}", postPublicId, category);
    }

    /**
     * 댓글 삭제 실시간 알림 전송 (브로드캐스트)
     *
     * @param postPublicId    게시글 Public ID
     * @param commentPublicId 삭제된 댓글 Public ID
     */
    public void sendCommentDeleted(UUID postPublicId, UUID commentPublicId) {
        NotificationDto dto = CommentDeletedNotificationDto.create(postPublicId, commentPublicId);
        redisTemplate.convertAndSend(RedisConstants.NOTIFICATION_TOPIC, dto);
        log.info("Redis Pub 전송 완료 - type: COMMENT_DELETED, postPublicId: {}, commentPublicId: {}", postPublicId, commentPublicId);
    }

    /**
     * 댓글 생성 알림 저장 (1:1 알림)
     *
     * @param commentPublicId 댓글 Public ID
     * @param postPublicId    게시글 Public ID
     * @param postTitle       게시글 제목
     * @param targetUserId    알림 받을 사용자 ID (게시글 작성자)
     * @param commentContent  댓글 내용
     * @return 저장된 알림의 userPublicId (중복 시 null)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID saveCommentNotification(UUID commentPublicId, UUID postPublicId, String postTitle, Long targetUserId, String commentContent) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BlindException(USER_NOT_FOUND));
        UUID userPublicId = targetUser.getPublicId();

        if (notificationRepository.existsByUserPublicIdAndCommentPublicId(userPublicId, commentPublicId)) {
            log.warn("중복 알림 감지 - type: COMMENT_CREATED, userPublicId: {}, commentPublicId: {}", userPublicId, commentPublicId);
            return null;  // 중복이므로 Redis Pub 이벤트 발행 안 함
        }

        try {
            CommentCreatedNotification entity = CommentCreatedNotification.create(userPublicId, commentPublicId, postPublicId, postTitle, commentContent);
            notificationRepository.save(entity);
            log.info("알림 DB 저장 완료 - type: COMMENT_CREATED, userPublicId: {}, commentPublicId: {}", userPublicId, commentPublicId);
            return userPublicId;
        } catch (DataIntegrityViolationException e) {
            log.warn("중복 알림 저장 시도 (UNIQUE 제약 위반) - type: COMMENT_CREATED, userPublicId: {}, commentPublicId: {}", userPublicId, commentPublicId);
            return null;
        }
    }

    /**
     * 대댓글 생성 알림 저장 (1:1 알림)
     *
     * @param replyPublicId 대댓글 Public ID
     * @param postPublicId  게시글 Public ID
     * @param postTitle     게시글 제목
     * @param targetUserId  알림 받을 사용자 ID (멘션된 사용자)
     * @param replyContent  대댓글 내용
     * @return 저장된 알림의 userPublicId (중복 시 null)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID saveReplyNotification(UUID replyPublicId, UUID postPublicId, String postTitle, Long targetUserId, String replyContent) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BlindException(USER_NOT_FOUND));
        UUID userPublicId = targetUser.getPublicId();

        // 중복 알림 체크
        if (notificationRepository.existsByUserPublicIdAndCommentPublicId(userPublicId, replyPublicId)) {
            log.warn("중복 알림 감지 - type: REPLY_CREATED, userPublicId: {}, replyPublicId: {}", userPublicId, replyPublicId);
            return null;  // 중복이므로 Redis Pub 이벤트 발행 안 함
        }

        try {
            ReplyCreatedNotification entity = ReplyCreatedNotification.create(userPublicId, replyPublicId, postPublicId, postTitle, replyContent);
            notificationRepository.save(entity);
            log.info("알림 DB 저장 완료 - type: REPLY_CREATED, userPublicId: {}, replyPublicId: {}", userPublicId, replyPublicId);
            return userPublicId;
        } catch (DataIntegrityViolationException e) {
            log.warn("중복 알림 저장 시도 (UNIQUE 제약 위반) - type: REPLY_CREATED, userPublicId: {}, replyPublicId: {}", userPublicId, replyPublicId);
            return null;
        }
    }

    /**
     * 댓글 추가 알림 전송 (브로드캐스트)
     *
     * @param postPublicId    게시글 Public ID
     * @param commentPublicId 댓글 Public ID
     */
    public void sendCommentAdded(UUID postPublicId, UUID commentPublicId) {
        NotificationDto dto = CommentAddedNotificationDto.create(postPublicId, commentPublicId);
        redisTemplate.convertAndSend(RedisConstants.NOTIFICATION_TOPIC, dto);
        log.info("Redis Pub 전송 완료 - type: COMMENT_ADDED, postPublicId: {}, commentPublicId: {}", postPublicId, commentPublicId);
    }

    /**
     * 알림 목록 조회
     *
     * @param userPublicId 사용자 Public ID
     * @param pageable     페이징 정보
     * @return 알림 목록
     */
    @Transactional(readOnly = true)
    public Page<GetNotificationsDto.Response> getNotifications(UUID userPublicId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository
                .findByUserPublicIdOrderByCreatedAtDesc(userPublicId, pageable);

        return notifications.map(GetNotificationsDto.Response::from);
    }

    /**
     * 읽지 않은 알림 개수 조회
     *
     * @param userPublicId 사용자 Public ID
     * @return 읽지 않은 알림 개수
     */
    @Transactional(readOnly = true)
    public int getUnreadCount(UUID userPublicId) {
        return notificationRepository.countByUserPublicIdAndIsReadFalse(userPublicId);
    }

    /**
     * 특정 알림 읽음 처리
     *
     * @param userPublicId   사용자 Public ID
     * @param notificationId 알림 ID
     */
    @Transactional
    public void markAsRead(UUID userPublicId, Long notificationId) {
        Notification notification = notificationRepository
                .findByIdAndUserPublicId(notificationId, userPublicId)
                .orElseThrow(() -> new BlindException(NOTIFICATION_NOT_FOUND));

        notification.markAsRead();
        log.info("알림 읽음 처리 완료 - notificationId: {}, userPublicId: {}", notificationId, userPublicId);
    }

    /**
     * 모든 알림 읽음 처리
     *
     * @param userPublicId 사용자 Public ID
     */
    @Transactional
    public void markAllAsRead(UUID userPublicId) {
        notificationRepository.markAllAsRead(userPublicId);
        log.info("모든 알림 읽음 처리 완료 - userPublicId: {}", userPublicId);
    }
}
