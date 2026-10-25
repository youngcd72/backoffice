package kr.or.coder.common.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 요청마다 로그 MDC(추적ID, 사용자ID, URI)를 채우고 요청이 끝나면 비우는 필터
 * (Spring Security 필터 체인 뒤에서 실행되어 인증 사용자를 읽을 수 있다)
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
@Component
public class LogMdcFilter extends OncePerRequestFilter {

    /**
     * MDC를 채운 뒤 다음 필터를 실행하고, 끝나면 MDC를 비운다
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param filterChain 필터 체인
     * @throws ServletException 다음 필터 실행 중 서블릿 예외
     * @throws IOException 다음 필터 실행 중 입출력 예외
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        try {
            MDC.put(LogMdcKey.TRACE_ID, traceId);
            MDC.put(LogMdcKey.URI, request.getRequestURI());
            MDC.put(LogMdcKey.METHOD, request.getMethod());
            String usrId = getUsrId();
            if (usrId != null) {
                MDC.put(LogMdcKey.USR_ID, usrId);
            }
            response.setHeader(LogMdcKey.TRACE_ID_HEADER, traceId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    /**
     * 현재 인증된 사용자 ID를 반환
     * @return 사용자 ID (미인증·익명이면 null)
     */
    private String getUsrId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }
}
