-- PostgreSQL / CLAUDE.md 5.x 표준 적용 (COMP_CD 제외)
-- ---------------------------------------------------------------------
-- TB_LNK_KEY_H : 연계키이력 (coder-api, documents/api/PRD.md 7장)
--   키 발급·상태 변경·폐기 이력 (이력 테이블이라 최초 등록 컬럼만 둔다)
-- ---------------------------------------------------------------------
CREATE SEQUENCE SQ_TB_LNK_KEY_H_KEY_HIST_SN START WITH 1 INCREMENT BY 1;

CREATE TABLE TB_LNK_KEY_H (
    KEY_HIST_SN           BIGINT        NOT NULL,
    KEY_ID                VARCHAR(40)   NOT NULL,
    KEY_STAT_CD           VARCHAR(2)    NOT NULL,
    CHG_CONT              VARCHAR(4000),
    KMS_AUDIT_ID          VARCHAR(100),
    FIRS_REGR_ID          VARCHAR(10)   NOT NULL,
    FIRS_REG_DT           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT TB_LNK_KEY_H_PK PRIMARY KEY (KEY_HIST_SN),
    CONSTRAINT TB_LNK_KEY_H_FK01 FOREIGN KEY (KEY_ID) REFERENCES TB_LNK_KEY_M (KEY_ID)
);

CREATE INDEX TB_LNK_KEY_H_IX01 ON TB_LNK_KEY_H (KEY_ID, FIRS_REG_DT);

COMMENT ON TABLE  TB_LNK_KEY_H IS '연계키이력';
COMMENT ON COLUMN TB_LNK_KEY_H.KEY_HIST_SN            IS '키이력일련번호';
COMMENT ON COLUMN TB_LNK_KEY_H.KEY_ID                 IS '키ID';
COMMENT ON COLUMN TB_LNK_KEY_H.KEY_STAT_CD            IS '키상태코드 (변경 후 상태)';
COMMENT ON COLUMN TB_LNK_KEY_H.CHG_CONT               IS '정정내용 (변경 내용: 발급, 상태 변경, 허용 IP·한도 수정 등)';
COMMENT ON COLUMN TB_LNK_KEY_H.KMS_AUDIT_ID           IS 'KMS감사ID (KMS 감사 로그 대조용)';
COMMENT ON COLUMN TB_LNK_KEY_H.FIRS_REGR_ID           IS '최초등록자ID';
COMMENT ON COLUMN TB_LNK_KEY_H.FIRS_REG_DT            IS '최초등록일시';
