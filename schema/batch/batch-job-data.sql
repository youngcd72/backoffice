-- =====================================================================
-- coder-batch 배치직무 등록 (TB_DSPT_JOB_M)
-- JOB_ID는 배치ID(BH + 업무 2자리 + 순번 3자리, 예: BHFL001)이고,
-- EXEC_CLASS_NM은 Job 빈을 정의한 클래스(패키지 포함)이다. 클래스당 Job 빈은 1개만 정의한다.
-- 기동 시 ScheduleSyncRunner가 이 테이블을 읽어 Quartz 스케줄을 등록한다.
-- =====================================================================

-- 사용자상태변경 적용: 매일 00:10, 퇴직·휴직 사용자 USE_YN='N', 퇴직은 USE_END_DT를 퇴사일자로
INSERT INTO TB_DSPT_JOB_M (
    JOB_ID, JOB_NM, EXEC_CLASS_NM, WRK_MDUL_CD, SCHE_EXPR, MSFR_PLCY_CD, MLT_EXEC_YN,
    JOB_PARM_CONT, USE_YN, RMK, FIRS_REGR_ID, FINA_REGR_ID
) VALUES (
    'BHFL001', '사용자상태변경 적용', 'kr.or.coder.batch.job.system.userstatusapply.UserStatusApplyJobConfig',
    'SY', '0 10 0 * * ?', '01', 'N',
    NULL, 'Y', '퇴직(R)·휴직(L) 사용자 사용여부 N, 퇴직은 사용종료일자를 퇴사일자로 (baseDt 미지정 시 실행일)', 'BATCH', 'BATCH'
) ON CONFLICT (JOB_ID) DO NOTHING;

-- 배치 실행이력 정리: 매일 03:00, 90일 보관
INSERT INTO TB_DSPT_JOB_M (
    JOB_ID, JOB_NM, EXEC_CLASS_NM, WRK_MDUL_CD, SCHE_EXPR, MSFR_PLCY_CD, MLT_EXEC_YN,
    JOB_PARM_CONT, USE_YN, RMK, FIRS_REGR_ID, FINA_REGR_ID
) VALUES (
    'BHFL002', '배치 실행이력 정리', 'kr.or.coder.batch.job.system.execlogpurge.ExecLogPurgeJobConfig',
    'SY', '0 0 3 * * ?', '01', 'N',
    '{"retentionDays":"90"}', 'Y', 'BATCH_* 메타 및 TB_DSPT_EXEC_L 보관기간 경과분 삭제', 'BATCH', 'BATCH'
) ON CONFLICT (JOB_ID) DO NOTHING;

-- 장기 미접속 사용자 잠금: 매일 02:30, 90일 미접속 (기본 미사용)
INSERT INTO TB_DSPT_JOB_M (
    JOB_ID, JOB_NM, EXEC_CLASS_NM, WRK_MDUL_CD, SCHE_EXPR, MSFR_PLCY_CD, MLT_EXEC_YN,
    JOB_PARM_CONT, USE_YN, RMK, FIRS_REGR_ID, FINA_REGR_ID
) VALUES (
    'BHFL003', '장기 미접속 사용자 잠금', 'kr.or.coder.batch.job.system.inactiveuserlock.InactiveUserLockJobConfig',
    'SY', '0 30 2 * * ?', '01', 'N',
    '{"idleDays":"90"}', 'N', 'Chunk형 샘플 - 운영 적용 전 USE_YN 검토', 'BATCH', 'BATCH'
) ON CONFLICT (JOB_ID) DO NOTHING;
