package kr.or.coder.core.security.config;

import kr.or.coder.core.security.jwt.JwtTokenProvider;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

/**
 * JWT 기반 OAuth2 리소스 서버 보안 설정 (Stateless 세션, 허용 URL, 인증/인가 실패 응답, CORS)
 * @author ywkim
 * @since 2026.09.09
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.09.09  ywkim        최초 생성
 * </pre>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties(OAuth2SecurityProperties.class)
@ConditionalOnProperty(name = "coder.security.enabled", havingValue = "true", matchIfMissing = true)
public class OAuth2SecurityConfig {

    private final OAuth2SecurityProperties securityProperties;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * CSRF/CORS/세션 정책과 URL별 접근 규칙, JWT 인증 및 인증·인가 실패 응답을 설정한 필터 체인을 생성
     * @param http HttpSecurity 설정 빌더
     * @return SecurityFilterChain
     * @throws Exception 보안 설정 구성 중 오류가 발생한 경우
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        if (!securityProperties.isCsrfEnabled()) {
            http.csrf(AbstractHttpConfigurer::disable);
        }

        if (securityProperties.isCorsEnabled()) {
            http.cors(Customizer.withDefaults());
        }

        // Stateless session management for JWT Bearer token authentication
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        List<String> permitUrls = securityProperties.getPermitUrls();
        String[] permitUrlArray = permitUrls.toArray(new String[0]);

        http
            .authorizeHttpRequests(auth -> auth
                // 비동기 응답(SseEmitter 등)의 ASYNC 디스패치는 최초 요청에서 이미 인가했으므로 다시 검사하지 않는다
                // (BearerTokenAuthenticationFilter 가 ASYNC 디스패치를 건너뛰어 익명으로 거부되는 것 방지)
                .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                .requestMatchers(permitUrlArray).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"success\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요하거나 유효하지 않은 JWT 토큰입니다.\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"success\":false,\"code\":\"FORBIDDEN\",\"message\":\"요청한 리소스에 대한 접근 권한이 없습니다.\"}");
                })
            );

        return http.build();
    }

    /**
     * HS256 비밀키로 JWT를 검증하는 디코더를 생성
     * @return JwtDecoder
     */
    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(jwtTokenProvider.getSecretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * JWT의 role, permissions 클레임을 권한(ROLE_ 접두사 역할, 권한 문자열)으로 변환하는 컨버터를 생성
     * @return JwtAuthenticationConverter
     */
    @Bean
    @ConditionalOnMissingBean(JwtAuthenticationConverter.class)
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<GrantedAuthority> authorities = new ArrayList<>();
            String role = jwt.getClaimAsString("role");
            if (role != null && !role.isBlank()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
            }
            List<String> permissions = jwt.getClaimAsStringList("permissions");
            if (permissions != null) {
                for (String perm : permissions) {
                    authorities.add(new SimpleGrantedAuthority(perm));
                }
            }
            return authorities;
        });
        return converter;
    }

    /**
     * 모든 경로에 대해 CORS 정책(전체 Origin 패턴, 주요 HTTP 메서드, 자격증명 허용)을 설정
     * @return CorsConfigurationSource
     */
    @Bean
    @ConditionalOnMissingBean(CorsConfigurationSource.class)
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
