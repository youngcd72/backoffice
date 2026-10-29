-- PostgreSQL / CLAUDE.md 5.x 표준 적용 (COMP_CD 제외)
-- ---------------------------------------------------------------------
-- TB_INFC_M : 인터페이스기본 (coder-api, documents/api/PRD.md 7장)
--   외부에 공개하는 API 단위. 업무 API Controller 의 @ApiInterface("INFC_ID") 와 1:1.
--   권한·본문 로그·마스킹을 인터페이스별로 설정한다.
-- ---------------------------------------------------------------------
CREATE TABLE TB_INFC_M (
    INFC_ID               VARCHAR(20)   NOT NULL,
    INFC_NM               VARCHAR(100)  NOT NULL,
    HTTP_METH_NM          VARCHAR(10)   NOT NULL,
    URL                   VARCHAR(200)  NOT NULL,
    BODY_LOG_YN           CHAR(1)       NOT NULL DEFAULT 'N',
    MASK_FLD_CONT         VARCHAR(1000),
    USE_YN                CHAR(1)       NOT NULL DEFAULT 'N',
    RMK                   VARCHAR(200),
    FIRS_REGR_ID          VARCHAR(10)   NOT NULL,
    FIRS_REG_DT           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FINA_REGR_ID          VARCHAR(10)   NOT NULL,
    FINA_REG_DT           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT TB_INFC_M_PK PRIMARY KEY (INFC_ID)
);

COMMENT ON TABLE  TB_INFC_M IS '인터페이스기본';
COMMENT ON COLUMN TB_INFC_M.INFC_ID                IS '인터페이스ID (IF + 업무약어 2자리 + 순번 4자리, 예: IFSY0001)';
COMMENT ON COLUMN TB_INFC_M.INFC_NM                IS '인터페이스명';
COMMENT ON COLUMN TB_INFC_M.HTTP_METH_NM           IS 'HTTP메서드명 (GET, POST ...)';
COMMENT ON COLUMN TB_INFC_M.URL                    IS 'URL (매핑 경로, 예: /api/v1/samples/{id})';
COMMENT ON COLUMN TB_INFC_M.BODY_LOG_YN            IS '본문로그여부 (Y면 요청·응답 본문을 마스킹해 호출 이력에 남김)';
COMMENT ON COLUMN TB_INFC_M.MASK_FLD_CONT          IS '마스킹필드내용 (쉼표 구분. 필드명이면 모든 위치, data.usrNm 처럼 점으로 이으면 그 경로만)';
COMMENT ON COLUMN TB_INFC_M.USE_YN                 IS '사용여부';
COMMENT ON COLUMN TB_INFC_M.RMK                    IS '비고';
COMMENT ON COLUMN TB_INFC_M.FIRS_REGR_ID           IS '최초등록자ID';
COMMENT ON COLUMN TB_INFC_M.FIRS_REG_DT            IS '최초등록일시';
COMMENT ON COLUMN TB_INFC_M.FINA_REGR_ID           IS '최종등록자ID';
COMMENT ON COLUMN TB_INFC_M.FINA_REG_DT            IS '최종등록일시';
