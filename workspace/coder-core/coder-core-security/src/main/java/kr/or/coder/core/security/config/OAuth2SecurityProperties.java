package kr.or.coder.core.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 보안 설정(coder.security.*) 프로퍼티 (인증 제외 URL, CSRF/CORS 사용 여부)
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
@Getter
@Setter
@ConfigurationProperties(prefix = "coder.security")
public class OAuth2SecurityProperties {

    private boolean enabled = true;
    private List<String> permitUrls = new ArrayList<>(List.of(
            "/",
            "/api/ping",
            "/api/v1/auth/login",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/actuator/**",
            "/error"
    ));
    private boolean csrfEnabled = false;
    private boolean corsEnabled = true;
}
