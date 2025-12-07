package kr.gravy.blind.notification.dto;

import kr.gravy.blind.board.model.PostCategory;
import kr.gravy.blind.notification.model.NotificationType;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 새 게시글 생성 알림 DTO (브로드캐스트)
 */
public record PostCreatedNotificationDto(
        NotificationType type,
        UUID postPublicId,      // 게시글 상세 페이지 이동용
        PostCategory category,  // 카테고리 필터링용
        String title,           // 알림 내용 (게시글 제목)
        LocalDateTime timestamp
) implements NotificationDto {

    @Nullable
    @Override
    public UUID userPublicId() {
        return null;
    }

    /**
     * @param postPublicId 게시글 Public ID
     * @param category     게시글 카테고리
     * @param title        게시글 제목
     * @return PostCreatedNotificationDto
     */
    public static PostCreatedNotificationDto create(UUID postPublicId, PostCategory category, String title) {
        return new PostCreatedNotificationDto(
                NotificationType.POST_CREATED,
                postPublicId,
                category,
                title,
                LocalDateTime.now()
        );
    }
}
