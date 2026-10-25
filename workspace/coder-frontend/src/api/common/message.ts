/*****************************************************************************************
	Program ID   : message
	Program Type : ts
	Program Name : 다국어 메시지 API
	Description  : coder-backend 에서 관리하는 다국어 메시지 전체 목록 조회
	               - 조회: GET /api/v1/messages (Accept-Language 헤더의 언어 기준, 인증 불필요)
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import apiClient, { type ApiResponse } from '@/api/client'

/**
 * getMessageListApi : 지정한 언어의 메시지 전체 목록을 조회
 * @param   lang - 언어 코드 (ko, en)
 * @return  메시지 키 → 메시지 문구 맵
 */
export async function getMessageListApi(lang: string): Promise<Record<string, string>> {
  const response = await apiClient.get<ApiResponse<Record<string, string>>>('/api/v1/messages', {
    headers: { 'Accept-Language': lang },
  })
  return response.data.data ?? {}
}
