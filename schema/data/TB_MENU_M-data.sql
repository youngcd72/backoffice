-- =====================================================================
-- 메뉴 등록 (TB_MENU_M)
-- 구조: 대분류(XX000000) > 중분류(XXnn0000) > 화면 링크(XXnnmm00)
-- MENU_TYPE_CD는 공통코드 SY/MENU_TYPE_CD (MENU 메뉴, LINK 링크)이다. LINK만 PRGM_ID를 가진다.
-- TB_COMM_CD-data.sql, TB_PRGM_M-data.sql 을 먼저 실행한다.
-- 재실행해도 기존 행은 건드리지 않는다 (ON CONFLICT DO NOTHING).
-- =====================================================================

INSERT INTO TB_MENU_M (
    MENU_ID, MENU_NM, ALIN_SEQ, MENU_TYPE_CD, UPPR_MENU_ID, USE_YN, PRGM_ID, FIRS_REGR_ID, FINA_REGR_ID
) VALUES
    -- 기준정보
    ('MS000000', '기준정보',           1, 'MENU', NULL,       'Y', NULL,      'admin', 'admin'),
    ('MS010000', '기초정보',           1, 'MENU', 'MS000000', 'Y', NULL,      'admin', 'admin'),
    ('MS010100', '공통코드관리',       1, 'LINK', 'MS010000', 'Y', 'MSBS010', 'admin', 'admin'),
    -- 시스템관리
    ('SY000000', '시스템관리',         2, 'MENU', NULL,       'Y', NULL,      'admin', 'admin'),
    -- 시스템관리 > 메뉴관리
    ('SY010000', '메뉴관리',           1, 'MENU', 'SY000000', 'Y', NULL,      'admin', 'admin'),
    ('SY010100', '프로그램관리',       1, 'LINK', 'SY010000', 'Y', 'SYPG010', 'admin', 'admin'),
    ('SY010200', '메뉴관리',           2, 'LINK', 'SY010000', 'Y', 'SYPG020', 'admin', 'admin'),
    -- 시스템관리 > 권한관리
    ('SY020000', '권한관리',           2, 'MENU', 'SY000000', 'Y', NULL,      'admin', 'admin'),
    ('SY020100', '사용자권한관리',     1, 'LINK', 'SY020000', 'Y', 'SYAU010', 'admin', 'admin'),
    ('SY020200', '그룹권한관리',       2, 'LINK', 'SY020000', 'Y', 'SYAU020', 'admin', 'admin'),
    ('SY020300', '그룹별사용자관리',   3, 'LINK', 'SY020000', 'Y', 'SYAU030', 'admin', 'admin'),
    -- 시스템관리 > LOG관리
    ('SY030000', 'LOG관리',            3, 'MENU', 'SY000000', 'Y', NULL,      'admin', 'admin'),
    ('SY030100', '시스템로그관리',     1, 'LINK', 'SY030000', 'Y', 'SYLO030', 'admin', 'admin'),
    -- 시스템관리 > 배치관리
    ('SY040000', '배치관리',           4, 'MENU', 'SY000000', 'Y', NULL,      'admin', 'admin'),
    ('SY040100', '배치관리',           1, 'LINK', 'SY040000', 'Y', 'SYLK050', 'admin', 'admin')
ON CONFLICT (MENU_ID) DO NOTHING;
