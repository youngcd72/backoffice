-- =====================================================================
-- coder-api 인터페이스 / 로컬 샘플 연계기관 등록 (TB_INFC_M, TB_LNK_ORG_M, TB_LNK_ORG_INFC_R)
-- 재실행해도 기존 행은 건드리지 않는다 (ON CONFLICT DO NOTHING).
-- 클라이언트 키는 KMS 로 비밀키를 암호화해야 하므로 SQL 로 넣지 않는다. coder-api 를 아래처럼 실행해 발급한다.
--   gradlew bootRun --args="--coder.api.key-issue.lnk-org-id=LOCAL01 --server.port=0"
-- =====================================================================

-- ---------------------------------------------------------------------
-- 인터페이스 (업무 API Controller 의 @ApiInterface 와 1:1)
-- ---------------------------------------------------------------------
INSERT INTO TB_INFC_M (
    INFC_ID, INFC_NM, HTTP_METH_NM, URL, BODY_LOG_YN, MASK_FLD_CONT, USE_YN, RMK,
    FIRS_REGR_ID, FINA_REGR_ID
) VALUES
    ('IFSY0001', '샘플 조회 (공통코드)', 'GET', '/api/v1/samples/{id}', 'Y', NULL, 'Y', 'coder-api 샘플 업무 API',
     'admin', 'admin')
ON CONFLICT (INFC_ID) DO NOTHING;

-- ---------------------------------------------------------------------
-- 로컬 개발용 샘플 연계기관 (운영 DB 에는 넣지 않는다)
-- ---------------------------------------------------------------------
INSERT INTO TB_LNK_ORG_M (
    LNK_ORG_ID, LNK_ORG_NM, CHNL_CLS_CD, USE_ST_DT, USE_END_DT, USE_YN, RMK,
    FIRS_REGR_ID, FINA_REGR_ID
) VALUES
    ('LOCAL01', '로컬 테스트 기관', 'I', NULL, NULL, 'Y', '로컬 개발·통합 테스트용',
     'admin', 'admin')
ON CONFLICT (LNK_ORG_ID) DO NOTHING;

INSERT INTO TB_LNK_ORG_INFC_R (
    LNK_ORG_ID, INFC_ID, USE_YN,
    FIRS_REGR_ID, FINA_REGR_ID
) VALUES
    ('LOCAL01', 'IFSY0001', 'Y', 'admin', 'admin')
ON CONFLICT (LNK_ORG_ID, INFC_ID) DO NOTHING;
