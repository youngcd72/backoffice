-- PostgreSQL / CLAUDE.md 5.x 표준 적용 (COMP_CD 제외)
-- ---------------------------------------------------------------------
-- TB_DSPT_JOB_M : 배치직무기본 (coder-batch, documents/batch/PRD.md 7.2)
-- ---------------------------------------------------------------------
CREATE TABLE TB_DSPT_JOB_M (
    JOB_ID                VARCHAR(100) NOT NULL,
    JOB_NM                VARCHAR(100) NOT NULL,
    EXEC_CLASS_NM         VARCHAR(200) NOT NULL,
    WRK_MDUL_CD           VARCHAR(2)   NOT NULL,
    SCHE_EXPR             VARCHAR(100),
    MSFR_PLCY_CD          VARCHAR(2)   NOT NULL DEFAULT '01',
    MLT_EXEC_YN           CHAR(1)      NOT NULL DEFAULT 'N',
    JOB_PARM_CONT         VARCHAR(4000),
    USE_YN                CHAR(1)      NOT NULL DEFAULT 'N',
    RMK                   VARCHAR(200),
    REFL_PRGM_ID          VARCHAR(50),
    FIRS_REGR_ID          VARCHAR(10)  NOT NULL,
    FIRS_REG_DT           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FINA_REGR_ID          VARCHAR(10)  NOT NULL,
    FINA_REG_DT           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT TB_DSPT_JOB_M_PK PRIMARY KEY (JOB_ID)
);

CREATE INDEX TB_DSPT_JOB_M_IX01 ON TB_DSPT_JOB_M (WRK_MDUL_CD, USE_YN);

COMMENT ON TABLE  TB_DSPT_JOB_M IS '배치직무기본';
COMMENT ON COLUMN TB_DSPT_JOB_M.JOB_ID                 IS '직무ID (배치ID, BH + 업무 2자리 + 순번 3자리, 예: BHFL001)';
COMMENT ON COLUMN TB_DSPT_JOB_M.JOB_NM                 IS '직무명';
COMMENT ON COLUMN TB_DSPT_JOB_M.EXEC_CLASS_NM          IS '실행클래스명 (Job 빈을 정의한 클래스, 패키지 포함)';
COMMENT ON COLUMN TB_DSPT_JOB_M.WRK_MDUL_CD            IS '업무모듈코드';
COMMENT ON COLUMN TB_DSPT_JOB_M.SCHE_EXPR              IS '스케줄표현식 (Quartz cron, NULL이면 수동 실행 전용)';
COMMENT ON COLUMN TB_DSPT_JOB_M.MSFR_PLCY_CD           IS '미스파이어정책코드 (01 건너뜀, 02 즉시 1회 실행)';
COMMENT ON COLUMN TB_DSPT_JOB_M.MLT_EXEC_YN            IS '다중실행여부';
COMMENT ON COLUMN TB_DSPT_JOB_M.JOB_PARM_CONT          IS '직무파라미터내용 (JSON)';
COMMENT ON COLUMN TB_DSPT_JOB_M.USE_YN                 IS '사용여부';
COMMENT ON COLUMN TB_DSPT_JOB_M.RMK                    IS '비고';
COMMENT ON COLUMN TB_DSPT_JOB_M.REFL_PRGM_ID           IS '반영프로그램ID';
COMMENT ON COLUMN TB_DSPT_JOB_M.FIRS_REGR_ID           IS '최초등록자ID';
COMMENT ON COLUMN TB_DSPT_JOB_M.FIRS_REG_DT            IS '최초등록일시';
COMMENT ON COLUMN TB_DSPT_JOB_M.FINA_REGR_ID           IS '최종등록자ID';
COMMENT ON COLUMN TB_DSPT_JOB_M.FINA_REG_DT            IS '최종등록일시';
