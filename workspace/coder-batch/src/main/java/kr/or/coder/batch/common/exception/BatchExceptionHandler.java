package kr.or.coder.batch.common.exception;

import kr.or.coder.core.common.exception.ErrorCode;
import kr.or.coder.core.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * core GlobalExceptionHandler보다 먼저 적용되는 배치 전용 예외 처리기.
 * @author ywkim
 * @since 2026.10.09
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.09  ywkim        최초 생성
 * </pre>
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BatchExceptionHandler {

    /**
     * BatchException을 예외에 지정된 HTTP 상태와 오류 응답으로 변환한다.
     * @param e 배치 업무 예외
     * @return 오류코드와 메시지를 담은 오류 응답
     */
    @ExceptionHandler(BatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleBatchException(BatchException e) {
        log.error("BatchException: code={}, message={}", e.getCode(), e.getMessage());
        return ResponseEntity
                .status(e.getStatus())
                .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    /**
     * {@code @PreAuthorize} 거부 시 core의 Exception 핸들러(500)로 넘어가지 않도록 403으로 응답한다.
     * @param e 접근 거부 예외
     * @return 403 FORBIDDEN 오류 응답
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("AccessDeniedException: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.FORBIDDEN.getStatus())
                .body(ApiResponse.error(ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getMessage()));
    }
}
