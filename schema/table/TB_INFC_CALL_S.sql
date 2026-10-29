-- PostgreSQL / CLAUDE.md 5.x 표준 적용 (COMP_CD 제외)
-- ---------------------------------------------------------------------
-- TB_INFC_CALL_S : 인터페이스호출집계 (coder-api, documents/api/PRD.md FR-07, 7장)
--   coder-batch 일 배치가 전일 TB_INFC_CALL_L 을 연계기관·인터페이스별로 집계한다 (Phase 2).
--   인증 전 실패(연계기관 없음)는 LNK_ORG_ID '-' 로 집계한다.
-- ---------------------------------------------------------------------
CREATE TABLE TB_INFC_CALL_S (
    STD_DT                CHAR(8)       NOT NULL,
    LNK_ORG_ID            VARCHAR(20)   NOT NULL,
    INFC_ID               VARCHAR(20)   NOT NULL,
    CALL_CNT              BIGINT        NOT NULL DEFAULT 0,
    SUCC_CNT              BIGINT        NOT NULL DEFAULT 0,
    FAIL_CNT              BIGINT        NOT NULL DEFAULT 0,
    AVG_ELAPSED_MS        BIGINT        NOT NULL DEFAULT 0,
    MAX_ELAPSED_MS        BIGINT        NOT NULL DEFAULT 0,
    FIRS_REGR_ID          VARCHAR(10)   NOT NULL,
    FIRS_REG_DT           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FINA_REGR_ID          VARCHAR(10)   NOT NULL,
    FINA_REG_DT           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT TB_INFC_CALL_S_PK PRIMARY KEY (STD_DT, LNK_ORG_ID, INFC_ID)
);

CREATE INDEX TB_INFC_CALL_S_IX01 ON TB_INFC_CALL_S (LNK_ORG_ID, STD_DT);

COMMENT ON TABLE  TB_INFC_CALL_S IS '인터페이스호출집계';
COMMENT ON COLUMN TB_INFC_CALL_S.STD_DT                 IS '기준일자 (YYYYMMDD)';
COMMENT ON COLUMN TB_INFC_CALL_S.LNK_ORG_ID             IS '연계기관ID (인증 전 실패는 -)';
COMMENT ON COLUMN TB_INFC_CALL_S.INFC_ID                IS '인터페이스ID';
COMMENT ON COLUMN TB_INFC_CALL_S.CALL_CNT               IS '호출건수';
COMMENT ON COLUMN TB_INFC_CALL_S.SUCC_CNT               IS '성공건수 (HTTP 2xx)';
COMMENT ON COLUMN TB_INFC_CALL_S.FAIL_CNT               IS '실패건수';
COMMENT ON COLUMN TB_INFC_CALL_S.AVG_ELAPSED_MS         IS '평균경과밀리초';
COMMENT ON COLUMN TB_INFC_CALL_S.MAX_ELAPSED_MS         IS '최대경과밀리초';
COMMENT ON COLUMN TB_INFC_CALL_S.FIRS_REGR_ID           IS '최초등록자ID';
COMMENT ON COLUMN TB_INFC_CALL_S.FIRS_REG_DT            IS '최초등록일시';
COMMENT ON COLUMN TB_INFC_CALL_S.FINA_REGR_ID           IS '최종등록자ID';
COMMENT ON COLUMN TB_INFC_CALL_S.FINA_REG_DT            IS '최종등록일시';
