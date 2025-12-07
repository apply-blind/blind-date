package kr.gravy.blind.configuration.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * HMAC 서명에 사용할 Base64 인코딩된 비밀키
 * 댓글 익명 닉네임 생성 시 사용
 */
@Validated
@ConfigurationProperties(prefix = "hmac")
public record HmacProperties(@NotBlank String secret) {
}