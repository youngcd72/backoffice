package kr.or.coder.biz.system.logs.controller;

import kr.or.coder.biz.system.logs.dto.SystemLogPageResponse;
import kr.or.coder.biz.system.logs.dto.SystemLogRequest;
import kr.or.coder.biz.system.logs.dto.SystemLogResponse;
import kr.or.coder.biz.system.logs.service.SystemLogService;
import kr.or.coder.common.auth.PrgmAuth;
import kr.or.coder.core.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SYLO030 시스템로그관리 Controller (에러 로그 파일 조회)
 * @author ywkim
 * @since 2026.10.09
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.09  ywkim        최초 생성
 * </pre>
 */
@RestController
@PrgmAuth("SYLO030")
@RequestMapping("/api/v1/system-logs")
@RequiredArgsConstructor
@Tag(name = "SYLO030 System Log", description = "시스템로그관리 (에러 로그 파일 조회)")
public class SystemLogController {

    private final SystemLogService systemLogService;

    /**
     * 조회 조건에 맞는 시스템로그 목록을 최근 순으로 조회
     * @param request 조회 조건 (stDt, endDt: YYYYMMDD 필수 / sysmClsCd, usrClsCd, usrId, usrNm)
     * @return 시스템로그 목록(스택트레이스 제외), 전체 건수, 잘림 여부
     */
    @Operation(summary = "시스템로그 목록 조회", description = "최대 31일, 최근 1000건")
    @GetMapping
    public ApiResponse<SystemLogPageResponse> getSystemLogList(@ModelAttribute SystemLogRequest request) {
        return ApiResponse.success(systemLogService.getSystemLogList(request));
    }

    /**
     * 로그 키로 시스템로그 상세(스택트레이스 포함)를 조회
     * @param logKey 로그 키 ({일자}-{파일순번}-{줄번호})
     * @return 시스템로그 상세
     */
    @Operation(summary = "시스템로그 상세 조회")
    @GetMapping("/{logKey}")
    public ApiResponse<SystemLogResponse> getSystemLog(@PathVariable String logKey) {
        return ApiResponse.success(systemLogService.getSystemLog(logKey));
    }
}
