package kr.gravy.blind.notification.controller;

import kr.gravy.blind.auth.annotation.CurrentApprovedUser;
import kr.gravy.blind.auth.annotation.CurrentUser;
import kr.gravy.blind.notification.dto.GetNotificationsDto;
import kr.gravy.blind.notification.dto.UnreadCountDto;
import kr.gravy.blind.notification.service.NotificationService;
import kr.gravy.blind.notification.service.SseEmitterService;
import kr.gravy.blind.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final SseEmitterService sseEmitterService;
    private final NotificationService notificationService;

    /**
     * SSE 스트림 구독
     * 프론트엔드에서 EventSource로 연결
     * JWT 쿠키로 인증된 사용자만 접근 가능
     *
     * @param user JWT에서 추출한 인증된 사용자
     * @return SseEmitter
     */
    @GetMapping(value = "/api/v1/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@CurrentUser User user) {
        log.info("SSE 스트림 구독 요청 - userPublicId: {}", user.getPublicId());
        return sseEmitterService.subscribe(user.getPublicId());
    }

    /**
     * @param user 현재 사용자
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 알림 목록
     */
    @GetMapping("/api/v1/notifications")
    public ResponseEntity<Page<GetNotificationsDto.Response>> getNotifications(
            @CurrentApprovedUser User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<GetNotificationsDto.Response> notifications = notificationService.getNotifications(user.getPublicId(), pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 읽지 않은 알림 개수 조회
     *
     * @param user 현재 사용자
     * @return 읽지 않은 알림 개수
     */
    @GetMapping("/api/v1/notifications/unread-count")
    public ResponseEntity<UnreadCountDto.Response> getUnreadCount(@CurrentApprovedUser User user) {
        int count = notificationService.getUnreadCount(user.getPublicId());
        return ResponseEntity.ok(UnreadCountDto.Response.of(count));
    }

    /**
     * 특정 알림 읽음 처리
     *
     * @param user           현재 사용자
     * @param notificationId 알림 ID
     * @return 204 No Content
     */
    @PatchMapping("/api/v1/notifications/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @CurrentApprovedUser User user,
            @PathVariable("id") Long notificationId
    ) {
        notificationService.markAsRead(user.getPublicId(), notificationId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 모든 알림 읽음 처리
     *
     * @param user 현재 사용자
     * @return 204 No Content
     */
    @PatchMapping("/api/v1/notifications/read-all")
    public ResponseEntity<Void> markAllAsRead(@CurrentApprovedUser User user) {
        notificationService.markAllAsRead(user.getPublicId());
        return ResponseEntity.noContent().build();
    }
}
