package kr.or.coder.api.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 대외 연계 공통 필터(인증·IP·인터페이스 권한·호출 한도) 오류 코드
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
@RequiredArgsConstructor
public enum ApiErrorCode {

    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "A001", "sy.api.token.invalid"),
    IP_NOT_ALLOWED(HttpStatus.FORBIDDEN, "A002", "sy.api.ip.not.allowed"),
    INTERFACE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "A003", "sy.api.interface.not.allowed"),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "A004", "sy.api.rate.limit.exceeded"),
    RATE_LIMIT_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "A005", "sy.api.rate.limit.unavailable");

    private final HttpStatus status;
    private final String code;
    private final String messageKey;
}
