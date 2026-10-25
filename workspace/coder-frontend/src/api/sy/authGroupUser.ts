/*****************************************************************************************
	Program ID   : SYAU030
	Program Type : ts
	Program Name : 그룹별사용자관리 API
	Description  : SYAU030 그룹별사용자관리 API (그룹별 사용자 조회, 사용자 추가/제외)
	               - TB_AUTH_GRP_M(권한그룹기본), TB_USR_AUTH_GRP_R(사용자권한그룹관계), TB_USR_M(사용자)
	               - 백엔드 API(/api/v1/auth-groups/user-counts, /api/v1/auth-groups/{usrGrpCd}/users) 연동
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-12  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import apiClient, { requestApi, type ApiResponse } from '../client'

/** 권한그룹 + 그룹별 사용자수 */
export interface AuthGrpUsrCnt {
  usrGrpCd: string
  usrGrpNm: string
  useYn: string
  usrCnt: number
}

/** 권한그룹에 속한 사용자 */
export interface AuthGrpUsr {
  usrGrpCd: string
  usrId: string
  usrNm: string
  usrClsCd: string
  deptCd: string
}

/** 권한그룹 1건의 사용자 변경분 */
export interface AuthGrpUsrSaveRequest {
  usrGrpCd: string
  /** 그룹에 추가할 사용자ID */
  insertList: string[]
  /** 그룹에서 제외할 사용자ID */
  deleteList: string[]
}

export interface AuthGrpUsrSrchParams {
  usrGrpCd?: string
  usrGrpNm?: string
  useYn?: string
}

/**
 * getAuthGrpUsrCntListApi : 권한그룹 목록과 그룹별 사용자수 조회, 그룹코드 순 (GET /api/v1/auth-groups/user-counts)
 * @param   params - 조회조건 (그룹ID, 그룹명, 사용여부)
 * @return  권한그룹별 사용자수 목록을 담은 API 응답
 */
export async function getAuthGrpUsrCntListApi(params: AuthGrpUsrSrchParams): Promise<ApiResponse<AuthGrpUsrCnt[]>> {
  const response = await apiClient.get<ApiResponse<AuthGrpUsrCnt[]>>('/api/v1/auth-groups/user-counts', { params })
  return response.data
}

/**
 * getAuthGrpUsrListApi : 권한그룹에 속한 사용자 목록 조회, 사용자ID 순 (GET /api/v1/auth-groups/{usrGrpCd}/users)
 * @param   usrGrpCd - 권한그룹코드
 * @return  사용자 목록을 담은 API 응답
 */
export async function getAuthGrpUsrListApi(usrGrpCd: string): Promise<ApiResponse<AuthGrpUsr[]>> {
  const response = await apiClient.get<ApiResponse<AuthGrpUsr[]>>(`/api/v1/auth-groups/${encodeURIComponent(usrGrpCd)}/users`)
  return response.data
}

/**
 * saveAuthGrpUsrApi : 권한그룹에 사용자를 추가하거나 제외한다 (POST /api/v1/auth-groups/{usrGrpCd}/users)
 * @param   request - 권한그룹과 추가/제외할 사용자ID 목록
 * @return  처리 건수를 담은 API 응답 (업무 오류면 success=false 와 서버 메시지)
 */
export async function saveAuthGrpUsrApi(request: AuthGrpUsrSaveRequest): Promise<ApiResponse<number | null>> {
  const { usrGrpCd, insertList, deleteList } = request
  return requestApi(apiClient.post<ApiResponse<number>>(`/api/v1/auth-groups/${encodeURIComponent(usrGrpCd)}/users`, {
    insertList,
    deleteList,
  }))
}
