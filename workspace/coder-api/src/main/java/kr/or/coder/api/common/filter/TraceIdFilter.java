package kr.or.coder.api.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.or.coder.api.common.log.LogMdcKey;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 요청마다 추적ID를 정해 MDC(추적ID, URI, 메서드)와 응답 헤더 X-Trace-Id 에 넣고, 요청이 끝나면 MDC를 비우는 필터.
 * 요청 헤더 X-Trace-Id 가 형식에 맞으면 이어받는다 (연계기관 시스템의 추적ID와 연결). 가장 먼저 실행된다.
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
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    /** 이어받을 수 있는 추적ID 형식 (로그·DB 오염 방지) */
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    /**
     * MDC와 응답 헤더에 추적ID를 넣은 뒤 다음 필터를 실행하고, 끝나면 MDC를 비운다
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param filterChain 필터 체인
     * @throws ServletException 다음 필터 실행 중 서블릿 예외
     * @throws IOException 다음 필터 실행 중 입출력 예외
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = getTraceId(request);
        try {
            MDC.put(LogMdcKey.TRACE_ID, traceId);
            MDC.put(LogMdcKey.URI, request.getRequestURI());
            MDC.put(LogMdcKey.METHOD, request.getMethod());
            response.setHeader(LogMdcKey.TRACE_ID_HEADER, traceId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    /**
     * 요청 헤더의 추적ID를 쓰고, 없거나 형식이 맞지 않으면 새로 만든다
     * @param request HTTP 요청
     * @return 추적ID
     */
    private String getTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(LogMdcKey.TRACE_ID_HEADER);
        if (traceId != null && TRACE_ID_PATTERN.matcher(traceId).matches()) {
            return traceId;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
