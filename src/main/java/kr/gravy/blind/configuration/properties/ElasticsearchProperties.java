package kr.gravy.blind.configuration.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "spring.elasticsearch")
public record ElasticsearchProperties(
        @NotBlank String uris,
        @Valid Connection connection
) {
    public record Connection(
            Duration connectTimeout,
            Duration socketTimeout,
            @Valid Pool pool
    ) {
        public record Pool(
                @Positive int maxTotal,
                @Positive int maxPerRoute
        ) {
        }
    }
}
