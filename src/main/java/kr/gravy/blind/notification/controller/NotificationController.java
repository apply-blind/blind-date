package kr.gravy.blind.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "알림", description = "SSE 알림 스트림 및 알림 관리 API")
@Slf4j
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final SseEmitterService sseEmitterService;
    private final NotificationService notificationService;

    @Operation(summary = "SSE 스트림 구독", description = "실시간 알림을 받기 위한 SSE 연결")
    @GetMapping(value = "/api/v1/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@CurrentUser User user) {
        log.info("SSE 스트림 구독 요청 - userPublicId: {}", user.getPublicId());
        return sseEmitterService.subscribe(user.getPublicId());
    }

    @Operation(summary = "알림 목록 조회", description = "알림 목록을 페이징 조회")
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

    @Operation(summary = "미읽음 알림 개수 조회", description = "읽지 않은 알림 개수 반환")
    @GetMapping("/api/v1/notifications/unread-count")
    public ResponseEntity<UnreadCountDto.Response> getUnreadCount(@CurrentApprovedUser User user) {
        int count = notificationService.getUnreadCount(user.getPublicId());
        return ResponseEntity.ok(UnreadCountDto.Response.of(count));
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음으로 표시")
    @PatchMapping("/api/v1/notifications/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @CurrentApprovedUser User user,
            @PathVariable("id") Long notificationId
    ) {
        notificationService.markAsRead(user.getPublicId(), notificationId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "전체 알림 읽음 처리", description = "모든 알림을 읽음으로 표시")
    @PatchMapping("/api/v1/notifications/read-all")
    public ResponseEntity<Void> markAllAsRead(@CurrentApprovedUser User user) {
        notificationService.markAllAsRead(user.getPublicId());
        return ResponseEntity.noContent().build();
    }
}
