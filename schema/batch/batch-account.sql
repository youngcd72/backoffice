-- =====================================================================
-- coder-batch 전용 계정(BATCH) / 스키마(BATCH) 생성 및 권한 부여 (PostgreSQL)
--
-- * 슈퍼유저(postgres)로 coder DB에 접속해 최초 1회 실행한다.
--     psql -U postgres -d coder -f batch-account.sql
-- * 실행 순서
--     1) schema/table/TB_DSPT_JOB_M.sql, TB_DSPT_EXEC_L.sql, TB_DSPT_ALRM_RCVR_R.sql, TB_ALRM_L.sql
--                                                          (CODER 계정, CODER 스키마)
--     2) batch-account.sql                                  (이 파일, postgres)
--     3) spring-batch-schema-postgresql.sql, quartz-tables-postgresql.sql
--                                                          (BATCH 계정, BATCH 스키마)
--     4) batch-job-data.sql                                 (CODER 계정)
-- * 비밀번호는 로컬 개발용 기본값이다. 운영은 ALTER ROLE BATCH PASSWORD 로 변경하고
--   coder-batch 실행 환경변수 CODER_BATCH_DB_PASSWORD 에 같은 값을 준다.
--
-- 권한 구성
--   BATCH : BATCH 스키마 소유 (BATCH_*, QRTZ_*)
--           CODER 스키마는 배치가 쓰는 테이블만 DML 허용
--   CODER  : BATCH 스키마 읽기 전용 (배치관리 화면 조회용)
-- =====================================================================

CREATE ROLE BATCH LOGIN PASSWORD 'batch1234!';

CREATE SCHEMA BATCH AUTHORIZATION BATCH;

-- BATCH 계정 기본 검색 순서: 자기 스키마 → 업무 스키마
ALTER ROLE BATCH SET search_path = BATCH, CODER;

-- ---------------------------------------------------------------------
-- BATCH → CODER 스키마 (배치가 쓰는 업무 테이블만)
-- ---------------------------------------------------------------------
GRANT USAGE ON SCHEMA CODER TO BATCH;

GRANT SELECT, INSERT, UPDATE, DELETE ON CODER.TB_DSPT_JOB_M  TO BATCH;   -- 배치직무 (스케줄 동기화)
GRANT SELECT, INSERT, UPDATE, DELETE ON CODER.TB_DSPT_EXEC_L TO BATCH;   -- 배치 실행이력 (기록, 보관기간 정리)
GRANT USAGE, SELECT ON SEQUENCE CODER.SQ_TB_DSPT_EXEC_L_DSPT_EXEC_SN TO BATCH;
GRANT SELECT, UPDATE ON CODER.TB_USR_M TO BATCH;                          -- inactiveUserLockJob (장기 미접속 잠금), userStatusApplyJob (사용자상태변경 적용)
GRANT SELECT, INSERT, UPDATE, DELETE ON CODER.TB_DSPT_ALRM_RCVR_R TO BATCH; -- 배치알람수신자 (실패 알람 수신자 조회, 직무 삭제 시 정리)
GRANT INSERT ON CODER.TB_ALRM_L TO BATCH;                                  -- 알람내역 (배치 실패 알람 등록)
GRANT USAGE, SELECT ON SEQUENCE CODER.SQ_TB_ALRM_L_ALRM_SN TO BATCH;

-- ---------------------------------------------------------------------
-- CODER → BATCH 스키마 (읽기 전용)
-- ---------------------------------------------------------------------
GRANT USAGE ON SCHEMA BATCH TO CODER;
GRANT SELECT ON ALL TABLES IN SCHEMA BATCH TO CODER;
-- 이후 BATCH 계정이 만드는 테이블에도 자동으로 SELECT 부여
ALTER DEFAULT PRIVILEGES FOR ROLE BATCH IN SCHEMA BATCH GRANT SELECT ON TABLES TO CODER;
