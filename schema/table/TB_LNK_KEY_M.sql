-- PostgreSQL / CLAUDE.md 5.x 표준 적용 (COMP_CD 제외)
-- ---------------------------------------------------------------------
-- TB_LNK_KEY_M : 연계키기본 (coder-api, documents/api/PRD.md 7장)
--   연계기관에 발급한 클라이언트 키 (client_id = KEY_ID). 한 기관에 여러 개 발급할 수 있다 (교체 기간 중첩).
--   비밀키 원문은 저장하지 않고 KMS 로 암호화한 값만 저장한다.
-- ---------------------------------------------------------------------
CREATE TABLE TB_LNK_KEY_M (
    KEY_ID                VARCHAR(40)   NOT NULL,
    LNK_ORG_ID            VARCHAR(20)   NOT NULL,
    ENC_KEY_VAL           VARCHAR(500)  NOT NULL,
    KMS_KEY_ID            VARCHAR(100)  NOT NULL,
    KEY_STAT_CD           VARCHAR(2)    NOT NULL DEFAULT '01',
    ISSU_DTTM             TIMESTAMP     NOT NULL,
    EXPI_DTTM             TIMESTAMP,
    ALLW_IP_ADDR          VARCHAR(1000),
    MI_CALL_LMT_CNT       INTEGER,
    DAY_CALL_LMT_CNT      INTEGER,
    SIMU_RQMT_LMT_CNT     INTEGER,
    RMK                   VARCHAR(200),
    FIRS_REGR_ID          VARCHAR(10)   NOT NULL,
    FIRS_REG_DT           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FINA_REGR_ID          VARCHAR(10)   NOT NULL,
    FINA_REG_DT           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT TB_LNK_KEY_M_PK PRIMARY KEY (KEY_ID),
    CONSTRAINT TB_LNK_KEY_M_FK01 FOREIGN KEY (LNK_ORG_ID) REFERENCES TB_LNK_ORG_M (LNK_ORG_ID)
);

CREATE INDEX TB_LNK_KEY_M_IX01 ON TB_LNK_KEY_M (LNK_ORG_ID);

COMMENT ON TABLE  TB_LNK_KEY_M IS '연계키기본';
COMMENT ON COLUMN TB_LNK_KEY_M.KEY_ID                 IS '키ID (OAuth2 client_id, ck + 난수 24자리)';
COMMENT ON COLUMN TB_LNK_KEY_M.LNK_ORG_ID             IS '연계기관ID';
COMMENT ON COLUMN TB_LNK_KEY_M.ENC_KEY_VAL            IS '암호화키값 (KMS 로 암호화한 client_secret)';
COMMENT ON COLUMN TB_LNK_KEY_M.KMS_KEY_ID             IS 'KMS키ID (암호화에 쓴 KEK ID)';
COMMENT ON COLUMN TB_LNK_KEY_M.KEY_STAT_CD            IS '키상태코드 (SY/KEY_STAT_CD: 01 사용, 02 사용중지, 03 폐기)';
COMMENT ON COLUMN TB_LNK_KEY_M.ISSU_DTTM              IS '발급일시';
COMMENT ON COLUMN TB_LNK_KEY_M.EXPI_DTTM              IS '만료일시 (NULL이면 만료 없음)';
COMMENT ON COLUMN TB_LNK_KEY_M.ALLW_IP_ADDR           IS '허용IP주소 (IP/CIDR 쉼표 구분, NULL이면 제한 없음)';
COMMENT ON COLUMN TB_LNK_KEY_M.MI_CALL_LMT_CNT        IS '분호출한도건수 (NULL이면 제한 없음)';
COMMENT ON COLUMN TB_LNK_KEY_M.DAY_CALL_LMT_CNT       IS '일호출한도건수 (NULL이면 제한 없음)';
COMMENT ON COLUMN TB_LNK_KEY_M.SIMU_RQMT_LMT_CNT      IS '동시요청한도건수 (NULL이면 제한 없음)';
COMMENT ON COLUMN TB_LNK_KEY_M.RMK                    IS '비고';
COMMENT ON COLUMN TB_LNK_KEY_M.FIRS_REGR_ID           IS '최초등록자ID';
COMMENT ON COLUMN TB_LNK_KEY_M.FIRS_REG_DT            IS '최초등록일시';
COMMENT ON COLUMN TB_LNK_KEY_M.FINA_REGR_ID           IS '최종등록자ID';
COMMENT ON COLUMN TB_LNK_KEY_M.FINA_REG_DT            IS '최종등록일시';
