package kr.or.coder.api.common.auth;

import kr.or.coder.api.common.auth.dto.OAuth2ErrorResponse;
import kr.or.coder.api.common.log.LogMdcKey;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 토큰 발급 실패를 OAuth2 표준 오류 응답으로 변환 (전역 ApiResponse 오류 처리보다 먼저 적용)
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
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = TokenController.class)
public class TokenExceptionHandler {

    /**
     * 토큰 발급 실패 예외를 OAuth2 오류 응답으로 변환하고 사유를 WARN 로그로 남긴다
     * @param e 토큰 발급 실패 예외
     * @return OAuth2 오류 응답 (invalid_client 401 + WWW-Authenticate, 그 외 400)
     */
    @ExceptionHandler(OAuth2TokenException.class)
    public ResponseEntity<OAuth2ErrorResponse> handleOAuth2TokenException(OAuth2TokenException e) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable(LogMdcKey.ERR_CD, e.getError())) {
            log.warn("token request rejected: {}", e.getMessage());
        }
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(e.getStatus())
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache");
        if ("invalid_client".equals(e.getError())) {
            builder.header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"coder-api\"");
        }
        return builder.body(new OAuth2ErrorResponse(e.getError(), e.getDescription()));
    }
}
