-- =====================================================================
-- 프로그램 컴포넌트 등록 (TB_PRGM_CMPN_M)
-- CMPN_ID는 화면 버튼 등의 id 와 같아야 한다 (CLAUDE.md Ref / ID 네이밍).
-- TB_PRGM_M-data.sql 을 먼저 실행한다 (PRGM_ID FK).
-- 재실행해도 기존 행은 건드리지 않는다 (ON CONFLICT DO NOTHING).
-- =====================================================================

INSERT INTO TB_PRGM_CMPN_M (
    PRGM_ID, CMPN_ID, CMPN_NM, ALIN_SEQ, USE_YN, FIRS_REGR_ID, FINA_REGR_ID
) VALUES
    -- SYPG010 프로그램관리 (views/system/menu/ProgramManageView.vue)
    ('SYPG010', 'btnNew',        '신규',            1, 'Y', 'admin', 'admin'),
    ('SYPG010', 'btnSave',       '저장',            2, 'Y', 'admin', 'admin'),
    ('SYPG010', 'btnXlsDnld',    '엑셀',            3, 'Y', 'admin', 'admin'),
    ('SYPG010', 'btnSrch',       '조회',            4, 'Y', 'admin', 'admin'),
    ('SYPG010', 'btnRowAdd',     '행추가',          5, 'Y', 'admin', 'admin'),
    ('SYPG010', 'btnRowDel',     '행삭제',          6, 'Y', 'admin', 'admin'),
    ('SYPG010', 'btnCmpnRowAdd', '컴포넌트 행추가', 7, 'Y', 'admin', 'admin'),
    ('SYPG010', 'btnCmpnRowDel', '컴포넌트 행삭제', 8, 'Y', 'admin', 'admin'),
    -- SYPG020 메뉴관리 (views/system/menu/MenuManageView.vue)
    ('SYPG020', 'btnNew',           '신규',                   1, 'Y', 'admin', 'admin'),
    ('SYPG020', 'btnSave',          '저장',                   2, 'Y', 'admin', 'admin'),
    ('SYPG020', 'btnXlsDnld',       '엑셀',                   3, 'Y', 'admin', 'admin'),
    ('SYPG020', 'btnSrch',          '조회',                   4, 'Y', 'admin', 'admin'),
    ('SYPG020', 'btnRowAdd',        '행추가',                 5, 'Y', 'admin', 'admin'),
    ('SYPG020', 'btnRowDel',        '행삭제',                 6, 'Y', 'admin', 'admin'),
    ('SYPG020', 'btnUpprMenuRoot',  '상위메뉴 최상위로 지정', 7, 'Y', 'admin', 'admin'),
    ('SYPG020', 'btnUpprMenuClose', '상위메뉴 팝업 닫기',     8, 'Y', 'admin', 'admin'),
    -- MSBS010 공통코드관리 (views/master/basic/CommonCodeManageView.vue)
    ('MSBS010', 'btnNew',           '신규',                   1, 'Y', 'admin', 'admin'),
    ('MSBS010', 'btnSave',          '저장',                   2, 'Y', 'admin', 'admin'),
    ('MSBS010', 'btnDel',           '삭제',                   3, 'Y', 'admin', 'admin'),
    ('MSBS010', 'btnSrch',          '조회',                   4, 'Y', 'admin', 'admin'),
    ('MSBS010', 'btnDtlRowAdd',     '코드 행추가',            5, 'Y', 'admin', 'admin'),
    ('MSBS010', 'btnDtlRowDel',     '코드 행삭제',            6, 'Y', 'admin', 'admin'),
    -- SYAU010 사용자권한관리 (views/system/auths/UserAuthManageView.vue)
    ('SYAU010', 'btnSave',          '저장',                   1, 'Y', 'admin', 'admin'),
    ('SYAU010', 'btnSrch',          '조회',                   2, 'Y', 'admin', 'admin'),
    ('SYAU010', 'btnRowAdd',        '사용자 행추가',          3, 'Y', 'admin', 'admin'),
    ('SYAU010', 'btnRowDel',        '사용자 행삭제',          4, 'Y', 'admin', 'admin'),
    ('SYAU010', 'btnPwdInit',       '비밀번호 초기화',        5, 'Y', 'admin', 'admin'),
    ('SYAU010', 'btnAuthCopy',      '권한복사',               6, 'Y', 'admin', 'admin'),
    ('SYAU010', 'btnScrnAdd',       '화면추가',               7, 'Y', 'admin', 'admin'),
    ('SYAU010', 'btnAuthRowDel',    '권한 행삭제',            8, 'Y', 'admin', 'admin'),
    ('SYAU010', 'btnScrnSrchAdd',   '화면검색 팝업 추가',     9, 'Y', 'admin', 'admin'),
    ('SYAU010', 'btnScrnSrchClose', '화면검색 팝업 닫기',    10, 'Y', 'admin', 'admin'),
    -- SYAU020 그룹권한관리 (views/system/auths/AuthGroupManageView.vue)
    ('SYAU020', 'btnSave',          '저장',                   1, 'Y', 'admin', 'admin'),
    ('SYAU020', 'btnSrch',          '조회',                   2, 'Y', 'admin', 'admin'),
    ('SYAU020', 'btnRowAdd',        '그룹 행추가',            3, 'Y', 'admin', 'admin'),
    ('SYAU020', 'btnRowDel',        '그룹 행삭제',            4, 'Y', 'admin', 'admin'),
    ('SYAU020', 'btnAuthCopy',      '권한복사',               5, 'Y', 'admin', 'admin'),
    ('SYAU020', 'btnScrnAdd',       '화면추가',               6, 'Y', 'admin', 'admin'),
    ('SYAU020', 'btnAuthRowDel',    '권한 행삭제',            7, 'Y', 'admin', 'admin'),
    ('SYAU020', 'btnScrnSrchAdd',   '화면검색 팝업 추가',     8, 'Y', 'admin', 'admin'),
    ('SYAU020', 'btnScrnSrchClose', '화면검색 팝업 닫기',     9, 'Y', 'admin', 'admin'),
    -- SYAU030 그룹별사용자관리 (views/system/auths/AuthGroupUserManageView.vue)
    ('SYAU030', 'btnSave',          '저장',                   1, 'Y', 'admin', 'admin'),
    ('SYAU030', 'btnSrch',          '조회',                   2, 'Y', 'admin', 'admin'),
    ('SYAU030', 'btnUsrAdd',        '사용자추가',             3, 'Y', 'admin', 'admin'),
    ('SYAU030', 'btnUsrRowDel',     '사용자 행삭제',          4, 'Y', 'admin', 'admin'),
    ('SYAU030', 'btnUsrSrchAdd',    '사용자검색 팝업 추가',   5, 'Y', 'admin', 'admin'),
    ('SYAU030', 'btnUsrSrchClose',  '사용자검색 팝업 닫기',   6, 'Y', 'admin', 'admin'),
    -- SYLO030 시스템로그관리 (views/system/logs/SystemLogManageView.vue)
    ('SYLO030', 'btnXlsDnld',       '엑셀',                   1, 'Y', 'admin', 'admin'),
    ('SYLO030', 'btnSrch',          '조회',                   2, 'Y', 'admin', 'admin'),
    -- SYLK050 배치관리 (views/system/links/BatchManageView.vue)
    --   팝업 안 버튼(실행내역 조회, 실행, 배치 저장, 스케줄 일시정지 등)은 등록하지 않는다 (화면 버튼만 권한 제어)
    ('SYLK050', 'btnJobReg',        '배치등록',               1, 'Y', 'admin', 'admin'),
    ('SYLK050', 'btnXlsDnld',       '엑셀',                   2, 'Y', 'admin', 'admin'),
    ('SYLK050', 'btnSrch',          '조회',                   3, 'Y', 'admin', 'admin'),
    --   그리드 수동실행 열의 실행 버튼 (행마다 반복되어 id 없이 v-permission="'btnJobExec'" 로 지정)
    ('SYLK050', 'btnJobExec',       '수동실행',               4, 'Y', 'admin', 'admin')
ON CONFLICT (PRGM_ID, CMPN_ID) DO NOTHING;
