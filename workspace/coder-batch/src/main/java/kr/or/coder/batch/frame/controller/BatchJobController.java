package kr.or.coder.batch.frame.controller;

import kr.or.coder.batch.common.exception.BatchErrorCode;
import kr.or.coder.batch.common.exception.BatchException;
import kr.or.coder.batch.frame.dto.BatchJobExecutionRequest;
import kr.or.coder.batch.frame.dto.BatchJobRequest;
import kr.or.coder.batch.frame.dto.BatchJobResponse;
import kr.or.coder.batch.frame.dto.BatchJobScheduleRequest;
import kr.or.coder.batch.frame.dto.BatchJobSearchRequest;
import kr.or.coder.batch.frame.service.BatchJobService;
import kr.or.coder.core.common.response.ApiResponse;
import kr.or.coder.core.common.util.MessageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 배치직무 등록/수정/삭제, 스케줄 제어, 수동 실행 Controller
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
@RequestMapping("/api/v1/batch-jobs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Batch Jobs", description = "배치직무 등록/스케줄/수동 실행 API")
public class BatchJobController {

    private final BatchJobService batchJobService;

    /**
     * 업무모듈/사용여부/키워드 조건으로 배치직무와 스케줄 상태를 조회한다.
     * @param request 배치직무 검색 조건
     * @return 배치직무 목록
     */
    @Operation(summary = "배치직무 목록", description = "업무모듈/사용여부/키워드 조건으로 배치직무와 스케줄 상태를 조회한다.")
    @GetMapping
    public ApiResponse<List<BatchJobResponse>> getBatchJobList(@ModelAttribute BatchJobSearchRequest request) {
        return ApiResponse.success(batchJobService.getBatchJobList(request));
    }

    /**
     * 배치직무 상세를 조회한다.
     * @param jobId 배치직무 ID
     * @return 배치직무 상세
     */
    @Operation(summary = "배치직무 상세")
    @GetMapping("/{jobId}")
    public ApiResponse<BatchJobResponse> getBatchJob(@PathVariable String jobId) {
        return ApiResponse.success(batchJobService.getBatchJob(jobId));
    }

    /**
     * 배치직무를 신규 등록하고 Quartz 스케줄을 등록한다.
     * @param request 배치직무 저장 요청
     * @return 처리 성공 메시지 응답
     * @throws BatchException 이미 등록된 배치ID인 경우
     */
    @Operation(summary = "배치직무 등록", description = "배치직무를 신규 등록하고 Quartz 스케줄을 등록한다. 이미 있는 배치ID면 409(sy.batch.job.duplicate).")
    @PostMapping
    public ApiResponse<Void> saveBatchJob(@Valid @RequestBody BatchJobRequest request) {
        batchJobService.saveBatchJob(request, true);
        return ApiResponse.success(MessageUtil.getMessage("cm.common.success"), null);
    }

    /**
     * 배치직무를 수정하고 Quartz 스케줄을 즉시 재반영한다.
     * @param jobId 경로의 배치직무 ID
     * @param request 배치직무 저장 요청
     * @return 처리 성공 메시지 응답
     * @throws BatchException 경로의 jobId와 요청 본문의 jobId가 다른 경우
     */
    @Operation(summary = "배치직무 수정", description = "배치직무를 저장하고 Quartz 스케줄을 즉시 재반영한다.")
    @PutMapping("/{jobId}")
    public ApiResponse<Void> saveBatchJob(@PathVariable String jobId, @Valid @RequestBody BatchJobRequest request) {
        if (!jobId.equals(request.getJobId())) {
            throw new BatchException(BatchErrorCode.JOB_ID_MISMATCH, jobId, request.getJobId());
        }
        batchJobService.saveBatchJob(request, false);
        return ApiResponse.success(MessageUtil.getMessage("cm.common.success"), null);
    }

    /**
     * 배치직무, 실행내역, Quartz 스케줄을 함께 삭제한다.
     * @param jobId 배치직무 ID
     * @return 처리 성공 메시지 응답
     */
    @Operation(summary = "배치직무 삭제", description = "배치직무, 실행내역, Quartz 스케줄을 함께 삭제한다.")
    @DeleteMapping("/{jobId}")
    public ApiResponse<Void> deleteBatchJob(@PathVariable String jobId) {
        batchJobService.deleteBatchJob(jobId);
        return ApiResponse.success(MessageUtil.getMessage("cm.common.success"), null);
    }

    /**
     * 스케줄을 일시정지(PAUSED)하거나 재개(NORMAL)한다.
     * @param jobId 배치직무 ID
     * @param request 스케줄 상태 변경 요청
     * @return 처리 성공 메시지 응답
     */
    @Operation(summary = "스케줄 일시정지/재개", description = "schedStatus: PAUSED(일시정지) / NORMAL(재개)")
    @PatchMapping("/{jobId}/schedule")
    public ApiResponse<Void> saveBatchJobSchedule(@PathVariable String jobId,
                                                  @Valid @RequestBody BatchJobScheduleRequest request) {
        batchJobService.saveBatchJobSchedule(jobId, request);
        return ApiResponse.success(MessageUtil.getMessage("cm.common.success"), null);
    }

    /**
     * Quartz를 통해 배치직무 즉시 실행을 요청한다 (비동기).
     * @param jobId 배치직무 ID
     * @param request 수동 실행 요청 (파라미터 등, 생략 가능)
     * @return 실행 요청 접수 메시지 응답
     */
    @Operation(summary = "수동 실행", description = "Quartz를 통해 즉시 실행을 요청한다 (비동기). 결과는 실행 이력에서 확인한다.")
    @PostMapping("/{jobId}/executions")
    public ApiResponse<Void> saveBatchJobExecution(@PathVariable String jobId,
                                                   @RequestBody(required = false) BatchJobExecutionRequest request) {
        batchJobService.saveBatchJobExecution(jobId, request);
        return ApiResponse.success(MessageUtil.getMessage("sy.batch.job.execution.requested"), null);
    }
}
