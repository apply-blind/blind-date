package kr.gravy.blind.configuration.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import kr.gravy.blind.configuration.properties.BlindProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 설정
 */
@Configuration
@RequiredArgsConstructor
public class SwaggerConfiguration {

    private final BlindProperties blindProperties;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Blind API")
                        .version("1.0.0")
                        .description("Blind 데이팅 앱 API"))
                .servers(List.of(
                        new Server().url(blindProperties.baseUrl()).description("Blind server")
                ));
    }
}
