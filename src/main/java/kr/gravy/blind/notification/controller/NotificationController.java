package kr.gravy.blind.notification.controller;

import kr.gravy.blind.auth.annotation.CurrentUser;
import kr.gravy.blind.notification.service.SseEmitterService;
import kr.gravy.blind.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 알림 API
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final SseEmitterService sseEmitterService;

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
}
