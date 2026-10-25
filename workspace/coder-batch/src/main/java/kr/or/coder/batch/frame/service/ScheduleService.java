package kr.or.coder.batch.frame.service;

import kr.or.coder.batch.frame.dto.BatchJobResponse;
import kr.or.coder.batch.frame.entity.BatchJob;

/**
 * Quartz Scheduler 제어 (JobDetail / CronTrigger 등록·변경·삭제, 즉시 실행).
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
public interface ScheduleService {

    /**
     * TB_DSPT_JOB_M 전체를 Quartz와 동기화한다 (추가·변경·삭제).
     * @throws BatchException 스케줄러 오류가 발생한 경우
     */
    void syncSchedules();

    /**
     * 배치직무 1건의 JobDetail/Trigger를 등록하거나 갱신한다.
     * @param batchJob 대상 배치직무
     * @param previousWrkMdulCd 수정 전 업무모듈코드 (변경 시 이전 JobKey 삭제). 신규면 null
     * @throws BatchException 스케줄 표현식이 올바르지 않거나 스케줄러 오류가 발생한 경우
     */
    void saveSchedule(BatchJob batchJob, String previousWrkMdulCd);

    /**
     * 배치직무의 Quartz JobDetail/Trigger를 삭제한다.
     * @param batchJob 대상 배치직무
     * @throws BatchException 스케줄러 오류가 발생한 경우
     */
    void deleteSchedule(BatchJob batchJob);

    /**
     * 스케줄 일시정지(PAUSED) / 재개(NORMAL).
     * @param batchJob 대상 배치직무
     * @param schedStatus 변경할 스케줄 상태 (PAUSED / NORMAL)
     * @throws BatchException 스케줄이 없거나 상태 값이 올바르지 않거나 스케줄러 오류가 발생한 경우
     */
    void saveScheduleStatus(BatchJob batchJob, String schedStatus);

    /**
     * Quartz를 통해 즉시 실행한다 (클러스터 단일 실행 경로 유지).
     * @param batchJob 대상 배치직무
     * @param execClsCd 실행구분코드
     * @param reqUsrId 실행 요청 사용자 ID
     * @param manualParmCont 수동 실행 파라미터(JSON). 없으면 null
     * @param restartJobExecId 재시작 대상 JobExecution ID. 재시작이 아니면 null
     * @throws BatchException 스케줄러 오류가 발생한 경우
     */
    void triggerJob(BatchJob batchJob, String execClsCd, String reqUsrId, String manualParmCont, Long restartJobExecId);

    /**
     * 응답에 스케줄 상태, 다음/이전 실행 일시를 채운다.
     * @param response 값을 채울 배치직무 응답
     */
    void fillScheduleInfo(BatchJobResponse response);

    /**
     * 클러스터 전체에서 해당 배치직무의 Quartz Job이 실행 중인지 확인한다.
     * @param batchJob 대상 배치직무
     * @return 실행 중이면 true
     * @throws BatchException 스케줄러 오류가 발생한 경우
     */
    boolean isRunningInCluster(BatchJob batchJob);
}
