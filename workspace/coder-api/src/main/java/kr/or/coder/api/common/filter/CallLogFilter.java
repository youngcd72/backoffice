package kr.or.coder.api.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.or.coder.api.common.annotation.ApiInterfaceResolver;
import kr.or.coder.api.common.calllog.CallLog;
import kr.or.coder.api.common.calllog.CallLogMasker;
import kr.or.coder.api.common.calllog.CallLogRecorder;
import kr.or.coder.api.common.client.LinkClient;
import kr.or.coder.api.common.client.LinkClientCache;
import kr.or.coder.api.common.client.LinkInterface;
import kr.or.coder.api.common.config.ApiProperties;
import kr.or.coder.api.common.log.LogMdcKey;
import kr.or.coder.api.common.support.ApiRequestContext;
import kr.or.coder.api.common.support.ClientIpResolver;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 업무 API(@ApiInterface) 호출마다 호출 이력을 남기는 필터. 인증·권한·한도 실패도 남도록 Spring Security 보다 먼저 실행된다.
 * 요청·응답 본문은 인터페이스의 본문로그여부(BODY_LOG_YN)가 Y 일 때만 마스킹해 남기고, 헤더(Authorization 등)는 남기지 않는다.
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
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CallLogFilter extends OncePerRequestFilter {

    /** 마스킹을 위해 메모리에 담는 요청 본문 최대 크기 (넘으면 JSON 해석에 실패해 본문을 남기지 않는다) */
    private static final int REQUEST_CACHE_LIMIT = 1024 * 1024;

    private final ApiInterfaceResolver apiInterfaceResolver;
    private final LinkClientCache linkClientCache;
    private final ClientIpResolver clientIpResolver;
    private final CallLogMasker callLogMasker;
    private final CallLogRecorder callLogRecorder;
    private final ApiProperties apiProperties;

    /**
     * 업무 API면 요청을 처리한 뒤 호출 이력을 큐에 넣는다
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param filterChain 필터 체인
     * @throws ServletException 다음 필터 실행 중 서블릿 예외
     * @throws IOException 다음 필터 실행 중 입출력 예외
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String infcId = apiInterfaceResolver.resolve(request);
        if (infcId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        LinkInterface linkInterface = linkClientCache.getInterface(infcId);
        boolean bodyLog = linkInterface != null && linkInterface.isBodyLog();
        ContentCachingRequestWrapper requestWrapper = bodyLog ? new ContentCachingRequestWrapper(request, REQUEST_CACHE_LIMIT) : null;
        ContentCachingResponseWrapper responseWrapper = bodyLog ? new ContentCachingResponseWrapper(response) : null;

        LocalDateTime rqmtDttm = LocalDateTime.now();
        long startNanos = System.nanoTime();
        boolean failed = true;
        try {
            filterChain.doFilter(requestWrapper != null ? requestWrapper : request,
                    responseWrapper != null ? responseWrapper : response);
            failed = false;
        } finally {
            CallLog callLog = createCallLog(request, response, infcId, rqmtDttm, startNanos, failed);
            if (bodyLog) {
                List<String> maskFields = linkInterface.getMaskFieldList();
                int maxLength = apiProperties.getCallLog().getBodyMaxLength();
                callLog.setRqmtCont(callLogMasker.mask(requestWrapper.getContentAsByteArray(), maskFields, maxLength));
                callLog.setRspnCont(callLogMasker.mask(responseWrapper.getContentAsByteArray(), maskFields, maxLength));
                responseWrapper.copyBodyToResponse();
            }
            callLogRecorder.record(callLog);
        }
    }

    /**
     * 요청·응답 정보로 호출 이력을 만든다 (본문 제외)
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param infcId 인터페이스ID
     * @param rqmtDttm 요청일시
     * @param startNanos 요청 시작 시각 (System.nanoTime)
     * @param failed 처리 중 예외가 났는지 여부 (예외면 500으로 기록)
     * @return 호출 이력
     */
    private CallLog createCallLog(HttpServletRequest request, HttpServletResponse response, String infcId,
                                  LocalDateTime rqmtDttm, long startNanos, boolean failed) {
        CallLog callLog = new CallLog();
        callLog.setTrcId(MDC.get(LogMdcKey.TRACE_ID));
        callLog.setInfcId(infcId);
        LinkClient client = ApiRequestContext.getClient(request);
        if (client != null) {
            callLog.setKeyId(client.getKeyId());
            callLog.setLnkOrgId(client.getLnkOrgId());
        }
        callLog.setHttpMethNm(request.getMethod());
        callLog.setUrl(request.getRequestURI());
        callLog.setHttpStatCd(String.valueOf(failed ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : response.getStatus()));
        callLog.setClntIpAddr(clientIpResolver.resolve(request));
        callLog.setRqmtDttm(rqmtDttm);
        callLog.setRspnDttm(LocalDateTime.now());
        callLog.setElapsedMs((System.nanoTime() - startNanos) / 1_000_000);
        callLog.setErrCd(ApiRequestContext.getErrCd(request));
        callLog.setRegrId(apiProperties.getSystemUserId());
        return callLog;
    }
}
