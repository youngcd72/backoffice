/*****************************************************************************************
	Program ID   : favorite
	Program Type : ts
	Program Name : 즐겨찾기 API
	Description  : 로그인 사용자의 즐겨찾기 메뉴 조회 (TB_PRGM_FAV_L + TB_MENU_M)
	               - 조회: GET /api/v1/favorites (JWT 의 사용자 기준)
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import apiClient, { type ApiResponse } from '@/api/client'

/** 즐겨찾기 메뉴 1건 */
export interface Favorite {
  menuId: string
  menuNm?: string
  prgmId?: string
  alinSeq?: number
}

/**
 * getFavoriteListApi : 로그인 사용자의 즐겨찾기 메뉴 목록을 조회
 * @return  즐겨찾기 메뉴 목록 (정렬순번 순)
 */
export async function getFavoriteListApi(): Promise<Favorite[]> {
  const response = await apiClient.get<ApiResponse<Favorite[]>>('/api/v1/favorites')
  return response.data.data ?? []
}
