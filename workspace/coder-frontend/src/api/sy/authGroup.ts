/*****************************************************************************************
	Program ID   : SYAU020
	Program Type : ts
	Program Name : 그룹권한관리 API
	Description  : SYAU020 그룹권한관리 API
	               - TB_AUTH_GRP_M(권한그룹기본), TB_AUTH_GRP_MENU_R(그룹별권한내역),
	                 TB_AUTH_GRP_CMPN_R(권한그룹컴포넌트관계)
	               - 백엔드 API: /api/v1/auth-groups (목록·화면 권한·컴포넌트 권한·일괄 저장)
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-05  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import apiClient, { requestApi, type ApiResponse } from '../client'

export interface AuthGrp {
  usrGrpCd: string
  usrGrpNm: string
  useYn: string
  stDt: string
  endDt: string
  rmk: string
}

/** 그룹에 권한을 준 화면 (메뉴 + 프로그램) */
export interface AuthGrpMenu {
  usrGrpCd: string
  menuId: string
  prgmId: string
}

/** 그룹의 프로그램 컴포넌트별 권한 */
export interface AuthGrpCmpn {
  usrGrpCd: string
  prgmId: string
  cmpnId: string
  authYn: string
}

/** 권한그룹 1건의 권한목록 변경분 */
export interface AuthGrpMenuSave {
  usrGrpCd: string
  insertList: AuthGrpMenu[]
  /** 삭제된 화면의 프로그램에 다른 메뉴 권한이 남아 있지 않으면 컴포넌트 권한도 함께 삭제된다 */
  deleteList: AuthGrpMenu[]
}

/** 권한그룹·프로그램 1건의 컴포넌트 권한 변경분 */
export interface AuthGrpCmpnSave {
  usrGrpCd: string
  prgmId: string
  insertList: AuthGrpCmpn[]
  updateList: AuthGrpCmpn[]
}

export interface AuthGrpSaveRequest {
  insertList: AuthGrp[]
  updateList: AuthGrp[]
  /** 삭제된 그룹의 권한목록·컴포넌트 권한도 함께 삭제된다 */
  deleteList: AuthGrp[]
  menuSaveList: AuthGrpMenuSave[]
  cmpnSaveList: AuthGrpCmpnSave[]
}

export interface AuthGrpSrchParams {
  usrGrpCd?: string
  usrGrpNm?: string
}

/**
 * getAuthGrpListApi : 권한그룹 목록 조회, 그룹코드 순 (GET /api/v1/auth-groups)
 * @param   params - 조회조건 (그룹코드, 그룹명)
 * @return  권한그룹 목록을 담은 API 응답
 */
export async function getAuthGrpListApi(params: AuthGrpSrchParams): Promise<ApiResponse<AuthGrp[]>> {
  const response = await apiClient.get<ApiResponse<AuthGrp[]>>('/api/v1/auth-groups', { params })
  return response.data
}

/**
 * getAuthGrpMenuListApi : 권한그룹별 권한목록(화면) 조회 (GET /api/v1/auth-groups/{usrGrpCd}/menus)
 * @param   usrGrpCd - 권한그룹코드
 * @return  그룹에 권한이 부여된 화면 목록을 담은 API 응답
 */
export async function getAuthGrpMenuListApi(usrGrpCd: string): Promise<ApiResponse<AuthGrpMenu[]>> {
  const response = await apiClient.get<ApiResponse<AuthGrpMenu[]>>(`/api/v1/auth-groups/${encodeURIComponent(usrGrpCd)}/menus`)
  return response.data
}

/**
 * getAuthGrpCmpnListApi : 권한그룹의 컴포넌트 권한 조회 (GET /api/v1/auth-groups/{usrGrpCd}/components?prgmId=), prgmId 를 생략하면 그룹의 전체 컴포넌트 권한을 반환
 * @param   usrGrpCd - 권한그룹코드
 * @param   prgmId   - 프로그램ID (생략 시 전체)
 * @return  컴포넌트 권한 목록을 담은 API 응답
 */
export async function getAuthGrpCmpnListApi(usrGrpCd: string, prgmId?: string): Promise<ApiResponse<AuthGrpCmpn[]>> {
  const response = await apiClient.get<ApiResponse<AuthGrpCmpn[]>>(
    `/api/v1/auth-groups/${encodeURIComponent(usrGrpCd)}/components`,
    { params: prgmId ? { prgmId } : {} },
  )
  return response.data
}

/**
 * saveAuthGrpListApi : 권한그룹·권한목록·컴포넌트 권한 일괄 저장 (POST /api/v1/auth-groups), 한 트랜잭션
 * @param   request - 그룹, 권한목록, 컴포넌트 권한의 추가/수정/삭제 변경분
 * @return  저장된 건수를 담은 API 응답 (그룹ID 중복 등 업무 오류면 success=false 와 서버 메시지)
 */
export async function saveAuthGrpListApi(request: AuthGrpSaveRequest): Promise<ApiResponse<number | null>> {
  return requestApi(apiClient.post<ApiResponse<number>>('/api/v1/auth-groups', request))
}
