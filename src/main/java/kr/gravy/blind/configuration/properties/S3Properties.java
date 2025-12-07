package kr.gravy.blind.configuration.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "aws.s3")
public record S3Properties(
        @NotBlank String region,
        @NotBlank String bucketName,
        String baseUrl,
        @NotBlank String folderName,
        Duration presignedUrlExpiration,
        int minImageCount,
        int maxImageCount,
        DataSize maxFileSize,
        String cloudFrontDomain  // CDN 도메인 (예: d111111abcdef8.cloudfront.net)
) {
}
