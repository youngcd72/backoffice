/*****************************************************************************************
	Program ID   : auth
	Program Type : ts
	Program Name : 인증/사용자 API
	Description  : 로그인, 전체 사용자 목록 조회 API
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-09-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import apiClient, { type ApiResponse } from './client'

export interface LoginPayload {
  username: string
  password: string
}

export interface UserProfile {
  id?: number | string
  username: string
  name: string
  email?: string
  role: string
  permissions?: string[]
  status?: string
}

export interface LoginResult {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: UserProfile
}

/**
 * loginApi : 로그인 요청 후 액세스 토큰과 사용자 정보를 반환 (POST /api/v1/auth/login)
 * @param   payload - 로그인 요청 정보 (사용자명, 비밀번호)
 * @return  토큰과 사용자 정보를 담은 API 응답
 */
export async function loginApi(payload: LoginPayload): Promise<ApiResponse<LoginResult>> {
  const response = await apiClient.post<ApiResponse<LoginResult>>('/api/v1/auth/login', payload)
  return response.data
}

/**
 * getAllUsersApi : 전체 사용자 목록 조회 (GET /api/v1/users)
 * @return  사용자 프로필 목록을 담은 API 응답
 */
export async function getAllUsersApi(): Promise<ApiResponse<UserProfile[]>> {
  const response = await apiClient.get<ApiResponse<UserProfile[]>>('/api/v1/users')
  return response.data
}
