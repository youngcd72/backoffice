package kr.or.coder.api.common.auth;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import jakarta.annotation.PostConstruct;
import kr.or.coder.api.common.config.ApiProperties;
import kr.or.coder.core.kms.KmsClient;
import kr.or.coder.core.kms.SigningKeyVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.util.List;

/**
 * 액세스 토큰 서명키(KMS RS256)를 관리한다. 개인키는 KMS 밖으로 나오지 않고 서명만 KMS(KmsClient.sign)에 맡긴다.
 * - 기동 시 서명키 버전이 없으면 새로 만든다 (coder.api.token.signing-key-id)
 * - 토큰 헤더 kid 로 서명키 버전을 표시하고, 검증·JWKS 에는 사용 중인 버전과 직전 버전의 공개키를 쓴다 (서명키 교체 중첩)
 * - 서명키 버전 목록은 coder.api.cache.ttl-seconds 마다 KMS에서 다시 읽는다 (다른 곳에서 교체한 버전 반영)
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
@Component
public class SigningKeyManager {

    /** 검증·JWKS 에 쓰는 서명키 버전 수 (사용 중 + 직전) */
    private static final int ACTIVE_VERSION_COUNT = 2;

    private final KmsClient kmsClient;
    private final String signingKeyId;
    private final long refreshMillis;
    private final Clock clock;

    private volatile List<SigningKeyVersion> versions = List.of();
    private volatile JWKSet jwkSet = new JWKSet();
    private volatile long loadedAt;

    /**
     * KMS 와 토큰 설정으로 생성
     * @param kmsClient KMS 클라이언트
     * @param apiProperties 대외 연계 API 설정
     * @param clock 시간 기준 Clock
     */
    public SigningKeyManager(KmsClient kmsClient, ApiProperties apiProperties, Clock clock) {
        this.kmsClient = kmsClient;
        this.signingKeyId = apiProperties.getToken().getSigningKeyId();
        this.refreshMillis = apiProperties.getCache().getTtlSeconds() * 1000L;
        this.clock = clock;
    }

    /**
     * 서명키 버전을 읽고, 없으면 새 버전을 만든다
     */
    @PostConstruct
    public void init() {
        if (kmsClient.getSigningKeyVersions(signingKeyId).isEmpty()) {
            SigningKeyVersion created = kmsClient.createSigningKeyVersion(signingKeyId);
            log.info("signing key version created: {} (kid={})", created.version(), created.kid());
        }
        reload();
    }

    /**
     * 사용 중인 서명키 버전으로 클레임에 서명해 JWT(RS256) 문자열을 만든다
     * @param claims 토큰 클레임
     * @return 직렬화된 JWT
     */
    public String sign(JWTClaimsSet claims) {
        SigningKeyVersion current = getVersions().get(0);
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(current.kid())
                .type(JOSEObjectType.JWT)
                .build();
        String signingInput = header.toBase64URL() + "." + new Payload(claims.toJSONObject()).toBase64URL();
        byte[] signature = kmsClient.sign(signingKeyId, current.version(), signingInput.getBytes(StandardCharsets.US_ASCII));
        return signingInput + "." + Base64URL.encode(signature);
    }

    /**
     * 사용 중인 버전과 직전 버전의 공개키 JWKS 를 반환
     * @return 공개키 JWKS
     */
    public JWKSet getJwkSet() {
        getVersions();
        return jwkSet;
    }

    /**
     * 이 서명키의 공개키(JWKS)와 발급자로 토큰을 검증하는 디코더를 만든다 (서명, 만료, 발급자 검증)
     * @param issuer 토큰 발급자 (iss)
     * @return JwtDecoder
     */
    public JwtDecoder createJwtDecoder(String issuer) {
        JWKSource<SecurityContext> jwkSource = (selector, context) -> selector.select(getJwkSet());
        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource));
        // 만료·발급자 등 클레임 검증은 Spring Security 검증기(JwtValidators)가 한다
        processor.setJWTClaimsSetVerifier((claims, context) -> {
        });
        NimbusJwtDecoder decoder = new NimbusJwtDecoder(processor);
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    /**
     * 서명키 버전 목록을 반환하고, 다시 읽을 시간이 지났으면 KMS 에서 다시 읽는다
     * @return 최신순 서명키 버전 목록
     */
    private List<SigningKeyVersion> getVersions() {
        if (clock.millis() - loadedAt > refreshMillis) {
            reload();
        }
        return versions;
    }

    /**
     * KMS 에서 서명키 버전 목록을 읽어 JWKS 를 다시 만든다
     * @throws IllegalStateException 서명키 버전이 없는 경우
     */
    private synchronized void reload() {
        List<SigningKeyVersion> loaded = kmsClient.getSigningKeyVersions(signingKeyId);
        if (loaded.isEmpty()) {
            throw new IllegalStateException("no signing key version for " + signingKeyId);
        }
        List<JWK> keys = loaded.stream()
                .limit(ACTIVE_VERSION_COUNT)
                .map(version -> (JWK) new RSAKey.Builder((RSAPublicKey) version.publicKey())
                        .keyID(version.kid())
                        .keyUse(KeyUse.SIGNATURE)
                        .algorithm(JWSAlgorithm.RS256)
                        .build())
                .toList();
        this.versions = List.copyOf(loaded);
        this.jwkSet = new JWKSet(keys);
        this.loadedAt = clock.millis();
    }
}
