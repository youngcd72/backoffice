package kr.or.coder.api.common.auth;

import com.nimbusds.jwt.JWTClaimsSet;
import kr.or.coder.api.common.config.ApiProperties;
import kr.or.coder.core.kms.local.LocalKmsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SigningKeyManagerTest {

    private static final String ISSUER = "coder-api";

    private LocalKmsClient kmsClient;
    private ApiProperties properties;
    private SigningKeyManager manager;

    @BeforeEach
    void setUp() {
        kmsClient = new LocalKmsClient(null, Clock.systemUTC());
        properties = new ApiProperties();
        properties.getCache().setTtlSeconds(0);
        manager = new SigningKeyManager(kmsClient, properties, Clock.systemUTC());
        manager.init();
    }

    private JWTClaimsSet claims(String issuer, Instant expiresAt) {
        return new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("ck0001")
                .issueTime(new Date())
                .expirationTime(Date.from(expiresAt))
                .claim(TokenClaims.ORG, "LOCAL01")
                .claim(TokenClaims.SCOPE, "IFSY0001 IFSY0002")
                .build();
    }

    @Test
    void signedTokenIsVerifiedByDecoder() {
        String token = manager.sign(claims(ISSUER, Instant.now().plusSeconds(60)));

        Jwt jwt = manager.createJwtDecoder(ISSUER).decode(token);

        assertThat(jwt.getSubject()).isEqualTo("ck0001");
        assertThat(jwt.getHeaders()).containsEntry("alg", "RS256");
        assertThat(jwt.getHeaders().get("kid")).isEqualTo(kmsClient.getSigningKeyVersions("coder-api-token").get(0).kid());
        assertThat(TokenClaims.getScopes(jwt)).containsExactlyInAnyOrder("IFSY0001", "IFSY0002");
    }

    @Test
    void rejectsTamperedExpiredAndForeignIssuerTokens() {
        JwtDecoder decoder = manager.createJwtDecoder(ISSUER);
        String token = manager.sign(claims(ISSUER, Instant.now().plusSeconds(60)));
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + manager.sign(claims(ISSUER, Instant.now().plusSeconds(999))).split("\\.")[1] + "." + parts[2];

        assertThatThrownBy(() -> decoder.decode(tampered)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decoder.decode(manager.sign(claims(ISSUER, Instant.now().minusSeconds(3600)))))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decoder.decode(manager.sign(claims("someone-else", Instant.now().plusSeconds(60)))))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void previousVersionStillVerifiesAfterRotation() {
        String oldToken = manager.sign(claims(ISSUER, Instant.now().plusSeconds(60)));
        kmsClient.createSigningKeyVersion("coder-api-token");
        String newToken = manager.sign(claims(ISSUER, Instant.now().plusSeconds(60)));
        JwtDecoder decoder = manager.createJwtDecoder(ISSUER);

        assertThat(decoder.decode(oldToken).getSubject()).isEqualTo("ck0001");
        assertThat(decoder.decode(newToken).getHeaders().get("kid")).isNotEqualTo(decoder.decode(oldToken).getHeaders().get("kid"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keys = (List<Map<String, Object>>) manager.getJwkSet().toJSONObject(true).get("keys");
        assertThat(keys).hasSize(2).allSatisfy(key -> assertThat(key).doesNotContainKey("d"));
    }
}
