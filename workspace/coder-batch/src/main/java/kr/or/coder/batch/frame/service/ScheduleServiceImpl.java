package kr.or.coder.batch.frame.service;

import kr.or.coder.batch.common.config.BatchProperties;
import kr.or.coder.batch.common.exception.BatchErrorCode;
import kr.or.coder.batch.common.exception.BatchException;
import kr.or.coder.batch.common.quartz.BatchLaunchQuartzJob;
import kr.or.coder.batch.common.quartz.SerialBatchLaunchQuartzJob;
import kr.or.coder.batch.common.support.BatchConstants;
import kr.or.coder.batch.common.support.BatchJobLocator;
import kr.or.coder.batch.frame.dto.BatchJobResponse;
import kr.or.coder.batch.frame.dto.BatchJobSearchRequest;
import kr.or.coder.batch.frame.entity.BatchJob;
import kr.or.coder.batch.frame.mapper.BatchExecutionMapper;
import kr.or.coder.batch.frame.mapper.BatchJobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/**
 * Quartz Scheduler 제어 Service 구현체
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
public class ScheduleServiceImpl implements ScheduleService {

    private final Scheduler scheduler;
    private final BatchJobLocator batchJobLocator;
    private final BatchJobMapper batchJobMapper;
    private final BatchExecutionMapper batchExecutionMapper;
    private final BatchProperties batchProperties;

    /**
     * TB_DSPT_JOB_M 전체를 Quartz와 동기화한다. 실행클래스에 Job 빈이 없는 직무는 건너뛰고, DB에 없는 Quartz Job은 삭제한다.
     * @throws BatchException 스케줄러 오류가 발생한 경우
     */
    @Override
    public void syncSchedules() {
        List<BatchJob> batchJobs = batchJobMapper.selectBatchJobList(new BatchJobSearchRequest());
        Set<JobKey> validJobKeys = new HashSet<>();

        for (BatchJob batchJob : batchJobs) {
            if (!batchJobLocator.isRegistered(batchJob.getExecClassNm())) {
                log.warn("[ScheduleSync] job bean of '{}' is not registered. schedule skipped. jobId={}",
                        batchJob.getExecClassNm(), batchJob.getJobId());
                continue;
            }
            try {
                saveSchedule(batchJob, null);
                validJobKeys.add(createJobKey(batchJob));
            } catch (Exception e) {
                log.error("[ScheduleSync] failed to register schedule. jobId={}", batchJob.getJobId(), e);
            }
        }

        try {
            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.anyJobGroup())) {
                if (!validJobKeys.contains(jobKey)) {
                    scheduler.deleteJob(jobKey);
                    log.info("[ScheduleSync] removed orphan quartz job. jobKey={}", jobKey);
                }
            }
        } catch (SchedulerException e) {
            throw new BatchException(BatchErrorCode.SCHEDULER_ERROR, e.getMessage());
        }

        log.info("[ScheduleSync] synchronized {} batch job(s) with quartz.", validJobKeys.size());
    }

    /**
     * JobDetail을 저장하고 사용여부·스케줄 표현식에 따라 CronTrigger를 등록/변경/해제한다.
     * @param batchJob 대상 배치직무
     * @param previousWrkMdulCd 수정 전 업무모듈코드 (변경 시 이전 JobKey 삭제). 신규면 null
     * @throws BatchException 스케줄 표현식이 올바르지 않거나 스케줄러 오류가 발생한 경우
     */
    @Override
    public void saveSchedule(BatchJob batchJob, String previousWrkMdulCd) {
        try {
            if (previousWrkMdulCd != null && !previousWrkMdulCd.equals(batchJob.getWrkMdulCd())) {
                scheduler.deleteJob(JobKey.jobKey(batchJob.getJobId(), previousWrkMdulCd));
            }

            JobKey jobKey = createJobKey(batchJob);
            scheduler.addJob(createJobDetail(batchJob, jobKey), true);

            TriggerKey triggerKey = createTriggerKey(batchJob);
            if (!isScheduled(batchJob)) {
                if (scheduler.checkExists(triggerKey)) {
                    scheduler.unscheduleJob(triggerKey);
                    log.info("[Schedule] unscheduled. triggerKey={}", triggerKey);
                }
                return;
            }

            CronTrigger newTrigger = createCronTrigger(batchJob, jobKey, triggerKey);
            Trigger existing = scheduler.getTrigger(triggerKey);
            if (existing == null) {
                scheduleNewTrigger(newTrigger);
                log.info("[Schedule] scheduled. triggerKey={}, cron={}", triggerKey, batchJob.getScheExpr());
            } else if (isTriggerChanged(existing, newTrigger)) {
                // 일시정지 상태는 재스케줄 후에도 유지한다.
                Trigger.TriggerState state = scheduler.getTriggerState(triggerKey);
                scheduler.rescheduleJob(triggerKey, newTrigger);
                if (state == Trigger.TriggerState.PAUSED) {
                    scheduler.pauseTrigger(triggerKey);
                }
                log.info("[Schedule] rescheduled. triggerKey={}, cron={}", triggerKey, batchJob.getScheExpr());
            }
        } catch (SchedulerException e) {
            throw new BatchException(BatchErrorCode.SCHEDULER_ERROR, e.getMessage());
        }
    }

    /**
     * 배치직무의 Quartz Job을 삭제한다.
     * @param batchJob 대상 배치직무
     * @throws BatchException 스케줄러 오류가 발생한 경우
     */
    @Override
    public void deleteSchedule(BatchJob batchJob) {
        try {
            scheduler.deleteJob(createJobKey(batchJob));
        } catch (SchedulerException e) {
            throw new BatchException(BatchErrorCode.SCHEDULER_ERROR, e.getMessage());
        }
    }

    /**
     * Trigger를 일시정지(PAUSED)하거나 재개(NORMAL)한다.
     * @param batchJob 대상 배치직무
     * @param schedStatus 변경할 스케줄 상태 (PAUSED / NORMAL)
     * @throws BatchException 스케줄이 없거나 상태 값이 올바르지 않거나 스케줄러 오류가 발생한 경우
     */
    @Override
    public void saveScheduleStatus(BatchJob batchJob, String schedStatus) {
        TriggerKey triggerKey = createTriggerKey(batchJob);
        try {
            if (!scheduler.checkExists(triggerKey)) {
                throw new BatchException(BatchErrorCode.SCHEDULE_NOT_FOUND, batchJob.getJobId());
            }
            if (Trigger.TriggerState.PAUSED.name().equals(schedStatus)) {
                scheduler.pauseTrigger(triggerKey);
            } else if (Trigger.TriggerState.NORMAL.name().equals(schedStatus)) {
                scheduler.resumeTrigger(triggerKey);
            } else {
                throw new BatchException(BatchErrorCode.SCHEDULE_STATUS_INVALID, schedStatus);
            }
            log.info("[Schedule] status changed. triggerKey={}, schedStatus={}", triggerKey, schedStatus);
        } catch (SchedulerException e) {
            throw new BatchException(BatchErrorCode.SCHEDULER_ERROR, e.getMessage());
        }
    }

    /**
     * 실행 정보를 JobDataMap에 담아 Quartz Job을 즉시 실행한다. Job이 없으면 먼저 등록한다.
     * @param batchJob 대상 배치직무
     * @param execClsCd 실행구분코드
     * @param reqUsrId 실행 요청 사용자 ID
     * @param manualParmCont 수동 실행 파라미터(JSON). 없으면 null
     * @param restartJobExecId 재시작 대상 JobExecution ID. 재시작이 아니면 null
     * @throws BatchException 스케줄러 오류가 발생한 경우
     */
    @Override
    public void triggerJob(BatchJob batchJob, String execClsCd, String reqUsrId,
                           String manualParmCont, Long restartJobExecId) {
        JobKey jobKey = createJobKey(batchJob);
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(BatchConstants.DATA_JOB_ID, batchJob.getJobId());
        dataMap.put(BatchConstants.DATA_EXEC_CLS_CD, execClsCd);
        dataMap.put(BatchConstants.DATA_REQ_USR_ID, reqUsrId);
        if (manualParmCont != null) {
            dataMap.put(BatchConstants.DATA_JOB_PARM_CONT, manualParmCont);
        }
        if (restartJobExecId != null) {
            dataMap.put(BatchConstants.DATA_RESTART_JOB_EXEC_ID, String.valueOf(restartJobExecId));
        }

        try {
            if (!scheduler.checkExists(jobKey)) {
                saveSchedule(batchJob, null);
            }
            scheduler.triggerJob(jobKey, dataMap);
            log.info("[Schedule] triggered. jobKey={}, execClsCd={}, reqUsrId={}", jobKey, execClsCd, reqUsrId);
        } catch (SchedulerException e) {
            throw new BatchException(BatchErrorCode.SCHEDULER_ERROR, e.getMessage());
        }
    }

    /**
     * 응답에 Trigger 상태와 다음/이전 실행 일시를 채운다. Trigger가 없으면 NONE, 조회 실패 시 ERROR.
     * @param response 값을 채울 배치직무 응답
     */
    @Override
    public void fillScheduleInfo(BatchJobResponse response) {
        TriggerKey triggerKey = TriggerKey.triggerKey(response.getJobId(), response.getWrkMdulCd());
        try {
            Trigger trigger = scheduler.getTrigger(triggerKey);
            if (trigger == null) {
                response.setSchedStatus(Trigger.TriggerState.NONE.name());
                return;
            }
            response.setSchedStatus(scheduler.getTriggerState(triggerKey).name());
            response.setNextFireDttm(toLocalDateTime(trigger.getNextFireTime()));
            response.setPrevFireDttm(toLocalDateTime(trigger.getPreviousFireTime()));
        } catch (SchedulerException e) {
            log.warn("[Schedule] failed to read trigger. triggerKey={}", triggerKey, e);
            response.setSchedStatus(Trigger.TriggerState.ERROR.name());
        }
    }

    /**
     * QRTZ_FIRED_TRIGGERS 기준으로 클러스터 전체에서 실행 중인지 확인한다.
     * @param batchJob 대상 배치직무
     * @return 실행 중이면 true
     * @throws BatchException 스케줄러 오류가 발생한 경우
     */
    @Override
    public boolean isRunningInCluster(BatchJob batchJob) {
        try {
            return batchExecutionMapper.selectFiredTriggerCount(
                    scheduler.getSchedulerName(), batchJob.getJobId(), batchJob.getWrkMdulCd()) > 0;
        } catch (SchedulerException e) {
            throw new BatchException(BatchErrorCode.SCHEDULER_ERROR, e.getMessage());
        }
    }

    /**
     * 새 Trigger를 등록한다. 다른 노드가 동시에 등록했으면 재스케줄한다.
     * @param trigger 등록할 CronTrigger
     * @throws SchedulerException 스케줄러 오류가 발생한 경우
     */
    private void scheduleNewTrigger(CronTrigger trigger) throws SchedulerException {
        try {
            scheduler.scheduleJob(trigger);
        } catch (ObjectAlreadyExistsException e) {
            // 클러스터의 다른 노드가 동시에 등록한 경우
            scheduler.rescheduleJob(trigger.getKey(), trigger);
        }
    }

    /**
     * 사용여부가 Y이고 스케줄 표현식이 있어 Trigger 등록 대상인지 판별한다.
     * @param batchJob 대상 배치직무
     * @return Trigger 등록 대상이면 true
     */
    private boolean isScheduled(BatchJob batchJob) {
        return BatchConstants.YES.equals(batchJob.getUseYn()) && StringUtils.hasText(batchJob.getScheExpr());
    }

    /**
     * 다중실행여부에 따라 병렬/직렬 Quartz Job 클래스를 선택해 JobDetail을 만든다.
     * @param batchJob 대상 배치직무
     * @param jobKey Quartz JobKey
     * @return 영속(durable) JobDetail
     */
    private JobDetail createJobDetail(BatchJob batchJob, JobKey jobKey) {
        Class<? extends BatchLaunchQuartzJob> jobClass = BatchConstants.YES.equals(batchJob.getMltExecYn())
                ? BatchLaunchQuartzJob.class
                : SerialBatchLaunchQuartzJob.class;
        return JobBuilder.newJob(jobClass)
                .withIdentity(jobKey)
                .withDescription(batchJob.getJobNm())
                .usingJobData(BatchConstants.DATA_JOB_ID, batchJob.getJobId())
                .storeDurably()
                .build();
    }

    /**
     * 스케줄 표현식, 시간대, 미스파이어정책으로 CronTrigger를 만든다.
     * @param batchJob 대상 배치직무
     * @param jobKey Quartz JobKey
     * @param triggerKey Quartz TriggerKey
     * @return 생성된 CronTrigger
     * @throws BatchException 스케줄 표현식이 올바르지 않은 경우
     */
    private CronTrigger createCronTrigger(BatchJob batchJob, JobKey jobKey, TriggerKey triggerKey) {
        CronScheduleBuilder scheduleBuilder;
        try {
            scheduleBuilder = CronScheduleBuilder.cronSchedule(batchJob.getScheExpr())
                    .inTimeZone(TimeZone.getTimeZone(batchProperties.getTimeZone()));
        } catch (RuntimeException e) {
            throw new BatchException(BatchErrorCode.SCHEDULE_INVALID_EXPRESSION, batchJob.getScheExpr());
        }

        if (BatchConstants.MSFR_PLCY_FIRE_ONCE_NOW.equals(batchJob.getMsfrPlcyCd())) {
            scheduleBuilder = scheduleBuilder.withMisfireHandlingInstructionFireAndProceed();
        } else {
            scheduleBuilder = scheduleBuilder.withMisfireHandlingInstructionDoNothing();
        }

        return TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .withDescription(batchJob.getJobNm())
                .forJob(jobKey)
                .withSchedule(scheduleBuilder)
                .build();
    }

    /**
     * 기존 Trigger와 새 Trigger의 cron 표현식, 미스파이어 정책, 시간대가 다른지 비교한다.
     * @param existing 기존 Trigger
     * @param newTrigger 새 CronTrigger
     * @return 변경되었으면 true
     */
    private boolean isTriggerChanged(Trigger existing, CronTrigger newTrigger) {
        if (!(existing instanceof CronTrigger cronTrigger)) {
            return true;
        }
        return !cronTrigger.getCronExpression().equals(newTrigger.getCronExpression())
                || cronTrigger.getMisfireInstruction() != newTrigger.getMisfireInstruction()
                || !cronTrigger.getTimeZone().getID().equals(newTrigger.getTimeZone().getID());
    }

    /**
     * 배치직무 ID와 업무모듈코드로 Quartz JobKey를 만든다.
     * @param batchJob 대상 배치직무
     * @return Quartz JobKey
     */
    private JobKey createJobKey(BatchJob batchJob) {
        return JobKey.jobKey(batchJob.getJobId(), batchJob.getWrkMdulCd());
    }

    /**
     * 배치직무 ID와 업무모듈코드로 Quartz TriggerKey를 만든다.
     * @param batchJob 대상 배치직무
     * @return Quartz TriggerKey
     */
    private TriggerKey createTriggerKey(BatchJob batchJob) {
        return TriggerKey.triggerKey(batchJob.getJobId(), batchJob.getWrkMdulCd());
    }

    /**
     * Date를 설정된 시간대의 LocalDateTime으로 변환한다.
     * @param date 변환할 일시
     * @return LocalDateTime, date가 null이면 null
     */
    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.of(batchProperties.getTimeZone()));
    }
}
