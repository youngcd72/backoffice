package kr.or.coder.api.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.or.coder.api.common.log.LogMdcKey;
import kr.or.coder.api.common.support.ApiRequestContext;
import kr.or.coder.core.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 서블릿 필터(DispatcherServlet 밖)에서 오류 응답을 ApiResponse 형식으로 쓴다.
 * 메시지는 Accept-Language(없으면 coder.message.default-locale)로 고르고, 오류코드는 호출 이력에 남긴다.
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
@Component
@RequiredArgsConstructor
public class ApiErrorWriter {

    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;

    /**
     * 오류 코드의 HTTP 상태와 메시지로 응답을 쓰고 WARN 로그를 남긴다
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param errorCode 오류 코드
     * @param reason 로그에 남길 상세 사유 (응답에는 넣지 않음)
     * @throws IOException 응답 쓰기에 실패한 경우
     */
    public void write(HttpServletRequest request, HttpServletResponse response, ApiErrorCode errorCode, String reason)
            throws IOException {
        ApiRequestContext.setErrCd(request, errorCode.getCode());
        try (MDC.MDCCloseable ignored = MDC.putCloseable(LogMdcKey.ERR_CD, errorCode.getCode())) {
            log.warn("{} {} {}", errorCode.name(), request.getRequestURI(), reason);
        }

        String message = messageSource.getMessage(errorCode.getMessageKey(), null, localeResolver.resolveLocale(request));
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode.getCode(), message));
    }
}
