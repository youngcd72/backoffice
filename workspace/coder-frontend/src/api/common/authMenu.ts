/*****************************************************************************************
	Program ID   : authMenu
	Program Type : ts
	Program Name : 권한 메뉴 API
	Description  : 로그인 사용자에게 권한이 있는 메뉴 조회 (상단 메뉴·사이드바·메뉴 검색)
	               - 조회: GET /api/v1/auth-menus (JWT 의 사용자 기준, 권한그룹 + 사용자 메뉴 권한)
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-19  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import apiClient, { type ApiResponse } from '@/api/client'

/** 권한 메뉴 1건 (권한 있는 화면 메뉴와 그 상위 메뉴) */
export interface AuthMenu {
  menuId: string
  menuNm: string
  /** 상위메뉴ID (최상위 메뉴는 null) */
  upprMenuId?: string | null
  alinSeq?: number
  /** 프로그램ID (화면 메뉴만) */
  prgmId?: string | null
}

/**
 * getAuthMenuListApi : 로그인 사용자에게 권한이 있는 화면 메뉴와 그 상위 메뉴를 조회
 * @return  권한 메뉴 목록 (상위 메뉴 먼저, 정렬순번 순)
 */
export async function getAuthMenuListApi(): Promise<AuthMenu[]> {
  const response = await apiClient.get<ApiResponse<AuthMenu[]>>('/api/v1/auth-menus')
  return response.data.data ?? []
}
