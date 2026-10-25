package kr.or.coder.biz.system.menu.controller;

import kr.or.coder.biz.system.menu.dto.ProgramComponentResponse;
import kr.or.coder.biz.system.menu.dto.ProgramRequest;
import kr.or.coder.biz.system.menu.dto.ProgramResponse;
import kr.or.coder.biz.system.menu.dto.ProgramSaveRequest;
import kr.or.coder.biz.system.menu.service.ProgramService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * SYPG010 프로그램관리 Controller (TB_PRGM_M)
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
@RequestMapping("/api/v1/programs")
@RequiredArgsConstructor
@Tag(name = "SYPG010 Program", description = "프로그램관리")
public class ProgramController {

    private final ProgramService programService;

    /**
     * 조회 조건에 맞는 프로그램 목록을 정렬순번 순으로 조회
     * @param request 조회 조건 (prgmCrsCd, useYn: 일치 / prgmId, prgmNm: 부분 일치)
     * @return 프로그램 목록
     */
    @Operation(summary = "프로그램 목록 조회")
    @GetMapping
    public ApiResponse<List<ProgramResponse>> getProgramList(@ModelAttribute ProgramRequest request) {
        return ApiResponse.success(programService.getProgramList(request));
    }

    /**
     * 프로그램의 컴포넌트 목록을 정렬순번 순으로 조회
     * @param prgmId 프로그램ID
     * @return 컴포넌트 목록
     */
    @Operation(summary = "프로그램 컴포넌트 목록 조회")
    @GetMapping("/{prgmId}/components")
    public ApiResponse<List<ProgramComponentResponse>> getProgramComponentList(@PathVariable String prgmId) {
        return ApiResponse.success(programService.getProgramComponentList(prgmId));
    }

    /**
     * 프로그램·컴포넌트 변경분(행 상태 I/U/D 인 항목)을 일괄 저장
     * @param request 프로그램·컴포넌트 등록/수정/삭제 목록
     * @param authentication 로그인 사용자 인증 정보 (등록/수정자 ID 추출용)
     * @return 저장 건수
     */
    @Operation(summary = "프로그램·컴포넌트 일괄 저장", description = "등록(I)·수정(U)·삭제(D) 목록만 한 트랜잭션으로 저장")
    @PrgmAuth("SYPG010")
    @PostMapping
    public ApiResponse<Integer> saveProgramList(@Valid @RequestBody ProgramSaveRequest request,
                                                Authentication authentication) {
        return ApiResponse.success(programService.saveProgramList(request, authentication.getName()));
    }
}
