/*****************************************************************************************
	Program ID   : alarm
	Program Type : ts
	Program Name : 알림 센터 API
	Description  : 로그인 사용자의 알람(TB_ALRM_L) 목록·건수 조회와 확인(읽음) 처리
	               - 실시간 수신: GET /api/v1/alarms/stream (SSE, stores/alarm.ts 에서 연결)
	               - 목록: GET /api/v1/alarms (최근 50건)
	               - 건수: GET /api/v1/alarms/unconfirmed-count
	               - 확인: PATCH /api/v1/alarms/{alrmSn}, 전체 확인: PATCH /api/v1/alarms
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-17  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import apiClient, { type ApiResponse } from '@/api/client'

/** SSE 연결 경로 */
export const ALARM_STREAM_URL = '/api/v1/alarms/stream'

/** 알람 1건 */
export interface Alarm {
  alrmSn: number
  /** 알람유형코드 (SY/ALRM_TYPE_CD) */
  alrmTypeCd: string
  alrmTitlNm: string
  alrmCont?: string
  /** 클릭 시 열 화면의 프로그램ID */
  prgmId?: string
  /** 확인여부 (Y/N) */
  cfmYn: 'Y' | 'N'
  firsRegrId?: string
  /** 발송일시 (yyyy-MM-ddTHH:mm:ss) */
  firsRegDt: string
}

/**
 * getAlarmListApi : 로그인 사용자의 최근 알람 목록을 조회
 * @return  알람 목록 (최근 발송 순, 최대 50건)
 */
export async function getAlarmListApi(): Promise<Alarm[]> {
  const response = await apiClient.get<ApiResponse<Alarm[]>>('/api/v1/alarms')
  return response.data.data ?? []
}

/**
 * getUnconfirmedAlarmCountApi : 로그인 사용자의 확인하지 않은 알람 건수를 조회
 * @return  미확인 알람 건수
 */
export async function getUnconfirmedAlarmCountApi(): Promise<number> {
  const response = await apiClient.get<ApiResponse<number>>('/api/v1/alarms/unconfirmed-count')
  return response.data.data ?? 0
}

/**
 * saveAlarmConfirmApi : 알람 1건을 확인(읽음) 처리
 * @param   alrmSn - 알람일련번호
 * @return  수정 건수 (이미 확인한 알람이면 0)
 */
export async function saveAlarmConfirmApi(alrmSn: number): Promise<number> {
  const response = await apiClient.patch<ApiResponse<number>>(`/api/v1/alarms/${alrmSn}`, { cfmYn: 'Y' })
  return response.data.data ?? 0
}

/**
 * saveAlarmConfirmAllApi : 미확인 알람 전체를 확인(읽음) 처리
 * @return  수정 건수
 */
export async function saveAlarmConfirmAllApi(): Promise<number> {
  const response = await apiClient.patch<ApiResponse<number>>('/api/v1/alarms', { cfmYn: 'Y' })
  return response.data.data ?? 0
}
