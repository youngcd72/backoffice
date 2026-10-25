package kr.or.coder.common.frame.controller;

import kr.or.coder.common.frame.service.MessageService;
import kr.or.coder.core.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 화면(coder-frontend)용 다국어 메시지 조회 Controller
 * 로그인 전 화면에서도 사용하므로 인증 없이 호출할 수 있다 (application.yml permit-urls).
 * @author ywkim
 * @since 2026.10.09
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.09  ywkim        최초 생성
 * </pre>
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Messages", description = "CODER i18n Message APIs")
public class MessageController {

    private final MessageService messageService;

    /**
     * Accept-Language 헤더의 언어로 메시지 전체 목록을 조회
     * @return 메시지 키 → 메시지 문구 맵
     */
    @Operation(summary = "Get Messages", description = "Returns all i18n messages for the Accept-Language locale")
    @GetMapping("/messages")
    public ApiResponse<Map<String, String>> getMessageList() {
        return ApiResponse.success(messageService.getMessageList(LocaleContextHolder.getLocale()));
    }
}
