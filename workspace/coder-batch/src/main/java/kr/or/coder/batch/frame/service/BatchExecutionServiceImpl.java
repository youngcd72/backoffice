package kr.or.coder.batch.frame.service;

import kr.or.coder.batch.common.config.BatchProperties;
import kr.or.coder.batch.common.exception.BatchErrorCode;
import kr.or.coder.batch.common.exception.BatchException;
import kr.or.coder.batch.common.listener.JobExecutionLogListener;
import kr.or.coder.batch.common.support.BatchConstants;
import kr.or.coder.batch.common.support.PageResponse;
import kr.or.coder.batch.common.support.SecurityUserUtil;
import kr.or.coder.batch.frame.dto.BatchExecutionResponse;
import kr.or.coder.batch.frame.dto.BatchExecutionSearchRequest;
import kr.or.coder.batch.frame.dto.BatchExecutionStatusRequest;
import kr.or.coder.batch.frame.dto.BatchStepExecutionResponse;
import kr.or.coder.batch.frame.entity.BatchExecution;
import kr.or.coder.batch.frame.entity.BatchJob;
import kr.or.coder.batch.frame.mapper.BatchExecutionMapper;
import kr.or.coder.batch.frame.mapper.BatchJobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 배치 실행 이력 조회/재시작/중지 Service 구현체
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
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchExecutionServiceImpl implements BatchExecutionService {

    private final BatchExecutionMapper batchExecutionMapper;
    private final BatchJobMapper batchJobMapper;
    private final ScheduleService scheduleService;
    private final JobExplorer jobExplorer;
    private final JobOperator jobOperator;
    private final JobRepository jobRepository;
    private final BatchProperties batchProperties;

    /**
     * 실행 이력 목록과 전체 건수를 조회해 페이지 응답으로 반환한다.
     * @param request 실행 이력 검색 조건
     * @return 실행 이력 페이지 응답
     */
    @Override
    public PageResponse<BatchExecutionResponse> getBatchExecutionList(BatchExecutionSearchRequest request) {
        return PageResponse.<BatchExecutionResponse>builder()
                .list(batchExecutionMapper.selectBatchExecutionList(request))
                .totalCount(batchExecutionMapper.selectBatchExecutionCount(request))
                .page(request.getPage())
                .size(request.getSize())
                .build();
    }

    /**
     * 실행 이력을 조회하고 JobParameters와 Step별 처리 현황을 채운다.
     * @param dsptExecSn 배치실행일련번호
     * @return 실행 이력 상세
     * @throws BatchException 실행 이력이 없는 경우
     */
    @Override
    public BatchExecutionResponse getBatchExecution(Long dsptExecSn) {
        BatchExecutionResponse response = getExistingExecution(dsptExecSn);
        if (response.getJobExecId() == null) {
            return response;
        }

        JobExecution jobExecution = jobExplorer.getJobExecution(response.getJobExecId());
        if (jobExecution == null) {
            return response;
        }

        Map<String, String> jobParams = new LinkedHashMap<>();
        jobExecution.getJobParameters().getParameters()
                .forEach((key, parameter) -> jobParams.put(key, String.valueOf(parameter.getValue())));
        response.setJobParams(jobParams);
        response.setSteps(jobExecution.getStepExecutions()
                .stream()
                .sorted(Comparator.comparing(StepExecution::getId))
                .map(this::toStepResponse)
                .toList());
        return response;
    }

    /**
     * FAILED / STOPPED 상태이면서 JobInstance의 마지막 실행인 건을 Quartz를 통해 재시작 요청한다.
     * @param dsptExecSn 배치실행일련번호
     * @throws BatchException 실행 이력이 없거나 재시작할 수 없는 상태인 경우
     */
    @Override
    public void saveBatchExecutionRestart(Long dsptExecSn) {
        BatchExecutionResponse execution = getExistingExecution(dsptExecSn);
        JobExecution jobExecution = getJobExecution(execution, BatchErrorCode.EXECUTION_NOT_RESTARTABLE);

        BatchStatus status = jobExecution.getStatus();
        if (status != BatchStatus.FAILED && status != BatchStatus.STOPPED) {
            throw new BatchException(BatchErrorCode.EXECUTION_NOT_RESTARTABLE, dsptExecSn, status);
        }

        // 같은 JobInstance의 마지막 실행만 재시작할 수 있다.
        JobExecution lastExecution = jobExplorer.getLastJobExecution(jobExecution.getJobInstance());
        if (lastExecution == null || !lastExecution.getId().equals(jobExecution.getId())) {
            throw new BatchException(BatchErrorCode.EXECUTION_NOT_RESTARTABLE, dsptExecSn, status);
        }

        scheduleService.triggerJob(getBatchJob(execution.getJobId()),
                BatchConstants.EXEC_CLS_RESTART,
                SecurityUserUtil.getCurrentUserId(batchProperties.getSystemUserId()),
                null,
                jobExecution.getId());
    }

    /**
     * 요청 상태에 따라 실행 건을 중지(STOPPED)하거나 폐기(ABANDONED)한다.
     * @param dsptExecSn 배치실행일련번호
     * @param request 변경할 실행상태 요청
     * @throws BatchException 실행 이력이 없거나 중지/폐기할 수 없거나 상태 값이 올바르지 않은 경우
     */
    @Override
    @Transactional
    public void saveBatchExecutionStatus(Long dsptExecSn, BatchExecutionStatusRequest request) {
        BatchExecutionResponse execution = getExistingExecution(dsptExecSn);
        JobExecution jobExecution = getJobExecution(execution, BatchErrorCode.EXECUTION_NOT_STOPPABLE);

        if (BatchStatus.STOPPED.name().equals(request.getExecStatCd())) {
            stopExecution(dsptExecSn, jobExecution);
        } else if (BatchStatus.ABANDONED.name().equals(request.getExecStatCd())) {
            abandonExecution(dsptExecSn, execution, jobExecution);
        } else {
            throw new BatchException(BatchErrorCode.EXECUTION_STATUS_INVALID, request.getExecStatCd());
        }
    }

    /**
     * 실행 중인 Job에 중지를 요청한다. 실행 노드가 다음 chunk 경계에서 STOPPED로 종료하며,
     * 실행내역 갱신은 JobExecutionLogListener가 처리한다.
     * @param dsptExecSn 배치실행일련번호
     * @param jobExecution 중지할 Job 실행 정보
     * @throws BatchException 실행 중이 아니어서 중지할 수 없는 경우
     */
    private void stopExecution(Long dsptExecSn, JobExecution jobExecution) {
        try {
            jobOperator.stop(jobExecution.getId());
            log.info("[BatchExecution] stop requested. jobExecutionId={}", jobExecution.getId());
        } catch (NoSuchJobExecutionException | JobExecutionNotRunningException e) {
            throw new BatchException(BatchErrorCode.EXECUTION_NOT_STOPPABLE, dsptExecSn, jobExecution.getStatus());
        }
    }

    /**
     * 실행 건을 폐기(ABANDONED)한다. 폐기된 JobInstance는 재시작할 수 없다.
     * 노드 다운 등으로 STARTED 상태에 남은 건은 클러스터 어디에서도 실행 중이 아닐 때만 정리한다.
     * @param dsptExecSn 배치실행일련번호
     * @param execution 실행 이력
     * @param jobExecution 폐기할 Job 실행 정보
     * @throws BatchException 이미 완료/폐기되었거나 아직 실행 중인 경우
     */
    private void abandonExecution(Long dsptExecSn, BatchExecutionResponse execution, JobExecution jobExecution) {
        BatchStatus status = jobExecution.getStatus();
        if (status == BatchStatus.COMPLETED || status == BatchStatus.ABANDONED) {
            throw new BatchException(BatchErrorCode.EXECUTION_NOT_STOPPABLE, dsptExecSn, status);
        }

        JobExecution abandoned;
        if (status.isRunning()) {
            if (scheduleService.isRunningInCluster(getBatchJob(execution.getJobId()))) {
                throw new BatchException(BatchErrorCode.EXECUTION_STILL_RUNNING, dsptExecSn);
            }
            abandoned = abandonOrphanExecution(jobExecution);
        } else {
            try {
                abandoned = jobOperator.abandon(jobExecution.getId());
            } catch (NoSuchJobExecutionException | JobExecutionAlreadyRunningException e) {
                throw new BatchException(BatchErrorCode.EXECUTION_NOT_STOPPABLE, dsptExecSn, status);
            }
        }

        BatchExecution summary = JobExecutionLogListener.summarize(abandoned);
        summary.setFinaRegrId(SecurityUserUtil.getCurrentUserId(batchProperties.getSystemUserId()));
        batchExecutionMapper.updateBatchExecution(summary);
        log.info("[BatchExecution] abandoned. jobExecutionId={}, previousStatus={}", jobExecution.getId(), status);
    }

    /**
     * JobOperator.abandon은 실행 중 상태를 거부하므로, 실행 주체가 사라진(orphan) 실행 건은 직접 상태를 바꾼다.
     * @param jobExecution 폐기할 Job 실행 정보
     * @return ABANDONED로 변경된 Job 실행 정보
     */
    private JobExecution abandonOrphanExecution(JobExecution jobExecution) {
        LocalDateTime now = LocalDateTime.now();
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            if (stepExecution.getStatus().isRunning()) {
                stepExecution.setStatus(BatchStatus.ABANDONED);
                stepExecution.setExitStatus(ExitStatus.UNKNOWN);
                stepExecution.setEndTime(now);
                jobRepository.update(stepExecution);
            }
        }
        jobExecution.setStatus(BatchStatus.ABANDONED);
        jobExecution.setExitStatus(new ExitStatus(BatchStatus.ABANDONED.name(), "Abandoned by administrator"));
        jobExecution.setEndTime(now);
        jobRepository.update(jobExecution);
        return jobExecution;
    }

    /**
     * 실행 이력을 조회하고 없으면 예외를 던진다.
     * @param dsptExecSn 배치실행일련번호
     * @return 실행 이력
     * @throws BatchException 실행 이력이 없는 경우
     */
    private BatchExecutionResponse getExistingExecution(Long dsptExecSn) {
        BatchExecutionResponse execution = batchExecutionMapper.selectBatchExecution(dsptExecSn);
        if (execution == null) {
            throw new BatchException(BatchErrorCode.EXECUTION_NOT_FOUND, dsptExecSn);
        }
        return execution;
    }

    /**
     * 실행 이력에 연결된 Spring Batch JobExecution을 조회한다.
     * @param execution 실행 이력
     * @param errorCode 연결된 JobExecution이 없을 때 사용할 오류코드
     * @return Job 실행 정보
     * @throws BatchException 연결된 JobExecution이 없는 경우
     */
    private JobExecution getJobExecution(BatchExecutionResponse execution, BatchErrorCode errorCode) {
        if (execution.getJobExecId() == null) {
            throw new BatchException(errorCode, execution.getDsptExecSn(), execution.getExecStatCd());
        }
        JobExecution jobExecution = jobExplorer.getJobExecution(execution.getJobExecId());
        if (jobExecution == null) {
            throw new BatchException(errorCode, execution.getDsptExecSn(), execution.getExecStatCd());
        }
        return jobExecution;
    }

    /**
     * 배치직무를 조회하고 없으면 예외를 던진다.
     * @param jobId 배치직무 ID
     * @return 배치직무
     * @throws BatchException 배치직무가 없는 경우
     */
    private BatchJob getBatchJob(String jobId) {
        BatchJob batchJob = batchJobMapper.selectBatchJob(jobId);
        if (batchJob == null) {
            throw new BatchException(BatchErrorCode.JOB_NOT_FOUND, jobId);
        }
        return batchJob;
    }

    /**
     * StepExecution을 Step 처리 현황 응답으로 변환한다.
     * @param stepExecution Step 실행 정보
     * @return Step 처리 현황 응답
     */
    private BatchStepExecutionResponse toStepResponse(StepExecution stepExecution) {
        return BatchStepExecutionResponse.builder()
                .stepExecutionId(stepExecution.getId())
                .stepName(stepExecution.getStepName())
                .status(stepExecution.getStatus().name())
                .exitCode(stepExecution.getExitStatus().getExitCode())
                .exitDescription(stepExecution.getExitStatus().getExitDescription())
                .startTime(stepExecution.getStartTime())
                .endTime(stepExecution.getEndTime())
                .readCount(stepExecution.getReadCount())
                .writeCount(stepExecution.getWriteCount())
                .commitCount(stepExecution.getCommitCount())
                .rollbackCount(stepExecution.getRollbackCount())
                .readSkipCount(stepExecution.getReadSkipCount())
                .processSkipCount(stepExecution.getProcessSkipCount())
                .writeSkipCount(stepExecution.getWriteSkipCount())
                .filterCount(stepExecution.getFilterCount())
                .build();
    }
}
