package kr.or.coder.batch.frame.service;

import kr.or.coder.batch.common.config.BatchProperties;
import kr.or.coder.batch.common.exception.BatchErrorCode;
import kr.or.coder.batch.common.exception.BatchException;
import kr.or.coder.batch.common.support.BatchConstants;
import kr.or.coder.batch.common.support.BatchJobLocator;
import kr.or.coder.batch.common.support.JobParametersFactory;
import kr.or.coder.batch.common.support.SecurityUserUtil;
import kr.or.coder.batch.frame.dto.BatchJobExecutionRequest;
import kr.or.coder.batch.frame.dto.BatchJobRequest;
import kr.or.coder.batch.frame.dto.BatchJobResponse;
import kr.or.coder.batch.frame.dto.BatchJobScheduleRequest;
import kr.or.coder.batch.frame.dto.BatchJobSearchRequest;
import kr.or.coder.batch.frame.entity.BatchJob;
import kr.or.coder.batch.frame.mapper.BatchAlarmMapper;
import kr.or.coder.batch.frame.mapper.BatchExecutionMapper;
import kr.or.coder.batch.frame.mapper.BatchJobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronExpression;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 배치직무 등록/수정/삭제, 스케줄 제어, 수동 실행 Service 구현체
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
public class BatchJobServiceImpl implements BatchJobService {

    private final BatchJobMapper batchJobMapper;
    private final BatchExecutionMapper batchExecutionMapper;
    private final BatchAlarmMapper batchAlarmMapper;
    private final ScheduleService scheduleService;
    private final BatchJobLocator batchJobLocator;
    private final JobExplorer jobExplorer;
    private final JobParametersFactory jobParametersFactory;
    private final BatchProperties batchProperties;

    /**
     * 배치직무 목록을 조회하고 Job 빈 등록 여부와 스케줄 상태를 채운다.
     * @param request 배치직무 검색 조건
     * @return 배치직무 목록
     */
    @Override
    public List<BatchJobResponse> getBatchJobList(BatchJobSearchRequest request) {
        return batchJobMapper.selectBatchJobList(request)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 배치직무 1건을 조회하고 Job 빈 등록 여부, 스케줄 상태, 실패 알람 수신자를 채운다.
     * @param jobId 배치직무 ID
     * @return 배치직무 상세
     * @throws BatchException 배치직무가 없는 경우
     */
    @Override
    public BatchJobResponse getBatchJob(String jobId) {
        BatchJobResponse response = toResponse(getExistingBatchJob(jobId));
        response.setAlrmRcvrList(batchAlarmMapper.selectBatchAlarmRcvrList(jobId));
        return response;
    }

    /**
     * 배치직무를 검증·저장(insert/update 분기)하고 실패 알람 수신자(요청에 있을 때)와 Quartz 스케줄을 반영한다.
     * @param request 배치직무 저장 요청
     * @param isNew 신규 등록 요청 여부 (true면 이미 있는 배치ID를 덮어쓰지 않고 오류)
     * @throws BatchException 신규 등록인데 이미 있는 배치ID이거나, Job 빈이 미등록이거나 스케줄 표현식/파라미터 형식이 올바르지 않거나 없는 알람 수신자가 있는 경우
     */
    @Override
    @Transactional
    public void saveBatchJob(BatchJobRequest request, boolean isNew) {
        BatchJob existing = batchJobMapper.selectBatchJob(request.getJobId());
        if (isNew && existing != null) {
            throw new BatchException(BatchErrorCode.JOB_DUPLICATE, request.getJobId());
        }
        validateBatchJob(request);

        String userId = SecurityUserUtil.getCurrentUserId(batchProperties.getSystemUserId());
        BatchJob batchJob = BatchJob.builder()
                .jobId(request.getJobId())
                .jobNm(request.getJobNm())
                .execClassNm(request.getExecClassNm().trim())
                .wrkMdulCd(request.getWrkMdulCd())
                .scheExpr(StringUtils.hasText(request.getScheExpr()) ? request.getScheExpr().trim() : null)
                .msfrPlcyCd(StringUtils.hasText(request.getMsfrPlcyCd())
                        ? request.getMsfrPlcyCd() : BatchConstants.MSFR_PLCY_DO_NOTHING)
                .mltExecYn(StringUtils.hasText(request.getMltExecYn()) ? request.getMltExecYn() : BatchConstants.NO)
                .jobParmCont(StringUtils.hasText(request.getJobParmCont()) ? request.getJobParmCont() : null)
                .useYn(request.getUseYn())
                .rmk(request.getRmk())
                .reflPrgmId(request.getReflPrgmId())
                .firsRegrId(userId)
                .finaRegrId(userId)
                .build();

        if (existing == null) {
            batchJobMapper.insertBatchJob(batchJob);
        } else {
            batchJobMapper.updateBatchJob(batchJob);
        }
        if (request.getAlrmRcvrIdList() != null) {
            saveBatchAlarmRcvrList(request.getJobId(), request.getAlrmRcvrIdList(), userId);
        }

        // Quartz JDBC JobStore는 같은 DataSource/트랜잭션에 참여하므로 DB 저장과 함께 커밋·롤백된다.
        scheduleService.saveSchedule(batchJob, existing == null ? null : existing.getWrkMdulCd());
        log.info("[BatchJob] saved. jobId={}, useYn={}, scheExpr={}", batchJob.getJobId(), batchJob.getUseYn(), batchJob.getScheExpr());
    }

    /**
     * 배치직무의 실패 알람 수신자를 요청 목록으로 전체 교체한다 (중복 제거).
     * @param jobId 배치직무 ID
     * @param rcvrIdList 수신자 사용자ID 목록 (빈 목록이면 전체 삭제)
     * @param userId 저장 사용자ID
     * @throws BatchException 사용자 테이블에 없는 수신자가 있는 경우
     */
    private void saveBatchAlarmRcvrList(String jobId, List<String> rcvrIdList, String userId) {
        List<String> distinctIdList = rcvrIdList.stream().map(String::trim).distinct().toList();
        if (!distinctIdList.isEmpty()) {
            Set<String> existingIdSet = new HashSet<>(batchAlarmMapper.selectExistingUsrIdList(distinctIdList));
            for (String usrId : distinctIdList) {
                if (!existingIdSet.contains(usrId)) {
                    throw new BatchException(BatchErrorCode.ALARM_RECEIVER_NOT_FOUND, usrId);
                }
            }
        }

        batchAlarmMapper.deleteBatchAlarmRcvrByJobId(jobId);
        for (String usrId : distinctIdList) {
            batchAlarmMapper.insertBatchAlarmRcvr(jobId, usrId, userId);
        }
    }

    /**
     * 실행 중이 아닌 배치직무를 실행내역, 알람수신자, Quartz 스케줄과 함께 삭제한다.
     * @param jobId 배치직무 ID
     * @throws BatchException 배치직무가 없거나 클러스터에서 실행 중인 경우
     */
    @Override
    @Transactional
    public void deleteBatchJob(String jobId) {
        BatchJob batchJob = getExistingBatchJob(jobId);
        if (scheduleService.isRunningInCluster(batchJob)) {
            throw new BatchException(BatchErrorCode.JOB_ALREADY_RUNNING, jobId);
        }

        batchExecutionMapper.deleteBatchExecutionByJobId(jobId);
        batchAlarmMapper.deleteBatchAlarmRcvrByJobId(jobId);
        batchJobMapper.deleteBatchJob(jobId);
        scheduleService.deleteSchedule(batchJob);
        log.info("[BatchJob] deleted. jobId={}", jobId);
    }

    /**
     * 배치직무 스케줄을 일시정지하거나 재개한다.
     * @param jobId 배치직무 ID
     * @param request 스케줄 상태 변경 요청
     * @throws BatchException 배치직무 또는 스케줄이 없거나 상태 값이 올바르지 않은 경우
     */
    @Override
    @Transactional
    public void saveBatchJobSchedule(String jobId, BatchJobScheduleRequest request) {
        scheduleService.saveScheduleStatus(getExistingBatchJob(jobId), request.getSchedStatus());
    }

    /**
     * 배치직무 수동 실행을 Quartz를 통해 요청한다. 다중실행 불가 직무가 실행 중이면 거부한다.
     * @param jobId 배치직무 ID
     * @param request 수동 실행 요청 (null 가능)
     * @throws BatchException 배치직무가 없거나 Job 빈이 미등록이거나 이미 실행 중인 경우
     */
    @Override
    public void saveBatchJobExecution(String jobId, BatchJobExecutionRequest request) {
        BatchJob batchJob = getExistingBatchJob(jobId);
        Job job = batchJobLocator.getJob(batchJob.getExecClassNm());

        if (!BatchConstants.YES.equals(batchJob.getMltExecYn())
                && !jobExplorer.findRunningJobExecutions(job.getName()).isEmpty()) {
            throw new BatchException(BatchErrorCode.JOB_ALREADY_RUNNING, jobId);
        }

        String manualParmCont = request == null ? null : jobParametersFactory.toJson(request.getParams());
        scheduleService.triggerJob(batchJob,
                BatchConstants.EXEC_CLS_MANUAL,
                SecurityUserUtil.getCurrentUserId(batchProperties.getSystemUserId()),
                manualParmCont,
                null);
    }

    /**
     * 배치직무를 조회하고 없으면 예외를 던진다.
     * @param jobId 배치직무 ID
     * @return 배치직무
     * @throws BatchException 배치직무가 없는 경우
     */
    private BatchJob getExistingBatchJob(String jobId) {
        BatchJob batchJob = batchJobMapper.selectBatchJob(jobId);
        if (batchJob == null) {
            throw new BatchException(BatchErrorCode.JOB_NOT_FOUND, jobId);
        }
        return batchJob;
    }

    /**
     * 저장 요청의 Job 빈 등록 여부, 스케줄 표현식, 파라미터 JSON 형식을 검증한다.
     * @param request 배치직무 저장 요청
     * @throws BatchException 검증에 실패한 경우
     */
    private void validateBatchJob(BatchJobRequest request) {
        // 실행클래스에 Job 빈이 1개 정의되어 있는지 검증 (오류 시 BatchException)
        batchJobLocator.getJob(request.getExecClassNm().trim());

        if (StringUtils.hasText(request.getScheExpr())
                && !CronExpression.isValidExpression(request.getScheExpr().trim())) {
            throw new BatchException(BatchErrorCode.SCHEDULE_INVALID_EXPRESSION, request.getScheExpr());
        }

        // JSON 형식 검증 (오류 시 BatchException)
        jobParametersFactory.parseParameters(request.getJobParmCont());
    }

    /**
     * 배치직무를 응답으로 변환하고 Job 빈 등록 여부와 스케줄 정보를 채운다.
     * @param batchJob 배치직무
     * @return 배치직무 응답
     */
    private BatchJobResponse toResponse(BatchJob batchJob) {
        BatchJobResponse response = BatchJobResponse.builder()
                .jobId(batchJob.getJobId())
                .jobNm(batchJob.getJobNm())
                .execClassNm(batchJob.getExecClassNm())
                .wrkMdulCd(batchJob.getWrkMdulCd())
                .scheExpr(batchJob.getScheExpr())
                .msfrPlcyCd(batchJob.getMsfrPlcyCd())
                .mltExecYn(batchJob.getMltExecYn())
                .jobParmCont(batchJob.getJobParmCont())
                .useYn(batchJob.getUseYn())
                .rmk(batchJob.getRmk())
                .reflPrgmId(batchJob.getReflPrgmId())
                .firsRegrId(batchJob.getFirsRegrId())
                .firsRegDt(batchJob.getFirsRegDt())
                .finaRegrId(batchJob.getFinaRegrId())
                .finaRegDt(batchJob.getFinaRegDt())
                .jobBeanYn(batchJobLocator.isRegistered(batchJob.getExecClassNm()) ? BatchConstants.YES : BatchConstants.NO)
                .lastDsptExecSn(batchJob.getLastDsptExecSn())
                .lastExecStatCd(batchJob.getLastExecStatCd())
                .lastExecStDttm(batchJob.getLastExecStDttm())
                .lastExecEndDttm(batchJob.getLastExecEndDttm())
                .lastProcCnt(batchJob.getLastProcCnt())
                .lastSuccCnt(batchJob.getLastSuccCnt())
                .lastFailCnt(batchJob.getLastFailCnt())
                .lastErrMsgCont(batchJob.getLastErrMsgCont())
                .build();
        scheduleService.fillScheduleInfo(response);
        return response;
    }
}
