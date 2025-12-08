package kr.gravy.blind.board.dto;

import kr.gravy.blind.board.entity.PostDocument;
import kr.gravy.blind.board.model.PostCategory;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class PostSearchDto {

    /**
     * 검색 요청
     *
     * @param keyword  검색 키워드 (title, content 대상)
     * @param category 카테고리 필터 (선택적, null이면 전체 검색)
     */
    public record Request(
            String keyword,
            PostCategory category
    ) {
        public static Request of(String keyword, PostCategory category) {
            return new Request(keyword, category);
        }
    }

    /**
     * @param publicId  게시글 Public ID
     * @param title     제목
     * @param content   내용 (미리보기, 200자 제한)
     * @param category  카테고리
     * @param imageUrl  이미지 URL (null 가능)
     * @param createdAt 생성 시각
     */
    public record PostResult(
            UUID publicId,
            String title,
            String content,
            PostCategory category,
            String imageUrl,
            LocalDateTime createdAt
    ) {
        // 미리보기 길이 상수
        private static final int CONTENT_PREVIEW_MAX_LENGTH = 200;

        public static PostResult from(PostDocument document) {
            return new PostResult(
                    UUID.fromString(document.getId()),
                    document.getTitle(),
                    truncateContent(document.getContent(), CONTENT_PREVIEW_MAX_LENGTH),  // 미리보기
                    PostCategory.valueOf(document.getCategory()),
                    document.getImageUrl(),
                    document.getCreatedAt()
            );
        }

        /**
         * 내용 길이 제한 (미리보기)
         */
        private static String truncateContent(String content, int maxLength) {
            if (content == null || content.length() <= maxLength) {
                return content;
            }
            return content.substring(0, maxLength) + "...";
        }
    }

    /**
     * @param posts         검색 결과 목록
     * @param totalPages    전체 페이지 수
     * @param totalElements 전체 게시글 수
     * @param currentPage   현재 페이지 (0-based)
     * @param size          페이지 크기
     */
    public record PageResponse(
            List<PostResult> posts,
            int totalPages,
            long totalElements,
            int currentPage,
            int size
    ) {
        public static PageResponse of(Page<PostDocument> page) {
            List<PostResult> posts = page.getContent().stream()
                    .map(PostResult::from)
                    .toList();

            return new PageResponse(
                    posts,
                    page.getTotalPages(),
                    page.getTotalElements(),
                    page.getNumber(),  // 0-based
                    page.getSize()
            );
        }
    }
}
