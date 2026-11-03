package kr.or.coder.biz.system.link.controller;

import kr.or.coder.biz.system.link.dto.InterfaceRequest;
import kr.or.coder.biz.system.link.dto.InterfaceResponse;
import kr.or.coder.biz.system.link.dto.InterfaceSaveRequest;
import kr.or.coder.biz.system.link.service.InterfaceService;
import kr.or.coder.common.auth.PrgmAuth;
import kr.or.coder.core.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * SYLK010 API관리 인터페이스 Controller (TB_INFC_M)
 * @author ywkim
 * @since 2026.11.03
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.11.03  ywkim        최초 생성
 * </pre>
 */
@RestController
@PrgmAuth("SYLK010")
@RequestMapping("/api/v1/interfaces")
@RequiredArgsConstructor
@Tag(name = "SYLK010 Interface", description = "API관리 - 인터페이스")
public class InterfaceController {

    private final InterfaceService interfaceService;

    /**
     * 조회 조건에 맞는 인터페이스 목록을 조회 (허용 기관 수 포함)
     * @param request 조회 조건 (keyword: 인터페이스ID·명·URL 부분 일치, useYn)
     * @return 인터페이스 목록
     */
    @Operation(summary = "인터페이스 목록 조회")
    @GetMapping
    public ApiResponse<List<InterfaceResponse>> getInterfaceList(@ModelAttribute InterfaceRequest request) {
        return ApiResponse.success(interfaceService.getInterfaceList(request));
    }

    /**
     * 인터페이스를 등록 또는 수정
     * @param request 인터페이스 (isNew 로 등록/수정 구분)
     * @param authentication 로그인 사용자 인증 정보 (등록/수정자 ID 추출용)
     * @return 처리 결과
     */
    @Operation(summary = "인터페이스 저장", description = "isNew=true 면 등록, false 면 수정. 저장 후 coder-api 캐시 삭제를 알린다")
    @PostMapping
    public ApiResponse<Void> saveInterface(@Valid @RequestBody InterfaceSaveRequest request, Authentication authentication) {
        interfaceService.saveInterface(request, authentication.getName());
        return ApiResponse.success(null);
    }
}
