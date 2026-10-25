package kr.or.coder.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.or.coder.common.frame.mapper.AuthMenuMapper;
import kr.or.coder.core.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * {@link PrgmAuth} 가 붙은 API 호출 전에 로그인 사용자의 화면(프로그램) 권한을 확인하는 Interceptor
 * 권한이 없으면 403(cm.error.forbidden)으로 응답한다.
 * @author ywkim
 * @since 2026.10.20
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.20  ywkim        최초 생성
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrgmAuthInterceptor implements HandlerInterceptor {

    private static final String FORBIDDEN_MESSAGE_KEY = "cm.error.forbidden";

    private final AuthMenuMapper authMenuMapper;
    private final MessageSource messageSource;
    private final ObjectMapper objectMapper;

    /**
     * 컨트롤러 메서드(없으면 클래스)의 {@link PrgmAuth} 프로그램 중 하나라도 화면 권한이 있는지 확인
     * @param request HTTP 요청
     * @param response HTTP 응답 (권한이 없으면 403 본문을 쓴다)
     * @param handler 호출 대상 핸들러
     * @return 권한이 있거나 확인 대상이 아니면 true
     * @throws Exception 403 응답 본문을 쓰는 중 오류가 발생한 경우
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        PrgmAuth prgmAuth = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), PrgmAuth.class);
        if (prgmAuth == null) {
            prgmAuth = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), PrgmAuth.class);
        }
        if (prgmAuth == null) {
            return true;
        }

        String usrId = getCurrentUserId();
        List<String> prgmIds = List.of(prgmAuth.value());
        if (usrId != null && authMenuMapper.existsPrgmAuth(usrId, prgmIds)) {
            return true;
        }

        log.warn("Program authority denied: usrId={}, prgmIds={}, uri={} {}", usrId, prgmIds, request.getMethod(), request.getRequestURI());
        String message = messageSource.getMessage(FORBIDDEN_MESSAGE_KEY, null, LocaleContextHolder.getLocale());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(FORBIDDEN_MESSAGE_KEY, message));
        return false;
    }

    /**
     * 인증된 로그인 사용자ID를 반환
     * @return 사용자ID (인증되지 않았으면 null)
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }
}
