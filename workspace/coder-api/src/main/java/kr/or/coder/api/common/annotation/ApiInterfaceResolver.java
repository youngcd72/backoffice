package kr.or.coder.api.common.annotation;

import jakarta.servlet.http.HttpServletRequest;
import kr.or.coder.api.common.config.ApiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.server.RequestPath;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;

import java.util.List;

/**
 * 요청이 호출하는 Controller 메서드의 @ApiInterface 인터페이스ID를 찾는다.
 * Spring MVC 보다 앞선 서블릿 필터(호출 이력, 인증·권한·한도)에서 업무 API 여부를 판단하는 데 쓴다.
 * 기동 시 업무 API 패키지의 모든 Controller 메서드에 @ApiInterface 가 있는지 검사한다.
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
public class ApiInterfaceResolver implements SmartInitializingSingleton {

    /** 요청 속성: 찾은 인터페이스ID (없으면 NONE) */
    private static final String ATTR_INFC_ID = ApiInterfaceResolver.class.getName() + ".infcId";
    private static final String NONE = "";

    private final RequestMappingHandlerMapping handlerMapping;
    private final ApiProperties apiProperties;

    /**
     * MVC 요청 매핑과 설정을 주입받아 생성
     * @param handlerMapping Spring MVC Controller 요청 매핑
     * @param apiProperties 대외 연계 API 설정
     */
    public ApiInterfaceResolver(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
                                ApiProperties apiProperties) {
        this.handlerMapping = handlerMapping;
        this.apiProperties = apiProperties;
    }

    /**
     * 요청의 인터페이스ID를 반환 (한 요청 안에서는 처음 찾은 값을 재사용)
     * @param request HTTP 요청
     * @return 인터페이스ID (업무 API가 아니면 null)
     */
    public String resolve(HttpServletRequest request) {
        Object cached = request.getAttribute(ATTR_INFC_ID);
        if (cached instanceof String infcId) {
            return NONE.equals(infcId) ? null : infcId;
        }
        String infcId = findInfcId(request);
        request.setAttribute(ATTR_INFC_ID, infcId == null ? NONE : infcId);
        return infcId;
    }

    /**
     * 기동 시 업무 API 패키지의 Controller 메서드 중 @ApiInterface 가 없는 것을 찾아 기동을 막는다
     * @throws IllegalStateException @ApiInterface 선언이 없는 업무 API가 있는 경우
     */
    @Override
    public void afterSingletonsInstantiated() {
        List<String> missing = handlerMapping.getHandlerMethods().values().stream()
                .filter(handlerMethod -> handlerMethod.getBeanType().getName().startsWith(apiProperties.getBizBasePackage()))
                .filter(handlerMethod -> getInfcId(handlerMethod) == null)
                .map(HandlerMethod::toString)
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("@ApiInterface is required on business API methods: " + missing);
        }
    }

    /**
     * 요청 경로로 Controller 메서드를 찾아 인터페이스ID를 읽는다
     * (DispatcherServlet 밖이라 경로 파싱 결과를 직접 만들고, 끝나면 원래대로 되돌린다)
     * @param request HTTP 요청
     * @return 인터페이스ID (매핑된 메서드가 없거나 선언이 없으면 null)
     */
    private String findInfcId(HttpServletRequest request) {
        RequestPath previousPath = (RequestPath) request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE);
        ServletRequestPathUtils.parseAndCache(request);
        try {
            HandlerExecutionChain chain = handlerMapping.getHandler(request);
            if (chain != null && chain.getHandler() instanceof HandlerMethod handlerMethod) {
                return getInfcId(handlerMethod);
            }
            return null;
        } catch (Exception e) {
            // 지원하지 않는 메서드 등 매핑 오류는 DispatcherServlet 이 다시 판단해 응답한다
            log.debug("handler lookup failed: {}", e.getMessage());
            return null;
        } finally {
            ServletRequestPathUtils.setParsedRequestPath(previousPath, request);
        }
    }

    /**
     * Controller 메서드(없으면 클래스)에 선언한 @ApiInterface 값을 읽는다
     * @param handlerMethod Controller 메서드
     * @return 인터페이스ID (선언이 없으면 null)
     */
    private String getInfcId(HandlerMethod handlerMethod) {
        ApiInterface apiInterface = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), ApiInterface.class);
        if (apiInterface == null) {
            apiInterface = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), ApiInterface.class);
        }
        return apiInterface == null ? null : apiInterface.value();
    }
}
