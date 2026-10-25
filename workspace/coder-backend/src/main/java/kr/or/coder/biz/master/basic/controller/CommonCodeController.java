package kr.or.coder.biz.master.basic.controller;

import kr.or.coder.biz.master.basic.dto.CommonCodeDetailResponse;
import kr.or.coder.biz.master.basic.dto.CommonCodeRequest;
import kr.or.coder.biz.master.basic.dto.CommonCodeResponse;
import kr.or.coder.biz.master.basic.dto.CommonCodeSaveRequest;
import kr.or.coder.biz.master.basic.service.CommonCodeService;
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
 * MSBS010 공통코드관리 Controller (TB_COMM_CD_M, TB_COMM_CD_D)
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
@RequestMapping("/api/v1/common-codes")
@RequiredArgsConstructor
@Tag(name = "MSBS010 Common Code", description = "공통코드관리")
public class CommonCodeController {

    private final CommonCodeService commonCodeService;

    /**
     * 조회 조건에 맞는 공통코드 분류 목록을 업무모듈·정렬순번 순으로 조회
     * @param request 조회 조건 (wrkMdulCd: 일치 / commCtgCd, commCtgNm: 부분 일치)
     * @return 공통코드 분류 목록
     */
    @Operation(summary = "공통코드 분류 목록 조회")
    @PrgmAuth("MSBS010")
    @GetMapping
    public ApiResponse<List<CommonCodeResponse>> getCommonCodeList(@ModelAttribute CommonCodeRequest request) {
        return ApiResponse.success(commonCodeService.getCommonCodeList(request));
    }

    /**
     * 공통분류의 상세코드 목록을 정렬순번 순으로 조회
     * @param wrkMdulCd 업무모듈코드 (예: SY)
     * @param commCtgCd 공통분류코드 (예: PRGM_CRS_CD)
     * @param useYn 사용여부 조건 (Y: 사용 중인 분류의 사용 중인 코드만 - 화면 콤보용, 없으면 전체 - 공통코드관리용)
     * @return 공통코드 상세 목록
     */
    @Operation(summary = "공통코드 상세 목록 조회", description = "정렬순번 순. useYn=Y 이면 사용 중인 코드만 (화면 콤보용)")
    @GetMapping("/{wrkMdulCd}/{commCtgCd}/details")
    public ApiResponse<List<CommonCodeDetailResponse>> getCommonCodeDetailList(@PathVariable String wrkMdulCd,
                                                                                @PathVariable String commCtgCd,
                                                                                @RequestParam(required = false) String useYn) {
        return ApiResponse.success(commonCodeService.getCommonCodeDetailList(wrkMdulCd, commCtgCd, useYn));
    }

    /**
     * 공통코드 분류·코드 변경분(행 상태 I/U/D 인 항목)을 일괄 저장 (코드는 등록·수정만, 삭제하지 않음)
     * @param request 분류 등록/수정/삭제 목록과 분류별 코드 등록/수정 목록
     * @param authentication 로그인 사용자 인증 정보 (등록/수정자 ID 추출용)
     * @return 저장 건수
     */
    @Operation(summary = "공통코드 분류·코드 일괄 저장", description = "분류 I/U/D, 코드 I/U 를 한 트랜잭션으로 저장. 코드가 있는 분류는 삭제 불가")
    @PrgmAuth("MSBS010")
    @PostMapping
    public ApiResponse<Integer> saveCommonCodeList(@Valid @RequestBody CommonCodeSaveRequest request,
                                                   Authentication authentication) {
        return ApiResponse.success(commonCodeService.saveCommonCodeList(request, authentication.getName()));
    }
}
