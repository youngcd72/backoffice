/*****************************************************************************************
	Program ID   : MSBS010
	Program Type : ts
	Program Name : 공통코드관리 API
	Description  : MSBS010 공통코드관리 API
	               - TB_COMM_CD_M(공통코드기본, 분류), TB_COMM_CD_D(공통코드상세, 코드)
	               - 코드(상세)는 삭제하지 않고 사용여부로 미사용 처리한다.
	               - 분류 목록 조회: GET /api/v1/common-codes (TB_COMM_CD_M)
	               - 코드 목록 조회: GET /api/v1/common-codes/{wrkMdulCd}/{commCtgCd}/details (TB_COMM_CD_D, 사용여부 무관 전체)
	               - 저장: POST /api/v1/common-codes (분류 I/U/D, 코드 I/U 를 한 트랜잭션으로)
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-12  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import apiClient, { requestApi, type ApiResponse } from '../client'

/** 공통속성 개수 (COMM_PROP_NM_1~10, COMM_PROP_CONT_1~10) */
export const COMM_PROP_CNT = 10

/** 상세코드 최대 길이 (TB_COMM_CD_D.COMM_CTG_DTL_CD VARCHAR(30)) */
export const MAX_COMM_CD_DNUM = 30

/** 공통코드 분류 (TB_COMM_CD_M) */
export interface CommCd {
  wrkMdulCd: string
  commCtgCd: string
  commCtgNm: string
  commPropNm1: string
  commPropNm2: string
  commPropNm3: string
  commPropNm4: string
  commPropNm5: string
  commPropNm6: string
  commPropNm7: string
  commPropNm8: string
  commPropNm9: string
  commPropNm10: string
  useYn: string
  /** 사용자관리여부 (Y: 화면에서 코드를 관리) */
  usrMngeYn: string
  alinSeq: number | null
  rmk: string
}

/** 공통코드 상세 (TB_COMM_CD_D) */
export interface CommCdDtl {
  wrkMdulCd: string
  commCtgCd: string
  commCtgDtlCd: string
  commCtgDtlNm: string
  commPropCont1: string
  commPropCont2: string
  commPropCont3: string
  commPropCont4: string
  commPropCont5: string
  commPropCont6: string
  commPropCont7: string
  commPropCont8: string
  commPropCont9: string
  commPropCont10: string
  rmk: string
  useYn: string
  alinSeq: number | null
}

/** 분류 1건의 코드 변경분 (코드는 삭제하지 않음) */
export interface CommCdDtlSave {
  wrkMdulCd: string
  commCtgCd: string
  insertList: CommCdDtl[]
  updateList: CommCdDtl[]
}

export interface CommCdSaveRequest {
  insertList: CommCd[]
  updateList: CommCd[]
  /** 코드가 등록되지 않은 분류만 삭제할 수 있다 */
  deleteList: CommCd[]
  dtlSaveList: CommCdDtlSave[]
}

export interface CommCdSrchParams {
  wrkMdulCd?: string
  commCtgCd?: string
  commCtgNm?: string
}

export type CommPropNmKey = `commPropNm${1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10}`
export type CommPropContKey = `commPropCont${1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10}`

/** 공통속성명 필드 키 목록 (commPropNm1 ~ commPropNm10) */
export const COMM_PROP_NM_KEYS = Array.from({ length: COMM_PROP_CNT }, (_, idx) => `commPropNm${idx + 1}` as CommPropNmKey)

/** 공통속성내용 필드 키 목록 (commPropCont1 ~ commPropCont10) */
export const COMM_PROP_CONT_KEYS = Array.from({ length: COMM_PROP_CNT }, (_, idx) => `commPropCont${idx + 1}` as CommPropContKey)

/**
 * createCommCd : 빈 공통코드 분류 생성
 * @param   wrkMdulCd - 업무모듈코드
 * @return  기본값을 채운 공통코드 분류
 */
export function createCommCd(wrkMdulCd: string): CommCd {
  return {
    wrkMdulCd,
    commCtgCd: '',
    commCtgNm: '',
    ...(Object.fromEntries(COMM_PROP_NM_KEYS.map((key) => [key, ''])) as Record<CommPropNmKey, string>),
    useYn: 'Y',
    usrMngeYn: 'Y',
    alinSeq: null,
    rmk: '',
  }
}

/**
 * createCommCdDtl : 빈 공통코드 상세 생성
 * @param   wrkMdulCd - 업무모듈코드
 * @param   commCtgCd - 공통분류코드
 * @return  기본값을 채운 공통코드 상세
 */
export function createCommCdDtl(wrkMdulCd: string, commCtgCd: string): CommCdDtl {
  return {
    wrkMdulCd,
    commCtgCd,
    commCtgDtlCd: '',
    commCtgDtlNm: '',
    ...(Object.fromEntries(COMM_PROP_CONT_KEYS.map((key) => [key, ''])) as Record<CommPropContKey, string>),
    rmk: '',
    useYn: 'Y',
    alinSeq: null,
  }
}

/**
 * getCommCdListApi : 공통코드 분류 목록 조회, 업무모듈·정렬순번 순 (GET /api/v1/common-codes, TB_COMM_CD_M)
 * @param   params - 조회조건 (업무모듈, 분류코드, 분류명)
 * @return  공통코드 분류 목록을 담은 API 응답
 */
export async function getCommCdListApi(params: CommCdSrchParams): Promise<ApiResponse<CommCd[]>> {
  const response = await apiClient.get<ApiResponse<CommCd[]>>('/api/v1/common-codes', { params })
  return response.data
}

/**
 * getCommCdDtlListApi : 공통코드 분류의 코드 목록 조회, 사용여부 무관 전체·정렬순번 순 (GET /api/v1/common-codes/{wrkMdulCd}/{commCtgCd}/details)
 * @param   wrkMdulCd - 업무모듈코드
 * @param   commCtgCd - 공통분류코드
 * @return  코드 목록을 담은 API 응답
 */
export async function getCommCdDtlListApi(wrkMdulCd: string, commCtgCd: string): Promise<ApiResponse<CommCdDtl[]>> {
  const response = await apiClient.get<ApiResponse<CommCdDtl[]>>(
    `/api/v1/common-codes/${encodeURIComponent(wrkMdulCd)}/${encodeURIComponent(commCtgCd)}/details`,
  )
  return response.data
}

/**
 * saveCommCdApi : 공통코드 분류·코드 일괄 저장 (POST /api/v1/common-codes), 한 트랜잭션
 * @param   request - 분류 등록/수정/삭제 목록과 분류별 코드 등록/수정 목록 (코드는 삭제하지 않음)
 * @return  처리 건수를 담은 API 응답 (업무 오류면 success=false 와 서버 메시지)
 */
export async function saveCommCdApi(request: CommCdSaveRequest): Promise<ApiResponse<number | null>> {
  return requestApi(apiClient.post<ApiResponse<number>>('/api/v1/common-codes', request))
}
