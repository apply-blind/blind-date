package kr.gravy.blind.board.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;

@Document(indexName = "posts")
@Setting(settingPath = "elasticsearch/post-index-settings.json")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostDocument {

    /**
     * Elasticsearch document ID
     * - MySQL publicId와 동일하게 설정
     */
    @Id
    private String id;  // publicId.toString()

    /**
     * 제목 (Nori 분석 대상)
     * - type=Text: 전문 검색 대상
     * - analyzer = nori_analyzer: 한글 형태소 분석
     */
    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    private String title;

    /**
     * 내용 (Nori 분석 대상)
     * 제목과 동일하게 한글 형태소 분석
     */
    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    private String content;

    /**
     * 카테고리 (필터링용)
     * - type=Keyword: exact match (형태소 분석 안 함)
     * - 카테고리별 검색 필터링 (FREE_TALK, GENTLEMEN 등)
     */
    @Field(type = FieldType.Keyword)
    private String category;  // PostCategory.name()

    /**
     * 생성 시각 (정렬용)
     * 최신순 정렬 (createdAt DESC)
     */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime createdAt;

    /**
     * 이미지 URL (표시 전용)
     * - type=Keyword: 검색 불필요 (index=false)
     * - CQRS Read Model: 프론트엔드가 바로 렌더링할 수 있는 CDN URL 저장
     * - null 허용: 이미지 없는 게시글
     */
    @Field(type = FieldType.Keyword, index = false)
    private String imageUrl;

    /**
     *
     * @param post     Post 엔티티
     * @param imageUrl CDN 이미지 URL (null 가능)
     */
    public static PostDocument from(Post post, String imageUrl) {
        PostDocument document = new PostDocument();
        document.id = post.getPublicId().toString();
        document.title = post.getTitle();
        document.content = post.getContent();
        document.category = post.getCategory().name();
        document.createdAt = post.getCreatedAt();
        document.imageUrl = imageUrl;
        return document;
    }

    private PostDocument(String id, String title, String content, String category, LocalDateTime createdAt, String imageUrl) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.createdAt = createdAt;
        this.imageUrl = imageUrl;
    }
}
