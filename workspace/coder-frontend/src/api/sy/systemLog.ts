/*****************************************************************************************
	Program ID   : SYLO030
	Program Type : ts
	Program Name : 시스템로그관리 API
	Description  : SYLO030 시스템로그관리 API (coder-backend 에러 로그 파일 logs/coder-backend-error.{일자}.{순번}.log)
	               - 조회 기간은 최대 MAX_PERIOD_DAYS 일, 최근 MAX_ROWS 건까지 반환한다.
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import apiClient, { type ApiResponse } from '../client'

/** 최대 조회 기간 (일) — backend SystemLogServiceImpl.MAX_PERIOD_DAYS 와 같은 값 */
export const MAX_PERIOD_DAYS = 31
/** 최대 반환 건수 — backend SystemLogServiceImpl.MAX_ROWS 와 같은 값 */
export const MAX_ROWS = 1000

/** 시스템로그 1건 */
export interface SystemLog {
  /** 로그 키 ({일자}-{파일순번}-{줄번호}, 상세 조회용) */
  logKey: string
  /** 로그일시 (yyyy-MM-dd HH:mm:ss.SSS) */
  logDttm: string
  /** 시스템구분코드 (SY003) */
  sysmClsCd?: string
  usrId?: string
  usrNm?: string
  /** 사용자구분코드 (SY002) */
  usrClsCd?: string
  deptCd?: string
  /** 에러내용 */
  errMsg?: string
  /** SQL ID ({매퍼명}.{쿼리ID}) */
  sqlId?: string
  errCd?: string
  traceId?: string
  uri?: string
  method?: string
  logger?: string
  thread?: string
  /** 스택트레이스 (상세 조회에서만 채워진다) */
  stackTrace?: string
}

export interface SystemLogSrchParams {
  /** 기간 (YYYYMMDD) */
  stDt: string
  endDt: string
  sysmClsCd?: string
  usrClsCd?: string
  usrId?: string
  usrNm?: string
}

export interface SystemLogPage {
  list: SystemLog[]
  totalCount: number
  /** MAX_ROWS 초과로 잘렸는지 */
  truncated: boolean
}

/**
 * getSystemLogListApi : 시스템로그 목록 조회 (GET /api/v1/system-logs), 최근 순 MAX_ROWS 건까지
 * @param   params - 조회조건 (기간, 시스템구분, 사용자구분, 사용자ID/명)
 * @return  로그 목록·전체 건수·잘림 여부를 담은 API 응답
 */
export async function getSystemLogListApi(params: SystemLogSrchParams): Promise<ApiResponse<SystemLogPage>> {
  const response = await apiClient.get<ApiResponse<SystemLogPage>>('/api/v1/system-logs', { params })
  return response.data
}

/**
 * getSystemLogApi : 시스템로그 상세 조회 (GET /api/v1/system-logs/{logKey}), 스택트레이스 포함
 * @param   logKey - 로그 키
 * @return  시스템로그 상세를 담은 API 응답
 */
export async function getSystemLogApi(logKey: string): Promise<ApiResponse<SystemLog>> {
  const response = await apiClient.get<ApiResponse<SystemLog>>(`/api/v1/system-logs/${encodeURIComponent(logKey)}`)
  return response.data
}
