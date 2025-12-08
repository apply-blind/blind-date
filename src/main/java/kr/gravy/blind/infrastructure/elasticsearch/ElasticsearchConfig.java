package kr.gravy.blind.infrastructure.elasticsearch;

import kr.gravy.blind.configuration.properties.ElasticsearchProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;

@Configuration
@RequiredArgsConstructor
public class ElasticsearchConfig {

    private final ElasticsearchProperties elasticsearchProperties;

    @Bean
    public ClientConfiguration clientConfiguration() {
        String host = elasticsearchProperties.uris()
                .replace("http://", "")
                .replace("https://", "");

        ElasticsearchProperties.Connection connection = elasticsearchProperties.connection();

        return ClientConfiguration.builder()
                .connectedTo(host)
                .withConnectTimeout(connection.connectTimeout())
                .withSocketTimeout(connection.socketTimeout())
                .withClientConfigurer(
                        ElasticsearchClients.ElasticsearchHttpClientConfigurationCallback.from(
                                httpAsyncClientBuilder -> httpAsyncClientBuilder
                                        .setMaxConnTotal(connection.pool().maxTotal())
                                        .setMaxConnPerRoute(connection.pool().maxPerRoute())
                        )
                )
                .build();
    }
}
