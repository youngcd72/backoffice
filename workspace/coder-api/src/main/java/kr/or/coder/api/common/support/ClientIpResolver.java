package kr.or.coder.api.common.support;

import jakarta.servlet.http.HttpServletRequest;
import kr.or.coder.api.common.config.ApiProperties;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 요청 IP를 찾는다. 신뢰하는 프록시(coder.api.trusted-proxies)를 거친 요청만 X-Forwarded-For 를 인정하고,
 * 오른쪽(가장 가까운 프록시)부터 거슬러 올라가 처음 나오는 신뢰하지 않는 IP를 요청 IP로 본다.
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
public class ClientIpResolver {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private final List<IpAddressMatcher> trustedProxies;

    /**
     * 신뢰 프록시 목록으로 생성
     * @param apiProperties 대외 연계 API 설정
     */
    public ClientIpResolver(ApiProperties apiProperties) {
        this.trustedProxies = apiProperties.getTrustedProxies().stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(IpAddressMatcher::new)
                .toList();
    }

    /**
     * 요청 IP를 반환
     * @param request HTTP 요청
     * @return 요청 IP
     */
    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        String forwardedFor = request.getHeader(X_FORWARDED_FOR);
        if (!isTrusted(remoteAddr) || !StringUtils.hasText(forwardedFor)) {
            return remoteAddr;
        }
        String[] hops = forwardedFor.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String hop = hops[i].trim();
            if (StringUtils.hasText(hop) && !isTrusted(hop)) {
                return hop;
            }
        }
        return hops[0].trim();
    }

    /**
     * IP가 허용 목록(쉼표로 구분한 IP/CIDR)에 있는지 확인 (목록이 비어 있으면 제한하지 않는다)
     * @param allowList 허용 IP/CIDR 목록 (TB_LNK_KEY_M.ALLW_IP_ADDR)
     * @param ip 확인할 IP
     * @return 허용되면 true
     */
    public static boolean isAllowed(String allowList, String ip) {
        if (!StringUtils.hasText(allowList)) {
            return true;
        }
        for (String allowed : allowList.split(",")) {
            String range = allowed.trim();
            if (StringUtils.hasText(range) && matches(range, ip)) {
                return true;
            }
        }
        return false;
    }

    /**
     * IP가 신뢰 프록시인지 확인
     * @param ip 확인할 IP
     * @return 신뢰 프록시면 true
     */
    private boolean isTrusted(String ip) {
        return trustedProxies.stream().anyMatch(matcher -> safeMatches(matcher, ip));
    }

    /**
     * IP/CIDR 범위에 IP가 포함되는지 확인
     * @param range IP 또는 CIDR
     * @param ip 확인할 IP
     * @return 포함되면 true (형식 오류면 false)
     */
    private static boolean matches(String range, String ip) {
        try {
            return new IpAddressMatcher(range).matches(ip);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 형식이 잘못된 IP(X-Forwarded-For 위조 값 등)는 일치하지 않는 것으로 본다
     * @param matcher IP 범위
     * @param ip 확인할 IP
     * @return 포함되면 true
     */
    private static boolean safeMatches(IpAddressMatcher matcher, String ip) {
        try {
            return matcher.matches(ip);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
