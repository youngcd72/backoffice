-- PostgreSQL / CLAUDE.md 5.x 표준 적용 (COMP_CD 제외)
-- ---------------------------------------------------------------------
-- TB_USR_M : 사용자마스터기본
-- ---------------------------------------------------------------------
CREATE TABLE TB_USR_M (
    USR_ID                VARCHAR(10)  NOT NULL,
    USR_NM                VARCHAR(50),
    EMP_NO                VARCHAR(10),
    PWD                   VARCHAR(200),
    DEPT_CD               VARCHAR(10),
    MOBL_NO               VARCHAR(64),
    EML_ADDR              VARCHAR(500),
    PWD_CHG_DTTM          TIMESTAMP,
    LGIN_ERR_CNT          INTEGER,
    FINA_ACC_DTTM         TIMESTAMP,
    USE_YN                CHAR(1)      NOT NULL DEFAULT 'N',
    USR_CLS_CD            CHAR(1),
    USR_STAT_CD           CHAR(1),
    USR_LOCK_YN           CHAR(1),
    USR_LOCK_DTTM         TIMESTAMP,
    FIRS_LGIN_YN          CHAR(1),
    USE_ST_DT             CHAR(8),
    USE_END_DT            CHAR(8),
    ECOM_DT               CHAR(8),
    RESI_DT               CHAR(8),
    REFL_PRGM_ID          VARCHAR(50),
    FIRS_REGR_ID          VARCHAR(10)  NOT NULL,
    FIRS_REG_DT           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FINA_REGR_ID          VARCHAR(10)  NOT NULL,
    FINA_REG_DT           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT TB_USR_M_PK PRIMARY KEY (USR_ID)
);

COMMENT ON TABLE  TB_USR_M IS '사용자마스터기본';
COMMENT ON COLUMN TB_USR_M.USR_ID                 IS '사용자ID';
COMMENT ON COLUMN TB_USR_M.USR_NM                 IS '사용자명';
COMMENT ON COLUMN TB_USR_M.EMP_NO                 IS '사원번호';
COMMENT ON COLUMN TB_USR_M.PWD                    IS '비밀번호';
COMMENT ON COLUMN TB_USR_M.DEPT_CD                IS '부서코드';
COMMENT ON COLUMN TB_USR_M.MOBL_NO                IS '모바일번호';
COMMENT ON COLUMN TB_USR_M.EML_ADDR               IS '이메일주소';
COMMENT ON COLUMN TB_USR_M.PWD_CHG_DTTM           IS '비밀번호변경일시';
COMMENT ON COLUMN TB_USR_M.LGIN_ERR_CNT           IS '로그인오류건수';
COMMENT ON COLUMN TB_USR_M.FINA_ACC_DTTM          IS '최종접속일시';
COMMENT ON COLUMN TB_USR_M.USE_YN                 IS '사용여부';
COMMENT ON COLUMN TB_USR_M.USR_CLS_CD             IS '사용자구분코드 (공통코드 SY/USR_CLS_CD: E 직원, P 협력업체)';
COMMENT ON COLUMN TB_USR_M.USR_STAT_CD            IS '사용자상태코드 (공통코드 SY/USR_STAT_CD: T 재직, L 휴직, R 퇴직)';
COMMENT ON COLUMN TB_USR_M.USR_LOCK_YN            IS '사용자잠금여부';
COMMENT ON COLUMN TB_USR_M.USR_LOCK_DTTM          IS '사용자잠금일시';
COMMENT ON COLUMN TB_USR_M.FIRS_LGIN_YN           IS '최초로그인여부';
COMMENT ON COLUMN TB_USR_M.USE_ST_DT              IS '사용시작일자';
COMMENT ON COLUMN TB_USR_M.USE_END_DT             IS '사용종료일자';
COMMENT ON COLUMN TB_USR_M.ECOM_DT                IS '입사일자';
COMMENT ON COLUMN TB_USR_M.RESI_DT                IS '퇴사일자';
COMMENT ON COLUMN TB_USR_M.REFL_PRGM_ID           IS '반영프로그램ID';
COMMENT ON COLUMN TB_USR_M.FIRS_REGR_ID           IS '최초등록자ID';
COMMENT ON COLUMN TB_USR_M.FIRS_REG_DT            IS '최초등록일시';
COMMENT ON COLUMN TB_USR_M.FINA_REGR_ID           IS '최종등록자ID';
COMMENT ON COLUMN TB_USR_M.FINA_REG_DT            IS '최종등록일시';
