-- PostgreSQL / CLAUDE.md 5.x 표준 적용 (COMP_CD 제외)
-- ---------------------------------------------------------------------
-- TB_INFC_CALL_L : 인터페이스호출내역 (coder-api, documents/api/PRD.md FR-06, 7장)
--   업무 API 호출(인증·권한·한도 실패 포함) 1건당 1행. coder-api CallLogRecorder 가 모아서 일괄 INSERT 한다.
--   건수가 많아 요청일시(RQMT_DTTM) 기준 월 파티션으로 둔다 (PK 에 파티션 키 포함).
--   내역 테이블이라 최초 등록 컬럼만 둔다. 보관 기간이 지난 파티션은 coder-batch 정리 배치가 삭제한다 (Phase 2).
--   FK 는 두지 않는다 (인증 실패 호출은 KEY_ID 가 없고, 키 폐기 후에도 이력은 남긴다).
-- ---------------------------------------------------------------------
CREATE SEQUENCE SQ_TB_INFC_CALL_L_CALL_SN START WITH 1 INCREMENT BY 1 CACHE 100;

CREATE TABLE TB_INFC_CALL_L (
    CALL_SN               BIGINT        NOT NULL,
    TRC_ID                VARCHAR(64)   NOT NULL,
    KEY_ID                VARCHAR(40),
    LNK_ORG_ID            VARCHAR(20),
    INFC_ID               VARCHAR(20)   NOT NULL,
    HTTP_METH_NM          VARCHAR(10)   NOT NULL,
    URL                   VARCHAR(500)  NOT NULL,
    HTTP_STAT_CD          VARCHAR(3)    NOT NULL,
    CLNT_IP_ADDR          VARCHAR(45),
    RQMT_DTTM             TIMESTAMP     NOT NULL,
    RSPN_DTTM             TIMESTAMP     NOT NULL,
    ELAPSED_MS            BIGINT        NOT NULL DEFAULT 0,
    ERR_CD                VARCHAR(100),
    RQMT_CONT             VARCHAR(4000),
    RSPN_CONT             VARCHAR(4000),
    FIRS_REGR_ID          VARCHAR(10)   NOT NULL,
    FIRS_REG_DT           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT TB_INFC_CALL_L_PK PRIMARY KEY (CALL_SN, RQMT_DTTM)
) PARTITION BY RANGE (RQMT_DTTM);

CREATE INDEX TB_INFC_CALL_L_IX01 ON TB_INFC_CALL_L (LNK_ORG_ID, RQMT_DTTM);
CREATE INDEX TB_INFC_CALL_L_IX02 ON TB_INFC_CALL_L (TRC_ID);

COMMENT ON TABLE  TB_INFC_CALL_L IS '인터페이스호출내역';
COMMENT ON COLUMN TB_INFC_CALL_L.CALL_SN                IS '호출일련번호';
COMMENT ON COLUMN TB_INFC_CALL_L.TRC_ID                 IS '추적ID (X-Trace-Id, 에러 로그 traceId 와 같은 값)';
COMMENT ON COLUMN TB_INFC_CALL_L.KEY_ID                 IS '키ID (토큰 인증 전 실패면 NULL)';
COMMENT ON COLUMN TB_INFC_CALL_L.LNK_ORG_ID             IS '연계기관ID (토큰 인증 전 실패면 NULL)';
COMMENT ON COLUMN TB_INFC_CALL_L.INFC_ID                IS '인터페이스ID';
COMMENT ON COLUMN TB_INFC_CALL_L.HTTP_METH_NM           IS 'HTTP메서드명';
COMMENT ON COLUMN TB_INFC_CALL_L.URL                    IS 'URL (요청 경로, 쿼리 문자열 제외)';
COMMENT ON COLUMN TB_INFC_CALL_L.HTTP_STAT_CD           IS 'HTTP상태코드';
COMMENT ON COLUMN TB_INFC_CALL_L.CLNT_IP_ADDR           IS '클라이언트IP주소';
COMMENT ON COLUMN TB_INFC_CALL_L.RQMT_DTTM              IS '요청일시';
COMMENT ON COLUMN TB_INFC_CALL_L.RSPN_DTTM              IS '응답일시';
COMMENT ON COLUMN TB_INFC_CALL_L.ELAPSED_MS             IS '경과밀리초 (소요시간)';
COMMENT ON COLUMN TB_INFC_CALL_L.ERR_CD                 IS '오류코드 (A001~A005, C001 등, 업무 예외는 메시지 키)';
COMMENT ON COLUMN TB_INFC_CALL_L.RQMT_CONT              IS '요청내용 (BODY_LOG_YN=Y 일 때만, 마스킹 후 최대 4000자)';
COMMENT ON COLUMN TB_INFC_CALL_L.RSPN_CONT              IS '응답내용 (BODY_LOG_YN=Y 일 때만, 마스킹 후 최대 4000자)';
COMMENT ON COLUMN TB_INFC_CALL_L.FIRS_REGR_ID           IS '최초등록자ID';
COMMENT ON COLUMN TB_INFC_CALL_L.FIRS_REG_DT            IS '최초등록일시';

-- 월 파티션: 2026-10 ~ 2027-12 + 기본 파티션 (이후 파티션은 coder-batch 가 미리 만든다, Phase 2)
DO $$
DECLARE
    month_start DATE := DATE '2026-10-01';
BEGIN
    WHILE month_start < DATE '2028-01-01' LOOP
        EXECUTE format('CREATE TABLE IF NOT EXISTS TB_INFC_CALL_L_%s PARTITION OF TB_INFC_CALL_L FOR VALUES FROM (%L) TO (%L)',
                       to_char(month_start, 'YYYYMM'), month_start, (month_start + INTERVAL '1 month')::DATE);
        month_start := (month_start + INTERVAL '1 month')::DATE;
    END LOOP;
END $$;

CREATE TABLE TB_INFC_CALL_L_DEF PARTITION OF TB_INFC_CALL_L DEFAULT;
