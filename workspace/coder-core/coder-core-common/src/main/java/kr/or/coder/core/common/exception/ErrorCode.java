package kr.or.coder.core.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 공통 에러 코드 정의 (HTTP 상태, 에러 코드, 기본 메시지)
 * @author ywkim
 * @since 2026.08.04
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.08.04  ywkim        최초 생성
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "Invalid input value provided."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "HTTP method is not supported."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "Requested resource was not found."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "An unexpected internal server error occurred."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C005", "Authentication failed or token is invalid."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "C006", "Access denied. Insufficient permissions.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
