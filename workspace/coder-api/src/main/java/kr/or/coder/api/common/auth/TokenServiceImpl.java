package kr.or.coder.api.common.auth;

import com.nimbusds.jwt.JWTClaimsSet;
import kr.or.coder.api.common.auth.dto.TokenRequest;
import kr.or.coder.api.common.auth.dto.TokenResponse;
import kr.or.coder.api.common.client.LinkClient;
import kr.or.coder.api.common.client.LinkClientCache;
import kr.or.coder.api.common.config.ApiProperties;
import kr.or.coder.api.common.support.ClientIpResolver;
import kr.or.coder.core.kms.KmsClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * 액세스 토큰 발급 Service 구현.
 * 검증 순서: 잠금 → 키 존재 → 키 상태·만료 → 연계기관 사용 여부·사용기간 → 허용 IP → 비밀키 일치(KMS 복호화 후 상수 시간 비교) → scope.
 * 클라이언트 인증 실패는 사유를 구분하지 않고 invalid_client 로 응답하고, 사유는 WARN 로그로만 남긴다.
 * 비밀키를 연속으로 틀리면(coder.api.token.max-secret-fail-count) 일정 시간 키를 막는다 (Redis api:tkn:fail:{KEY_ID}).
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
@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    private static final String GRANT_TYPE = "client_credentials";
    private static final String TOKEN_TYPE = "Bearer";
    private static final String FAIL_KEY_PREFIX = "api:tkn:fail:";

    private final LinkClientCache linkClientCache;
    private final SigningKeyManager signingKeyManager;
    private final KmsClient kmsClient;
    private final StringRedisTemplate redisTemplate;
    private final ApiProperties.Token properties;
    private final Clock clock;

    /**
     * 토큰 발급에 필요한 구성 요소로 생성
     * @param linkClientCache 연계 정보 캐시
     * @param signingKeyManager 토큰 서명키 관리
     * @param kmsClient KMS 클라이언트 (비밀키 복호화)
     * @param redisTemplate 문자열 RedisTemplate (비밀키 실패 횟수)
     * @param apiProperties 대외 연계 API 설정
     * @param clock 시간 기준 Clock
     */
    public TokenServiceImpl(LinkClientCache linkClientCache, SigningKeyManager signingKeyManager, KmsClient kmsClient,
                            StringRedisTemplate redisTemplate, ApiProperties apiProperties, Clock clock) {
        this.linkClientCache = linkClientCache;
        this.signingKeyManager = signingKeyManager;
        this.kmsClient = kmsClient;
        this.redisTemplate = redisTemplate;
        this.properties = apiProperties.getToken();
        this.clock = clock;
    }

    /**
     * 클라이언트 키를 검증하고 KMS 로 서명한 액세스 토큰을 발급
     * @param request 토큰 발급 요청
     * @return 액세스 토큰 응답
     * @throws OAuth2TokenException 요청이 잘못되었거나 클라이언트 인증·scope 검증에 실패한 경우
     */
    @Override
    public TokenResponse issueToken(TokenRequest request) {
        if (!StringUtils.hasText(request.grantType())) {
            throw OAuth2TokenException.invalidRequest("grant_type is required.");
        }
        if (!GRANT_TYPE.equals(request.grantType())) {
            throw new OAuth2TokenException("unsupported_grant_type", "Only client_credentials is supported.",
                    "unsupported grant_type " + request.grantType());
        }
        if (!StringUtils.hasText(request.clientId()) || !StringUtils.hasText(request.clientSecret())) {
            throw OAuth2TokenException.invalidRequest("client_id and client_secret are required.");
        }

        LinkClient client = authenticate(request);
        String scope = String.join(" ", getGrantedScopes(client, request.scope()));

        Instant now = clock.instant();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(properties.getIssuer())
                .subject(client.getKeyId())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(properties.getExpiresIn())))
                .jwtID(UUID.randomUUID().toString())
                .claim(TokenClaims.ORG, client.getLnkOrgId())
                .claim(TokenClaims.CHNL, client.getChnlClsCd())
                .claim(TokenClaims.SCOPE, scope)
                .build();
        String accessToken = signingKeyManager.sign(claims);
        log.info("access token issued keyId={} org={} scope=[{}]", client.getKeyId(), client.getLnkOrgId(), scope);
        return new TokenResponse(accessToken, TOKEN_TYPE, properties.getExpiresIn(), scope);
    }

    /**
     * 클라이언트 키를 검증하고 연계키 정보를 반환 (성공하면 비밀키 실패 횟수를 지운다)
     * @param request 토큰 발급 요청
     * @return 연계키 정보
     * @throws OAuth2TokenException 클라이언트 인증에 실패한 경우 (invalid_client)
     */
    private LinkClient authenticate(TokenRequest request) {
        String keyId = request.clientId();
        if (isLocked(keyId)) {
            throw OAuth2TokenException.invalidClient("client key is locked by secret failures: " + keyId);
        }
        LinkClient client = linkClientCache.getClient(keyId);
        if (client == null) {
            throw OAuth2TokenException.invalidClient("client key not found: " + keyId);
        }
        if (!client.isUsable(LocalDateTime.now(clock))) {
            throw OAuth2TokenException.invalidClient("client key is not usable: " + keyId);
        }
        if (!ClientIpResolver.isAllowed(client.getAllwIpAddr(), request.clientIp())) {
            throw OAuth2TokenException.invalidClient("ip not allowed: " + keyId + " " + request.clientIp());
        }
        byte[] expected = kmsClient.decrypt(client.getEncKeyVal());
        byte[] actual = request.clientSecret().getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            recordSecretFailure(keyId);
            throw OAuth2TokenException.invalidClient("client secret mismatch: " + keyId);
        }
        clearSecretFailure(keyId);
        return client;
    }

    /**
     * 토큰에 넣을 scope 를 정한다 (요청 scope 가 있으면 허용 범위 안에서 좁히고, 없으면 허용된 전체)
     * @param client 연계키 정보
     * @param requestedScope 요청 scope (공백 구분)
     * @return 정렬된 허용 인터페이스ID 집합
     * @throws OAuth2TokenException 허용되지 않은 인터페이스를 요청한 경우 (invalid_scope)
     */
    private Set<String> getGrantedScopes(LinkClient client, String requestedScope) {
        Set<String> allowed = linkClientCache.getAllowedInfcIds(client.getLnkOrgId());
        Set<String> requested = TokenClaims.parseScopes(requestedScope);
        if (requested.isEmpty()) {
            return new TreeSet<>(allowed);
        }
        if (!allowed.containsAll(requested)) {
            throw new OAuth2TokenException("invalid_scope", "The requested scope is not allowed.",
                    "scope not allowed: " + client.getKeyId() + " " + requested);
        }
        return new TreeSet<>(requested);
    }

    /**
     * 비밀키 연속 실패로 키가 막혔는지 확인 (Redis 장애 시 막지 않는다)
     * @param keyId 키ID
     * @return 막혔으면 true
     */
    private boolean isLocked(String keyId) {
        try {
            String count = redisTemplate.opsForValue().get(FAIL_KEY_PREFIX + keyId);
            return count != null && Long.parseLong(count) >= properties.getMaxSecretFailCount();
        } catch (RuntimeException e) {
            log.warn("failed to read secret failure count: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 비밀키 실패 횟수를 늘린다. 처음 실패하면 잠금 시간만큼 TTL 을 걸고(그 안의 실패만 센다),
     * 잠금 횟수에 이르면 그때부터 잠금 시간 동안 막히도록 TTL 을 다시 건다.
     * @param keyId 키ID
     */
    private void recordSecretFailure(String keyId) {
        try {
            String key = FAIL_KEY_PREFIX + keyId;
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && (count == 1 || count == properties.getMaxSecretFailCount())) {
                redisTemplate.expire(key, Duration.ofMinutes(properties.getSecretFailLockMinutes()));
            }
            if (count != null && count == properties.getMaxSecretFailCount()) {
                log.warn("client key locked for {} minutes by secret failures: {}", properties.getSecretFailLockMinutes(), keyId);
            }
        } catch (RuntimeException e) {
            log.warn("failed to record secret failure: {}", e.getMessage());
        }
    }

    /**
     * 비밀키 실패 횟수를 지운다
     * @param keyId 키ID
     */
    private void clearSecretFailure(String keyId) {
        try {
            redisTemplate.delete(FAIL_KEY_PREFIX + keyId);
        } catch (RuntimeException e) {
            log.warn("failed to clear secret failure count: {}", e.getMessage());
        }
    }
}
