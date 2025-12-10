package kr.gravy.blind.infrastructure.elasticsearch;

import kr.gravy.blind.configuration.properties.ElasticsearchProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 로컬 환경 OpenSearch Configuration (Docker OpenSearch 컨테이너)
 *
 * 설정:
 * - Docker OpenSearch 2.11.0 (보안 비활성화)
 * - HTTP 프로토콜 (인증 불필요)
 * - localhost:9200 연결
 */
@Slf4j
@Configuration
@Profile("local")  // 로컬 환경 전용
@EnableAutoConfiguration(exclude = {ElasticsearchDataAutoConfiguration.class})
@RequiredArgsConstructor
public class LocalOpenSearchConfig {

    private final ElasticsearchProperties elasticsearchProperties;

    @Bean
    public OpenSearchClient openSearchClient() {
        try {
            String uri = elasticsearchProperties.uris();
            log.info("Local OpenSearch Endpoint: {}", uri);

            // HttpHost 파싱
            HttpHost host = HttpHost.create(uri);

            // ApacheHttpClient5Transport: 기본 HTTP 클라이언트 (인증 없음)
            var transport = ApacheHttpClient5TransportBuilder
                    .builder(host)
                    .build();

            OpenSearchClient client = new OpenSearchClient(transport);

            log.info("✅ OpenSearchClient initialized for local environment");
            return client;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize OpenSearchClient", e);
        }
    }
}
