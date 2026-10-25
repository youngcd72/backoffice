package kr.or.coder.batch.frame.controller;

import kr.or.coder.batch.common.support.PageResponse;
import kr.or.coder.batch.frame.dto.BatchExecutionResponse;
import kr.or.coder.batch.frame.dto.BatchExecutionSearchRequest;
import kr.or.coder.batch.frame.dto.BatchExecutionStatusRequest;
import kr.or.coder.batch.frame.service.BatchExecutionService;
import kr.or.coder.core.common.response.ApiResponse;
import kr.or.coder.core.common.util.MessageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 배치 실행 이력 조회/재시작/중지 Controller
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
@RequestMapping("/api/v1/batch-executions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Batch Executions", description = "배치 실행 이력/재시작/중지 API")
public class BatchExecutionController {

    private final BatchExecutionService batchExecutionService;

    /**
     * 기간·업무모듈·직무ID·상태 조건으로 실행 이력 목록을 페이징 조회한다.
     * @param request 실행 이력 검색 조건
     * @return 실행 이력 페이지 응답
     */
    @Operation(summary = "실행 이력 목록", description = "기간(YYYYMMDD)·업무모듈·직무ID·상태 조건, 페이징(page 1부터)")
    @GetMapping
    public ApiResponse<PageResponse<BatchExecutionResponse>> getBatchExecutionList(
            @Valid @ModelAttribute BatchExecutionSearchRequest request) {
        return ApiResponse.success(batchExecutionService.getBatchExecutionList(request));
    }

    /**
     * 실행 이력 상세를 JobParameters와 Step별 처리 현황과 함께 조회한다.
     * @param dsptExecSn 배치실행일련번호
     * @return 실행 이력 상세
     */
    @Operation(summary = "실행 상세", description = "JobParameters와 Step별 처리 현황을 포함한다.")
    @GetMapping("/{dsptExecSn}")
    public ApiResponse<BatchExecutionResponse> getBatchExecution(@PathVariable Long dsptExecSn) {
        return ApiResponse.success(batchExecutionService.getBatchExecution(dsptExecSn));
    }

    /**
     * FAILED / STOPPED 실행 건을 같은 JobParameters로 재시작 요청한다 (비동기).
     * @param dsptExecSn 배치실행일련번호
     * @return 요청 접수 메시지 응답
     */
    @Operation(summary = "재시작", description = "FAILED / STOPPED 실행 건을 같은 JobParameters로 재시작한다 (비동기).")
    @PostMapping("/{dsptExecSn}/restarts")
    public ApiResponse<Void> saveBatchExecutionRestart(@PathVariable Long dsptExecSn) {
        batchExecutionService.saveBatchExecutionRestart(dsptExecSn);
        return ApiResponse.success(MessageUtil.getMessage("sy.batch.job.execution.requested"), null);
    }

    /**
     * 실행 건에 중지(STOPPED) 또는 폐기(ABANDONED) 상태 변경을 요청한다.
     * @param dsptExecSn 배치실행일련번호
     * @param request 변경할 실행상태 요청
     * @return 처리 성공 메시지 응답
     */
    @Operation(summary = "실행 중지/폐기", description = "execStatCd: STOPPED(중지 요청) / ABANDONED(폐기)")
    @PatchMapping("/{dsptExecSn}")
    public ApiResponse<Void> saveBatchExecutionStatus(@PathVariable Long dsptExecSn,
                                                      @Valid @RequestBody BatchExecutionStatusRequest request) {
        batchExecutionService.saveBatchExecutionStatus(dsptExecSn, request);
        return ApiResponse.success(MessageUtil.getMessage("cm.common.success"), null);
    }
}
