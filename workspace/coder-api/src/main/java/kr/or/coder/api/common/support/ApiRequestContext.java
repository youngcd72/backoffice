package kr.or.coder.api.common.support;

import jakarta.servlet.http.HttpServletRequest;
import kr.or.coder.api.common.client.LinkClient;

/**
 * 요청 처리 중 필터 사이에 주고받는 값(인증된 연계키, 오류코드)을 요청 속성으로 보관한다.
 * 호출 이력(CallLogFilter)은 요청이 끝난 뒤 이 값을 읽어 이력을 남긴다.
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
public final class ApiRequestContext {

    private static final String PREFIX = ApiRequestContext.class.getName() + ".";
    private static final String ATTR_CLIENT = PREFIX + "client";
    private static final String ATTR_ERR_CD = PREFIX + "errCd";

    private ApiRequestContext() {
    }

    /**
     * 토큰으로 인증된 연계키 정보를 보관
     * @param request HTTP 요청
     * @param client 연계키 정보
     */
    public static void setClient(HttpServletRequest request, LinkClient client) {
        request.setAttribute(ATTR_CLIENT, client);
    }

    /**
     * 토큰으로 인증된 연계키 정보를 반환
     * @param request HTTP 요청
     * @return 연계키 정보 (인증 전이거나 인증 실패면 null)
     */
    public static LinkClient getClient(HttpServletRequest request) {
        return (LinkClient) request.getAttribute(ATTR_CLIENT);
    }

    /**
     * 응답 오류코드를 보관 (호출 이력 ERR_CD)
     * @param request HTTP 요청
     * @param errCd 오류코드
     */
    public static void setErrCd(HttpServletRequest request, String errCd) {
        request.setAttribute(ATTR_ERR_CD, errCd);
    }

    /**
     * 응답 오류코드를 반환
     * @param request HTTP 요청
     * @return 오류코드 (정상 응답이면 null)
     */
    public static String getErrCd(HttpServletRequest request) {
        return (String) request.getAttribute(ATTR_ERR_CD);
    }
}
