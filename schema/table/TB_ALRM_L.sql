-- PostgreSQL / CLAUDE.md 5.x 표준 적용 (COMP_CD 제외)
-- ---------------------------------------------------------------------
-- TB_ALRM_L : 알람내역 (상단 알림 센터, SSE 실시간 전송)
--   수신자별 알림 1건 = 1행. 접속 중이면 SSE 로 즉시 전송하고, 미접속이면 다음 접속 시 조회한다.
-- ---------------------------------------------------------------------
CREATE SEQUENCE SQ_TB_ALRM_L_ALRM_SN START WITH 1 INCREMENT BY 1;

CREATE TABLE TB_ALRM_L (
    ALRM_SN               BIGINT        NOT NULL,
    RCVR_ID               VARCHAR(10)   NOT NULL,
    ALRM_TYPE_CD          VARCHAR(20)   NOT NULL,
    ALRM_TITL_NM          VARCHAR(200)  NOT NULL,
    ALRM_CONT             VARCHAR(4000),
    PRGM_ID               VARCHAR(100),
    CFM_YN                CHAR(1)       NOT NULL DEFAULT 'N',
    CFM_DTTM              TIMESTAMP,
    FIRS_REGR_ID          VARCHAR(10)   NOT NULL,
    FIRS_REG_DT           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FINA_REGR_ID          VARCHAR(10)   NOT NULL,
    FINA_REG_DT           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT TB_ALRM_L_PK PRIMARY KEY (ALRM_SN),
    CONSTRAINT TB_ALRM_L_FK01 FOREIGN KEY (RCVR_ID) REFERENCES TB_USR_M (USR_ID),
    CONSTRAINT TB_ALRM_L_CK01 CHECK (CFM_YN IN ('Y', 'N'))
);

CREATE INDEX TB_ALRM_L_IX01 ON TB_ALRM_L (RCVR_ID, CFM_YN, FIRS_REG_DT);

COMMENT ON SEQUENCE SQ_TB_ALRM_L_ALRM_SN IS '알람내역 알람일련번호 채번';
COMMENT ON TABLE  TB_ALRM_L IS '알람내역';
COMMENT ON COLUMN TB_ALRM_L.ALRM_SN                IS '알람일련번호';
COMMENT ON COLUMN TB_ALRM_L.RCVR_ID                IS '수신자ID (TB_USR_M.USR_ID)';
COMMENT ON COLUMN TB_ALRM_L.ALRM_TYPE_CD           IS '알람유형코드 (공통코드 SY/ALRM_TYPE_CD)';
COMMENT ON COLUMN TB_ALRM_L.ALRM_TITL_NM           IS '알람제목명';
COMMENT ON COLUMN TB_ALRM_L.ALRM_CONT              IS '알람내용';
COMMENT ON COLUMN TB_ALRM_L.PRGM_ID                IS '프로그램ID (클릭 시 열 화면, 없으면 이동 없음)';
COMMENT ON COLUMN TB_ALRM_L.CFM_YN                 IS '확인여부 (Y 읽음, N 안 읽음)';
COMMENT ON COLUMN TB_ALRM_L.CFM_DTTM               IS '확인일시';
COMMENT ON COLUMN TB_ALRM_L.FIRS_REGR_ID           IS '최초등록자ID (발송자)';
COMMENT ON COLUMN TB_ALRM_L.FIRS_REG_DT            IS '최초등록일시 (발송일시)';
COMMENT ON COLUMN TB_ALRM_L.FINA_REGR_ID           IS '최종등록자ID';
COMMENT ON COLUMN TB_ALRM_L.FINA_REG_DT            IS '최종등록일시';
