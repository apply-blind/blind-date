package kr.gravy.blind.configuration.properties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        JwtProperties.class,
        KakaoProperties.class,
        BlindProperties.class,
        S3Properties.class,
        CorsProperties.class
})
public class PropertiesConfiguration {
}
