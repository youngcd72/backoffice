-- PostgreSQL / CLAUDE.md 5.x 표준 적용 (COMP_CD 제외)
-- ---------------------------------------------------------------------
-- TB_USR_AUTH_GRP_R : 사용자권한그룹관계 (ERD 없음 — 신규 설계)
--   사용자가 속한 권한그룹과 적용기간.
-- ---------------------------------------------------------------------
CREATE TABLE TB_USR_AUTH_GRP_R (
    USR_ID                VARCHAR(10)  NOT NULL,
    USR_GRP_CD            VARCHAR(20)  NOT NULL,
    ST_DT                 CHAR(8),
    END_DT                CHAR(8),
    REFL_PRGM_ID          VARCHAR(50),
    FIRS_REGR_ID          VARCHAR(10)  NOT NULL,
    FIRS_REG_DT           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FINA_REGR_ID          VARCHAR(10)  NOT NULL,
    FINA_REG_DT           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT TB_USR_AUTH_GRP_R_PK PRIMARY KEY (USR_ID, USR_GRP_CD),
    CONSTRAINT TB_USR_AUTH_GRP_R_FK01 FOREIGN KEY (USR_ID)
        REFERENCES TB_USR_M (USR_ID),
    CONSTRAINT TB_USR_AUTH_GRP_R_FK02 FOREIGN KEY (USR_GRP_CD)
        REFERENCES TB_AUTH_GRP_M (USR_GRP_CD)
);

COMMENT ON TABLE  TB_USR_AUTH_GRP_R IS '사용자권한그룹관계';
COMMENT ON COLUMN TB_USR_AUTH_GRP_R.USR_ID                 IS '사용자ID';
COMMENT ON COLUMN TB_USR_AUTH_GRP_R.USR_GRP_CD             IS '사용자그룹코드';
COMMENT ON COLUMN TB_USR_AUTH_GRP_R.ST_DT                  IS '시작일자';
COMMENT ON COLUMN TB_USR_AUTH_GRP_R.END_DT                 IS '종료일자';
COMMENT ON COLUMN TB_USR_AUTH_GRP_R.REFL_PRGM_ID           IS '반영프로그램ID';
COMMENT ON COLUMN TB_USR_AUTH_GRP_R.FIRS_REGR_ID           IS '최초등록자ID';
COMMENT ON COLUMN TB_USR_AUTH_GRP_R.FIRS_REG_DT            IS '최초등록일시';
COMMENT ON COLUMN TB_USR_AUTH_GRP_R.FINA_REGR_ID           IS '최종등록자ID';
COMMENT ON COLUMN TB_USR_AUTH_GRP_R.FINA_REG_DT            IS '최종등록일시';
