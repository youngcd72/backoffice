-- PostgreSQL / CLAUDE.md 5.x 표준 적용 (COMP_CD 제외)
-- ---------------------------------------------------------------------
-- TB_USR_CMPN_R : 사용자컴포넌트관계 (ERD 없음 — 신규 설계)
--   사용자별 프로그램 컴포넌트 사용 권한.
-- ---------------------------------------------------------------------
CREATE TABLE TB_USR_CMPN_R (
    USR_ID                VARCHAR(10)  NOT NULL,
    PRGM_ID               VARCHAR(100) NOT NULL,
    CMPN_ID               VARCHAR(50)  NOT NULL,
    AUTH_YN               CHAR(1)      NOT NULL DEFAULT 'N',
    REFL_PRGM_ID          VARCHAR(50),
    FIRS_REGR_ID          VARCHAR(10)  NOT NULL,
    FIRS_REG_DT           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FINA_REGR_ID          VARCHAR(10)  NOT NULL,
    FINA_REG_DT           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT TB_USR_CMPN_R_PK PRIMARY KEY (USR_ID, PRGM_ID, CMPN_ID),
    CONSTRAINT TB_USR_CMPN_R_FK01 FOREIGN KEY (USR_ID)
        REFERENCES TB_USR_M (USR_ID),
    CONSTRAINT TB_USR_CMPN_R_FK02 FOREIGN KEY (PRGM_ID, CMPN_ID)
        REFERENCES TB_PRGM_CMPN_M (PRGM_ID, CMPN_ID)
);

COMMENT ON TABLE  TB_USR_CMPN_R IS '사용자컴포넌트관계';
COMMENT ON COLUMN TB_USR_CMPN_R.USR_ID                 IS '사용자ID';
COMMENT ON COLUMN TB_USR_CMPN_R.PRGM_ID                IS '프로그램ID';
COMMENT ON COLUMN TB_USR_CMPN_R.CMPN_ID                IS '컴포넌트ID';
COMMENT ON COLUMN TB_USR_CMPN_R.AUTH_YN                IS '권한여부';
COMMENT ON COLUMN TB_USR_CMPN_R.REFL_PRGM_ID           IS '반영프로그램ID';
COMMENT ON COLUMN TB_USR_CMPN_R.FIRS_REGR_ID           IS '최초등록자ID';
COMMENT ON COLUMN TB_USR_CMPN_R.FIRS_REG_DT            IS '최초등록일시';
COMMENT ON COLUMN TB_USR_CMPN_R.FINA_REGR_ID           IS '최종등록자ID';
COMMENT ON COLUMN TB_USR_CMPN_R.FINA_REG_DT            IS '최종등록일시';
