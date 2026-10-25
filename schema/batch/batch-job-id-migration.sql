-- =====================================================================
-- 배치ID 체계 변경 이행 (2026-10-17)
--   1) TB_DSPT_JOB_M에 실행클래스명(EXEC_CLASS_NM) 컬럼 추가
--   2) JOB_ID를 Job 빈 이름(camelCase)에서 배치ID(BHFL001 형식)로 변경
--      TB_DSPT_EXEC_L.JOB_ID가 FK로 참조하므로 새 행 등록 → 실행내역 이관 → 기존 행 삭제 순서로 처리한다.
-- 테이블 소유자(CODER) 계정으로 1회 실행한다. 다시 실행해도 결과는 같다.
-- 옛 JOB_ID로 등록된 Quartz Job은 coder-batch 기동 시 ScheduleSyncRunner가 orphan으로 삭제한다.
-- =====================================================================

ALTER TABLE TB_DSPT_JOB_M ADD COLUMN IF NOT EXISTS EXEC_CLASS_NM VARCHAR(200);
COMMENT ON COLUMN TB_DSPT_JOB_M.JOB_ID        IS '직무ID (배치ID, BH + 업무 2자리 + 순번 3자리, 예: BHFL001)';
COMMENT ON COLUMN TB_DSPT_JOB_M.EXEC_CLASS_NM IS '실행클래스명 (Job 빈을 정의한 클래스, 패키지 포함)';

CREATE TEMP TABLE TMP_DSPT_JOB_ID_MAP (OLD_JOB_ID VARCHAR(100), NEW_JOB_ID VARCHAR(100), EXEC_CLASS_NM VARCHAR(200));
INSERT INTO TMP_DSPT_JOB_ID_MAP VALUES
    ('userStatusApplyJob',  'BHFL001', 'kr.or.coder.batch.job.system.userstatusapply.UserStatusApplyJobConfig'),
    ('execLogPurgeJob',     'BHFL002', 'kr.or.coder.batch.job.system.execlogpurge.ExecLogPurgeJobConfig'),
    ('inactiveUserLockJob', 'BHFL003', 'kr.or.coder.batch.job.system.inactiveuserlock.InactiveUserLockJobConfig');

-- 1. 새 배치ID로 행 복사 (직무명 앞의 [BHFL001] 표기는 제거)
INSERT INTO TB_DSPT_JOB_M (
    JOB_ID, JOB_NM, EXEC_CLASS_NM, WRK_MDUL_CD, SCHE_EXPR, MSFR_PLCY_CD, MLT_EXEC_YN,
    JOB_PARM_CONT, USE_YN, RMK, REFL_PRGM_ID, FIRS_REGR_ID, FIRS_REG_DT, FINA_REGR_ID, FINA_REG_DT
)
SELECT M.NEW_JOB_ID, REGEXP_REPLACE(J.JOB_NM, '^\[BH[A-Z]{2}[0-9]{3}\] *', ''), M.EXEC_CLASS_NM,
       J.WRK_MDUL_CD, J.SCHE_EXPR, J.MSFR_PLCY_CD, J.MLT_EXEC_YN,
       J.JOB_PARM_CONT, J.USE_YN, J.RMK, J.REFL_PRGM_ID, J.FIRS_REGR_ID, J.FIRS_REG_DT, 'BATCH', CURRENT_TIMESTAMP
  FROM TB_DSPT_JOB_M J
  JOIN TMP_DSPT_JOB_ID_MAP M ON M.OLD_JOB_ID = J.JOB_ID
ON CONFLICT (JOB_ID) DO NOTHING;

-- 2. 실행내역을 새 배치ID로 이관
UPDATE TB_DSPT_EXEC_L L
   SET JOB_ID = M.NEW_JOB_ID
  FROM TMP_DSPT_JOB_ID_MAP M
 WHERE L.JOB_ID = M.OLD_JOB_ID;

-- 3. 기존 행 삭제
DELETE FROM TB_DSPT_JOB_M J
 USING TMP_DSPT_JOB_ID_MAP M
 WHERE J.JOB_ID = M.OLD_JOB_ID;

DROP TABLE TMP_DSPT_JOB_ID_MAP;

-- 4. 이행 대상 외 직무가 있으면 EXEC_CLASS_NM을 먼저 채운 뒤 NOT NULL을 건다
ALTER TABLE TB_DSPT_JOB_M ALTER COLUMN EXEC_CLASS_NM SET NOT NULL;
