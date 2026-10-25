package kr.or.coder.common.frame.controller;

import kr.or.coder.common.frame.dto.auth.ComponentAuthResponse;
import kr.or.coder.common.frame.service.ComponentAuthService;
import kr.or.coder.core.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 로그인 사용자의 화면 컴포넌트(버튼 등) 권한 조회 Controller (frontend v-permission)
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
@Tag(name = "Component Auths", description = "CODER Signed-in User Component Auth APIs")
public class ComponentAuthController {

    private final ComponentAuthService componentAuthService;

    /**
     * 로그인 사용자(JWT)의 프로그램 컴포넌트별 권한여부를 조회
     * @param authentication 인증 정보 (사용자ID)
     * @param prgmId 프로그램ID
     * @return 컴포넌트 권한 목록 (TB_PRGM_CMPN_M 에 사용 중으로 등록된 컴포넌트만)
     */
    @Operation(summary = "Get My Component Auths", description = "Returns component auths of the signed-in user for a program")
    @GetMapping("/component-auths")
    public ApiResponse<List<ComponentAuthResponse>> getComponentAuthList(Authentication authentication,
                                                                         @RequestParam String prgmId) {
        return ApiResponse.success(componentAuthService.getComponentAuthList(authentication.getName(), prgmId));
    }
}
