package kr.or.coder.batch.common.listener;

import kr.or.coder.batch.common.config.BatchProperties;
import kr.or.coder.batch.common.support.BatchConstants;
import kr.or.coder.batch.common.support.BatchTextUtil;
import kr.or.coder.batch.frame.entity.BatchExecution;
import kr.or.coder.batch.frame.mapper.BatchExecutionMapper;
import kr.or.coder.batch.frame.service.BatchAlarmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 모든 Job의 시작/종료를 TB_DSPT_EXEC_L(배치실행내역)에 기록하고 MDC를 설정한다.
 * 실행내역 기록 실패가 업무 Job 실패로 번지지 않도록 예외는 로그만 남긴다.
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
@Component
@RequiredArgsConstructor
public class JobExecutionLogListener implements JobExecutionListener {

    private final BatchExecutionMapper batchExecutionMapper;
    private final BatchProperties batchProperties;
    private final BatchAlarmService batchAlarmService;

    /**
     * Job 시작 시 MDC를 설정하고 실행내역을 신규 등록한다.
     * @param jobExecution 시작되는 Job 실행 정보
     */
    @Override
    public void beforeJob(JobExecution jobExecution) {
        String jobId = getJobId(jobExecution);
        MDC.put(BatchConstants.MDC_JOB_ID, jobId);
        MDC.put(BatchConstants.MDC_JOB_EXECUTION_ID, String.valueOf(jobExecution.getId()));

        JobParameters params = jobExecution.getJobParameters();
        String execClsCd = params.getString(BatchConstants.PARM_EXEC_CLS_CD, BatchConstants.EXEC_CLS_SCHEDULE);
        String regrId = BatchTextUtil.limitUserId(
                params.getString(BatchConstants.PARM_REQ_USR_ID, batchProperties.getSystemUserId()));

        log.info("[{}] job started. jobExecutionId={}, execClsCd={}, params={}",
                jobId, jobExecution.getId(), execClsCd, params);

        try {
            batchExecutionMapper.insertBatchExecution(BatchExecution.builder()
                    .jobId(jobId)
                    .jobExecId(jobExecution.getId())
                    .execClsCd(execClsCd)
                    .execStatCd(jobExecution.getStatus().name())
                    .execStDttm(jobExecution.getStartTime() != null ? jobExecution.getStartTime() : LocalDateTime.now())
                    .execSrvNm(params.getString(BatchConstants.PARM_EXEC_SRV_NM))
                    .firsRegrId(regrId)
                    .finaRegrId(regrId)
                    .build());
        } catch (Exception e) {
            log.error("[{}] failed to insert TB_DSPT_EXEC_L. jobExecutionId={}", jobId, jobExecution.getId(), e);
        }
    }

    /**
     * Job 종료 시 실행내역에 종료 상태와 처리 건수를 반영하고, 실패했으면 알람을 보낸 뒤 MDC를 정리한다.
     * @param jobExecution 종료된 Job 실행 정보
     */
    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobId = getJobId(jobExecution);
        try {
            BatchExecution execution = summarize(jobExecution);
            execution.setFinaRegrId(BatchTextUtil.limitUserId(jobExecution.getJobParameters()
                    .getString(BatchConstants.PARM_REQ_USR_ID, batchProperties.getSystemUserId())));
            batchExecutionMapper.updateBatchExecution(execution);

            log.info("[{}] job finished. jobExecutionId={}, status={}, procCnt={}, succCnt={}, failCnt={}",
                    jobId, jobExecution.getId(), execution.getExecStatCd(),
                    execution.getProcCnt(), execution.getSuccCnt(), execution.getFailCnt());
        } catch (Exception e) {
            log.error("[{}] failed to update TB_DSPT_EXEC_L. jobExecutionId={}", jobId, jobExecution.getId(), e);
        }

        try {
            // 실행이 실패하면 배치직무에 등록된 수신자에게 알람 (실행내역 기록 성공 여부와 무관)
            if (jobExecution.getStatus() == BatchStatus.FAILED) {
                List<Throwable> failures = jobExecution.getAllFailureExceptions();
                batchAlarmService.saveFailureAlarm(jobId,
                        failures.isEmpty() ? null : BatchTextUtil.formatErrorMessage(failures.get(0)));
            }
        } finally {
            MDC.remove(BatchConstants.MDC_JOB_ID);
            MDC.remove(BatchConstants.MDC_JOB_EXECUTION_ID);
        }
    }

    /**
     * 실행내역에 기록할 배치ID를 구한다. 스케줄·수동 실행은 JobParameter(jobId)로 전달되고,
     * 없으면(테스트에서 JobLauncher로 직접 실행 등) Spring Batch Job 이름을 쓴다.
     * @param jobExecution Job 실행 정보
     * @return 배치ID
     */
    private String getJobId(JobExecution jobExecution) {
        return jobExecution.getJobParameters().getString(BatchConstants.PARM_JOB_ID,
                jobExecution.getJobInstance().getJobName());
    }

    /**
     * Step별 read/write/skip 건수를 합산하고 종료 상태와 오류메시지를 만든다.
     * @param jobExecution 집계 대상 Job 실행 정보
     * @return 처리/성공/실패 건수, 종료 상태, 오류메시지를 담은 실행내역
     */
    public static BatchExecution summarize(JobExecution jobExecution) {
        long procCnt = 0;
        long succCnt = 0;
        long failCnt = 0;
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            procCnt += stepExecution.getReadCount();
            succCnt += stepExecution.getWriteCount();
            failCnt += stepExecution.getSkipCount();
        }

        List<Throwable> failures = jobExecution.getAllFailureExceptions();
        String errMsgCont = failures.isEmpty() ? null : BatchTextUtil.formatErrorMessage(failures.get(0));

        return BatchExecution.builder()
                .jobExecId(jobExecution.getId())
                .execStatCd(jobExecution.getStatus().name())
                .execEndDttm(jobExecution.getEndTime() != null ? jobExecution.getEndTime() : LocalDateTime.now())
                .procCnt(procCnt)
                .succCnt(succCnt)
                .failCnt(failCnt)
                .errMsgCont(errMsgCont)
                .build();
    }
}
