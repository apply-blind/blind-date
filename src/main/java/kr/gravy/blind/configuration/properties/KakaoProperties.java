package kr.gravy.blind.configuration.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 카카오 API 설정값
 */
@Validated
@ConfigurationProperties(prefix = "kakao")
public record KakaoProperties(
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        @NotBlank String redirectUri,
        @NotBlank String tokenUri,
        @NotBlank String userInfoUri
) {
}
