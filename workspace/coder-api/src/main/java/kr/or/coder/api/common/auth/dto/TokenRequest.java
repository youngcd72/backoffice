package kr.or.coder.api.common.auth.dto;

/**
 * 액세스 토큰 발급 요청 (OAuth2 Client Credentials)
 * @param grantType grant_type (client_credentials 만 지원)
 * @param clientId client_id (KEY_ID)
 * @param clientSecret client_secret
 * @param scope 요청 scope (공백 구분 인터페이스ID, 없으면 허용된 전체)
 * @param clientIp 요청 IP
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
public record TokenRequest(String grantType, String clientId, String clientSecret, String scope, String clientIp) {
}
