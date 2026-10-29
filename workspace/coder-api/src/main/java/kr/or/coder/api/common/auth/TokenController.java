package kr.or.coder.api.common.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import kr.or.coder.api.common.auth.dto.TokenRequest;
import kr.or.coder.api.common.auth.dto.TokenResponse;
import kr.or.coder.api.common.support.ClientIpResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * 액세스 토큰 발급(OAuth2 Client Credentials)과 토큰 서명 공개키(JWKS) Controller
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
@RestController
@RequiredArgsConstructor
@Tag(name = "Auth", description = "액세스 토큰 발급 (OAuth2 Client Credentials) / JWKS")
public class TokenController {

    private static final String BASIC_PREFIX = "Basic ";

    private final TokenService tokenService;
    private final SigningKeyManager signingKeyManager;
    private final ClientIpResolver clientIpResolver;

    /**
     * 클라이언트 키(client_id/client_secret)를 검증하고 액세스 토큰을 발급한다.
     * 클라이언트 인증은 본문(client_secret_post) 또는 Authorization: Basic(client_secret_basic) 중 하나로 보낸다.
     * @param grantType grant_type (client_credentials)
     * @param clientId client_id (본문)
     * @param clientSecret client_secret (본문)
     * @param scope 요청 scope (공백 구분 인터페이스ID, 선택)
     * @param authorization Authorization 헤더 (Basic, 선택)
     * @param request HTTP 요청
     * @return 액세스 토큰 응답 (Cache-Control: no-store)
     * @throws OAuth2TokenException 요청이 잘못되었거나 클라이언트 인증·scope 검증에 실패한 경우
     */
    @Operation(summary = "액세스 토큰 발급", description = "application/x-www-form-urlencoded 로 grant_type=client_credentials, "
            + "client_id, client_secret, scope(선택)를 보낸다. 실패 시 OAuth2 표준 오류(invalid_client 401 등).")
    @SecurityRequirements
    @PostMapping("/oauth2/token")
    public ResponseEntity<TokenResponse> issueToken(@RequestParam(name = "grant_type", required = false) String grantType,
                                                    @RequestParam(name = "client_id", required = false) String clientId,
                                                    @RequestParam(name = "client_secret", required = false) String clientSecret,
                                                    @RequestParam(name = "scope", required = false) String scope,
                                                    @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                                    HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (queryString != null && queryString.contains("client_secret")) {
            throw OAuth2TokenException.invalidRequest("client_secret must not be sent in the query string.");
        }
        if (authorization != null && authorization.startsWith(BASIC_PREFIX)) {
            if (StringUtils.hasText(clientId) || StringUtils.hasText(clientSecret)) {
                throw OAuth2TokenException.invalidRequest("Use only one client authentication method.");
            }
            String[] credentials = decodeBasic(authorization.substring(BASIC_PREFIX.length()));
            clientId = credentials[0];
            clientSecret = credentials[1];
        }

        TokenResponse response = tokenService.issueToken(
                new TokenRequest(grantType, clientId, clientSecret, scope, clientIpResolver.resolve(request)));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(response);
    }

    /**
     * 토큰 서명 공개키(사용 중인 버전과 직전 버전)를 JWKS 형식으로 조회한다.
     * @return JWKS ({"keys":[...]})
     */
    @Operation(summary = "토큰 서명 공개키 (JWKS)")
    @SecurityRequirements
    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> getJwks() {
        return signingKeyManager.getJwkSet().toJSONObject(true);
    }

    /**
     * Basic 인증 값을 client_id, client_secret 으로 나눈다 (각각 form-urlencoded, RFC 6749 2.3.1)
     * @param encoded Base64 인코딩된 "client_id:client_secret"
     * @return [client_id, client_secret]
     * @throws OAuth2TokenException 형식이 잘못된 경우
     */
    private String[] decodeBasic(String encoded) {
        try {
            String decoded = new String(Base64.getDecoder().decode(encoded.trim()), StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            if (colon < 0) {
                throw OAuth2TokenException.invalidRequest("Invalid basic authorization header.");
            }
            return new String[]{
                    URLDecoder.decode(decoded.substring(0, colon), StandardCharsets.UTF_8),
                    URLDecoder.decode(decoded.substring(colon + 1), StandardCharsets.UTF_8)
            };
        } catch (IllegalArgumentException e) {
            throw OAuth2TokenException.invalidRequest("Invalid basic authorization header.");
        }
    }
}
