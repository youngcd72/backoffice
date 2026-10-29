package kr.or.coder.api.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.or.coder.api.common.client.LinkClient;
import kr.or.coder.api.common.exception.ApiErrorCode;
import kr.or.coder.api.common.exception.ApiErrorWriter;
import kr.or.coder.api.common.support.ApiRequestContext;
import kr.or.coder.api.common.support.ClientIpResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 요청 IP가 연계키의 허용 IP(ALLW_IP_ADDR, IP/CIDR 여러 개)에 없으면 403(A002)으로 막는 필터.
 * 허용 IP가 비어 있으면 제한하지 않는다. 연계키를 알아야 하므로 ApiClientFilter 뒤에서 실행된다.
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
public class IpAllowFilter extends OncePerRequestFilter {

    private final ClientIpResolver clientIpResolver;
    private final ApiErrorWriter apiErrorWriter;

    /**
     * 연계키가 확인된 요청이면 요청 IP를 허용 IP와 비교한다
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
            String clientIp = clientIpResolver.resolve(request);
            if (!ClientIpResolver.isAllowed(client.getAllwIpAddr(), clientIp)) {
                apiErrorWriter.write(request, response, ApiErrorCode.IP_NOT_ALLOWED, "ip=" + clientIp);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
