-- =====================================================================
-- 공통코드 등록 (TB_COMM_CD_M, TB_COMM_CD_D)
-- 재실행해도 기존 행은 건드리지 않는다 (ON CONFLICT DO NOTHING).
-- =====================================================================

-- ---------------------------------------------------------------------
-- SY / PRGM_CRS_CD : 프로그램경로 (TB_PRGM_M.PRGM_CRS_CD, 왼쪽 메뉴 상위 분류)
-- ---------------------------------------------------------------------
INSERT INTO TB_COMM_CD_M (
    WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_NM, USE_YN, USR_MNGE_YN, ALIN_SEQ, RMK,
    FIRS_REGR_ID, FINA_REGR_ID
) VALUES (
    'SY', 'PRGM_CRS_CD', '프로그램경로', 'Y', 'N', NULL, 'TB_PRGM_M.PRGM_CRS_CD',
    'admin', 'admin'
) ON CONFLICT (WRK_MDUL_CD, COMM_CTG_CD) DO NOTHING;

INSERT INTO TB_COMM_CD_D (
    WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_DTL_CD, COMM_CTG_DTL_NM, USE_YN, ALIN_SEQ,
    FIRS_REGR_ID, FINA_REGR_ID
) VALUES
    ('SY', 'PRGM_CRS_CD', 'SY', '시스템관리', 'Y', 1, 'admin', 'admin'),
    ('SY', 'PRGM_CRS_CD', 'MS', '기준정보',   'Y', 2, 'admin', 'admin')
ON CONFLICT (WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_DTL_CD) DO NOTHING;

-- ---------------------------------------------------------------------
-- SY / MENU_TYPE_CD : 메뉴타입 (TB_MENU_M.MENU_TYPE_CD)
-- ---------------------------------------------------------------------
INSERT INTO TB_COMM_CD_M (
    WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_NM, USE_YN, USR_MNGE_YN, ALIN_SEQ, RMK,
    FIRS_REGR_ID, FINA_REGR_ID
) VALUES (
    'SY', 'MENU_TYPE_CD', '메뉴타입', 'Y', 'N', NULL, 'TB_MENU_M.MENU_TYPE_CD',
    'admin', 'admin'
) ON CONFLICT (WRK_MDUL_CD, COMM_CTG_CD) DO NOTHING;

INSERT INTO TB_COMM_CD_D (
    WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_DTL_CD, COMM_CTG_DTL_NM, USE_YN, ALIN_SEQ,
    FIRS_REGR_ID, FINA_REGR_ID
) VALUES
    ('SY', 'MENU_TYPE_CD', 'MENU', '메뉴', 'Y', 1, 'admin', 'admin'),
    ('SY', 'MENU_TYPE_CD', 'LINK', '링크', 'Y', 2, 'admin', 'admin')
ON CONFLICT (WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_DTL_CD) DO NOTHING;

-- ---------------------------------------------------------------------
-- SY / WRK_MDUL_CD : 업무모듈 (업무약어 사전 schema/dictionary/business-abbreviations.md)
-- ---------------------------------------------------------------------
INSERT INTO TB_COMM_CD_M (
    WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_NM, USE_YN, USR_MNGE_YN, ALIN_SEQ, RMK,
    FIRS_REGR_ID, FINA_REGR_ID
) VALUES (
    'SY', 'WRK_MDUL_CD', '업무모듈', 'Y', 'N', NULL, '업무약어 (2자리)',
    'admin', 'admin'
) ON CONFLICT (WRK_MDUL_CD, COMM_CTG_CD) DO NOTHING;

INSERT INTO TB_COMM_CD_D (
    WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_DTL_CD, COMM_CTG_DTL_NM, USE_YN, ALIN_SEQ,
    FIRS_REGR_ID, FINA_REGR_ID
) VALUES
    ('SY', 'WRK_MDUL_CD', 'SY', '시스템',   'Y', 1, 'admin', 'admin'),
    ('SY', 'WRK_MDUL_CD', 'MS', '기준정보', 'Y', 2, 'admin', 'admin')
ON CONFLICT (WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_DTL_CD) DO NOTHING;

-- ---------------------------------------------------------------------
-- SY / USR_CLS_CD : 사용자구분 (TB_USR_M.USR_CLS_CD)
-- ---------------------------------------------------------------------
INSERT INTO TB_COMM_CD_M (
    WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_NM, USE_YN, USR_MNGE_YN, ALIN_SEQ, RMK,
    FIRS_REGR_ID, FINA_REGR_ID
) VALUES (
    'SY', 'USR_CLS_CD', '사용자구분', 'Y', 'N', NULL, 'TB_USR_M.USR_CLS_CD',
    'admin', 'admin'
) ON CONFLICT (WRK_MDUL_CD, COMM_CTG_CD) DO NOTHING;

INSERT INTO TB_COMM_CD_D (
    WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_DTL_CD, COMM_CTG_DTL_NM, USE_YN, ALIN_SEQ,
    FIRS_REGR_ID, FINA_REGR_ID
) VALUES
    ('SY', 'USR_CLS_CD', 'E', '직원',     'Y', 1, 'admin', 'admin'),
    ('SY', 'USR_CLS_CD', 'P', '협력업체', 'Y', 2, 'admin', 'admin')
ON CONFLICT (WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_DTL_CD) DO NOTHING;

-- ---------------------------------------------------------------------
-- SY / USR_STAT_CD : 사용자상태 (TB_USR_M.USR_STAT_CD, 재직여부)
-- ---------------------------------------------------------------------
INSERT INTO TB_COMM_CD_M (
    WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_NM, USE_YN, USR_MNGE_YN, ALIN_SEQ, RMK,
    FIRS_REGR_ID, FINA_REGR_ID
) VALUES (
    'SY', 'USR_STAT_CD', '사용자상태', 'Y', 'N', NULL, 'TB_USR_M.USR_STAT_CD (재직여부)',
    'admin', 'admin'
) ON CONFLICT (WRK_MDUL_CD, COMM_CTG_CD) DO NOTHING;

INSERT INTO TB_COMM_CD_D (
    WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_DTL_CD, COMM_CTG_DTL_NM, USE_YN, ALIN_SEQ,
    FIRS_REGR_ID, FINA_REGR_ID
) VALUES
    ('SY', 'USR_STAT_CD', 'T', '재직', 'Y', 1, 'admin', 'admin'),
    ('SY', 'USR_STAT_CD', 'L', '휴직', 'Y', 2, 'admin', 'admin'),
    ('SY', 'USR_STAT_CD', 'R', '퇴직', 'Y', 3, 'admin', 'admin')
ON CONFLICT (WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_DTL_CD) DO NOTHING;

-- ---------------------------------------------------------------------
-- SY / ALRM_TYPE_CD : 알람유형 (TB_ALRM_L.ALRM_TYPE_CD, 상단 알림 센터)
-- ---------------------------------------------------------------------
INSERT INTO TB_COMM_CD_M (
    WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_NM, USE_YN, USR_MNGE_YN, ALIN_SEQ, RMK,
    FIRS_REGR_ID, FINA_REGR_ID
) VALUES (
    'SY', 'ALRM_TYPE_CD', '알람유형', 'Y', 'N', NULL, 'TB_ALRM_L.ALRM_TYPE_CD',
    'admin', 'admin'
) ON CONFLICT (WRK_MDUL_CD, COMM_CTG_CD) DO NOTHING;

INSERT INTO TB_COMM_CD_D (
    WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_DTL_CD, COMM_CTG_DTL_NM, USE_YN, ALIN_SEQ,
    FIRS_REGR_ID, FINA_REGR_ID
) VALUES
    ('SY', 'ALRM_TYPE_CD', 'NTC',  '공지',   'Y', 1, 'admin', 'admin'),
    ('SY', 'ALRM_TYPE_CD', 'DSPT', '배치',   'Y', 2, 'admin', 'admin'),
    ('SY', 'ALRM_TYPE_CD', 'SYS',  '시스템', 'Y', 3, 'admin', 'admin')
ON CONFLICT (WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_DTL_CD) DO NOTHING;

-- ---------------------------------------------------------------------
-- SY / SYSM_CLS_CD : 시스템유형 (에러 로그 sysmClsCd, application.yml coder.log.sysm-cls-cd)
--   2026-10-20 추가. 기존 문서의 SY003 시스템유형에 해당
-- ---------------------------------------------------------------------
INSERT INTO TB_COMM_CD_M (
    WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_NM, USE_YN, USR_MNGE_YN, ALIN_SEQ, RMK,
    FIRS_REGR_ID, FINA_REGR_ID
) VALUES (
    'SY', 'SYSM_CLS_CD', '시스템유형', 'Y', 'N', NULL, '에러 로그 시스템유형 (SY003)',
    'admin', 'admin'
) ON CONFLICT (WRK_MDUL_CD, COMM_CTG_CD) DO NOTHING;

INSERT INTO TB_COMM_CD_D (
    WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_DTL_CD, COMM_CTG_DTL_NM, USE_YN, ALIN_SEQ,
    FIRS_REGR_ID, FINA_REGR_ID
) VALUES
    ('SY', 'SYSM_CLS_CD', '01', 'ERP',    'Y', 1, 'admin', 'admin')
ON CONFLICT (WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_DTL_CD) DO NOTHING;

-- ---------------------------------------------------------------------
-- SY / SYSM_CLS_CD : 시스템유형 - coder-api (application.yml coder.log.sysm-cls-cd)
--   2026-10-27 추가
-- ---------------------------------------------------------------------
INSERT INTO TB_COMM_CD_D (
    WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_DTL_CD, COMM_CTG_DTL_NM, USE_YN, ALIN_SEQ,
    FIRS_REGR_ID, FINA_REGR_ID
) VALUES
    ('SY', 'SYSM_CLS_CD', '02', '대외계', 'Y', 2, 'admin', 'admin')
ON CONFLICT (WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_DTL_CD) DO NOTHING;

-- ---------------------------------------------------------------------
-- SY / CHNL_CLS_CD : 채널구분 (TB_LNK_ORG_M.CHNL_CLS_CD, coder-api 연계기관)
--   2026-10-27 추가
-- ---------------------------------------------------------------------
INSERT INTO TB_COMM_CD_M (
    WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_NM, USE_YN, USR_MNGE_YN, ALIN_SEQ, RMK,
    FIRS_REGR_ID, FINA_REGR_ID
) VALUES (
    'SY', 'CHNL_CLS_CD', '채널구분', 'Y', 'N', NULL, 'TB_LNK_ORG_M.CHNL_CLS_CD',
    'admin', 'admin'
) ON CONFLICT (WRK_MDUL_CD, COMM_CTG_CD) DO NOTHING;

INSERT INTO TB_COMM_CD_D (
    WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_DTL_CD, COMM_CTG_DTL_NM, USE_YN, ALIN_SEQ,
    FIRS_REGR_ID, FINA_REGR_ID
) VALUES
    ('SY', 'CHNL_CLS_CD', 'B', 'B2B',         'Y', 1, 'admin', 'admin'),
    ('SY', 'CHNL_CLS_CD', 'C', 'B2C',         'Y', 2, 'admin', 'admin'),
    ('SY', 'CHNL_CLS_CD', 'I', '내부 시스템', 'Y', 3, 'admin', 'admin')
ON CONFLICT (WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_DTL_CD) DO NOTHING;

-- ---------------------------------------------------------------------
-- SY / KEY_STAT_CD : 키상태 (TB_LNK_KEY_M.KEY_STAT_CD, coder-api 클라이언트 키)
--   2026-10-27 추가
-- ---------------------------------------------------------------------
INSERT INTO TB_COMM_CD_M (
    WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_NM, USE_YN, USR_MNGE_YN, ALIN_SEQ, RMK,
    FIRS_REGR_ID, FINA_REGR_ID
) VALUES (
    'SY', 'KEY_STAT_CD', '키상태', 'Y', 'N', NULL, 'TB_LNK_KEY_M.KEY_STAT_CD',
    'admin', 'admin'
) ON CONFLICT (WRK_MDUL_CD, COMM_CTG_CD) DO NOTHING;

INSERT INTO TB_COMM_CD_D (
    WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_DTL_CD, COMM_CTG_DTL_NM, USE_YN, ALIN_SEQ,
    FIRS_REGR_ID, FINA_REGR_ID
) VALUES
    ('SY', 'KEY_STAT_CD', '01', '사용',     'Y', 1, 'admin', 'admin'),
    ('SY', 'KEY_STAT_CD', '02', '사용중지', 'Y', 2, 'admin', 'admin'),
    ('SY', 'KEY_STAT_CD', '03', '폐기',     'Y', 3, 'admin', 'admin')
ON CONFLICT (WRK_MDUL_CD, COMM_CTG_CD, COMM_CTG_DTL_CD) DO NOTHING;
