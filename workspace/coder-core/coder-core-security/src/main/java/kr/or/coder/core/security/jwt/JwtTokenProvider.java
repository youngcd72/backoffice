package kr.or.coder.core.security.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * JWT(HS256) 토큰 발급·검증·클레임 조회 컴포넌트
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
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtProperties.class)
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private byte[] secretKeyBytes;
    private SecretKey secretKey;

    /**
     * 설정된 비밀키를 256비트 이상으로 맞춰(짧으면 SHA-256 해시) 서명키를 초기화
     */
    @PostConstruct
    public void init() {
        String secret = jwtProperties.getSecret();
        byte[] rawBytes = secret.getBytes(StandardCharsets.UTF_8);

        // Ensure key is at least 256 bits (32 bytes) for HS256
        if (rawBytes.length < 32) {
            try {
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                this.secretKeyBytes = sha256.digest(rawBytes);
            } catch (Exception e) {
                log.error("[JwtTokenProvider] Failed to hash secret key to 256 bits", e);
                this.secretKeyBytes = rawBytes;
            }
        } else {
            this.secretKeyBytes = rawBytes;
        }

        this.secretKey = new SecretKeySpec(this.secretKeyBytes, "HmacSHA256");
        log.info("[JwtTokenProvider] Initialized JWT Token Provider with issuer: {}, expiration: {}s",
                jwtProperties.getIssuer(), jwtProperties.getAccessTokenExpiration());
    }

    /**
     * HMAC-SHA256 서명키를 반환 (초기화 전이면 초기화)
     * @return 서명키
     */
    public SecretKey getSecretKey() {
        if (this.secretKey == null) {
            init();
        }
        return this.secretKey;
    }

    /**
     * JWT 설정 객체를 반환
     * @return JWT 설정
     */
    public JwtProperties getProperties() {
        return this.jwtProperties;
    }

    /**
     * 사용자 정보로 서명된 JWT 액세스 토큰을 발급
     * @param username 사용자 아이디 (subject)
     * @param name 사용자 이름 (null이면 클레임 생략)
     * @param role 역할 (null이면 클레임 생략)
     * @param permissions 권한 목록 (null 또는 빈 목록이면 클레임 생략)
     * @param email 이메일 (null이면 클레임 생략)
     * @return 직렬화된 JWT 문자열
     * @throws RuntimeException 토큰 서명/생성에 실패한 경우
     */
    public String generateToken(String username, String name, String role, List<String> permissions, String email) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + (jwtProperties.getAccessTokenExpiration() * 1000L));

        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .subject(username)
                .issuer(jwtProperties.getIssuer())
                .issueTime(now)
                .expirationTime(validity);

        if (name != null) {
            claimsBuilder.claim("name", name);
        }
        if (role != null) {
            claimsBuilder.claim("role", role);
        }
        if (permissions != null && !permissions.isEmpty()) {
            claimsBuilder.claim("permissions", permissions);
        }
        if (email != null) {
            claimsBuilder.claim("email", email);
        }

        try {
            JWSSigner signer = new MACSigner(this.secretKeyBytes);
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsBuilder.build());
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            log.error("[JwtTokenProvider] Failed to generate JWT token for user: {}", username, e);
            throw new RuntimeException("Could not generate authentication token", e);
        }
    }

    /**
     * JWT의 서명과 만료 여부를 검증
     * @param token JWT 문자열
     * @return 유효하면 true, 아니면 false
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(this.secretKeyBytes);

            if (!signedJWT.verify(verifier)) {
                log.warn("[JwtTokenProvider] JWT token signature verification failed");
                return false;
            }

            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (expirationTime != null && expirationTime.before(new Date())) {
                log.warn("[JwtTokenProvider] JWT token is expired");
                return false;
            }

            return true;
        } catch (Exception e) {
            log.warn("[JwtTokenProvider] Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * JWT에서 사용자 아이디(subject)를 추출
     * @param token JWT 문자열
     * @return 사용자 아이디 (파싱 실패 시 null)
     */
    public String getUsername(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getSubject();
        } catch (Exception e) {
            log.warn("[JwtTokenProvider] Failed to extract username from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JWT의 전체 클레임을 추출
     * @param token JWT 문자열
     * @return 클레임 맵 (파싱 실패 시 빈 맵)
     */
    public Map<String, Object> getClaims(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getClaims();
        } catch (Exception e) {
            log.warn("[JwtTokenProvider] Failed to extract claims from token: {}", e.getMessage());
            return Map.of();
        }
    }
}
