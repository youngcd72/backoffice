package kr.or.coder.core.redis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis 설정(coder.redis.*) 프로퍼티
 * @author ywkim
 * @since 2026.08.04
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.08.04  ywkim        최초 생성
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "coder.redis")
public class RedisProperties {

    private String host = "localhost";
    private int port = 6379;
    private String password;
    private int database = 0;
    private int timeout = 3000;

    private Session session = new Session();

    /**
     * Redis HTTP 세션 설정
     */
    @Getter
    @Setter
    public static class Session {
        private boolean enabled = true;
        private int maxInactiveIntervalInSeconds = 1800; // 30 minutes
        private String namespace = "coder:session";
    }
}
