package kr.gravy.blind.board.repository;

import kr.gravy.blind.board.entity.PostDocument;
import kr.gravy.blind.board.exception.PostSearchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;

/**
 * PostDocument OpenSearch Repository (Native Client 사용)
 *
 * 변경 이력:
 * - 2025-12-10: ElasticsearchRepository → OpenSearchClient 직접 사용
 * - 2025-12-10: Spring Data 자동 구현 → 수동 구현
 * - 2025-12-11: 커스텀 예외 추가, Bulk 실패 로깅 개선
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PostSearchRepository {

    private static final String INDEX_NAME = "posts";

    private final OpenSearchClient openSearchClient;

    /**
     * 단일 Document 인덱싱
     */
    public void save(PostDocument document) {
        try {
            IndexRequest<PostDocument> request = IndexRequest.of(i -> i
                    .index(INDEX_NAME)
                    .id(document.getId())
                    .document(document)
                    .refresh(Refresh.True)
            );

            openSearchClient.index(request);
            log.debug("PostDocument 인덱싱 완료 - id: {}", document.getId());

        } catch (IOException e) {
            log.error("PostDocument 인덱싱 실패 - id: {}", document.getId(), e);
            throw new PostSearchException("게시글 인덱싱 실패: id=" + document.getId(), e);
        }
    }

    /**
     * 대량 Document 인덱싱 (Bulk API)
     */
    public void saveAll(List<PostDocument> documents) {
        if (documents.isEmpty()) {
            log.debug("빈 Document 리스트 - Bulk 인덱싱 스킵");
            return;
        }

        try {
            List<BulkOperation> operations = documents.stream()
                    .map(doc -> BulkOperation.of(op -> op
                            .index(idx -> idx
                                    .index(INDEX_NAME)
                                    .id(doc.getId())
                                    .document(doc)
                            )
                    ))
                    .toList();

            BulkRequest bulkRequest = BulkRequest.of(b -> b
                    .index(INDEX_NAME)
                    .operations(operations)
                    .refresh(Refresh.True)
            );

            BulkResponse response = openSearchClient.bulk(bulkRequest);

            if (response.errors()) {
                logBulkErrors(response);
            }

            log.debug("Bulk 인덱싱 완료 - {}건", documents.size());

        } catch (IOException e) {
            log.error("Bulk 인덱싱 실패 - size: {}", documents.size(), e);
            throw new PostSearchException("게시글 대량 인덱싱 실패", e);
        }
    }

    /**
     * Document 삭제
     */
    public void deleteById(String id) {
        try {
            DeleteRequest request = DeleteRequest.of(d -> d
                    .index(INDEX_NAME)
                    .id(id)
                    .refresh(Refresh.True)
            );

            openSearchClient.delete(request);
            log.debug("PostDocument 삭제 완료 - id: {}", id);

        } catch (IOException e) {
            log.error("PostDocument 삭제 실패 - id: {}", id, e);
            throw new PostSearchException("게시글 삭제 실패: id=" + id, e);
        }
    }

    private void logBulkErrors(BulkResponse response) {
        List<String> failedIds = response.items().stream()
                .filter(item -> item.error() != null)
                .map(BulkResponseItem::id)
                .toList();

        log.error("Bulk 인덱싱 일부 실패 - 실패 건수: {}, 실패 ID: {}", failedIds.size(), failedIds);
    }
}
