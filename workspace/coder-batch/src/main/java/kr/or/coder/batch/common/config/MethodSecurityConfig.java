package kr.or.coder.batch.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * 배치 관리 API의 관리자 권한 검사(@PreAuthorize)를 활성화한다.
 * SecurityFilterChain / JWT 검증은 coder-core-security의 OAuth2SecurityConfig를 그대로 사용한다.
 * @author ywkim
 * @since 2026.10.09
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.09  ywkim        최초 생성
 * </pre>
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
