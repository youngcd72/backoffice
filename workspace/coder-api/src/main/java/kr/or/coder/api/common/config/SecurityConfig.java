package kr.or.coder.api.common.config;

import kr.or.coder.api.common.annotation.ApiInterfaceResolver;
import kr.or.coder.api.common.auth.SigningKeyManager;
import kr.or.coder.api.common.client.LinkClientCache;
import kr.or.coder.api.common.exception.ApiErrorCode;
import kr.or.coder.api.common.exception.ApiErrorWriter;
import kr.or.coder.api.common.filter.ApiClientFilter;
import kr.or.coder.api.common.filter.InterfaceAuthFilter;
import kr.or.coder.api.common.filter.IpAllowFilter;
import kr.or.coder.api.common.filter.RateLimitFilter;
import kr.or.coder.api.common.support.ClientIpResolver;
import kr.or.coder.api.common.support.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.time.Clock;

/**
 * 대외 연계 API 보안 설정.
 * - 토큰: KMS 서명키(RS256) 공개키로 서명·만료·발급자 검증 (실패 401 A001)
 * - 토큰 검증 뒤 공통 필터를 순서대로 실행: 연계키 확인(401 A001) → IP 허용(403 A002) → 인터페이스 권한(403 A003) → 호출 한도(429 A004)
 * coder-core-security 의 기본 보안 설정(사용자 JWT HS256)은 coder.security.enabled=false 로 끄고 이 설정을 쓴다.
 * @author ywkim
 * @since 2026.10.27
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.27  ywkim        최초 생성
 * </pre>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** 인증 없이 호출할 수 있는 URL (토큰 발급, JWKS, 헬스체크, API 문서) */
    private static final String[] PERMIT_URLS = {
            "/oauth2/token",
            "/.well-known/jwks.json",
            "/api/ping",
            "/actuator/health",
            "/actuator/health/**",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/error"
    };

    private final ApiProperties apiProperties;
    private final ApiInterfaceResolver apiInterfaceResolver;
    private final LinkClientCache linkClientCache;
    private final ClientIpResolver clientIpResolver;
    private final RateLimiter rateLimiter;
    private final ApiErrorWriter apiErrorWriter;
    private final Clock clock;

    /**
     * Stateless JWT 리소스 서버와 대외 연계 공통 필터를 설정한 필터 체인을 생성
     * @param http HttpSecurity 설정 빌더
     * @param jwtDecoder 액세스 토큰 디코더
     * @return SecurityFilterChain
     * @throws Exception 보안 설정 구성 중 오류가 발생한 경우
     */
    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        AuthenticationEntryPoint entryPoint = (request, response, authException) -> {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            apiErrorWriter.write(request, response, ApiErrorCode.TOKEN_INVALID, authException.getMessage());
        };
        AccessDeniedHandler accessDeniedHandler = (request, response, accessDeniedException) ->
                apiErrorWriter.write(request, response, ApiErrorCode.INTERFACE_NOT_ALLOWED, accessDeniedException.getMessage());

        http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .requestCache(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PERMIT_URLS).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder))
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            // 같은 기준 필터 뒤에 추가한 필터는 추가한 순서대로 실행된다
            .addFilterAfter(new ApiClientFilter(apiInterfaceResolver, linkClientCache, apiErrorWriter, clock),
                    BearerTokenAuthenticationFilter.class)
            .addFilterAfter(new IpAllowFilter(clientIpResolver, apiErrorWriter), BearerTokenAuthenticationFilter.class)
            .addFilterAfter(new InterfaceAuthFilter(apiInterfaceResolver, linkClientCache, apiErrorWriter),
                    BearerTokenAuthenticationFilter.class)
            .addFilterAfter(new RateLimitFilter(rateLimiter, apiErrorWriter), BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    /**
     * KMS 서명키 공개키로 액세스 토큰을 검증하는 디코더를 생성
     * @param signingKeyManager 토큰 서명키 관리
     * @return JwtDecoder
     */
    @Bean
    public JwtDecoder jwtDecoder(SigningKeyManager signingKeyManager) {
        return signingKeyManager.createJwtDecoder(apiProperties.getToken().getIssuer());
    }
}
