-- PostgreSQL / CLAUDE.md 5.x 표준 적용 (COMP_CD 제외)
-- ---------------------------------------------------------------------
-- TB_PRGM_FAV_L : 프로그램즐겨찾기내역 (ERD: SY0031L 시스템_프로그램즐겨찾기내역)
--   사용자별 즐겨찾기 메뉴. (구 TB_FAV_M 을 엔티티 유형 L 기준으로 개명)
-- ---------------------------------------------------------------------
CREATE TABLE TB_PRGM_FAV_L (
    USR_ID                VARCHAR(10)  NOT NULL,
    MENU_ID               VARCHAR(20)  NOT NULL,
    ALIN_SEQ              INTEGER,
    REFL_PRGM_ID          VARCHAR(50),
    FIRS_REGR_ID          VARCHAR(10)  NOT NULL,
    FIRS_REG_DT           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FINA_REGR_ID          VARCHAR(10)  NOT NULL,
    FINA_REG_DT           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT TB_PRGM_FAV_L_PK PRIMARY KEY (USR_ID, MENU_ID),
    CONSTRAINT TB_PRGM_FAV_L_FK01 FOREIGN KEY (USR_ID)
        REFERENCES TB_USR_M (USR_ID),
    CONSTRAINT TB_PRGM_FAV_L_FK02 FOREIGN KEY (MENU_ID)
        REFERENCES TB_MENU_M (MENU_ID)
);

COMMENT ON TABLE  TB_PRGM_FAV_L IS '프로그램즐겨찾기내역 (ERD: SY0031L)';
COMMENT ON COLUMN TB_PRGM_FAV_L.USR_ID                 IS '사용자ID';
COMMENT ON COLUMN TB_PRGM_FAV_L.MENU_ID                IS '메뉴ID';
COMMENT ON COLUMN TB_PRGM_FAV_L.ALIN_SEQ               IS '정렬순번';
COMMENT ON COLUMN TB_PRGM_FAV_L.REFL_PRGM_ID           IS '반영프로그램ID';
COMMENT ON COLUMN TB_PRGM_FAV_L.FIRS_REGR_ID           IS '최초등록자ID';
COMMENT ON COLUMN TB_PRGM_FAV_L.FIRS_REG_DT            IS '최초등록일시';
COMMENT ON COLUMN TB_PRGM_FAV_L.FINA_REGR_ID           IS '최종등록자ID';
COMMENT ON COLUMN TB_PRGM_FAV_L.FINA_REG_DT            IS '최종등록일시';
