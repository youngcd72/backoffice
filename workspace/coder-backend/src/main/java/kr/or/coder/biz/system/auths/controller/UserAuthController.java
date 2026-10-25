package kr.or.coder.biz.system.auths.controller;

import kr.or.coder.biz.system.auths.dto.UserAuthRequest;
import kr.or.coder.biz.system.auths.dto.UserAuthResponse;
import kr.or.coder.biz.system.auths.dto.UserAuthSaveRequest;
import kr.or.coder.biz.system.auths.dto.UserComponentAuthResponse;
import kr.or.coder.biz.system.auths.dto.UserMenuAuthResponse;
import kr.or.coder.biz.system.auths.service.UserAuthService;
import kr.or.coder.common.auth.PrgmAuth;
import kr.or.coder.core.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * SYAU010 사용자권한관리 Controller (TB_USR_M, TB_USR_MENU_R, TB_USR_CMPN_R)
 * @author ywkim
 * @since 2026.10.17
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.17  ywkim        최초 생성
 * </pre>
 */
@RestController
@PrgmAuth("SYAU010")
@RequestMapping("/api/v1/user-auths")
@RequiredArgsConstructor
@Tag(name = "SYAU010 User Auth", description = "사용자권한관리")
public class UserAuthController {

    private final UserAuthService userAuthService;

    /**
     * 조회 조건에 맞는 사용자 목록을 사용자ID 순으로 조회
     * @param request 조회 조건 (usrId, usrNm: 부분 일치 / usrClsCd, useYn: 일치)
     * @return 사용자 목록
     */
    @Operation(summary = "사용자 목록 조회")
    @PrgmAuth({"SYAU010", "SYAU030"})
    @GetMapping
    public ApiResponse<List<UserAuthResponse>> getUserAuthList(@ModelAttribute UserAuthRequest request) {
        return ApiResponse.success(userAuthService.getUserAuthList(request));
    }

    /**
     * 사용자ID가 등록되어 있는지 확인
     * @param usrId 사용자ID
     * @return 등록되어 있으면 true
     */
    @Operation(summary = "사용자ID 등록 여부 확인")
    @GetMapping("/{usrId}/exists")
    public ApiResponse<Boolean> existsUser(@PathVariable String usrId) {
        return ApiResponse.success(userAuthService.existsUser(usrId));
    }

    /**
     * 사용자의 화면 권한 목록을 조회
     * @param usrId 사용자ID
     * @return 화면 권한 목록
     */
    @Operation(summary = "사용자 화면 권한 조회")
    @GetMapping("/{usrId}/menu-auths")
    public ApiResponse<List<UserMenuAuthResponse>> getUserMenuAuthList(@PathVariable String usrId) {
        return ApiResponse.success(userAuthService.getUserMenuAuthList(usrId));
    }

    /**
     * 사용자의 컴포넌트 권한을 조회 (prgmId 가 없으면 사용자의 전체 컴포넌트 권한)
     * @param usrId 사용자ID
     * @param prgmId 프로그램ID (선택)
     * @return 컴포넌트 권한 목록
     */
    @Operation(summary = "사용자 컴포넌트 권한 조회")
    @GetMapping("/{usrId}/component-auths")
    public ApiResponse<List<UserComponentAuthResponse>> getUserComponentAuthList(@PathVariable String usrId,
                                                                                 @RequestParam(required = false) String prgmId) {
        return ApiResponse.success(userAuthService.getUserComponentAuthList(usrId, prgmId));
    }

    /**
     * 사용자·화면 권한·컴포넌트 권한 변경분(행 상태 I/U/D 인 항목)을 일괄 저장
     * @param request 사용자 등록/수정/삭제 목록과 사용자별 화면·컴포넌트 권한 변경분
     * @param authentication 로그인 사용자 인증 정보 (등록/수정자 ID 추출용)
     * @return 저장 건수
     */
    @Operation(summary = "사용자·권한 일괄 저장", description = "한 트랜잭션. 사용자 삭제 시 권한그룹·즐겨찾기·화면·컴포넌트 권한도 함께 삭제")
    @PostMapping
    public ApiResponse<Integer> saveUserAuthList(@Valid @RequestBody UserAuthSaveRequest request,
                                                 Authentication authentication) {
        return ApiResponse.success(userAuthService.saveUserAuthList(request, authentication.getName()));
    }

    /**
     * 사용자 비밀번호를 초기화
     * @param usrId 사용자ID
     * @param authentication 로그인 사용자 인증 정보 (처리자 ID 추출용)
     * @return 결과 없음
     */
    @Operation(summary = "사용자 비밀번호 초기화")
    @PostMapping("/{usrId}/password-resets")
    public ApiResponse<Void> saveUserPasswordReset(@PathVariable String usrId, Authentication authentication) {
        userAuthService.saveUserPasswordReset(usrId, authentication.getName());
        return ApiResponse.success(null);
    }
}
