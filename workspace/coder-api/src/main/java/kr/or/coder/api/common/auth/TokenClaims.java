package kr.or.coder.api.common.auth;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 액세스 토큰 클레임 이름과 조회 유틸 (sub=KEY_ID, org=연계기관ID, chnl=채널, scope=허용 인터페이스ID 공백 구분)
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
public final class TokenClaims {

    /** 연계기관ID */
    public static final String ORG = "org";
    /** 채널구분코드 */
    public static final String CHNL = "chnl";
    /** 허용 인터페이스ID (공백 구분, OAuth2 scope) */
    public static final String SCOPE = "scope";

    private TokenClaims() {
    }

    /**
     * 공백으로 구분한 scope 문자열을 집합으로 바꾼다
     * @param scope scope 문자열
     * @return scope 집합 (없으면 빈 집합)
     */
    public static Set<String> parseScopes(String scope) {
        if (!StringUtils.hasText(scope)) {
            return Set.of();
        }
        return Arrays.stream(scope.trim().split("\\s+")).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * 토큰의 scope 클레임을 집합으로 반환
     * @param jwt 검증된 토큰
     * @return 허용 인터페이스ID 집합
     */
    public static Set<String> getScopes(Jwt jwt) {
        return parseScopes(jwt.getClaimAsString(SCOPE));
    }
}
