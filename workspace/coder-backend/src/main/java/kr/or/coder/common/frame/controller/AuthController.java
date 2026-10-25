package kr.or.coder.common.frame.controller;

import kr.or.coder.common.frame.dto.auth.LoginRequest;
import kr.or.coder.common.frame.dto.auth.LoginResponse;
import kr.or.coder.common.frame.dto.auth.UserProfileResponse;
import kr.or.coder.common.frame.service.AuthService;
import kr.or.coder.core.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 인증(로그인) 및 사용자 목록 조회 Controller
 * @author ywkim
 * @since 2026.09.30
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.09.30  ywkim        최초 생성
 * </pre>
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Authentication & Users", description = "CODER User Authentication and Profile APIs")
public class AuthController {

    private final AuthService authService;

    /**
     * 사용자 아이디/비밀번호를 검증하고 JWT 액세스 토큰을 발급
     * @param request 로그인 요청 (아이디, 비밀번호)
     * @return 토큰과 사용자 정보
     */
    @Operation(summary = "User Login", description = "Authenticates user credentials and returns access token")
    @PostMapping("/auth/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    /**
     * 등록된 전체 사용자 목록을 조회 (사용자 코드서치용)
     * @return 사용자 프로필 목록
     */
    @Operation(summary = "List All Users", description = "Retrieves list of registered system users from Database via MyBatis")
    @GetMapping("/users")
    public ApiResponse<List<UserProfileResponse>> getUserList() {
        return ApiResponse.success(authService.getUserList());
    }
}
