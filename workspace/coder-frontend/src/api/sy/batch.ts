/*****************************************************************************************
	Program ID   : SYLK050
	Program Type : ts
	Program Name : 배치관리 API
	Description  : SYLK050 배치관리 API - coder-batch 관리 API(:8081, documents/batch/PRD.md 8장) 호출
	               배치직무(TB_DSPT_JOB_M)와 실행내역(TB_DSPT_EXEC_L)을 조회·저장하고 수동 실행·스케줄 제어를 요청한다.
	               로컬은 vite proxy(/api/v1/batch-* -> :8081), 서버는 VITE_BATCH_API_BASE_URL 로 호출한다.
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import apiClient, { requestApi, type ApiResponse } from '../client'
import type { CodeOption } from '@/utils/code'

/** coder-batch 관리 API 호출 옵션 (서버 환경은 별도 도메인, 로컬은 vite proxy) */
const BATCH_API = { baseURL: import.meta.env.VITE_BATCH_API_BASE_URL || '' }

/** 배치직무 (TB_DSPT_JOB_M) + 스케줄 상태 + 최종 실행내역 */
export interface BatchJob {
  /** 배치ID (BH + 업무 2자리 + 순번 3자리, 예: BHFL001) */
  jobId: string
  jobNm: string
  /** 실행클래스명: Job 빈을 정의한 클래스 (패키지 포함) */
  execClassNm: string
  /** 업무모듈코드 (공통코드 SY/WRK_MDUL_CD) */
  wrkMdulCd: string
  /** 스케줄표현식 (Quartz cron: 초 분 시 일 월 주 [년]), 없으면 수동 실행 전용 */
  scheExpr: string | null
  /** 미스파이어정책코드 (01 건너뜀, 02 즉시 1회 실행) */
  msfrPlcyCd: string
  /** 다중실행여부 */
  mltExecYn: string
  /** 직무파라미터내용 (JSON) */
  jobParmCont: string | null
  useYn: string
  rmk: string | null
  /** Job 빈 등록 여부 */
  jobBeanYn: string
  /** 스케줄 상태 (NORMAL, PAUSED, BLOCKED, ERROR, COMPLETE, NONE) */
  schedStatus: string
  /** 다음 / 이전 실행 일시 (ISO) */
  nextFireDttm: string | null
  prevFireDttm: string | null
  /** 최종 실행내역 */
  lastDsptExecSn: number | null
  lastExecStatCd: string | null
  lastExecStDttm: string | null
  lastExecEndDttm: string | null
  lastProcCnt: number | null
  lastSuccCnt: number | null
  lastFailCnt: number | null
  lastErrMsgCont: string | null
  /** 실패 알람 수신자 (상세 조회에서만 채워짐) */
  alrmRcvrList?: BatchAlarmRcvr[]
}

/** 배치 실패 알람 수신자 (TB_DSPT_ALRM_RCVR_R + TB_USR_M) */
export interface BatchAlarmRcvr {
  usrId: string
  usrNm: string | null
  /** 사용자 사용여부 (N 이면 알람을 받지 않음) */
  useYn: string | null
}

/** 배치직무 저장 요청 */
export type BatchJobSave = Pick<
  BatchJob,
  'jobId' | 'jobNm' | 'execClassNm' | 'wrkMdulCd' | 'scheExpr' | 'msfrPlcyCd' | 'mltExecYn' | 'jobParmCont' | 'useYn' | 'rmk'
> & {
  /** 실패 알람 수신자ID 목록 (전체 교체, 빈 목록이면 모두 삭제) */
  alrmRcvrIdList: string[]
}

/** 배치 실행내역 (TB_DSPT_EXEC_L) */
export interface BatchExecution {
  dsptExecSn: number
  jobId: string
  jobNm: string
  /** 실행구분코드 (S 스케줄, M 수동, R 재시작) */
  execClsCd: string
  /** 실행상태코드 (Spring Batch BatchStatus 이름 + SKIP) */
  execStatCd: string
  execStDttm: string
  execEndDttm: string | null
  procCnt: number
  succCnt: number
  failCnt: number
  errMsgCont: string | null
  execSrvNm: string | null
  firsRegrId: string
}

export interface BatchJobSrchParams {
  /** 직무ID / 직무명 부분 일치 */
  keyword?: string
  wrkMdulCd?: string
  useYn?: string
  /** 최종 실행시작일자 (YYYYMMDD) */
  lastExecDt?: string
  lastExecStatCd?: string
}

export interface BatchExecutionSrchParams {
  jobId: string
  /** 실행시작 기간 (YYYYMMDD) */
  stDt: string
  endDt: string
  execStatCd?: string
}

interface PageResponse<T> {
  list: T[]
  totalCount: number
}

/** 배치 이력 팝업 최대 조회 건수 (실행시작일시 내림차순 최근 건) */
export const EXEC_MAX_ROWS = 500

/** 실행내역 조회 결과 */
export interface BatchExecutionPage {
  list: BatchExecution[]
  totalCount: number
  /** EXEC_MAX_ROWS 초과로 잘렸는지 */
  truncated: boolean
}

/** 수동 실행 파라미터로 쓸 수 없는 키 (coder-batch가 실행마다 채우는 JobParameter) */
export const RESERVED_JOB_PARM_KEYS = ['jobId', 'requestDttm', 'fireId', 'execClsCd', 'reqUsrId', 'execSrvNm']

/** 실행 중으로 보는 실행상태 (배치상태 초기화 대상) */
export const RUNNING_EXEC_STAT_CDS = ['STARTING', 'STARTED', 'STOPPING']

/** 실행상태 (Spring Batch BatchStatus + SKIP) */
export const EXEC_STAT_OPTIONS: CodeOption[] = [
  { label: '완료', value: 'COMPLETED' },
  { label: '실패', value: 'FAILED' },
  { label: '실행중', value: 'STARTED' },
  { label: '시작중', value: 'STARTING' },
  { label: '중지중', value: 'STOPPING' },
  { label: '중지', value: 'STOPPED' },
  { label: '폐기', value: 'ABANDONED' },
  { label: '건너뜀', value: 'SKIP' },
  { label: '알수없음', value: 'UNKNOWN' },
]

/** 실행구분 */
export const EXEC_CLS_OPTIONS: CodeOption[] = [
  { label: '스케줄', value: 'S' },
  { label: '수동', value: 'M' },
  { label: '재시작', value: 'R' },
]

/** 스케줄 상태 (Quartz Trigger 상태) */
export const SCHED_STATUS_OPTIONS: CodeOption[] = [
  { label: '정상', value: 'NORMAL' },
  { label: '일시정지', value: 'PAUSED' },
  { label: '실행중', value: 'BLOCKED' },
  { label: '오류', value: 'ERROR' },
  { label: '종료', value: 'COMPLETE' },
  { label: '미등록', value: 'NONE' },
]

/** 미스파이어정책 */
export const MSFR_PLCY_OPTIONS: CodeOption[] = [
  { label: '건너뜀', value: '01' },
  { label: '즉시 1회 실행', value: '02' },
]

/**
 * getBatchJobListApi : 배치직무 목록 조회 (GET /api/v1/batch-jobs)
 * @param   params - 조회조건 (검색어, 업무모듈, 사용여부, 최종 실행일자, 최종 실행상태)
 * @return  배치직무 목록을 담은 API 응답
 */
export async function getBatchJobListApi(params: BatchJobSrchParams): Promise<ApiResponse<BatchJob[]>> {
  const response = await apiClient.get<ApiResponse<BatchJob[]>>('/api/v1/batch-jobs', { ...BATCH_API, params })
  return response.data
}

/**
 * getBatchJobApi : 배치직무 상세 조회 (GET /api/v1/batch-jobs/{jobId})
 * @param   jobId - 직무ID
 * @return  배치직무를 담은 API 응답 (없으면 success=false)
 */
export async function getBatchJobApi(jobId: string): Promise<ApiResponse<BatchJob | null>> {
  return requestApi(apiClient.get<ApiResponse<BatchJob>>(`/api/v1/batch-jobs/${encodeURIComponent(jobId)}`, BATCH_API))
}

/**
 * saveBatchJobApi : 배치직무 등록/수정 (POST /api/v1/batch-jobs · PUT /api/v1/batch-jobs/{jobId}), 스케줄에 즉시 반영
 * @param   batchJob - 저장할 배치직무
 * @param   isNew    - 신규 등록 여부
 * @return  처리 결과 API 응답 (Job 빈 미등록·cron 오류 등은 success=false)
 */
export async function saveBatchJobApi(batchJob: BatchJobSave, isNew: boolean): Promise<ApiResponse<null>> {
  return requestApi(isNew
    ? apiClient.post<ApiResponse<null>>('/api/v1/batch-jobs', batchJob, BATCH_API)
    : apiClient.put<ApiResponse<null>>(`/api/v1/batch-jobs/${encodeURIComponent(batchJob.jobId)}`, batchJob, BATCH_API))
}

/**
 * saveBatchJobExecutionApi : 수동 실행 요청 (POST /api/v1/batch-jobs/{jobId}/executions), 비동기로 실행되며 결과는 실행내역에서 확인
 * @param   jobId  - 직무ID
 * @param   params - 실행 파라미터 (배치직무 기본 파라미터보다 우선)
 * @return  처리 결과 API 응답 (이미 실행 중이면 success=false)
 * @example saveBatchJobExecutionApi('BHFL001', { baseDt: '20261017' })
 */
export async function saveBatchJobExecutionApi(jobId: string, params: Record<string, string> = {}): Promise<ApiResponse<null>> {
  return requestApi(apiClient.post<ApiResponse<null>>(
    `/api/v1/batch-jobs/${encodeURIComponent(jobId)}/executions`,
    { params },
    BATCH_API,
  ))
}

/**
 * saveBatchJobScheduleApi : 스케줄 일시정지/재개 (PATCH /api/v1/batch-jobs/{jobId}/schedule)
 * @param   jobId       - 직무ID
 * @param   schedStatus - PAUSED(일시정지) / NORMAL(재개)
 * @return  처리 결과 API 응답
 */
export async function saveBatchJobScheduleApi(jobId: string, schedStatus: 'PAUSED' | 'NORMAL'): Promise<ApiResponse<null>> {
  return requestApi(apiClient.patch<ApiResponse<null>>(
    `/api/v1/batch-jobs/${encodeURIComponent(jobId)}/schedule`,
    { schedStatus },
    BATCH_API,
  ))
}

/**
 * getBatchExecutionListApi : 배치직무의 실행내역 조회 (GET /api/v1/batch-executions), 실행시작일시 내림차순 최대 EXEC_MAX_ROWS 건
 * @param   params - 조회조건 (직무ID, 기간, 실행상태)
 * @return  실행내역 목록·전체 건수·잘림 여부를 담은 API 응답
 */
export async function getBatchExecutionListApi(params: BatchExecutionSrchParams): Promise<ApiResponse<BatchExecutionPage>> {
  const response = await apiClient.get<ApiResponse<PageResponse<BatchExecution>>>('/api/v1/batch-executions', {
    ...BATCH_API,
    params: { ...params, page: 1, size: EXEC_MAX_ROWS },
  })
  const list = response.data.data?.list ?? []
  const totalCount = response.data.data?.totalCount ?? list.length
  return { ...response.data, data: { list, totalCount, truncated: totalCount > list.length } }
}

/**
 * saveBatchExecutionAbandonApi : 배치상태 초기화 - 실행 중 상태로 멈춘 실행 건을 폐기 (PATCH /api/v1/batch-executions/{dsptExecSn} ABANDONED)
 * @param   dsptExecSn - 배치실행일련번호
 * @return  처리 결과 API 응답 (실제로 실행 중이면 success=false)
 */
export async function saveBatchExecutionAbandonApi(dsptExecSn: number): Promise<ApiResponse<null>> {
  return requestApi(apiClient.patch<ApiResponse<null>>(
    `/api/v1/batch-executions/${dsptExecSn}`,
    { execStatCd: 'ABANDONED' },
    BATCH_API,
  ))
}
