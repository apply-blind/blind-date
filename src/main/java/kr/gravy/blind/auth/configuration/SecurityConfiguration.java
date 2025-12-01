package kr.gravy.blind.auth.configuration;

import jakarta.servlet.DispatcherType;
import kr.gravy.blind.auth.jwt.JWTAuthenticationFilter;
import kr.gravy.blind.configuration.properties.CorsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JWTAuthenticationFilter jwtAuthenticationFilter;
    private final CorsProperties corsProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // JWT 기반이므로 서버 세션을 만들지 않음
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // CSRF는 세션/폼 기반일 때 주로 사용되므로 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                // 공개 API는 인증 없이 접근 허용, 그 외에는 USER 이상 권한 필요
                .authorizeHttpRequests(auth -> auth
                        // SSE 비동기 dispatch 허용 (Spring Security 6 대응)
                        // Spring Security 6.0부터 shouldFilterAllDispatcherTypes=true가 기본값
                        // → ASYNC/ERROR dispatch에서도 보안 필터가 실행됨
                        // 문제: SessionCreationPolicy.STATELESS 사용 시 ASYNC dispatch에서 SecurityContext 복원 불가
                        // 해결: ASYNC/ERROR는 초기 REQUEST의 후속 처리이므로 인증 체크 생략
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/", "/ping", "/error", "/favicon.ico").permitAll()
                        // Swagger UI 및 에러 문서 접근 허용
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/error/**"
                        ).permitAll()
                        // 사용자 인증 API - HTTP Method별 권한 설정
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/tokens").permitAll()    // 로그인
                        .requestMatchers(HttpMethod.PUT, "/api/v1/auth/tokens").permitAll()     // 토큰 갱신
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/auth/tokens").hasRole("USER")  // 로그아웃
                        // 관리자 API
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/admins").permitAll()  // 관리자 회원가입
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/auth/tokens").permitAll()  // 관리자 로그인
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")  // 그 외 관리자 API (ADMIN 권한 필요)
                        .anyRequest().hasRole("USER")
                )
                // 폼 로그인/베이직 인증은 사용하지 않음
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 두어 토큰을 먼저 검증
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() { // CORS 설정
        CorsConfiguration cors = new CorsConfiguration();

        cors.setAllowedOrigins(corsProperties.allowedOrigins()); // 환경별 허용 도메인 설정
        cors.setAllowCredentials(true); // 쿠키 전송 허용 (JWT 사용을 위해 필요)
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*")); // 클라이언트가 요청 때 보낼 수 있는 헤더 목록
        cors.setExposedHeaders(List.of("Set-Cookie", "Location", "Content-Disposition"));
        cors.setMaxAge(600L); // Preflight 요청 캐시 시간 (10분)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
