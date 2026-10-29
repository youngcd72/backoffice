package kr.or.coder.api.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 대외 연계 API 설정(coder.api.*) 프로퍼티 (토큰, 신뢰 프록시, 호출 한도, 호출 이력, 캐시, 키 발급)
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
@Getter
@Setter
@ConfigurationProperties(prefix = "coder.api")
public class ApiProperties {

    /** 이력 테이블 FIRS_REGR_ID / FINA_REGR_ID 에 넣는 시스템 사용자ID */
    private String systemUserId = "API";

    /** @ApiInterface 선언을 강제하는 업무 API 패키지 */
    private String bizBasePackage = "kr.or.coder.api.biz";

    /** X-Forwarded-For 를 믿을 프록시 IP/CIDR 목록 (비어 있으면 X-Forwarded-For 를 쓰지 않는다) */
    private List<String> trustedProxies = new ArrayList<>();

    private Token token = new Token();
    private RateLimit rateLimit = new RateLimit();
    private CallLog callLog = new CallLog();
    private Cache cache = new Cache();
    private KeyIssue keyIssue = new KeyIssue();

    /**
     * 액세스 토큰 발급·서명 설정
     */
    @Getter
    @Setter
    public static class Token {
        /** 토큰 발급자 (iss) */
        private String issuer = "coder-api";
        /** 토큰 만료 시간 (초) */
        private long expiresIn = 3600;
        /** KMS 서명키 ID (RS256) */
        private String signingKeyId = "coder-api-token";
        /** 클라이언트 비밀키를 암호화하는 KMS KEK ID */
        private String clientSecretKekId = "coder-api-client-secret";
        /** 비밀키를 연속으로 틀리면 키를 막는 횟수 */
        private int maxSecretFailCount = 5;
        /** 비밀키 연속 실패 시 키를 막는 시간 (분) */
        private int secretFailLockMinutes = 10;
    }

    /**
     * 호출 한도 설정
     */
    @Getter
    @Setter
    public static class RateLimit {
        /** Redis 장애 시 동작: allow(허용) / deny(거부, 503) */
        private String redisFailureMode = "allow";
        /** 동시 요청 카운터 TTL (초). 요청이 비정상 종료되어 감소하지 못한 카운터를 정리한다 */
        private int simuTtlSeconds = 60;
    }

    /**
     * 호출 이력 저장 설정
     */
    @Getter
    @Setter
    public static class CallLog {
        /** 메모리 큐 크기 (가득 차면 이력을 버린다) */
        private int queueCapacity = 10000;
        /** 한 번에 INSERT 하는 최대 건수 */
        private int batchSize = 500;
        /** 큐를 비우는 간격 (밀리초) */
        private long flushIntervalMs = 1000;
        /** 요청·응답 본문 최대 저장 길이 (문자) */
        private int bodyMaxLength = 4000;
    }

    /**
     * 연계키·인터페이스·권한 캐시 설정
     */
    @Getter
    @Setter
    public static class Cache {
        /** 캐시 유지 시간 (초) */
        private long ttlSeconds = 300;
        /** coder-backend 가 연계 정보를 바꾸면 발행하는 캐시 삭제 Redis 채널 */
        private String evictChannel = "coder:api:cache-evict";
    }

    /**
     * 기동 시 클라이언트 키 발급 (ClientKeyIssueRunner, 관리 화면 전 임시 발급 수단)
     */
    @Getter
    @Setter
    public static class KeyIssue {
        /** 키를 발급할 연계기관ID (값이 있을 때만 발급 후 종료) */
        private String lnkOrgId;
        /** 키 유효 기간 (일, 0 이하면 만료 없음) */
        private int expireDays = 365;
    }
}
