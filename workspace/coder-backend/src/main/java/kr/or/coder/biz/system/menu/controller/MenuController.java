package kr.or.coder.biz.system.menu.controller;

import kr.or.coder.biz.system.menu.dto.MenuResponse;
import kr.or.coder.biz.system.menu.dto.MenuSaveRequest;
import kr.or.coder.biz.system.menu.service.MenuService;
import kr.or.coder.common.auth.PrgmAuth;
import kr.or.coder.core.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * SYPG020 메뉴관리 Controller (TB_MENU_M)
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
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
@Tag(name = "SYPG020 Menu", description = "메뉴관리")
public class MenuController {

    private final MenuService menuService;

    /**
     * 전체 메뉴 목록을 정렬순번 순으로 조회 (트리 구성·조건 필터링은 화면에서 처리)
     * @return 메뉴 목록
     */
    @Operation(summary = "메뉴 전체 목록 조회")
    @GetMapping
    public ApiResponse<List<MenuResponse>> getMenuList() {
        return ApiResponse.success(menuService.getMenuList());
    }

    /**
     * 메뉴 변경분(행 상태 I/U/D 인 항목)을 일괄 저장
     * @param request 메뉴 등록/수정/삭제 목록
     * @param authentication 로그인 사용자 인증 정보 (등록/수정자 ID 추출용)
     * @return 저장 건수
     */
    @Operation(summary = "메뉴 일괄 저장", description = "등록(I)·수정(U)·삭제(D) 목록만 한 트랜잭션으로 저장")
    @PrgmAuth("SYPG020")
    @PostMapping
    public ApiResponse<Integer> saveMenuList(@Valid @RequestBody MenuSaveRequest request,
                                             Authentication authentication) {
        return ApiResponse.success(menuService.saveMenuList(request, authentication.getName()));
    }
}
