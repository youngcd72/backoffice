-- PostgreSQL / CLAUDE.md 5.x 표준 적용 (COMP_CD 제외)
-- ---------------------------------------------------------------------
-- TB_LNK_ORG_M : 연계기관기본 (coder-api, documents/api/PRD.md 7장)
--   API를 호출하는 외부 주체 (거래처, 기관, 그룹사 시스템, 앱)
-- ---------------------------------------------------------------------
CREATE TABLE TB_LNK_ORG_M (
    LNK_ORG_ID            VARCHAR(20)   NOT NULL,
    LNK_ORG_NM            VARCHAR(100)  NOT NULL,
    CHNL_CLS_CD           VARCHAR(1)    NOT NULL,
    USE_ST_DT             CHAR(8),
    USE_END_DT            CHAR(8),
    USE_YN                CHAR(1)       NOT NULL DEFAULT 'N',
    RMK                   VARCHAR(200),
    FIRS_REGR_ID          VARCHAR(10)   NOT NULL,
    FIRS_REG_DT           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FINA_REGR_ID          VARCHAR(10)   NOT NULL,
    FINA_REG_DT           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT TB_LNK_ORG_M_PK PRIMARY KEY (LNK_ORG_ID)
);

COMMENT ON TABLE  TB_LNK_ORG_M IS '연계기관기본';
COMMENT ON COLUMN TB_LNK_ORG_M.LNK_ORG_ID             IS '연계기관ID';
COMMENT ON COLUMN TB_LNK_ORG_M.LNK_ORG_NM             IS '연계기관명';
COMMENT ON COLUMN TB_LNK_ORG_M.CHNL_CLS_CD            IS '채널구분코드 (SY/CHNL_CLS_CD: B B2B, C B2C, I 내부 시스템)';
COMMENT ON COLUMN TB_LNK_ORG_M.USE_ST_DT              IS '사용시작일자 (YYYYMMDD, NULL이면 제한 없음)';
COMMENT ON COLUMN TB_LNK_ORG_M.USE_END_DT             IS '사용종료일자 (YYYYMMDD, NULL이면 제한 없음)';
COMMENT ON COLUMN TB_LNK_ORG_M.USE_YN                 IS '사용여부';
COMMENT ON COLUMN TB_LNK_ORG_M.RMK                    IS '비고';
COMMENT ON COLUMN TB_LNK_ORG_M.FIRS_REGR_ID           IS '최초등록자ID';
COMMENT ON COLUMN TB_LNK_ORG_M.FIRS_REG_DT            IS '최초등록일시';
COMMENT ON COLUMN TB_LNK_ORG_M.FINA_REGR_ID           IS '최종등록자ID';
COMMENT ON COLUMN TB_LNK_ORG_M.FINA_REG_DT            IS '최종등록일시';
