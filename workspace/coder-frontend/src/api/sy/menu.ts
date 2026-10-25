/*****************************************************************************************
	Program ID   : SYPG020
	Program Type : ts
	Program Name : 메뉴관리 API
	Description  : SYPG020 메뉴관리 API
	               - TB_MENU_M(시스템메뉴내역)
	               - 조회: GET /api/v1/menus (TB_MENU_M 전체)
	               - 저장: POST /api/v1/menus (등록/수정/삭제 일괄)
	               - 메뉴 수가 많지 않아 전체 목록을 한 번에 조회하고, 트리/검색 필터링은 화면에서 처리한다.
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import apiClient, { requestApi, type ApiResponse } from '../client'

/** 메뉴타입코드 (TB_MENU_M.MENU_TYPE_CD, 공통코드 SY/MENU_TYPE_CD) */
export const MENU_TYPE_CD = {
  MENU: 'MENU',
  LINK: 'LINK',
} as const

export interface Menu {
  menuId: string
  menuNm: string
  upprMenuId: string
  alinSeq: number | null
  menuTypeCd: string
  useYn: string
  prgmId: string
  rmk: string
}

export interface MenuSaveRequest {
  insertList: Menu[]
  updateList: Menu[]
  deleteList: Menu[]
}

/**
 * getMenuListApi : 메뉴 전체 목록 조회, 정렬순번 순 (GET /api/v1/menus, TB_MENU_M)
 * @return  메뉴 목록을 담은 API 응답
 */
export async function getMenuListApi(): Promise<ApiResponse<Menu[]>> {
  const response = await apiClient.get<ApiResponse<Menu[]>>('/api/v1/menus')
  return response.data
}

/**
 * saveMenuListApi : 메뉴 목록 일괄 저장 (POST /api/v1/menus), 삭제된 메뉴의 즐겨찾기·메뉴 권한도 함께 삭제
 * @param   request - 행 상태가 등록(I)/수정(U)/삭제(D)인 메뉴 목록
 * @return  저장된 건수를 담은 API 응답 (업무 오류면 success=false 와 서버 메시지)
 */
export async function saveMenuListApi(request: MenuSaveRequest): Promise<ApiResponse<number | null>> {
  return requestApi(apiClient.post<ApiResponse<number>>('/api/v1/menus', request))
}
