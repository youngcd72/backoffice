package kr.or.coder.batch.common.quartz;

import kr.or.coder.batch.common.config.BatchProperties;
import kr.or.coder.batch.common.support.BatchConstants;
import kr.or.coder.batch.common.support.BatchJobLocator;
import kr.or.coder.batch.common.support.BatchTextUtil;
import kr.or.coder.batch.common.support.JobParametersFactory;
import kr.or.coder.batch.frame.entity.BatchExecution;
import kr.or.coder.batch.frame.entity.BatchJob;
import kr.or.coder.batch.frame.mapper.BatchExecutionMapper;
import kr.or.coder.batch.frame.mapper.BatchJobMapper;
import kr.or.coder.batch.frame.service.BatchAlarmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * Quartz Trigger 발생 시 Spring Batch Job을 실행한다.
 * <p>
 * JobLauncher는 동기(SyncTaskExecutor)로 동작하므로 Quartz 워커 스레드가 Job 종료까지 점유된다.
 * 클러스터 단일 실행 보장과 동시 실행 제어를 Quartz에 맡기기 위함이다.
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
public class BatchLaunchExecutor {

    private final BatchJobMapper batchJobMapper;
    private final BatchExecutionMapper batchExecutionMapper;
    private final BatchJobLocator batchJobLocator;
    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;
    private final JobParametersFactory jobParametersFactory;
    private final BatchProperties batchProperties;
    private final BatchAlarmService batchAlarmService;

    /**
     * Quartz 실행 컨텍스트의 JobDataMap을 읽어 해당 배치 Job을 기동한다.
     * 미등록 직무나 다중실행 불가 직무의 중복 실행은 건너뛰고, 기동 실패는 실행내역에 남긴다.
     * @param context Quartz 실행 컨텍스트
     */
    public void launch(JobExecutionContext context) {
        JobDataMap dataMap = context.getMergedJobDataMap();
        String jobId = dataMap.getString(BatchConstants.DATA_JOB_ID);
        String execClsCd = StringUtils.hasText(dataMap.getString(BatchConstants.DATA_EXEC_CLS_CD))
                ? dataMap.getString(BatchConstants.DATA_EXEC_CLS_CD)
                : BatchConstants.EXEC_CLS_SCHEDULE;
        String reqUsrId = StringUtils.hasText(dataMap.getString(BatchConstants.DATA_REQ_USR_ID))
                ? dataMap.getString(BatchConstants.DATA_REQ_USR_ID)
                : batchProperties.getSystemUserId();
        String execSrvNm = getSchedulerInstanceId(context);

        BatchJob batchJob = batchJobMapper.selectBatchJob(jobId);
        if (batchJob == null) {
            log.warn("[{}] batch job is not registered in TB_DSPT_JOB_M. launch skipped.", jobId);
            return;
        }

        try {
            Job job = batchJobLocator.getJob(batchJob.getExecClassNm());

            if (!BatchConstants.YES.equals(batchJob.getMltExecYn())
                    && !jobExplorer.findRunningJobExecutions(job.getName()).isEmpty()) {
                log.warn("[{}] job is already running and multiple execution is not allowed. launch skipped.", jobId);
                saveLaunchResult(jobId, execClsCd, BatchConstants.EXEC_STAT_SKIP, null, execSrvNm, reqUsrId);
                return;
            }

            JobParameters jobParameters = createJobParameters(context, dataMap, batchJob, execClsCd, reqUsrId, execSrvNm);
            JobExecution jobExecution = jobLauncher.run(job, jobParameters);
            log.info("[{}] launch completed. jobExecutionId={}, status={}",
                    jobId, jobExecution.getId(), jobExecution.getStatus());
        } catch (Exception e) {
            // Job 실행 자체가 시작되지 못한 경우 (Job 미등록, 파라미터 오류, 이미 완료된 JobInstance 재실행 등)
            log.error("[{}] batch launch failed.", jobId, e);
            String errMsgCont = BatchTextUtil.formatErrorMessage(e);
            saveLaunchResult(jobId, execClsCd, BatchConstants.EXEC_STAT_FAILED, errMsgCont, execSrvNm, reqUsrId);
            // Job 실행 중 실패는 JobExecutionLogListener 가, 기동 자체가 실패한 경우는 여기서 알람을 보낸다
            batchAlarmService.saveFailureAlarm(jobId, errMsgCont);
        }
    }

    /**
     * 재시작 여부에 따라 신규 실행용 또는 재시작용 JobParameters를 만든다.
     * @param context Quartz 실행 컨텍스트
     * @param dataMap Quartz JobDataMap
     * @param batchJob 배치직무 기본 정보
     * @param execClsCd 실행구분코드
     * @param reqUsrId 실행 요청 사용자 ID
     * @param execSrvNm 실행 서버명
     * @return 생성된 JobParameters
     * @throws IllegalStateException 재시작 대상 JobExecution이 없는 경우
     */
    private JobParameters createJobParameters(JobExecutionContext context,
                                              JobDataMap dataMap,
                                              BatchJob batchJob,
                                              String execClsCd,
                                              String reqUsrId,
                                              String execSrvNm) {
        String restartJobExecId = dataMap.getString(BatchConstants.DATA_RESTART_JOB_EXEC_ID);
        if (StringUtils.hasText(restartJobExecId)) {
            JobExecution previous = jobExplorer.getJobExecution(Long.valueOf(restartJobExecId));
            if (previous == null) {
                throw new IllegalStateException("JobExecution not found. jobExecutionId=" + restartJobExecId);
            }
            return jobParametersFactory.createRestartJobParameters(
                    previous.getJobParameters(), batchJob.getJobId(), execClsCd, reqUsrId, execSrvNm);
        }
        return jobParametersFactory.createJobParameters(
                batchJob.getJobId(),
                batchJob.getJobParmCont(),
                dataMap.getString(BatchConstants.DATA_JOB_PARM_CONT),
                context.getFireInstanceId(),
                execClsCd,
                reqUsrId,
                execSrvNm);
    }

    /**
     * JobExecution이 생성되지 않은 실행(SKIP, 기동 실패)을 실행내역에 남긴다.
     * @param jobId 배치직무 ID
     * @param execClsCd 실행구분코드
     * @param execStatCd 실행상태코드
     * @param errMsgCont 오류메시지 내용 (없으면 null)
     * @param execSrvNm 실행 서버명
     * @param reqUsrId 실행 요청 사용자 ID
     */
    private void saveLaunchResult(String jobId, String execClsCd, String execStatCd,
                                  String errMsgCont, String execSrvNm, String reqUsrId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            String regrId = BatchTextUtil.limitUserId(reqUsrId);
            batchExecutionMapper.insertBatchExecution(BatchExecution.builder()
                    .jobId(jobId)
                    .execClsCd(execClsCd)
                    .execStatCd(execStatCd)
                    .execStDttm(now)
                    .execEndDttm(now)
                    .errMsgCont(errMsgCont)
                    .execSrvNm(execSrvNm)
                    .firsRegrId(regrId)
                    .finaRegrId(regrId)
                    .build());
        } catch (Exception e) {
            log.error("[{}] failed to insert TB_DSPT_EXEC_L. execStatCd={}", jobId, execStatCd, e);
        }
    }

    /**
     * Quartz 스케줄러 인스턴스 ID(실행 서버명)를 조회한다.
     * @param context Quartz 실행 컨텍스트
     * @return 스케줄러 인스턴스 ID, 조회 실패 시 null
     */
    private String getSchedulerInstanceId(JobExecutionContext context) {
        try {
            return context.getScheduler().getSchedulerInstanceId();
        } catch (SchedulerException e) {
            return null;
        }
    }
}
