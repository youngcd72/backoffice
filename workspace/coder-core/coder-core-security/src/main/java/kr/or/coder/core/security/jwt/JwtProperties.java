package kr.or.coder.core.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 설정(coder.security.jwt.*) 프로퍼티 (서명키, 만료시간, 발급자, 헤더)
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
@ConfigurationProperties(prefix = "coder.security.jwt")
public class JwtProperties {

    /**
     * Secret key for HMAC-SHA256 signing (minimum 256-bit / 32 bytes).
     */
    private String secret = "coder-enterprise-super-secure-jwt-signing-key-256bit-secret-value!";

    /**
     * Access token expiration time in seconds (default: 86400 = 24 hours).
     */
    private long accessTokenExpiration = 86400L;

    /**
     * Token issuer identifier.
     */
    private String issuer = "coder-auth";

    /**
     * HTTP Header name (default: Authorization).
     */
    private String header = "Authorization";

    /**
     * Token prefix (default: Bearer ).
     */
    private String tokenPrefix = "Bearer ";
}
