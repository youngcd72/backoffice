/*****************************************************************************************
	Program ID   : SYAU010
	Program Type : ts
	Program Name : 사용자권한관리 API
	Description  : SYAU010 사용자권한관리 API
	               - TB_USR_M(사용자, 부서코드 DEPT_CD 포함), TB_USR_MENU_R(사용자별 화면 권한),
	                 TB_USR_CMPN_R(사용자별 프로그램 컴포넌트 권한여부)
	               - 부서명(DEPT_OPTIONS)은 부서 마스터가 아직 스키마에 없어 임시 목록으로 둔다.
	               - 백엔드 API: /api/v1/user-auths (목록·ID 확인·화면 권한·컴포넌트 권한·일괄 저장·비밀번호 초기화)
	               - 사용자구분은 공통코드 SY/USR_CLS_CD (E 직원, P 협력업체), 입사·퇴사일자는 TB_USR_M.ECOM_DT/RESI_DT
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import apiClient, { requestApi, type ApiResponse } from '../client'

/** 부서 (TB_USR_M.DEPT_CD — 부서 마스터 연동 전 임시 값) */
export const DEPT_OPTIONS = [
  { label: '경영지원팀', value: 'MGT' },
  { label: 'IT팀', value: 'IT' },
]

/** 사용자 (TB_USR_M + 사원마스터 + 미정 테이블 항목) */
export interface UsrMng {
  usrId: string
  usrNm: string
  /** 사원번호 (TB_USR_M.EMP_NO, 읽기 전용) */
  empNo: string
  /** 부서코드 (TB_USR_M.DEPT_CD) */
  deptCd: string
  /** 이메일주소 (TB_USR_M.EML_ADDR) */
  emlAddr: string
  /** 모바일번호 (TB_USR_M.MOBL_NO) */
  moblNo: string
  /** 사용자상태코드 (TB_USR_M.USR_STAT_CD, 재직여부 - 공통코드 SY/USR_STAT_CD: T 재직, L 휴직, R 퇴직) */
  usrStatCd: string
  /** 사용자잠금여부 (TB_USR_M.USR_LOCK_YN) */
  usrLockYn: string
  /** 사용자구분코드 (공통코드 SY/USR_CLS_CD: E 직원, P 협력업체) */
  usrClsCd: string
  /** 입사일자 (TB_USR_M.ECOM_DT, 읽기 전용) */
  ecomDt: string
  /** 퇴사일자 (TB_USR_M.RESI_DT, 읽기 전용) */
  resiDt: string
  stDt: string
  endDt: string
  useYn: string
}

/** 사용자 화면 권한 (메뉴 + 프로그램) — TB_USR_MENU_R */
export interface UsrMenuAuth {
  usrId: string
  menuId: string
  prgmId: string
}

/** 사용자의 프로그램 컴포넌트별 권한 — TB_USR_CMPN_R */
export interface UsrCmpnAuth {
  usrId: string
  prgmId: string
  cmpnId: string
  authYn: string
}

export interface UsrMngSrchParams {
  usrId?: string
  usrNm?: string
  usrClsCd?: string
  useYn?: string
}

/** 사용자 1명의 화면 권한 변경분 */
export interface UsrMenuAuthSave {
  usrId: string
  insertList: UsrMenuAuth[]
  /** 삭제된 화면의 프로그램에 다른 메뉴 권한이 남아 있지 않으면 컴포넌트 권한도 함께 삭제된다 */
  deleteList: UsrMenuAuth[]
}

/** 사용자·프로그램 1건의 컴포넌트 권한 변경분 */
export interface UsrCmpnAuthSave {
  usrId: string
  prgmId: string
  insertList: UsrCmpnAuth[]
  updateList: UsrCmpnAuth[]
}

export interface UsrAuthSaveRequest {
  insertList: UsrMng[]
  updateList: UsrMng[]
  /** 삭제된 사용자의 화면·컴포넌트 권한도 함께 삭제된다 */
  deleteList: UsrMng[]
  menuAuthSaveList: UsrMenuAuthSave[]
  cmpnAuthSaveList: UsrCmpnAuthSave[]
}

/**
 * getUsrMngListApi : 사용자 목록 조회, 사용자ID 순 (GET /api/v1/user-auths)
 * @param   params - 조회조건 (사용자ID, 사용자명, 사용자구분, 사용여부)
 * @return  사용자 목록을 담은 API 응답
 */
export async function getUsrMngListApi(params: UsrMngSrchParams): Promise<ApiResponse<UsrMng[]>> {
  const response = await apiClient.get<ApiResponse<UsrMng[]>>('/api/v1/user-auths', { params })
  return response.data
}

/**
 * existsUsrIdApi : 사용자ID 등록 여부 확인 (GET /api/v1/user-auths/{usrId}/exists)
 * @param   usrId - 사용자ID
 * @return  이미 등록되어 있으면 true 를 담은 API 응답
 */
export async function existsUsrIdApi(usrId: string): Promise<ApiResponse<boolean>> {
  const response = await apiClient.get<ApiResponse<boolean>>(`/api/v1/user-auths/${encodeURIComponent(usrId)}/exists`)
  return response.data
}

/**
 * getUsrMenuAuthListApi : 사용자 화면 권한 목록 조회 (GET /api/v1/user-auths/{usrId}/menu-auths)
 * @param   usrId - 사용자ID
 * @return  화면 권한 목록을 담은 API 응답
 */
export async function getUsrMenuAuthListApi(usrId: string): Promise<ApiResponse<UsrMenuAuth[]>> {
  const response = await apiClient.get<ApiResponse<UsrMenuAuth[]>>(`/api/v1/user-auths/${encodeURIComponent(usrId)}/menu-auths`)
  return response.data
}

/**
 * getUsrCmpnAuthListApi : 사용자의 컴포넌트 권한 조회 (GET /api/v1/user-auths/{usrId}/component-auths?prgmId=), prgmId 를 생략하면 사용자의 전체 컴포넌트 권한을 반환
 * @param   usrId  - 사용자ID
 * @param   prgmId - 프로그램ID (생략 시 전체)
 * @return  컴포넌트 권한 목록을 담은 API 응답
 */
export async function getUsrCmpnAuthListApi(usrId: string, prgmId?: string): Promise<ApiResponse<UsrCmpnAuth[]>> {
  const response = await apiClient.get<ApiResponse<UsrCmpnAuth[]>>(
    `/api/v1/user-auths/${encodeURIComponent(usrId)}/component-auths`,
    { params: prgmId ? { prgmId } : {} },
  )
  return response.data
}

/**
 * saveUsrAuthApi : 사용자·화면 권한·컴포넌트 권한 일괄 저장 (POST /api/v1/user-auths), 한 트랜잭션
 * @param   request - 사용자 추가/수정/삭제, 사용자별 화면 권한, 사용자·프로그램별 컴포넌트 권한 변경분
 * @return  저장된 건수를 담은 API 응답 (사용자ID 중복 등 업무 오류면 success=false 와 서버 메시지)
 */
export async function saveUsrAuthApi(request: UsrAuthSaveRequest): Promise<ApiResponse<number | null>> {
  return requestApi(apiClient.post<ApiResponse<number>>('/api/v1/user-auths', request))
}

/**
 * initUsrPwdApi : 사용자 비밀번호 초기화 (POST /api/v1/user-auths/{usrId}/password-resets)
 * @param   usrId - 사용자ID
 * @return  처리 결과를 담은 API 응답 (등록되지 않은 사용자면 success=false 와 서버 메시지)
 */
export async function initUsrPwdApi(usrId: string): Promise<ApiResponse<null>> {
  return requestApi(apiClient.post<ApiResponse<null>>(`/api/v1/user-auths/${encodeURIComponent(usrId)}/password-resets`))
}
