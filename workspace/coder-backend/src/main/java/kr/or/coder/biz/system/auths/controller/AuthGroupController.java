package kr.or.coder.biz.system.auths.controller;

import kr.or.coder.biz.system.auths.dto.AuthGroupComponentResponse;
import kr.or.coder.biz.system.auths.dto.AuthGroupMenuResponse;
import kr.or.coder.biz.system.auths.dto.AuthGroupRequest;
import kr.or.coder.biz.system.auths.dto.AuthGroupResponse;
import kr.or.coder.biz.system.auths.dto.AuthGroupSaveRequest;
import kr.or.coder.biz.system.auths.dto.AuthGroupUserCountResponse;
import kr.or.coder.biz.system.auths.dto.AuthGroupUserResponse;
import kr.or.coder.biz.system.auths.dto.AuthGroupUserSaveRequest;
import kr.or.coder.biz.system.auths.service.AuthGroupService;
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
 * SYAU020 그룹권한관리 Controller (TB_AUTH_GRP_M, TB_AUTH_GRP_MENU_R, TB_AUTH_GRP_CMPN_R)
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
@RequestMapping("/api/v1/auth-groups")
@RequiredArgsConstructor
@Tag(name = "SYAU020 Auth Group", description = "그룹권한관리")
public class AuthGroupController {

    private final AuthGroupService authGroupService;

    /**
     * 조회 조건에 맞는 권한그룹 목록을 그룹코드 순으로 조회
     * @param request 조회 조건 (usrGrpCd, usrGrpNm: 부분 일치)
     * @return 권한그룹 목록
     */
    @Operation(summary = "권한그룹 목록 조회")
    @PrgmAuth("SYAU020")
    @GetMapping
    public ApiResponse<List<AuthGroupResponse>> getAuthGroupList(@ModelAttribute AuthGroupRequest request) {
        return ApiResponse.success(authGroupService.getAuthGroupList(request));
    }

    /**
     * 권한그룹의 화면 권한 목록을 조회
     * @param usrGrpCd 그룹코드
     * @return 화면 권한 목록
     */
    @Operation(summary = "권한그룹 화면 권한 조회")
    @PrgmAuth("SYAU020")
    @GetMapping("/{usrGrpCd}/menus")
    public ApiResponse<List<AuthGroupMenuResponse>> getAuthGroupMenuList(@PathVariable String usrGrpCd) {
        return ApiResponse.success(authGroupService.getAuthGroupMenuList(usrGrpCd));
    }

    /**
     * 권한그룹의 컴포넌트 권한을 조회 (prgmId 가 없으면 그룹의 전체 컴포넌트 권한)
     * @param usrGrpCd 그룹코드
     * @param prgmId 프로그램ID (선택)
     * @return 컴포넌트 권한 목록
     */
    @Operation(summary = "권한그룹 컴포넌트 권한 조회")
    @PrgmAuth("SYAU020")
    @GetMapping("/{usrGrpCd}/components")
    public ApiResponse<List<AuthGroupComponentResponse>> getAuthGroupComponentList(@PathVariable String usrGrpCd,
                                                                                   @RequestParam(required = false) String prgmId) {
        return ApiResponse.success(authGroupService.getAuthGroupComponentList(usrGrpCd, prgmId));
    }

    /**
     * 권한그룹·화면 권한·컴포넌트 권한 변경분(행 상태 I/U/D 인 항목)을 일괄 저장
     * @param request 그룹 등록/수정/삭제 목록과 그룹별 화면·컴포넌트 권한 변경분
     * @param authentication 로그인 사용자 인증 정보 (등록/수정자 ID 추출용)
     * @return 저장 건수
     */
    @Operation(summary = "권한그룹·권한 일괄 저장", description = "한 트랜잭션. 그룹 삭제 시 그룹 사용자·화면·컴포넌트 권한도 함께 삭제")
    @PrgmAuth("SYAU020")
    @PostMapping
    public ApiResponse<Integer> saveAuthGroupList(@Valid @RequestBody AuthGroupSaveRequest request,
                                                  Authentication authentication) {
        return ApiResponse.success(authGroupService.saveAuthGroupList(request, authentication.getName()));
    }

    /**
     * 조회 조건에 맞는 권한그룹 목록을 그룹별 사용자수와 함께 조회 (SYAU030)
     * @param request 조회 조건 (usrGrpCd, usrGrpNm: 부분 일치 / useYn: 일치)
     * @return 권한그룹·사용자수 목록
     */
    @Operation(summary = "권한그룹별 사용자수 조회")
    @PrgmAuth("SYAU030")
    @GetMapping("/user-counts")
    public ApiResponse<List<AuthGroupUserCountResponse>> getAuthGroupUserCountList(@ModelAttribute AuthGroupRequest request) {
        return ApiResponse.success(authGroupService.getAuthGroupUserCountList(request));
    }

    /**
     * 권한그룹에 속한 사용자 목록을 조회 (SYAU030)
     * @param usrGrpCd 그룹코드
     * @return 그룹 사용자 목록
     */
    @Operation(summary = "권한그룹 사용자 조회")
    @PrgmAuth("SYAU030")
    @GetMapping("/{usrGrpCd}/users")
    public ApiResponse<List<AuthGroupUserResponse>> getAuthGroupUserList(@PathVariable String usrGrpCd) {
        return ApiResponse.success(authGroupService.getAuthGroupUserList(usrGrpCd));
    }

    /**
     * 권한그룹에 사용자를 추가하거나 제외 (SYAU030)
     * @param usrGrpCd 그룹코드
     * @param request 추가/제외할 사용자ID 목록
     * @param authentication 로그인 사용자 인증 정보 (등록자 ID 추출용)
     * @return 처리 건수
     */
    @Operation(summary = "권한그룹 사용자 추가/제외")
    @PrgmAuth("SYAU030")
    @PostMapping("/{usrGrpCd}/users")
    public ApiResponse<Integer> saveAuthGroupUserList(@PathVariable String usrGrpCd,
                                                      @RequestBody AuthGroupUserSaveRequest request,
                                                      Authentication authentication) {
        return ApiResponse.success(authGroupService.saveAuthGroupUserList(usrGrpCd, request, authentication.getName()));
    }
}
