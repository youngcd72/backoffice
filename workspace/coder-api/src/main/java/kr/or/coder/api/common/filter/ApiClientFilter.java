package kr.or.coder.api.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.or.coder.api.common.annotation.ApiInterfaceResolver;
import kr.or.coder.api.common.client.LinkClient;
import kr.or.coder.api.common.client.LinkClientCache;
import kr.or.coder.api.common.exception.ApiErrorCode;
import kr.or.coder.api.common.exception.ApiErrorWriter;
import kr.or.coder.api.common.log.LogMdcKey;
import kr.or.coder.api.common.support.ApiRequestContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 토큰 검증(BearerTokenAuthenticationFilter) 뒤에서 토큰의 키ID(sub)로 연계키를 읽어 지금도 쓸 수 있는지 확인하는 필터.
 * 토큰 발급 뒤 키가 사용중지·폐기·만료되었거나 연계기관이 사용중지되었으면 401(A001)로 막는다.
 * 확인한 연계키는 이후 필터(IP·인터페이스 권한·호출 한도)와 호출 이력이 쓰도록 요청에 보관한다.
 * SecurityConfig 가 보안 필터 체인에만 등록한다 (서블릿 필터로 따로 등록하지 않도록 빈으로 만들지 않는다).
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
@RequiredArgsConstructor
public class ApiClientFilter extends OncePerRequestFilter {

    private final ApiInterfaceResolver apiInterfaceResolver;
    private final LinkClientCache linkClientCache;
    private final ApiErrorWriter apiErrorWriter;
    private final Clock clock;

    /**
     * 업무 API이고 토큰이 검증되었으면 연계키를 확인해 요청에 보관한다 (토큰이 없으면 인가 단계에서 401)
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param filterChain 필터 체인
     * @throws ServletException 다음 필터 실행 중 서블릿 예외
     * @throws IOException 다음 필터 실행 또는 응답 쓰기 중 입출력 예외
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (apiInterfaceResolver.resolve(request) == null || !(authentication instanceof JwtAuthenticationToken token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String keyId = token.getName();
        LinkClient client = linkClientCache.getClient(keyId);
        if (client == null || !client.isUsable(LocalDateTime.now(clock))) {
            apiErrorWriter.write(request, response, ApiErrorCode.TOKEN_INVALID, "client key is not usable: " + keyId);
            return;
        }
        ApiRequestContext.setClient(request, client);
        MDC.put(LogMdcKey.USR_ID, keyId);
        filterChain.doFilter(request, response);
    }
}
