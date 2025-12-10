package kr.gravy.blind.board.service;

import kr.gravy.blind.board.dto.PostSearchDto;
import kr.gravy.blind.board.entity.PostDocument;
import kr.gravy.blind.board.exception.PostSearchException;
import kr.gravy.blind.board.model.PostCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.MultiMatchQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

/**
 * 게시글 검색 서비스 (OpenSearch Native Client 사용)
 *
 * 변경 이력:
 * - 2025-12-10: Spring Data Elasticsearch → OpenSearch Java Client
 * - 2025-12-10: ElasticsearchOperations → OpenSearchClient
 * - 2025-12-10: co.elastic.clients.* → org.opensearch.client.*
 * - 2025-12-11: 커스텀 예외 추가, 필드명 상수화, NPE 방어
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostSearchService {

    private static final String INDEX_NAME = "posts";

    // 필드명
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_CATEGORY = "category";
    private static final String FIELD_CREATED_AT = "createdAt";

    // 검색 설정
    private static final double TITLE_BOOST_FACTOR = 3.0;
    private static final double CONTENT_BOOST_FACTOR = 1.0;
    private static final String FUZZINESS = "AUTO";
    private static final double TIE_BREAKER_SCORE = 0.3;

    private final OpenSearchClient openSearchClient;

    /**
     * 게시글 검색 (Multi-match + 카테고리 필터링)
     *
     * @param keyword  검색 키워드 (title, content 대상)
     * @param category 카테고리 필터 (선택적, null이면 전체 검색)
     * @param pageable 페이징 정보
     * @return 검색 결과 (페이징)
     */
    public PostSearchDto.PageResponse searchPosts(String keyword, PostCategory category, Pageable pageable) {
        log.debug("게시글 검색 - keyword: {}, category: {}, page: {}, size: {}",
                keyword, category, pageable.getPageNumber(), pageable.getPageSize());

        try {
            Query multiMatchQuery = buildMultiMatchQuery(keyword);

            Query finalQuery = (category != null)
                    ? buildBoolQueryWithCategoryFilter(multiMatchQuery, category)
                    : multiMatchQuery;

            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(INDEX_NAME)
                    .query(finalQuery)
                    .from((int) pageable.getOffset())
                    .size(pageable.getPageSize())
                    .sort(sort -> sort.field(f -> f.field(FIELD_CREATED_AT).order(SortOrder.Desc)))
            );

            SearchResponse<PostDocument> response = openSearchClient.search(searchRequest, PostDocument.class);
            Page<PostDocument> page = convertToPage(response, pageable);

            log.debug("게시글 검색 완료 - 결과: {}건", page.getTotalElements());
            return PostSearchDto.PageResponse.of(page);

        } catch (IOException e) {
            log.error("게시글 검색 실패 - keyword: {}, category: {}", keyword, category, e);
            throw new PostSearchException("게시글 검색 실패", e);
        }
    }

    private Query buildMultiMatchQuery(String keyword) {
        return Query.of(q -> q
                .multiMatch(MultiMatchQuery.of(m -> m
                        .query(keyword)
                        .fields(
                                FIELD_TITLE + "^" + TITLE_BOOST_FACTOR,
                                FIELD_CONTENT + "^" + CONTENT_BOOST_FACTOR
                        )
                        .fuzziness(FUZZINESS)
                        .tieBreaker(TIE_BREAKER_SCORE)
                ))
        );
    }

    private Query buildBoolQueryWithCategoryFilter(Query multiMatchQuery, PostCategory category) {
        return Query.of(q -> q
                .bool(BoolQuery.of(b -> b
                        .must(multiMatchQuery)
                        .filter(f -> f.term(t -> t
                                .field(FIELD_CATEGORY)
                                .value(FieldValue.of(category.name()))
                        ))
                ))
        );
    }

    private Page<PostDocument> convertToPage(SearchResponse<PostDocument> response, Pageable pageable) {
        List<PostDocument> content = response.hits().hits().stream()
                .map(Hit::source)
                .filter(doc -> doc != null)
                .toList();

        long totalHits = response.hits().total() != null
                ? response.hits().total().value()
                : 0;

        return new PageImpl<>(content, pageable, totalHits);
    }
}
