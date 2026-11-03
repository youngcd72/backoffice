/*****************************************************************************************
	Program ID   : link
	Program Type : ts
	Program Name : API관리 API
	Description  : SYLK010 API관리(연계기관·클라이언트 키·인터페이스·호출이력) API
	               coder-backend 관리 API (/api/v1/link-orgs, /api/v1/interfaces, /api/v1/interface-calls)
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-11-03  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import apiClient, { requestApi, type ApiResponse } from '../client'
import type { CodeOption } from '@/utils/code'

/** 연계기관 (TB_LNK_ORG_M) */
export interface LinkOrg {
  lnkOrgId: string
  lnkOrgNm: string
  /** 채널구분코드 (공통코드 SY/CHNL_CLS_CD: B B2B, C B2C, I 내부 시스템) */
  chnlClsCd: string
  /** 사용시작일자 / 사용종료일자 (YYYYMMDD) */
  useStDt: string | null
  useEndDt: string | null
  useYn: string
  rmk: string | null
  /** 사용 중이면서 만료되지 않은 키 수 (목록 조회에서만 채워짐) */
  useKeyCnt?: number
}

/** 연계기관 저장 요청 */
export type LinkOrgSave = Omit<LinkOrg, 'useKeyCnt'>

/** 클라이언트 키 (TB_LNK_KEY_M, 암호화 비밀키는 응답에 넣지 않음) */
export interface LinkKey {
  /** 키ID (OAuth2 client_id) */
  keyId: string
  lnkOrgId: string
  /** 키상태코드 (공통코드 SY/KEY_STAT_CD: 01 사용, 02 사용중지, 03 폐기) */
  keyStatCd: string
  /** 발급일시 / 만료일시 (ISO, 만료일시가 없으면 만료 없음) */
  issuDttm: string
  expiDttm: string | null
  /** 허용IP주소 (IP/CIDR 쉼표 구분, 없으면 제한 없음) */
  allwIpAddr: string | null
  miCallLmtCnt: number | null
  dayCallLmtCnt: number | null
  simuRqmtLmtCnt: number | null
  rmk: string | null
}

/** 클라이언트 키 수정 요청 */
export interface LinkKeySave {
  keyStatCd: string
  /** 만료일자 (YYYYMMDD, 없으면 만료 없음) */
  expiDt: string | null
  allwIpAddr: string | null
  miCallLmtCnt: number | null
  dayCallLmtCnt: number | null
  simuRqmtLmtCnt: number | null
  rmk: string | null
}

/** 클라이언트 키 발급 결과 (비밀키 원문은 이 응답에만 있음) */
export interface LinkKeyIssueResult {
  keyId: string
  clientSecret: string
  expiDttm: string | null
}

/** 클라이언트 키 이력 (TB_LNK_KEY_H) */
export interface LinkKeyHist {
  keyHistSn: number
  keyId: string
  keyStatCd: string
  chgCont: string | null
  firsRegrId: string
  firsRegDt: string
}

/** 인터페이스 (TB_INFC_M) */
export interface LinkInterface {
  /** 인터페이스ID (IF + 업무약어 2자리 + 순번 4자리, 예: IFSY0001) */
  infcId: string
  infcNm: string
  httpMethNm: string
  url: string
  bodyLogYn: string
  /** 마스킹필드내용 (쉼표 구분) */
  maskFldCont: string | null
  useYn: string
  rmk: string | null
  /** 이 인터페이스를 허용한 연계기관 수 (목록 조회에서만 채워짐) */
  allwOrgCnt?: number
}

/** 인터페이스 저장 요청 */
export type LinkInterfaceSave = Omit<LinkInterface, 'allwOrgCnt'>

/** 기관별 허용 인터페이스 (전체 인터페이스 + 허용 여부) */
export interface LinkOrgInterface {
  infcId: string
  infcNm: string
  httpMethNm: string
  url: string
  /** 인터페이스 자체의 사용여부 */
  infcUseYn: string
  /** 이 기관에 허용 여부 (TB_LNK_ORG_INFC_R.USE_YN) */
  allwYn: string
}

/** 인터페이스 호출내역 (TB_INFC_CALL_L) */
export interface InterfaceCall {
  callSn: number
  trcId: string
  keyId: string | null
  lnkOrgId: string | null
  lnkOrgNm: string | null
  infcId: string
  infcNm: string | null
  httpMethNm: string
  url: string
  httpStatCd: string
  clntIpAddr: string | null
  /** 요청일시 / 응답일시 (ISO, 밀리초 포함) */
  rqmtDttm: string
  rspnDttm: string
  elapsedMs: number
  errCd: string | null
  /** 요청·응답 본문 (상세 조회에서만 채워짐, 마스킹된 값) */
  rqmtCont?: string | null
  rspnCont?: string | null
}

/** 호출이력 조회 결과 */
export interface InterfaceCallPage {
  list: InterfaceCall[]
  totalCount: number
  /** 최대 건수를 넘어 잘렸는지 여부 */
  truncated: boolean
}

/** 호출이력 조회 조건 */
export interface InterfaceCallSearch {
  /** 요청일자 범위 (YYYYMMDD) */
  stDt: string
  endDt: string
  lnkOrgId: string
  infcId: string
  /** 결과 (S 성공, F 실패, 빈 값 전체) */
  rsltCd: string
  trcId: string
  size?: number
}

/** 키 발급 유효기간 옵션 (일, 0 = 만료 없음) */
export const KEY_EXPI_OPTIONS: CodeOption[] = [
  { label: '90일', value: '90' },
  { label: '180일', value: '180' },
  { label: '1년', value: '365' },
  { label: '2년', value: '730' },
  { label: '만료 없음', value: '0' },
]

/** HTTP 메서드 옵션 */
export const HTTP_METH_OPTIONS: CodeOption[] = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'].map((value) => ({ label: value, value }))

/** 호출 결과 옵션 */
export const CALL_RSLT_OPTIONS: CodeOption[] = [
  { label: '성공', value: 'S' },
  { label: '실패', value: 'F' },
]

/** 호출이력 최대 조회 기간 (일) */
export const CALL_MAX_DAYS = 31

/** 호출이력 최대 조회 건수 */
export const CALL_MAX_ROWS = 500

// ---------------------------------------------------------------------
// 연계기관
// ---------------------------------------------------------------------
/**
 * getLinkOrgListApi : 연계기관 목록 조회 (GET /api/v1/link-orgs)
 * @param   params - 검색어(기관ID·기관명), 채널구분코드, 사용여부
 * @return  연계기관 목록 (사용 가능 키 수 포함)
 */
export async function getLinkOrgListApi(params: { keyword: string; chnlClsCd: string; useYn: string }): Promise<ApiResponse<LinkOrg[]>> {
  const response = await apiClient.get<ApiResponse<LinkOrg[]>>('/api/v1/link-orgs', { params })
  return response.data
}

/**
 * getLinkOrgApi : 연계기관 단건 조회 (GET /api/v1/link-orgs/{lnkOrgId})
 * @param   lnkOrgId - 연계기관ID
 * @return  연계기관 (없으면 success=false)
 */
export async function getLinkOrgApi(lnkOrgId: string): Promise<ApiResponse<LinkOrg | null>> {
  return requestApi(apiClient.get<ApiResponse<LinkOrg>>(`/api/v1/link-orgs/${encodeURIComponent(lnkOrgId)}`))
}

/**
 * saveLinkOrgApi : 연계기관 저장 (POST /api/v1/link-orgs)
 * @param   org   - 연계기관
 * @param   isNew - 신규 등록 여부
 * @return  처리 결과 (신규인데 기관ID가 있으면 success=false)
 */
export async function saveLinkOrgApi(org: LinkOrgSave, isNew: boolean): Promise<ApiResponse<null>> {
  return requestApi(apiClient.post<ApiResponse<null>>('/api/v1/link-orgs', { ...org, isNew }))
}

// ---------------------------------------------------------------------
// 클라이언트 키
// ---------------------------------------------------------------------
/**
 * getLinkKeyListApi : 연계기관의 클라이언트 키 목록 조회 (GET /api/v1/link-orgs/{lnkOrgId}/keys)
 * @param   lnkOrgId - 연계기관ID
 * @return  클라이언트 키 목록 (상태 → 발급일시 내림차순)
 */
export async function getLinkKeyListApi(lnkOrgId: string): Promise<ApiResponse<LinkKey[] | null>> {
  return requestApi(apiClient.get<ApiResponse<LinkKey[]>>(`/api/v1/link-orgs/${encodeURIComponent(lnkOrgId)}/keys`))
}

/**
 * saveLinkKeyApi : 클라이언트 키 발급 (POST /api/v1/link-orgs/{lnkOrgId}/keys)
 * @param   lnkOrgId   - 연계기관ID
 * @param   expireDays - 유효기간 (일, 0이면 만료 없음)
 * @return  키ID·비밀키 원문·만료일시 (비밀키 원문은 이 응답에만 있음, KMS 미설정이면 success=false)
 */
export async function saveLinkKeyApi(lnkOrgId: string, expireDays: number): Promise<ApiResponse<LinkKeyIssueResult | null>> {
  return requestApi(
    apiClient.post<ApiResponse<LinkKeyIssueResult>>(`/api/v1/link-orgs/${encodeURIComponent(lnkOrgId)}/keys`, { expireDays }),
  )
}

/**
 * saveLinkKeyDtlApi : 클라이언트 키 수정 (PATCH /api/v1/link-orgs/{lnkOrgId}/keys/{keyId})
 * @param   lnkOrgId - 연계기관ID
 * @param   keyId    - 키ID
 * @param   save     - 상태·만료일·허용IP·호출 한도·비고
 * @return  처리 결과 (폐기된 키면 success=false)
 */
export async function saveLinkKeyDtlApi(lnkOrgId: string, keyId: string, save: LinkKeySave): Promise<ApiResponse<null>> {
  return requestApi(
    apiClient.patch<ApiResponse<null>>(
      `/api/v1/link-orgs/${encodeURIComponent(lnkOrgId)}/keys/${encodeURIComponent(keyId)}`,
      save,
    ),
  )
}

/**
 * getLinkKeyHistListApi : 클라이언트 키 이력 조회 (GET /api/v1/link-orgs/{lnkOrgId}/keys/{keyId}/histories)
 * @param   lnkOrgId - 연계기관ID
 * @param   keyId    - 키ID
 * @return  키 이력 목록 (최근순)
 */
export async function getLinkKeyHistListApi(lnkOrgId: string, keyId: string): Promise<ApiResponse<LinkKeyHist[] | null>> {
  return requestApi(
    apiClient.get<ApiResponse<LinkKeyHist[]>>(
      `/api/v1/link-orgs/${encodeURIComponent(lnkOrgId)}/keys/${encodeURIComponent(keyId)}/histories`,
    ),
  )
}

// ---------------------------------------------------------------------
// 기관별 허용 인터페이스
// ---------------------------------------------------------------------
/**
 * getLinkOrgInterfaceListApi : 기관별 허용 인터페이스 조회 (GET /api/v1/link-orgs/{lnkOrgId}/interfaces)
 * @param   lnkOrgId - 연계기관ID
 * @return  전체 인터페이스 + 이 기관 허용 여부
 */
export async function getLinkOrgInterfaceListApi(lnkOrgId: string): Promise<ApiResponse<LinkOrgInterface[] | null>> {
  return requestApi(apiClient.get<ApiResponse<LinkOrgInterface[]>>(`/api/v1/link-orgs/${encodeURIComponent(lnkOrgId)}/interfaces`))
}

/**
 * saveLinkOrgInterfaceListApi : 기관별 허용 인터페이스 저장 (POST /api/v1/link-orgs/{lnkOrgId}/interfaces, 전체 교체)
 * @param   lnkOrgId   - 연계기관ID
 * @param   infcIdList - 허용할 인터페이스ID 목록 (없는 기존 허용은 사용 안 함으로 변경)
 * @return  처리 결과
 */
export async function saveLinkOrgInterfaceListApi(lnkOrgId: string, infcIdList: string[]): Promise<ApiResponse<null>> {
  return requestApi(
    apiClient.post<ApiResponse<null>>(`/api/v1/link-orgs/${encodeURIComponent(lnkOrgId)}/interfaces`, { infcIdList }),
  )
}

// ---------------------------------------------------------------------
// 인터페이스
// ---------------------------------------------------------------------
/**
 * getInterfaceListApi : 인터페이스 목록 조회 (GET /api/v1/interfaces)
 * @param   params - 검색어(인터페이스ID·명·URL), 사용여부
 * @return  인터페이스 목록 (허용 기관 수 포함)
 */
export async function getInterfaceListApi(params: { keyword: string; useYn: string }): Promise<ApiResponse<LinkInterface[]>> {
  const response = await apiClient.get<ApiResponse<LinkInterface[]>>('/api/v1/interfaces', { params })
  return response.data
}

/**
 * saveInterfaceApi : 인터페이스 저장 (POST /api/v1/interfaces)
 * @param   infc  - 인터페이스
 * @param   isNew - 신규 등록 여부
 * @return  처리 결과 (신규인데 인터페이스ID가 있으면 success=false)
 */
export async function saveInterfaceApi(infc: LinkInterfaceSave, isNew: boolean): Promise<ApiResponse<null>> {
  return requestApi(apiClient.post<ApiResponse<null>>('/api/v1/interfaces', { ...infc, isNew }))
}

// ---------------------------------------------------------------------
// 호출이력
// ---------------------------------------------------------------------
/**
 * getInterfaceCallListApi : 호출이력 조회 (GET /api/v1/interface-calls)
 * @param   params - 요청일자 범위·연계기관·인터페이스·결과·추적ID
 * @return  호출이력 (요청일시 내림차순, 최대 size 건, 기간 오류면 success=false)
 */
export async function getInterfaceCallListApi(params: InterfaceCallSearch): Promise<ApiResponse<InterfaceCallPage | null>> {
  return requestApi(apiClient.get<ApiResponse<InterfaceCallPage>>('/api/v1/interface-calls', { params }))
}

/**
 * getInterfaceCallApi : 호출이력 상세 조회 (GET /api/v1/interface-calls/{callSn}?rqmtDttm=)
 * @param   callSn   - 호출일련번호
 * @param   rqmtDttm - 요청일시 (파티션 키, 밀리초까지)
 * @return  호출이력 (요청·응답 본문 포함)
 */
export async function getInterfaceCallApi(callSn: number, rqmtDttm: string): Promise<ApiResponse<InterfaceCall | null>> {
  return requestApi(apiClient.get<ApiResponse<InterfaceCall>>(`/api/v1/interface-calls/${callSn}`, { params: { rqmtDttm } }))
}
