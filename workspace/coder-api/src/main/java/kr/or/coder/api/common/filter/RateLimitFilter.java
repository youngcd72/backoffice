package kr.or.coder.api.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.or.coder.api.common.client.LinkClient;
import kr.or.coder.api.common.exception.ApiErrorCode;
import kr.or.coder.api.common.exception.ApiErrorWriter;
import kr.or.coder.api.common.support.ApiRequestContext;
import kr.or.coder.api.common.support.RateLimiter;
import kr.or.coder.api.common.support.RateLimiter.RateLimitResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 연계키별 호출 한도(분당·일·동시)를 적용하는 필터. 초과하면 429(A004)와 Retry-After 헤더로 응답한다.
 * 한도가 있으면 응답에 X-RateLimit-Limit / X-RateLimit-Remaining 헤더를 준다 (분당 한도 우선, 없으면 일 한도).
 * Redis 장애 시 coder.api.rate-limit.redis-failure-mode 가 deny 면 503(A005), allow 면 제한 없이 처리한다.
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
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String HEADER_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RETRY_AFTER = "Retry-After";

    private final RateLimiter rateLimiter;
    private final ApiErrorWriter apiErrorWriter;

    /**
     * 연계키가 확인된 요청이면 호출 한도를 확인하고, 처리가 끝나면 동시 요청 수를 되돌린다
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
        if (client == null) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitResult result = rateLimiter.acquire(client);
        if (result.limit() != null) {
            response.setHeader(HEADER_LIMIT, String.valueOf(result.limit()));
            response.setHeader(HEADER_REMAINING, String.valueOf(result.remaining()));
        }
        if (result.unavailable()) {
            apiErrorWriter.write(request, response, ApiErrorCode.RATE_LIMIT_UNAVAILABLE, "rate limit store unavailable");
            return;
        }
        if (result.exceeded() != null) {
            response.setHeader(HEADER_RETRY_AFTER, String.valueOf(result.retryAfterSeconds()));
            apiErrorWriter.write(request, response, ApiErrorCode.RATE_LIMIT_EXCEEDED,
                    result.exceeded() + " limit exceeded for " + client.getKeyId());
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (result.simuAcquired()) {
                rateLimiter.release(client);
            }
        }
    }
}
