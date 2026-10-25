package kr.or.coder.core.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * API 공통 응답 래퍼 (성공 여부, 코드, 메시지, 데이터, 시각)
 * @param <T> 응답 데이터 타입
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
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;

    /**
     * 기본 메시지로 성공 응답을 생성
     * @param data 응답 데이터
     * @return 성공 응답
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("SUCCESS")
                .message("Operation completed successfully.")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 지정한 메시지로 성공 응답을 생성
     * @param message 응답 메시지
     * @param data 응답 데이터
     * @return 성공 응답
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("SUCCESS")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 에러 응답을 생성
     * @param code 에러 코드
     * @param message 에러 메시지
     * @return 실패 응답
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 상세 정보를 포함한 에러 응답을 생성
     * @param code 에러 코드
     * @param message 에러 메시지
     * @param errorDetails 에러 상세 정보 (예: 필드별 오류)
     * @return 실패 응답
     */
    public static <T> ApiResponse<T> error(String code, String message, T errorDetails) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .data(errorDetails)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
