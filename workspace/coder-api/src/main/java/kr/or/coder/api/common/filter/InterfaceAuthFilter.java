package kr.or.coder.api.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.or.coder.api.common.annotation.ApiInterfaceResolver;
import kr.or.coder.api.common.auth.TokenClaims;
import kr.or.coder.api.common.client.LinkClient;
import kr.or.coder.api.common.client.LinkClientCache;
import kr.or.coder.api.common.exception.ApiErrorCode;
import kr.or.coder.api.common.exception.ApiErrorWriter;
import kr.or.coder.api.common.support.ApiRequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 업무 API의 인터페이스ID(@ApiInterface)가 토큰 scope 에 있고, 연계기관 허용 인터페이스(TB_LNK_ORG_INFC_R, 사용 중)인지
 * 확인하는 필터. 둘 중 하나라도 아니면 403(A003). 토큰 발급 뒤 권한을 회수한 경우도 막는다.
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
public class InterfaceAuthFilter extends OncePerRequestFilter {

    private final ApiInterfaceResolver apiInterfaceResolver;
    private final LinkClientCache linkClientCache;
    private final ApiErrorWriter apiErrorWriter;

    /**
     * 연계키가 확인된 요청이면 인터페이스 권한을 확인한다
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param filterChain 필터 체인
     * @throws ServletException 다음 필터 실행 중 서블릿 예외
     * @throws IOException 다음 필터 실행 또는 응답 쓰기 중 입출력 예외
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        LinkClient client = ApiRequestContext.getClient(request);
        if (client != null) {
            String infcId = apiInterfaceResolver.resolve(request);
            JwtAuthenticationToken token = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
            boolean inScope = TokenClaims.getScopes(token.getToken()).contains(infcId);
            if (!inScope || !linkClientCache.getAllowedInfcIds(client.getLnkOrgId()).contains(infcId)) {
                apiErrorWriter.write(request, response, ApiErrorCode.INTERFACE_NOT_ALLOWED,
                        "infcId=" + infcId + (inScope ? " not allowed for org " + client.getLnkOrgId() : " not in token scope"));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
