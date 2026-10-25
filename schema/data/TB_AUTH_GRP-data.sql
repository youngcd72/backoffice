-- =====================================================================
-- 권한그룹 등록 (TB_AUTH_GRP_M, TB_AUTH_GRP_MENU_R, TB_AUTH_GRP_CMPN_R, TB_USR_AUTH_GRP_R)
-- GRP_IT : 모든 링크 메뉴(TB_MENU_M.MENU_TYPE_CD = 'LINK') 권한,
--          화면 컴포넌트 권한(SYAU010, SYAU020, SYAU030, SYLO030, SYLK050), 사용자 admin/operator/viewer
-- TB_MENU_M-data.sql, TB_PRGM_CMPN_M-data.sql 을 먼저 실행한다.
-- 재실행해도 기존 행은 건드리지 않는다 (ON CONFLICT DO NOTHING).
-- 메뉴가 추가된 뒤 다시 실행하면 새 링크 메뉴만 GRP_IT 에 추가된다.
-- =====================================================================

INSERT INTO TB_AUTH_GRP_M (
    USR_GRP_CD, USR_GRP_NM, USE_YN, ST_DT, END_DT, RMK,
    FIRS_REGR_ID, FINA_REGR_ID
) VALUES (
    'GRP_IT', 'IT', 'Y', '20261017', '99991231', '모든 메뉴 권한',
    'admin', 'admin'
) ON CONFLICT (USR_GRP_CD) DO NOTHING;

INSERT INTO TB_AUTH_GRP_MENU_R (
    USR_GRP_CD, MENU_ID, PRGM_ID,
    FIRS_REGR_ID, FINA_REGR_ID
)
SELECT 'GRP_IT', MENU_ID, PRGM_ID,
       'admin', 'admin'
  FROM TB_MENU_M
 WHERE MENU_TYPE_CD = 'LINK'
   AND PRGM_ID IS NOT NULL
ON CONFLICT (USR_GRP_CD, MENU_ID, PRGM_ID) DO NOTHING;

-- GRP_IT 컴포넌트 권한: 화면 버튼 (권한관리 화면은 권한이 없으면 화면에서 컴포넌트 권한을 줄 수 없다)
INSERT INTO TB_AUTH_GRP_CMPN_R (
    USR_GRP_CD, PRGM_ID, CMPN_ID, AUTH_YN,
    FIRS_REGR_ID, FINA_REGR_ID
)
SELECT 'GRP_IT', PRGM_ID, CMPN_ID, 'Y',
       'admin', 'admin'
  FROM TB_PRGM_CMPN_M
 WHERE PRGM_ID IN ('SYAU010', 'SYAU020', 'SYAU030', 'SYLO030', 'SYLK050')
ON CONFLICT (USR_GRP_CD, PRGM_ID, CMPN_ID) DO NOTHING;

-- GRP_IT 사용자 (TB_USR_M 에 있는 사용자만)
INSERT INTO TB_USR_AUTH_GRP_R (
    USR_ID, USR_GRP_CD, ST_DT, END_DT,
    FIRS_REGR_ID, FINA_REGR_ID
)
SELECT USR_ID, 'GRP_IT', '20261017', '29991231',
       'admin', 'admin'
  FROM TB_USR_M
 WHERE USR_ID IN ('admin', 'operator', 'viewer')
ON CONFLICT (USR_ID, USR_GRP_CD) DO NOTHING;
