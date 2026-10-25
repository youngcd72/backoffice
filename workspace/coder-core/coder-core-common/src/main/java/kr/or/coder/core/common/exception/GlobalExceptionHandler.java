package kr.or.coder.core.common.exception;

import kr.or.coder.core.common.response.ApiResponse;
import kr.or.coder.core.common.util.MessageUtil;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리 핸들러 (예외를 공통 ApiResponse 형식으로 변환)
 * 업무 예외·잘못된 요청처럼 처리 흐름상 예상된 상황은 WARN, 처리되지 않은 예외만 ERROR(에러 로그 파일)로 남긴다.
 * @author ywkim
 * @since 2026.09.14
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.09.14  ywkim        최초 생성
 * </pre>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 업무 예외 중 400 이 아닌 상태로 응답하는 메시지 코드 */
    private static final Map<String, HttpStatus> BUSINESS_STATUS_MAP = Map.of(
            "cm.error.forbidden", HttpStatus.FORBIDDEN,
            "cm.error.entity.not.found", HttpStatus.NOT_FOUND
    );

    /**
     * 업무 예외를 응답으로 변환 (기본 400, 권한 없음 403, 대상 없음 404)
     * @param e 업무 예외
     * @return 에러 코드와 메시지를 담은 응답
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        logWarn(e.getCode(), e.getMessage());
        return ResponseEntity
                .status(BUSINESS_STATUS_MAP.getOrDefault(e.getCode(), HttpStatus.BAD_REQUEST))
                .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    /**
     * 요청 본문 검증 실패를 필드별 오류 목록과 함께 응답으로 변환
     * @param e 검증 실패 예외
     * @return 필드별 오류 메시지를 담은 INVALID_INPUT_VALUE 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        logWarn(ErrorCode.INVALID_INPUT_VALUE.getCode(), "입력값 검증 실패 " + errors);
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE.getCode(), ErrorCode.INVALID_INPUT_VALUE.getMessage(), errors));
    }

    /**
     * 요청 형식 오류(본문 해석 실패, 파라미터 누락·형식 불일치, 메서드 파라미터 검증 실패)를 400 응답으로 변환
     * @param e 요청 형식 오류 예외
     * @return INVALID_INPUT_VALUE 응답
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class, HandlerMethodValidationException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequestException(Exception e) {
        logWarn(ErrorCode.INVALID_INPUT_VALUE.getCode(), "잘못된 요청 " + e.getClass().getSimpleName() + ": " + e.getMessage());
        return errorResponse(ErrorCode.INVALID_INPUT_VALUE, "cm.error.invalid.input");
    }

    /**
     * 없는 경로 요청을 404 응답으로 변환
     * @param e 경로 없음 예외
     * @return ENTITY_NOT_FOUND 응답
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        logWarn(ErrorCode.ENTITY_NOT_FOUND.getCode(), "없는 경로 " + e.getHttpMethod() + " /" + e.getResourcePath());
        return errorResponse(ErrorCode.ENTITY_NOT_FOUND, "cm.error.entity.not.found");
    }

    /**
     * 지원하지 않는 HTTP 메서드 요청을 405 응답으로 변환
     * @param e 메서드 미지원 예외
     * @return METHOD_NOT_ALLOWED 응답
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        logWarn(ErrorCode.METHOD_NOT_ALLOWED.getCode(), e.getMessage());
        return errorResponse(ErrorCode.METHOD_NOT_ALLOWED, "cm.error.method.not.allowed");
    }

    /**
     * 처리되지 않은 모든 예외를 500 응답으로 변환
     * @param e 처리되지 않은 예외
     * @return INTERNAL_SERVER_ERROR 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        // 에러내용은 가장 안쪽 원인 메시지 (SQL 오류 등), 전체 내역은 스택트레이스로 남긴다
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(e);
        String errMsg = cause.getClass().getSimpleName() + ": " + cause.getMessage();
        logError(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), errMsg, e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }

    /**
     * 에러코드의 HTTP 상태와 메시지 코드의 다국어 메시지로 오류 응답을 만든다 (메시지가 없으면 에러코드 기본 메시지)
     * @param errorCode 에러코드
     * @param messageCode 메시지 코드
     * @return 오류 응답
     */
    private ResponseEntity<ApiResponse<Void>> errorResponse(ErrorCode errorCode, String messageCode) {
        String message;
        try {
            message = MessageUtil.getMessage(messageCode);
        } catch (NoSuchMessageException e) {
            message = errorCode.getMessage();
        }
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getCode(), message));
    }

    /**
     * 에러코드를 MDC errCd 에 담아 WARN 로그를 남긴다 (에러 로그 파일에는 남지 않는다)
     * @param errCd 에러 코드
     * @param errMsg 에러 내용
     */
    private void logWarn(String errCd, String errMsg) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable("errCd", errCd)) {
            log.warn(errMsg);
        }
    }

    /**
     * 에러코드를 MDC errCd 에 담아 ERROR 로그를 남긴다 (로그를 남긴 뒤 errCd 는 지운다)
     * @param errCd 에러 코드
     * @param errMsg 에러 내용
     * @param e 스택트레이스를 남길 예외
     */
    private void logError(String errCd, String errMsg, Throwable e) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable("errCd", errCd)) {
            log.error(errMsg, e);
        }
    }
}
