-- PostgreSQL / CLAUDE.md 5.x 표준 적용 (COMP_CD 제외)
-- ---------------------------------------------------------------------
-- TB_PRGM_CMPN_M : 프로그램컴포넌트기본 (ERD 없음 — 신규 설계)
--   프로그램 화면의 권한 제어 대상 컴포넌트(버튼 등) 목록. CMPN_ID는 화면의 ref/id 명(btnSave 등)과 동일.
-- ---------------------------------------------------------------------
CREATE TABLE TB_PRGM_CMPN_M (
    PRGM_ID               VARCHAR(100) NOT NULL,
    CMPN_ID               VARCHAR(50)  NOT NULL,
    CMPN_NM               VARCHAR(50),
    ALIN_SEQ              INTEGER,
    USE_YN                CHAR(1)      NOT NULL DEFAULT 'N',
    RMK                   VARCHAR(200),
    REFL_PRGM_ID          VARCHAR(50),
    FIRS_REGR_ID          VARCHAR(10)  NOT NULL,
    FIRS_REG_DT           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FINA_REGR_ID          VARCHAR(10)  NOT NULL,
    FINA_REG_DT           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT TB_PRGM_CMPN_M_PK PRIMARY KEY (PRGM_ID, CMPN_ID),
    CONSTRAINT TB_PRGM_CMPN_M_FK01 FOREIGN KEY (PRGM_ID)
        REFERENCES TB_PRGM_M (PRGM_ID)
);

COMMENT ON TABLE  TB_PRGM_CMPN_M IS '프로그램컴포넌트기본';
COMMENT ON COLUMN TB_PRGM_CMPN_M.PRGM_ID               IS '프로그램ID';
COMMENT ON COLUMN TB_PRGM_CMPN_M.CMPN_ID               IS '컴포넌트ID';
COMMENT ON COLUMN TB_PRGM_CMPN_M.CMPN_NM               IS '컴포넌트명';
COMMENT ON COLUMN TB_PRGM_CMPN_M.ALIN_SEQ              IS '정렬순번';
COMMENT ON COLUMN TB_PRGM_CMPN_M.USE_YN                IS '사용여부';
COMMENT ON COLUMN TB_PRGM_CMPN_M.RMK                   IS '비고';
COMMENT ON COLUMN TB_PRGM_CMPN_M.REFL_PRGM_ID          IS '반영프로그램ID';
COMMENT ON COLUMN TB_PRGM_CMPN_M.FIRS_REGR_ID          IS '최초등록자ID';
COMMENT ON COLUMN TB_PRGM_CMPN_M.FIRS_REG_DT           IS '최초등록일시';
COMMENT ON COLUMN TB_PRGM_CMPN_M.FINA_REGR_ID          IS '최종등록자ID';
COMMENT ON COLUMN TB_PRGM_CMPN_M.FINA_REG_DT           IS '최종등록일시';
