package kr.gravy.blind.configuration.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Blind 애플리케이션 설정
 */
@Validated
@ConfigurationProperties(prefix = "blind")
public record BlindProperties(
        @NotNull String baseUrl,
        @Valid @NotNull Security security
) {
    public record Security(@Valid @NotNull Cookie cookie) {
        public record Cookie(boolean secure) {
        }
    }
}
