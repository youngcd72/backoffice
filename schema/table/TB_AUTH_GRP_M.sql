-- PostgreSQL / CLAUDE.md 5.x 표준 적용 (COMP_CD 제외)
-- ---------------------------------------------------------------------
-- TB_AUTH_GRP_M : 권한그룹기본
-- ---------------------------------------------------------------------
CREATE TABLE TB_AUTH_GRP_M (
    USR_GRP_CD            VARCHAR(20)  NOT NULL,
    USR_GRP_NM            VARCHAR(30),
    USE_YN                CHAR(1)      NOT NULL DEFAULT 'N',
    ST_DT                 CHAR(8),
    END_DT                CHAR(8),
    RMK                   VARCHAR(200),
    REFL_PRGM_ID          VARCHAR(50),
    FIRS_REGR_ID          VARCHAR(10)  NOT NULL,
    FIRS_REG_DT           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FINA_REGR_ID          VARCHAR(10)  NOT NULL,
    FINA_REG_DT           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT TB_AUTH_GRP_M_PK PRIMARY KEY (USR_GRP_CD)
);

COMMENT ON TABLE  TB_AUTH_GRP_M IS '권한그룹기본 (ERD: SY0020M)';
COMMENT ON COLUMN TB_AUTH_GRP_M.USR_GRP_CD         IS '사용자그룹코드';
COMMENT ON COLUMN TB_AUTH_GRP_M.USR_GRP_NM         IS '사용자그룹명';
COMMENT ON COLUMN TB_AUTH_GRP_M.USE_YN             IS '사용여부';
COMMENT ON COLUMN TB_AUTH_GRP_M.ST_DT              IS '시작일자';
COMMENT ON COLUMN TB_AUTH_GRP_M.END_DT             IS '종료일자';
COMMENT ON COLUMN TB_AUTH_GRP_M.RMK                IS '비고';
COMMENT ON COLUMN TB_AUTH_GRP_M.REFL_PRGM_ID       IS '반영프로그램ID';
COMMENT ON COLUMN TB_AUTH_GRP_M.FIRS_REGR_ID       IS '최초등록자ID';
COMMENT ON COLUMN TB_AUTH_GRP_M.FIRS_REG_DT        IS '최초등록일시';
COMMENT ON COLUMN TB_AUTH_GRP_M.FINA_REGR_ID       IS '최종등록자ID';
COMMENT ON COLUMN TB_AUTH_GRP_M.FINA_REG_DT        IS '최종등록일시';
