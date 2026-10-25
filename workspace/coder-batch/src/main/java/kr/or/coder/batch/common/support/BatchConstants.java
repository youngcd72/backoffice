package kr.or.coder.batch.common.support;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 배치 모듈 공통 상수 (코드값, Quartz JobDataMap 키, JobParameters 키, MDC 키)
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
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BatchConstants {

    // -------------------------------------------------------------------
    // 실행구분코드 (EXEC_CLS_CD)
    // -------------------------------------------------------------------
    public static final String EXEC_CLS_SCHEDULE = "S";
    public static final String EXEC_CLS_MANUAL = "M";
    public static final String EXEC_CLS_RESTART = "R";

    // -------------------------------------------------------------------
    // 실행상태코드 (EXEC_STAT_CD) - BatchStatus 이름 + SKIP
    // -------------------------------------------------------------------
    public static final String EXEC_STAT_SKIP = "SKIP";
    public static final String EXEC_STAT_FAILED = "FAILED";

    // -------------------------------------------------------------------
    // 미스파이어정책코드 (MSFR_PLCY_CD)
    // -------------------------------------------------------------------
    public static final String MSFR_PLCY_DO_NOTHING = "01";
    public static final String MSFR_PLCY_FIRE_ONCE_NOW = "02";

    public static final String YES = "Y";
    public static final String NO = "N";

    // -------------------------------------------------------------------
    // Quartz JobDataMap 키
    // -------------------------------------------------------------------
    public static final String DATA_JOB_ID = "jobId";
    public static final String DATA_EXEC_CLS_CD = "execClsCd";
    public static final String DATA_REQ_USR_ID = "reqUsrId";
    public static final String DATA_JOB_PARM_CONT = "jobParmCont";
    public static final String DATA_RESTART_JOB_EXEC_ID = "restartJobExecId";

    // -------------------------------------------------------------------
    // JobParameters 키
    // -------------------------------------------------------------------
    /** identifying - 실행 요청 일시 (yyyyMMddHHmmssSSS) */
    public static final String PARM_REQUEST_DTTM = "requestDttm";
    /** non-identifying - 배치ID (TB_DSPT_JOB_M.JOB_ID). Spring Batch Job 이름(빈 이름)과 다르다. */
    public static final String PARM_JOB_ID = "jobId";
    /** identifying - Quartz fireInstanceId (동시 실행 시 JobInstance 구분) */
    public static final String PARM_FIRE_ID = "fireId";
    /** non-identifying - 실행구분코드 */
    public static final String PARM_EXEC_CLS_CD = "execClsCd";
    /** non-identifying - 실행 요청 사용자 ID */
    public static final String PARM_REQ_USR_ID = "reqUsrId";
    /** non-identifying - 실행 서버명 (Quartz instanceId) */
    public static final String PARM_EXEC_SRV_NM = "execSrvNm";

    // -------------------------------------------------------------------
    // MDC 키
    // -------------------------------------------------------------------
    public static final String MDC_JOB_ID = "jobId";
    public static final String MDC_JOB_EXECUTION_ID = "jobExecutionId";
    public static final String MDC_STEP_NAME = "stepName";

    /** 사용자ID 컬럼 길이 (VARCHAR(10)) */
    public static final int USR_ID_MAX_LENGTH = 10;
    /** 오류메시지내용 컬럼 길이 (VARCHAR(4000), byte 기준) */
    public static final int ERR_MSG_MAX_BYTES = 4000;

    // -------------------------------------------------------------------
    // 배치 실패 알람 (TB_ALRM_L, coder-backend 알림 센터)
    // -------------------------------------------------------------------
    /** 알람유형코드 (SY/ALRM_TYPE_CD 배치) */
    public static final String ALRM_TYPE_DSPT = "DSPT";
    /** 알람 클릭 시 열 화면 (배치관리) */
    public static final String ALRM_PRGM_ID = "SYLK050";
    /** coder-backend SSE 중계 Redis 채널 (coder-backend SsePublisher.SSE_CHANNEL 과 같은 값) */
    public static final String SSE_CHANNEL = "coder:sse";
    /** SSE 이벤트명: 새 알람 */
    public static final String SSE_EVENT_ALARM = "alarm";
}
