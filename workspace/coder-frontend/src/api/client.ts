/*****************************************************************************************
	Program ID   : client
	Program Type : ts
	Program Name : API 클라이언트
	Description  : 공통 Axios 인스턴스 및 요청/응답 인터셉터(토큰 첨부, 401 처리) 정의
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-09-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import axios, { type AxiosInstance, type AxiosResponse } from 'axios'

// CODER API Response Standard Envelope
export interface ApiResponse<T = any> {
  success: boolean
  code?: string
  message?: string
  data: T
  timestamp?: string
}

// Create configured Axios instance
const apiClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
})

// Request Interceptor: Attach Auth Token
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('coder_token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // 서버 메시지(오류 문구 등)를 화면 언어로 받도록 Accept-Language 지정 (utils/message.ts 와 같은 저장 키)
    if (config.headers && !config.headers['Accept-Language']) {
      config.headers['Accept-Language'] = localStorage.getItem('coder_lang') || 'ko'
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response Interceptor: Standard Error & Format Handling
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    return response
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        // Token expired or invalid
        localStorage.removeItem('coder_token')
        localStorage.removeItem('coder_user')
        console.warn('[CODER API] Unauthorized access. Session cleared.')
        if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
      } else if (status >= 500) {
        console.error('[CODER API] Internal Server Error:', error.response.data)
      }
    } else {
      console.warn('[CODER API] Network or Connection Error:', error.message)
    }
    return Promise.reject(error)
  }
)

export default apiClient

/**
 * toApiResponse : 업무 오류(4xx)로 거절된 요청의 응답 본문(ApiResponse)을 꺼내 반환 (서버 메시지를 화면에서 안내할 때 사용)
 * @param   error - axios 요청 오류
 * @return  success=false 인 API 응답 (응답 본문이 ApiResponse 가 아니면 오류를 그대로 다시 던짐)
 * @example try { ... } catch (e) { return toApiResponse(e) }
 */
export function toApiResponse<T = null>(error: unknown): ApiResponse<T | null> {
  const data = axios.isAxiosError(error) ? error.response?.data : undefined
  if (data && typeof data === 'object' && typeof (data as ApiResponse).success === 'boolean') {
    return { ...(data as ApiResponse<T | null>), data: null }
  }
  throw error
}

/**
 * requestApi : API 요청 결과의 응답 본문(ApiResponse)을 반환하고, 업무 오류(4xx)면 서버 메시지를 담은 success=false 응답을 반환
 * @param   request - apiClient 요청 (get/post/put/patch/delete 호출 결과)
 * @return  API 응답 (업무 오류면 success=false, data=null)
 * @example return requestApi(apiClient.post<ApiResponse<number>>('/api/v1/menus', request))
 */
export async function requestApi<T>(request: Promise<AxiosResponse<ApiResponse<T>>>): Promise<ApiResponse<T | null>> {
  try {
    return (await request).data
  } catch (error) {
    return toApiResponse<T>(error)
  }
}
