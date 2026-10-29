package kr.or.coder.api.common.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 토큰 발급 실패 응답 (OAuth2 표준 오류 형식, RFC 6749 5.2)
 * @param error 오류 코드 (invalid_request, invalid_client, invalid_scope, unsupported_grant_type)
 * @param errorDescription 오류 설명 (키 존재 여부 등 실패 사유는 구분하지 않는다)
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
public record OAuth2ErrorResponse(@JsonProperty("error") String error,
                                  @JsonProperty("error_description") String errorDescription) {
}
