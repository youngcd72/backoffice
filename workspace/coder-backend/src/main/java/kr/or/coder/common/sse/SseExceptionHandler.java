package kr.or.coder.common.sse;

import kr.or.coder.core.common.exception.GlobalExceptionHandler;
import kr.or.coder.core.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.io.IOException;

/**
 * SSE(비동기 요청) 연결 종료 예외 처리
 * 브라우저가 탭을 닫거나 연결 시간이 지나 끊기는 것은 예상된 상황이므로, core 의 GlobalExceptionHandler 가
 * ERROR 로그를 남기고 이미 응답 중인 text/event-stream 에 JSON 을 쓰려고 하지 않도록 먼저 처리한다.
 * @author ywkim
 * @since 2026.10.17
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.17  ywkim        최초 생성
 * </pre>
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class SseExceptionHandler {

    private final GlobalExceptionHandler globalExceptionHandler;

    /**
     * 클라이언트 연결이 끊겨 응답을 쓸 수 없는 경우 (탭 닫기, 네트워크 끊김)
     * @param e 비동기 응답 사용 불가 예외
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsable(AsyncRequestNotUsableException e) {
        log.debug("비동기 응답 연결 끊김: {}", e.getMessage());
    }

    /**
     * 입출력 예외 처리: SSE 응답 중이면 연결이 끊긴 것이므로(heartbeat 전송 실패 등) DEBUG 로만 남기고,
     * 그 외 요청은 core 의 GlobalExceptionHandler 와 같은 500 응답으로 처리
     * @param e 입출력 예외
     * @param response 현재 응답 (Content-Type 으로 SSE 여부 판단)
     * @return SSE 이면 null (응답 완료), 그 외에는 INTERNAL_SERVER_ERROR 응답
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiResponse<Void>> handleIOException(IOException e, HttpServletResponse response) {
        String contentType = response.getContentType();
        if (contentType != null && contentType.startsWith(MediaType.TEXT_EVENT_STREAM_VALUE)) {
            log.debug("SSE 연결 끊김: {}", e.getMessage());
            return null;
        }
        return globalExceptionHandler.handleException(e);
    }

    /**
     * 요청 본문 해석 실패를 core 의 GlobalExceptionHandler 와 같은 400 응답으로 처리
     * (원인이 IOException(JsonParseException 등)이라 위 IOException 처리가 원인 예외로 먼저 잡아 500 이 되는 것을 막는다)
     * @param e 요청 본문 해석 실패 예외
     * @return INVALID_INPUT_VALUE 응답
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        return globalExceptionHandler.handleBadRequestException(e);
    }

    /**
     * 비동기 요청 시간이 지난 경우 (SseEmitter 는 타임아웃 시 직접 완료하므로 보통 발생하지 않음)
     * @param e 비동기 요청 타임아웃 예외
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public void handleAsyncRequestTimeout(AsyncRequestTimeoutException e) {
        log.debug("비동기 요청 타임아웃");
    }
}
