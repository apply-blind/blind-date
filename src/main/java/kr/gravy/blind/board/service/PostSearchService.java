package kr.gravy.blind.board.service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import kr.gravy.blind.board.dto.PostSearchDto;
import kr.gravy.blind.board.entity.PostDocument;
import kr.gravy.blind.board.model.PostCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostSearchService {

    // 검색 설정 상수
    private static final double TITLE_BOOST_FACTOR = 3.0;
    private static final double CONTENT_BOOST_FACTOR = 1.0;
    private static final String FUZZINESS = "AUTO";
    private static final double TIE_BREAKER_SCORE = 0.3;

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 게시글 검색 (Multi-match + 카테고리 필터링)
     *
     * @param keyword  검색 키워드 (title, content 대상)
     * @param category 카테고리 필터 (선택적, null이면 전체 검색)
     * @param pageable 페이징 정보
     * @return 검색 결과 (페이징)
     */
    public PostSearchDto.PageResponse searchPosts(String keyword, PostCategory category, Pageable pageable) {
        log.info("게시글 검색: keyword={}, category={}, page={}, size={}",
                keyword, category, pageable.getPageNumber(), pageable.getPageSize());

        Query multiMatchQuery = buildMultiMatchQuery(keyword);

        Query finalQuery = (category != null)
                ? buildBoolQueryWithCategoryFilter(multiMatchQuery, category)
                : multiMatchQuery;

        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(finalQuery)
                .withPageable(pageable)
                .build();

        SearchHits<PostDocument> searchHits = elasticsearchOperations.search(searchQuery, PostDocument.class);
        Page<PostDocument> page = convertToPage(searchHits, pageable);

        return PostSearchDto.PageResponse.of(page);
    }

    private Query buildMultiMatchQuery(String keyword) {
        return Query.of(q -> q
                .multiMatch(MultiMatchQuery.of(m -> m
                        .query(keyword)
                        .fields("title^" + TITLE_BOOST_FACTOR, "content^" + CONTENT_BOOST_FACTOR)  // Boosting
                        .fuzziness(FUZZINESS)                          // 오타 허용
                        .tieBreaker(TIE_BREAKER_SCORE)                 // Tie Breaker
                ))
        );
    }

    private Query buildBoolQueryWithCategoryFilter(Query multiMatchQuery, PostCategory category) {
        return Query.of(q -> q
                .bool(BoolQuery.of(b -> b
                        .must(multiMatchQuery)
                        .filter(f -> f.term(t -> t
                                .field("category")
                                .value(category.name())
                        ))
                ))
        );
    }


    private Page<PostDocument> convertToPage(SearchHits<PostDocument> searchHits, Pageable pageable) {
        List<PostDocument> content = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        long totalHits = searchHits.getTotalHits();

        return new PageImpl<>(content, pageable, totalHits);
    }
}
