package kr.gravy.blind.board.event;

import kr.gravy.blind.board.model.PostCategory;

import java.util.UUID;

/**
 *
 * @param publicId  게시글 Public ID (Elasticsearch document ID로 사용)
 * @param title     제목 (Nori 분석 대상)
 * @param content   내용 (Nori 분석 대상)
 * @param category  카테고리 (필터링용)
 * @param imageUrl  이미지 URL (CDN URL, null 가능)
 * @param operation 인덱싱 작업 타입 (INDEX, DELETE)
 */
public record PostIndexingMessage(
        UUID publicId,
        String title,
        String content,
        PostCategory category,
        String imageUrl,
        IndexOperation operation
) {

    /**
     * @param publicId 게시글 Public ID
     * @param title    제목
     * @param content  내용
     * @param category 카테고리
     * @param imageUrl 이미지 URL (null 가능)
     */
    public static PostIndexingMessage forIndexing(
            UUID publicId,
            String title,
            String content,
            PostCategory category,
            String imageUrl) {
        return new PostIndexingMessage(
                publicId,
                title,
                content,
                category,
                imageUrl,
                IndexOperation.INDEX
        );
    }


    public static PostIndexingMessage forDeletion(UUID publicId) {
        return new PostIndexingMessage(
                publicId,
                null,  // 삭제 시 불필요
                null,  // 삭제 시 불필요
                null,  // 삭제 시 불필요
                null,  // 삭제 시 불필요
                IndexOperation.DELETE
        );
    }

    public enum IndexOperation {
        INDEX,   // 생성/수정
        DELETE   // 삭제
    }
}
