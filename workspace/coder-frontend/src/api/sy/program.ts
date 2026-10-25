/*****************************************************************************************
	Program ID   : SYPG010
	Program Type : ts
	Program Name : 프로그램관리 API
	Description  : SYPG010 프로그램관리 API
	               - TB_PRGM_M(프로그램기본), TB_PRGM_CMPN_M(프로그램컴포넌트기본)
	               - 조회: GET /api/v1/programs, GET /api/v1/programs/{prgmId}/components
	               - 저장: POST /api/v1/programs (행 상태 I/U/D 인 항목만 한 트랜잭션으로 저장)
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import apiClient, { requestApi, type ApiResponse } from '../client'

export interface Prgm {
  prgmId: string
  prgmNm: string
  lnkUrl: string
  /** 프로그램경로코드 (공통코드 SY/PRGM_CRS_CD) */
  prgmCrsCd: string
  useYn: string
  stDt: string
  endDt: string
  rmk: string
}

/** 프로그램 컴포넌트 (CMPN_ID = 화면 버튼 등의 ref/id) */
export interface PrgmCmpn {
  prgmId: string
  cmpnId: string
  cmpnNm: string
  alinSeq: number | null
  useYn: string
}

export interface PrgmSrchParams {
  prgmCrsCd?: string
  prgmId?: string
  prgmNm?: string
  useYn?: string
}

/** 프로그램 1건의 컴포넌트 변경분 */
export interface PrgmCmpnSave {
  prgmId: string
  insertList: PrgmCmpn[]
  updateList: PrgmCmpn[]
  deleteList: PrgmCmpn[]
}

export interface PrgmSaveRequest {
  insertList: Prgm[]
  updateList: Prgm[]
  /** 삭제된 프로그램의 컴포넌트도 함께 삭제된다 */
  deleteList: Prgm[]
  cmpnSaveList: PrgmCmpnSave[]
}

/**
 * getPrgmListApi : 프로그램 목록 조회, 정렬순번 순 (GET /api/v1/programs, TB_PRGM_M)
 * @param   params - 조회조건 (프로그램경로코드, 프로그램ID, 프로그램명, 사용여부)
 * @return  프로그램 목록을 담은 API 응답
 */
export async function getPrgmListApi(params: PrgmSrchParams): Promise<ApiResponse<Prgm[]>> {
  const response = await apiClient.get<ApiResponse<Prgm[]>>('/api/v1/programs', { params })
  return response.data
}

/**
 * getPrgmCmpnListApi : 프로그램 컴포넌트 목록 조회, 정렬순번 순 (GET /api/v1/programs/{prgmId}/components, TB_PRGM_CMPN_M)
 * @param   prgmId - 프로그램ID
 * @return  컴포넌트 목록을 담은 API 응답
 */
export async function getPrgmCmpnListApi(prgmId: string): Promise<ApiResponse<PrgmCmpn[]>> {
  const response = await apiClient.get<ApiResponse<PrgmCmpn[]>>(`/api/v1/programs/${encodeURIComponent(prgmId)}/components`)
  return response.data
}

/**
 * savePrgmListApi : 프로그램·컴포넌트 일괄 저장 (POST /api/v1/programs), 삭제된 프로그램의 컴포넌트도 함께 삭제
 * @param   request - 행 상태가 등록(I)/수정(U)/삭제(D)인 프로그램·컴포넌트 목록
 * @return  저장된 건수를 담은 API 응답 (업무 오류면 success=false 와 서버 메시지)
 */
export async function savePrgmListApi(request: PrgmSaveRequest): Promise<ApiResponse<number | null>> {
  return requestApi(apiClient.post<ApiResponse<number>>('/api/v1/programs', request))
}
