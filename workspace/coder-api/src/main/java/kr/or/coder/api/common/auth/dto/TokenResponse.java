package kr.or.coder.api.common.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 액세스 토큰 발급 응답 (OAuth2 표준 형식, ApiResponse 로 감싸지 않음)
 * @param accessToken 액세스 토큰 (JWT RS256)
 * @param tokenType 토큰 유형 (Bearer)
 * @param expiresIn 만료까지 남은 시간 (초)
 * @param scope 허용 인터페이스ID (공백 구분)
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
public record TokenResponse(@JsonProperty("access_token") String accessToken,
                            @JsonProperty("token_type") String tokenType,
                            @JsonProperty("expires_in") long expiresIn,
                            @JsonProperty("scope") String scope) {
}
