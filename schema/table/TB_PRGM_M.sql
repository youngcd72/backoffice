-- PostgreSQL / CLAUDE.md 5.x 표준 적용 (COMP_CD 제외)
-- ---------------------------------------------------------------------
-- TB_PRGM_M : 프로그램기본
-- ---------------------------------------------------------------------
CREATE TABLE TB_PRGM_M (
    PRGM_ID               VARCHAR(100) NOT NULL,
    PRGM_NM               VARCHAR(50),
    PRGM_CRS_CD           VARCHAR(2),
    ALIN_SEQ              INTEGER,
    USE_YN                CHAR(1)      NOT NULL DEFAULT 'N',
    ST_DT                 CHAR(8),
    END_DT                CHAR(8),
    RMK                   VARCHAR(200),
    LNK_URL               VARCHAR(1000),
    REFL_PRGM_ID          VARCHAR(50),
    FIRS_REGR_ID          VARCHAR(10)  NOT NULL,
    FIRS_REG_DT           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FINA_REGR_ID          VARCHAR(10)  NOT NULL,
    FINA_REG_DT           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT TB_PRGM_M_PK PRIMARY KEY (PRGM_ID)
);

COMMENT ON TABLE  TB_PRGM_M IS '프로그램기본';
COMMENT ON COLUMN TB_PRGM_M.PRGM_ID                IS '프로그램ID';
COMMENT ON COLUMN TB_PRGM_M.PRGM_NM                IS '프로그램명';
COMMENT ON COLUMN TB_PRGM_M.PRGM_CRS_CD            IS '프로그램경로코드 (공통코드 SY/PRGM_CRS_CD)';
COMMENT ON COLUMN TB_PRGM_M.ALIN_SEQ               IS '정렬순번';
COMMENT ON COLUMN TB_PRGM_M.USE_YN                 IS '사용여부';
COMMENT ON COLUMN TB_PRGM_M.ST_DT                  IS '시작일자';
COMMENT ON COLUMN TB_PRGM_M.END_DT                 IS '종료일자';
COMMENT ON COLUMN TB_PRGM_M.RMK                    IS '비고';
COMMENT ON COLUMN TB_PRGM_M.LNK_URL                IS '연계URL';
COMMENT ON COLUMN TB_PRGM_M.REFL_PRGM_ID           IS '반영프로그램ID';
COMMENT ON COLUMN TB_PRGM_M.FIRS_REGR_ID           IS '최초등록자ID';
COMMENT ON COLUMN TB_PRGM_M.FIRS_REG_DT            IS '최초등록일시';
COMMENT ON COLUMN TB_PRGM_M.FINA_REGR_ID           IS '최종등록자ID';
COMMENT ON COLUMN TB_PRGM_M.FINA_REG_DT            IS '최종등록일시';
