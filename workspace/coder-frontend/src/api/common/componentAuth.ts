/*****************************************************************************************
	Program ID   : componentAuth
	Program Type : ts
	Program Name : 컴포넌트 권한 API
	Description  : 로그인 사용자의 화면 컴포넌트(버튼 등) 권한 조회 (v-permission)
	               - 조회: GET /api/v1/component-auths?prgmId= (JWT 의 사용자 기준)
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-19  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import apiClient, { type ApiResponse } from '@/api/client'

/** 컴포넌트 권한 1건 (TB_PRGM_CMPN_M 에 사용 중으로 등록된 컴포넌트) */
export interface ComponentAuth {
  cmpnId: string
  /** 권한그룹 또는 사용자 권한 중 하나라도 Y 이면 Y */
  authYn: string
}

/**
 * getComponentAuthListApi : 로그인 사용자의 프로그램 컴포넌트별 권한여부를 조회
 * @param   prgmId - 프로그램ID
 * @return  컴포넌트 권한 목록 (등록되지 않은 컴포넌트는 포함되지 않음)
 */
export async function getComponentAuthListApi(prgmId: string): Promise<ComponentAuth[]> {
  const response = await apiClient.get<ApiResponse<ComponentAuth[]>>('/api/v1/component-auths', { params: { prgmId } })
  return response.data.data ?? []
}
