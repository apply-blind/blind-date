package kr.gravy.blind.auth.configuration;

import kr.gravy.blind.auth.resolver.CurrentAdminArgumentResolver;
import kr.gravy.blind.auth.resolver.CurrentApprovedUserArgumentResolver;
import kr.gravy.blind.auth.resolver.CurrentUserArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;
    private final CurrentAdminArgumentResolver currentAdminArgumentResolver;
    private final CurrentApprovedUserArgumentResolver currentApprovedUserArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
        resolvers.add(currentAdminArgumentResolver);
        resolvers.add(currentApprovedUserArgumentResolver);
    }
}
