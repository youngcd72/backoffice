-- =====================================================================
-- 왼쪽 메뉴 프로그램 등록 (TB_PRGM_M)
-- PRGM_ID는 화면 ID(라우트 name)와 같아야 한다. 왼쪽 메뉴는 TB_MENU_M.PRGM_ID 로 라우트를 찾는다.
-- PRGM_CRS_CD는 프로그램경로 공통코드(SY/PRGM_CRS_CD: SY 시스템관리, MS 기준정보)이다.
-- LNK_URL은 화면 라우트 경로(router/index.ts path, /{프로그램경로 소문자}/{프로그램ID 소문자})이다.
-- 재실행해도 기존 행은 건드리지 않는다 (ON CONFLICT DO NOTHING).
-- =====================================================================

INSERT INTO TB_PRGM_M (
    PRGM_ID, PRGM_NM, LNK_URL, PRGM_CRS_CD, ALIN_SEQ, USE_YN, ST_DT, END_DT, RMK,
    FIRS_REGR_ID, FINA_REGR_ID
) VALUES
    -- 시스템관리 > 메뉴관리
    ('SYPG010', '프로그램관리',       '/sy/sypg010', 'SY', 10,  'Y', '20261017', '99991231', NULL, 'admin', 'admin'),
    ('SYPG020', '메뉴관리',           '/sy/sypg020', 'SY', 20,  'Y', '20261017', '99991231', NULL, 'admin', 'admin'),
    -- 시스템관리 > 권한관리
    ('SYAU010', '사용자권한관리',     '/sy/syau010', 'SY', 30,  'Y', '20261017', '99991231', NULL, 'admin', 'admin'),
    ('SYAU020', '그룹권한관리',       '/sy/syau020', 'SY', 40,  'Y', '20261017', '99991231', NULL, 'admin', 'admin'),
    ('SYAU030', '그룹별사용자관리',   '/sy/syau030', 'SY', 50,  'Y', '20261017', '99991231', NULL, 'admin', 'admin'),
    -- 시스템관리 > LOG관리
    ('SYLO030', '시스템로그관리',     '/sy/sylo030', 'SY', 60,  'Y', '20261017', '99991231', NULL, 'admin', 'admin'),
    -- 시스템관리 > 배치관리
    ('SYLK050', '배치관리',           '/sy/sylk050', 'SY', 70,  'Y', '20261017', '99991231', NULL, 'admin', 'admin'),
    -- 기준정보 > 기초정보
    ('MSBS010', '공통코드관리',       '/ms/msbs010', 'MS', 80,  'Y', '20261017', '99991231', NULL, 'admin', 'admin')
ON CONFLICT (PRGM_ID) DO NOTHING;
