package kr.or.coder.api.common.exception;

import kr.or.coder.api.common.support.ApiRequestContext;
import kr.or.coder.core.common.response.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Controller·GlobalExceptionHandler 가 만든 실패 응답(ApiResponse.success=false)의 코드를 호출 이력 오류코드로 보관한다
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
@RestControllerAdvice
public class ApiErrorCodeAdvice implements ResponseBodyAdvice<Object> {

    /**
     * 모든 응답 본문에 적용
     * @param returnType 반환 타입
     * @param converterType 메시지 컨버터 타입
     * @return 항상 true
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    /**
     * 실패 응답이면 응답 코드를 요청 속성에 보관하고 본문은 그대로 반환
     * @param body 응답 본문
     * @param returnType 반환 타입
     * @param selectedContentType 응답 콘텐츠 타입
     * @param selectedConverterType 메시지 컨버터 타입
     * @param request 요청
     * @param response 응답
     * @return 원래 응답 본문
     */
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ApiResponse<?> apiResponse && !apiResponse.isSuccess()
                && request instanceof ServletServerHttpRequest servletRequest) {
            ApiRequestContext.setErrCd(servletRequest.getServletRequest(), apiResponse.getCode());
        }
        return body;
    }
}
