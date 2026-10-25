-- PostgreSQL / CLAUDE.md 5.x 표준 적용 (COMP_CD 제외)
-- ---------------------------------------------------------------------
-- TB_MENU_M : 시스템메뉴내역
-- ---------------------------------------------------------------------
CREATE TABLE TB_MENU_M (
    MENU_ID               VARCHAR(20)  NOT NULL,
    MENU_NM               VARCHAR(50),
    PRGM_ID               VARCHAR(100),
    UPPR_MENU_ID          VARCHAR(20),
    ALIN_SEQ              INTEGER,
    MENU_TYPE_CD          VARCHAR(4),
    RMK                   VARCHAR(200),
    USE_YN                CHAR(1)      NOT NULL DEFAULT 'N',
    REFL_PRGM_ID          VARCHAR(50),
    FIRS_REGR_ID          VARCHAR(10)  NOT NULL,
    FIRS_REG_DT           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FINA_REGR_ID          VARCHAR(10)  NOT NULL,
    FINA_REG_DT           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT TB_MENU_M_PK PRIMARY KEY (MENU_ID)
);

COMMENT ON TABLE  TB_MENU_M IS '시스템메뉴내역 (ERD: SY0040L)';
COMMENT ON COLUMN TB_MENU_M.MENU_ID                    IS '메뉴ID';
COMMENT ON COLUMN TB_MENU_M.MENU_NM                    IS '메뉴명';
COMMENT ON COLUMN TB_MENU_M.PRGM_ID                    IS '프로그램ID';
COMMENT ON COLUMN TB_MENU_M.UPPR_MENU_ID               IS '상위메뉴ID';
COMMENT ON COLUMN TB_MENU_M.ALIN_SEQ                   IS '정렬순번';
COMMENT ON COLUMN TB_MENU_M.MENU_TYPE_CD               IS '메뉴타입코드 (공통코드 SY/MENU_TYPE_CD: MENU 메뉴, LINK 링크)';
COMMENT ON COLUMN TB_MENU_M.RMK                        IS '비고';
COMMENT ON COLUMN TB_MENU_M.USE_YN                     IS '사용여부';
COMMENT ON COLUMN TB_MENU_M.REFL_PRGM_ID               IS '반영프로그램ID';
COMMENT ON COLUMN TB_MENU_M.FIRS_REGR_ID               IS '최초등록자ID';
COMMENT ON COLUMN TB_MENU_M.FIRS_REG_DT                IS '최초등록일시';
COMMENT ON COLUMN TB_MENU_M.FINA_REGR_ID               IS '최종등록자ID';
COMMENT ON COLUMN TB_MENU_M.FINA_REG_DT                IS '최종등록일시';
