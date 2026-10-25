package kr.or.coder.core.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Swagger 설정(coder.swagger.*) 프로퍼티
 * @author ywkim
 * @since 2026.08.06
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.08.06  ywkim        최초 생성
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "coder.swagger")
public class SwaggerProperties {

    private boolean enabled = true;
    private String title = "CODER Framework API";
    private String description = "CODER Core Framework Standard RESTful API Specification";
    private String version = "v1.0.0";
    private OAuth2Properties oauth2 = new OAuth2Properties();

    /**
     * Swagger UI의 OAuth2 인증 설정
     */
    @Getter
    @Setter
    public static class OAuth2Properties {
        private boolean enabled = true;
        private String authorizationUrl = "/oauth2/authorization";
        private String tokenUrl = "/oauth2/token";
        private String clientId = "coder-client";
        private String clientSecret;
        private String scope = "read write";
    }
}
