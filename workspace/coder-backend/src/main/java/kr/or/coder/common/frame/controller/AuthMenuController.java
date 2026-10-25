package kr.or.coder.common.frame.controller;

import kr.or.coder.common.frame.dto.auth.AuthMenuResponse;
import kr.or.coder.common.frame.service.AuthMenuService;
import kr.or.coder.core.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 로그인 사용자의 권한 메뉴 조회 Controller (상단 메뉴·사이드바·메뉴 검색)
 * @author ywkim
 * @since 2026.10.19
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.19  ywkim        최초 생성
 * </pre>
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Auth Menus", description = "CODER Signed-in User Menu APIs")
public class AuthMenuController {

    private final AuthMenuService authMenuService;

    /**
     * 로그인 사용자(JWT)에게 권한이 있는 화면 메뉴와 그 상위 메뉴를 조회
     * @param authentication 인증 정보 (사용자ID)
     * @return 권한 메뉴 목록 (상위 메뉴 먼저, 정렬순번 순)
     */
    @Operation(summary = "Get My Menus", description = "Returns menus the signed-in user is authorized for, with their parent menus")
    @GetMapping("/auth-menus")
    public ApiResponse<List<AuthMenuResponse>> getAuthMenuList(Authentication authentication) {
        return ApiResponse.success(authMenuService.getAuthMenuList(authentication.getName()));
    }
}
