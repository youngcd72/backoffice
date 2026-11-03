package kr.or.coder.biz.system.link.controller;

import kr.or.coder.biz.system.link.dto.InterfaceCallPageResponse;
import kr.or.coder.biz.system.link.dto.InterfaceCallRequest;
import kr.or.coder.biz.system.link.dto.InterfaceCallResponse;
import kr.or.coder.biz.system.link.service.InterfaceCallService;
import kr.or.coder.common.auth.PrgmAuth;
import kr.or.coder.core.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * SYLK010 API관리 호출이력 Controller (TB_INFC_CALL_L)
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
@RequestMapping("/api/v1/interface-calls")
@RequiredArgsConstructor
@Tag(name = "SYLK010 Interface Call", description = "API관리 - 호출이력")
public class InterfaceCallController {

    private final InterfaceCallService interfaceCallService;

    /**
     * 조회 조건에 맞는 호출이력을 요청일시 최근 순으로 최대 건수까지 조회 (본문 제외)
     * @param request 조회 조건 (stDt·endDt 필수, lnkOrgId, infcId, rsltCd, trcId, size)
     * @return 호출이력 목록, 전체 건수, 잘림 여부
     */
    @Operation(summary = "호출이력 조회", description = "기간은 최대 31일, 최근 500건까지")
    @GetMapping
    public ApiResponse<InterfaceCallPageResponse> getInterfaceCallList(@ModelAttribute InterfaceCallRequest request) {
        return ApiResponse.success(interfaceCallService.getInterfaceCallList(request));
    }

    /**
     * 호출이력 1건을 요청·응답 본문까지 조회
     * @param callSn 호출일련번호
     * @param rqmtDttm 요청일시 (YYYY-MM-DDTHH:mm:ss.SSS, 파티션 키)
     * @return 호출이력 (본문은 coder-api 가 저장할 때 마스킹한 값)
     */
    @Operation(summary = "호출이력 상세 조회")
    @GetMapping("/{callSn}")
    public ApiResponse<InterfaceCallResponse> getInterfaceCall(@PathVariable long callSn, @RequestParam String rqmtDttm) {
        return ApiResponse.success(interfaceCallService.getInterfaceCall(callSn, rqmtDttm));
    }
}
