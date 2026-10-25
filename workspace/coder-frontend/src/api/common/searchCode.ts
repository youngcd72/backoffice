/*****************************************************************************************
	Program ID   : searchCode
	Program Type : ts
	Program Name : 공통 코드서치 API
	Description  : 공통 코드서치
	               업무구분(type)별로 코드칸 폭, 자동조회 글자 수, 필수 조회조건, 조회 방법을 정의한다.
	               GET /api/common/search-codes/{type}?keyword= 형태의 공통 API로 통합할 예정이다.
	               그 전까지는 업무구분별 fetch 함수로 조회한다 (fetch 가 없는 업무구분은 미연동).
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-05  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { getAllUsersApi } from '../auth'
import { getPrgmListApi } from '../sy/program'
import { getMenuListApi, type Menu } from '../sy/menu'
import { getAuthGrpListApi } from '../sy/authGroup'

/** 조회 결과 1행. CD·NM 은 필수, 나머지는 업무구분별 부가 컬럼 */
export interface SearchCodeRow {
  CD: string
  NM: string
  [column: string]: string
}

/** 업무구분별 추가 조회조건 (원본 reqSearchParam) */
export type SearchCodeParams = Record<string, string | undefined>

export interface SearchCodeColumn {
  key: string
  label: string
  width?: string
}

export interface SearchCodeTypeDef {
  /** 팝업 제목·안내에 쓰는 업무명 */
  title: string
  codeLabel: string
  nameLabel: string
  /** 코드칸 폭(px). 원본 fn_edtCdResize 기준 */
  codeWidth: number
  /** 이 글자 수 이상 입력하면 자동조회 (원본 wordSizeMap). 없으면 자동조회 안 함 */
  wordSize?: number
  /** 영문 대문자 입력 (원본 inputmode="upper") */
  uppercase: boolean
  /** 팝업에 CD·NM 외에 추가로 보여줄 컬럼 */
  extraColumns?: SearchCodeColumn[]
  /** 조회 전에 반드시 있어야 하는 조회조건 */
  requiredParams?: Array<{ key: string; message: string }>
  /** 조회 함수. 없으면 아직 연동되지 않은 업무구분 */
  fetch?: (params: SearchCodeParams) => Promise<SearchCodeRow[]>
}

// ---------------------------------------------------------------------
// 업무구분별 조회 (연동된 업무구분)
// ---------------------------------------------------------------------
/**
 * fetchUsers : 사용자 코드서치 조회
 * @return  코드(사용자ID)·명(사용자명)·이메일·역할 행 목록
 */
async function fetchUsers(): Promise<SearchCodeRow[]> {
  const res = await getAllUsersApi()
  return res.data.map((usr) => ({
    CD: usr.username,
    NM: usr.name,
    EMAIL: usr.email ?? '',
    ROLE: usr.role,
  }))
}

/**
 * fetchPrograms : 프로그램 코드서치 조회
 * @param   params - 추가 조회조건 (USE_YN)
 * @return  코드(프로그램ID)·명(프로그램명)·경로 행 목록
 */
async function fetchPrograms(params: SearchCodeParams): Promise<SearchCodeRow[]> {
  const res = await getPrgmListApi({ useYn: params.USE_YN })
  return res.data.map((prgm) => ({ CD: prgm.prgmId, NM: prgm.prgmNm, PRGM_CRS_CD: prgm.prgmCrsCd }))
}

/**
 * fetchMenus : 메뉴 코드서치 조회 (상위 메뉴를 따라 메뉴 경로 생성)
 * @param   params - 추가 조회조건 (USE_YN)
 * @return  코드(메뉴ID)·명(메뉴명)·메뉴경로·프로그램ID 행 목록
 */
async function fetchMenus(params: SearchCodeParams): Promise<SearchCodeRow[]> {
  const res = await getMenuListApi()
  const menuMap = new Map(res.data.map((menu) => [menu.menuId, menu]))
  const getPath = (menu: Menu): string => {
    const names: string[] = []
    const visited = new Set<string>()
    let cur: Menu | undefined = menu
    while (cur && !visited.has(cur.menuId)) {
      names.unshift(cur.menuNm)
      visited.add(cur.menuId)
      cur = menuMap.get(cur.upprMenuId)
    }
    return names.join(' > ')
  }
  return res.data
    .filter((menu) => !params.USE_YN || menu.useYn === params.USE_YN)
    .map((menu) => ({ CD: menu.menuId, NM: menu.menuNm, MENU_PATH: getPath(menu), PRGM_ID: menu.prgmId }))
}

/**
 * fetchAuthGroups : 권한그룹 코드서치 조회
 * @param   params - 추가 조회조건 (USE_YN)
 * @return  코드(그룹ID)·명(그룹명)·시작일·종료일 행 목록
 */
async function fetchAuthGroups(params: SearchCodeParams): Promise<SearchCodeRow[]> {
  const res = await getAuthGrpListApi({})
  return res.data
    .filter((grp) => !params.USE_YN || grp.useYn === params.USE_YN)
    .map((grp) => ({ CD: grp.usrGrpCd, NM: grp.usrGrpNm, ST_DT: grp.stDt, END_DT: grp.endDt }))
}

// ---------------------------------------------------------------------
// 업무구분 정의
// ---------------------------------------------------------------------
const STOR_REQUIRED = [{ key: 'STOR_CD', message: 'cm.validate.store.required' }]

/**
 * legacyType : 원본 업무구분을 기본 설정으로 생성 (코드·명 라벨은 업무명 기준)
 * @param   title     - 업무명
 * @param   codeWidth - 코드칸 폭(px)
 * @param   wordSize  - 자동조회 글자 수
 * @param   extra     - 덮어쓸 추가 정의
 * @return  업무구분 정의
 */
function legacyType(title: string, codeWidth: number, wordSize?: number, extra: Partial<SearchCodeTypeDef> = {}): SearchCodeTypeDef {
  return { title, codeLabel: `${title}코드`, nameLabel: `${title}명`, codeWidth, wordSize, uppercase: true, ...extra }
}

export const SEARCH_CODE_TYPES: Record<string, SearchCodeTypeDef> = {
  // --- 이 시스템에서 연동된 업무구분 ---
  USR: {
    title: '사용자',
    codeLabel: '사용자ID',
    nameLabel: '사용자명',
    codeWidth: 70,
    wordSize: 5,
    uppercase: false,
    extraColumns: [{ key: 'EMAIL', label: '이메일' }],
    fetch: fetchUsers,
  },
  PRGM: {
    title: '프로그램',
    codeLabel: '프로그램ID',
    nameLabel: '프로그램명',
    codeWidth: 90,
    wordSize: 11,
    uppercase: true,
    extraColumns: [{ key: 'PRGM_CRS_CD', label: '경로', width: '80px' }],
    fetch: fetchPrograms,
  },
  MENU: {
    title: '메뉴',
    codeLabel: '메뉴ID',
    nameLabel: '메뉴명',
    codeWidth: 62,
    wordSize: 7,
    uppercase: true,
    extraColumns: [{ key: 'MENU_PATH', label: '메뉴 경로' }],
    fetch: fetchMenus,
  },
  AUTH_GRP: {
    title: '권한그룹',
    codeLabel: '그룹ID',
    nameLabel: '그룹명',
    codeWidth: 110,
    uppercase: true,
    fetch: fetchAuthGroups,
  },

  // --- 원본 업무구분 (조회 미연동: 백엔드 공통 API 구현 시 fetch 추가) ---
  COMP: legacyType('회사', 27, 2),
  STOR: legacyType('점', 27),
  DEPT: legacyType('부서', 43),
  CORN: legacyType('코너', 55, 3, { uppercase: false, requiredParams: STOR_REQUIRED }),
  CORN2: legacyType('코너', 27, 3),
  CUST: legacyType('거래처', 60, 5, { uppercase: false }),
  BRAN: legacyType('브랜드', 55, 3, { uppercase: false }),
  BRAN_REF: legacyType('브랜드레퍼', 112, 4),
  BRAN_CTG_L: legacyType('브랜드상품대분류', 55, 6),
  CLR: legacyType('브랜드별 색상', 77, undefined, { requiredParams: [{ key: 'BRAN_CD', message: 'cm.validate.param.required' }] }),
  GOOS: legacyType('상품', 79, 13),
  GOOS_BRCD: legacyType('상품바코드', 115),
  COMM: legacyType('공통', 55, 5),
  CTG: legacyType('상품분류', 27, 2),
  CTG_L: legacyType('상품대분류', 17, 2),
  CTG_M: legacyType('상품중분류', 27, 2),
  ZIP: legacyType('우편번호', 43),
  EQUI: legacyType('장비', 69, undefined, { requiredParams: [{ key: 'STOR_CD', message: 'cm.validate.param.required' }] }),
  NOTI: legacyType('공지사항', 43),
  EMP: legacyType('사원', 65, 8, { uppercase: false }),
  COOP: legacyType('협력사원', 65, 8, { uppercase: false }),
  LAND: legacyType('코너위치', 29),
}

/**
 * getSearchCodeType : 업무구분 정의 조회 (대소문자 구분 없음)
 * @param   type - 업무구분
 * @return  업무구분 정의 (없으면 undefined)
 */
export function getSearchCodeType(type: string): SearchCodeTypeDef | undefined {
  return SEARCH_CODE_TYPES[type.toUpperCase()]
}

/**
 * searchCodeApi : 코드서치 조회: 코드 또는 명칭에 keyword 가 포함된 행을 반환, 코드가 정확히 일치하는 행을 맨 앞에 둔다 (USE_YN 기본값 Y)
 * @param   type    - 업무구분
 * @param   keyword - 검색어
 * @param   params  - 추가 조회조건
 * @return  조회 결과 행 목록 (미연동 업무구분은 빈 배열)
 * @example searchCodeApi('USR', 'kim')
 */
export async function searchCodeApi(type: string, keyword: string, params: SearchCodeParams = {}): Promise<SearchCodeRow[]> {
  const def = getSearchCodeType(type)
  if (!def?.fetch) return []
  const rows = await def.fetch({ USE_YN: 'Y', ...params })
  const kw = keyword.trim().toUpperCase()
  return rows
    .filter((row) => !kw || row.CD.toUpperCase().includes(kw) || row.NM.toUpperCase().includes(kw))
    .sort((a, b) => Number(b.CD.toUpperCase() === kw) - Number(a.CD.toUpperCase() === kw))
}
