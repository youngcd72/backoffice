-- PostgreSQL / CLAUDE.md 5.x 표준 적용 (COMP_CD 제외)
-- ---------------------------------------------------------------------
-- TB_LNK_ORG_INFC_R : 연계기관인터페이스관계 (coder-api, documents/api/PRD.md 7장)
--   연계기관이 호출할 수 있는 인터페이스 (토큰 scope 와 함께 InterfaceAuthFilter 가 확인)
-- ---------------------------------------------------------------------
CREATE TABLE TB_LNK_ORG_INFC_R (
    LNK_ORG_ID            VARCHAR(20)   NOT NULL,
    INFC_ID               VARCHAR(20)   NOT NULL,
    USE_YN                CHAR(1)       NOT NULL DEFAULT 'N',
    FIRS_REGR_ID          VARCHAR(10)   NOT NULL,
    FIRS_REG_DT           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FINA_REGR_ID          VARCHAR(10)   NOT NULL,
    FINA_REG_DT           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT TB_LNK_ORG_INFC_R_PK PRIMARY KEY (LNK_ORG_ID, INFC_ID),
    CONSTRAINT TB_LNK_ORG_INFC_R_FK01 FOREIGN KEY (LNK_ORG_ID) REFERENCES TB_LNK_ORG_M (LNK_ORG_ID),
    CONSTRAINT TB_LNK_ORG_INFC_R_FK02 FOREIGN KEY (INFC_ID) REFERENCES TB_INFC_M (INFC_ID)
);

CREATE INDEX TB_LNK_ORG_INFC_R_IX01 ON TB_LNK_ORG_INFC_R (INFC_ID);

COMMENT ON TABLE  TB_LNK_ORG_INFC_R IS '연계기관인터페이스관계';
COMMENT ON COLUMN TB_LNK_ORG_INFC_R.LNK_ORG_ID             IS '연계기관ID';
COMMENT ON COLUMN TB_LNK_ORG_INFC_R.INFC_ID                IS '인터페이스ID';
COMMENT ON COLUMN TB_LNK_ORG_INFC_R.USE_YN                 IS '사용여부';
COMMENT ON COLUMN TB_LNK_ORG_INFC_R.FIRS_REGR_ID           IS '최초등록자ID';
COMMENT ON COLUMN TB_LNK_ORG_INFC_R.FIRS_REG_DT            IS '최초등록일시';
COMMENT ON COLUMN TB_LNK_ORG_INFC_R.FINA_REGR_ID           IS '최종등록자ID';
COMMENT ON COLUMN TB_LNK_ORG_INFC_R.FINA_REG_DT            IS '최종등록일시';
