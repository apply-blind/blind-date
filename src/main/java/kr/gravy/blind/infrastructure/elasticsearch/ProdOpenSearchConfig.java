package kr.gravy.blind.infrastructure.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kr.gravy.blind.common.exception.BlindException;
import kr.gravy.blind.configuration.properties.ElasticsearchProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.regions.Region;

import static kr.gravy.blind.common.exception.Status.INTERNAL_SERVER_ERROR;

/**
 * 프로덕션 환경 OpenSearch Configuration (AWS OpenSearch VPC 도메인)
 *
 * 설정:
 * - AWS OpenSearch Service 2.11 (IAM 인증)
 * - AwsSdk2Transport (AWS SigV4 서명 자동 추가)
 * - VPC + Security Group + IAM 3단계 보안
 * - AwsCrtHttpClient (GET/DELETE body 지원)
 *
 * 변경 이력:
 * - 2025-12-10: Elasticsearch Client → OpenSearch Client (라이선스 체크 회피)
 * - 2025-12-10: 수동 SigV4 구현 → AwsSdk2Transport (자동 처리)
 * - 2025-12-10: Apache HttpClient → AwsCrtHttpClient (body 지원)
 * - 2025-12-11: ElasticsearchProperties.pool() 설정 반영, 로그 레벨 개선
 */
@Slf4j
@Configuration
@Profile("prod")
@EnableAutoConfiguration(exclude = {ElasticsearchDataAutoConfiguration.class})
@RequiredArgsConstructor
public class ProdOpenSearchConfig {

    private static final String AWS_SERVICE_NAME = "es";

    private final ElasticsearchProperties elasticsearchProperties;

    @Value("${aws.s3.region}")
    private String awsRegion;

    /**
     * AWS OpenSearch용 OpenSearchClient (AwsSdk2Transport 사용)
     *
     * IAM 인증 흐름:
     * 1. AwsSdk2Transport가 DefaultCredentialsProvider로 EC2 IAM Role 자격증명 획득
     * 2. 모든 HTTP 요청에 AWS SigV4 서명 자동 추가 (요청 본문 포함)
     * 3. OpenSearch 도메인이 IAM 인증 확인 후 접근 허용
     */
    @Bean
    public OpenSearchClient openSearchClient() {
        try {
            String uri = elasticsearchProperties.uris();
            String endpoint = normalizeEndpoint(uri);

            log.debug("OpenSearch 클라이언트 초기화 - endpoint: {}, region: {}", endpoint, awsRegion);

            SdkHttpClient httpClient = createHttpClient();
            JacksonJsonpMapper jsonpMapper = createJsonMapper();
            AwsSdk2Transport transport = createTransport(httpClient, endpoint, jsonpMapper);

            OpenSearchClient client = new OpenSearchClient(transport);

            log.debug("OpenSearchClient 초기화 완료 - AwsSdk2Transport (SigV4 + JavaTimeModule)");
            return client;

        } catch (Exception e) {
            log.error("OpenSearchClient 초기화 실패", e);
            throw new BlindException(INTERNAL_SERVER_ERROR, e);
        }
    }

    private String normalizeEndpoint(String uri) {
        return uri.replace("https://", "").replace("http://", "");
    }

    private SdkHttpClient createHttpClient() {
        ElasticsearchProperties.Connection connection = elasticsearchProperties.connection();
        ElasticsearchProperties.Connection.Pool pool = connection.pool();

        return AwsCrtHttpClient.builder()
                .connectionTimeout(connection.connectTimeout())
                .maxConcurrency(pool.maxTotal())
                .build();
    }

    private JacksonJsonpMapper createJsonMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new JacksonJsonpMapper(objectMapper);
    }

    private AwsSdk2Transport createTransport(
            SdkHttpClient httpClient,
            String endpoint,
            JacksonJsonpMapper jsonpMapper
    ) {
        return new AwsSdk2Transport(
                httpClient,
                endpoint,
                AWS_SERVICE_NAME,
                Region.of(awsRegion),
                AwsSdk2TransportOptions.builder()
                        .setMapper(jsonpMapper)
                        .build()
        );
    }
}
