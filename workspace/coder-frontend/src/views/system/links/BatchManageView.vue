<script setup lang="ts">
/*****************************************************************************************
	Program ID   : SYLK050
	Program Type : vue
	Program Name : 배치관리
	Description  : 배치직무(TB_DSPT_JOB_M) 목록 조회, 등록/수정, 수동 실행, 스케줄 일시정지/재개 및 실행내역(TB_DSPT_EXEC_L) 조회 화면
	               coder-batch 관리 API(:8081)를 호출한다.
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { ref, computed, onMounted, nextTick } from 'vue'
import { Button, Input, Select, DataTable, DatePicker, PopupWindow, ComSearchCodePopup, useDialog, type SearchCodeRow } from '@/components/common'
import { msg } from '@/utils/message'
import { getCommCdOptions } from '@/api/common/commonCode'
import {
  EXEC_CLS_OPTIONS,
  EXEC_MAX_ROWS,
  EXEC_STAT_OPTIONS,
  MSFR_PLCY_OPTIONS,
  RESERVED_JOB_PARM_KEYS,
  RUNNING_EXEC_STAT_CDS,
  SCHED_STATUS_OPTIONS,
  getBatchExecutionListApi,
  getBatchJobApi,
  getBatchJobListApi,
  saveBatchExecutionAbandonApi,
  saveBatchJobApi,
  saveBatchJobExecutionApi,
  saveBatchJobScheduleApi,
  type BatchAlarmRcvr,
  type BatchExecution,
  type BatchJob,
  type BatchJobSave,
} from '@/api/sy/batch'
import { USE_YN_OPTIONS, codeName, withAll, type CodeOption } from '@/utils/code'
import { downloadCsv } from '@/utils/csv'
import { dashToYmd, todayDash, todayYmd } from '@/utils/date'
import { formatCount } from '@/utils/format'
import { validateFields } from '@/utils/validate'
import { renumber } from '@/utils/rowFlag'

/** 배치직무 그리드 행 (표시용 값 포함) */
interface BatchJobRow extends BatchJob {
  no?: number
  wrkMdulNm: string
  lastExecStatNm: string
  schedStatusNm: string
  lastExecStDttmText: string
  lastExecEndDttmText: string
  nextFireDttmText: string
  useYnNm: string
  /** 최종 수행시간 (초) */
  lastDiffTm: number | null
}

/** 실행내역 그리드 행 (표시용 값 포함) */
interface BatchExecutionRow extends BatchExecution {
  no?: number
  execStatNm: string
  execClsNm: string
  execStDttmText: string
  execEndDttmText: string
  diffTm: number | null
}

/** 실행 파라미터 입력 행 */
interface RunParmRow {
  key: string
  value: string
}

/** 배치 등록/수정 팝업 폼 (빈 값은 빈 문자열) */
interface BatchJobForm {
  jobId: string
  jobNm: string
  execClassNm: string
  wrkMdulCd: string
  scheExpr: string
  msfrPlcyCd: string
  mltExecYn: string
  jobParmCont: string
  useYn: string
  rmk: string
}

const PAGE_TITLE = '배치관리'
const PAGE_ID = 'SYLK050'

const jobColumns = [
  { key: 'no', label: 'NO', width: '48px', align: 'center' },
  { key: 'jobId', label: '배치ID', width: '90px', sortable: true },
  { key: 'jobNm', label: '배치명', width: '200px', sortable: true },
  { key: 'manlExec', label: '수동실행', width: '70px', align: 'center' },
  { key: 'lastExecStatNm', label: '실행상태', width: '72px', align: 'center' },
  { key: 'lastExecStDttmText', label: '최종수행시작일시', width: '150px', align: 'center', sortable: true },
  { key: 'lastExecEndDttmText', label: '최종수행종료일시', width: '150px', align: 'center' },
  { key: 'lastDiffTm', label: '최종수행시간', width: '84px', align: 'right' },
  { key: 'lastProcCnt', label: '처리건수', width: '72px', align: 'right' },
  { key: 'lastSuccCnt', label: '성공건수', width: '72px', align: 'right' },
  { key: 'lastFailCnt', label: '실패건수', width: '72px', align: 'right' },
  { key: 'lastErrMsgCont', label: '에러메시지', width: '220px' },
  { key: 'wrkMdulNm', label: '업무모듈', width: '80px', align: 'center' },
  { key: 'scheExpr', label: '스케줄표현식', width: '120px' },
  { key: 'schedStatusNm', label: '스케줄상태', width: '76px', align: 'center' },
  { key: 'nextFireDttmText', label: '다음실행일시', width: '150px', align: 'center' },
  { key: 'useYnNm', label: '사용여부', width: '64px', align: 'center' },
  { key: 'execClassNm', label: '실행클래스', width: '260px' },
  { key: 'rmk', label: '비고', width: '200px' },
]

const execColumns = [
  { key: 'no', label: 'NO', width: '44px', align: 'center' },
  { key: 'execStDttmText', label: '시작일시', width: '150px', align: 'center' },
  { key: 'execEndDttmText', label: '종료일시', width: '150px', align: 'center' },
  { key: 'diffTm', label: '소요시간', width: '70px', align: 'right' },
  { key: 'execStatNm', label: '실행상태', width: '72px', align: 'center' },
  { key: 'execClsNm', label: '실행구분', width: '64px', align: 'center' },
  { key: 'procCnt', label: '처리', width: '64px', align: 'right' },
  { key: 'succCnt', label: '성공', width: '64px', align: 'right' },
  { key: 'failCnt', label: '실패', width: '64px', align: 'right' },
  { key: 'firsRegrId', label: '요청자', width: '80px', align: 'center' },
  { key: 'errMsgCont', label: '에러메시지' },
]

/** 엑셀 다운로드 컬럼 (화면 버튼 컬럼 제외) */
const EXCEL_COLUMNS = jobColumns.filter((col) => col.key !== 'manlExec')

const { alert, confirm } = useDialog()

// 공통코드
const wrkMdulOptions = ref<CodeOption[]>([])
const useYnOptions = USE_YN_OPTIONS

// 조회 조건
const searchForm = ref({ keyword: '', wrkMdulCd: '', useYn: 'Y', lastExecDt: '', lastExecStatCd: '' })

// 배치직무 그리드
const jobRows = ref<BatchJobRow[]>([])
const activeJobRow = ref<BatchJobRow | null>(null)
const isJobLoading = ref(false)
const jobTotalCount = computed(() => jobRows.value.length)

// 실행내역 팝업
const isExecPopupOpen = ref(false)
const execTarget = ref<BatchJobRow | null>(null)
const execSearchForm = ref<{ prid: string[]; execStatCd: string }>({ prid: [], execStatCd: '' })
const execRows = ref<BatchExecutionRow[]>([])
const isExecLoading = ref(false)
const isExecTruncated = ref(false)

// 실행 파라미터 팝업
const isRunPopupOpen = ref(false)
const runTarget = ref<BatchJobRow | null>(null)
const runParmRows = ref<RunParmRow[]>([])

// 배치 등록/수정 팝업
const isDtlPopupOpen = ref(false)
const isNewJob = ref(true)
const dtlForm = ref<BatchJobForm>(createEmptyForm())
const dtlJob = ref<BatchJob | null>(null)
const dtlPopupTitle = computed(() => (isNewJob.value ? '배치 등록' : `배치 관리 - ${dtlForm.value.jobNm}`))
const isDtlRunning = computed(() => isRunning(dtlJob.value?.lastExecStatCd))
const dtlSchedStatus = computed(() => dtlJob.value?.schedStatus ?? '')
// 알람수신자 (저장 버튼으로 배치 기본과 함께 저장, 전체 교체)
const alrmRcvrRows = ref<BatchAlarmRcvr[]>([])
const checkedAlrmRcvrIds = ref<string[]>([])
const isAllAlrmRcvrChecked = computed(
  () => alrmRcvrRows.value.length > 0 && checkedAlrmRcvrIds.value.length === alrmRcvrRows.value.length,
)
const isAlrmRcvrPopupOpen = ref(false)

// ---------------------------------------------------------------------
// 공통 함수
// ---------------------------------------------------------------------
/**
 * formatDttm : ISO 일시(2026-10-17T00:10:00.123)를 YYYY-MM-DD HH:mm:ss 형식으로 변환
 * @param   dttm - ISO 일시 문자열
 * @return  YYYY-MM-DD HH:mm:ss 문자열 (값이 없으면 빈 문자열)
 */
function formatDttm(dttm: string | null | undefined): string {
  return dttm ? dttm.replace('T', ' ').slice(0, 19) : ''
}

/**
 * diffSeconds : 시작·종료 일시 사이의 초 계산
 * @param   stDttm  - 시작 일시 (ISO)
 * @param   endDttm - 종료 일시 (ISO)
 * @return  소요 시간(초) (종료 일시가 없으면 null)
 */
function diffSeconds(stDttm: string | null | undefined, endDttm: string | null | undefined): number | null {
  if (!stDttm || !endDttm) return null
  return Math.max(0, Math.round((new Date(endDttm).getTime() - new Date(stDttm).getTime()) / 1000))
}

/**
 * formatDiffTm : 초 단위 시간을 "N분 NN초" 형식으로 포맷
 * @param   value - 소요 시간(초)
 * @return  포맷된 문자열 (값이 없으면 빈 문자열)
 */
function formatDiffTm(value: number | null): string {
  if (value === null || value === undefined) return ''
  const minutes = Math.floor(value / 60)
  const seconds = String(value % 60).padStart(2, '0')
  return minutes > 0 ? `${minutes}분 ${seconds}초` : `${seconds}초`
}

/**
 * isRunning : 실행상태가 실행 중(시작중·실행중·중지중)인지 판별
 * @param   execStatCd - 실행상태코드
 * @return  실행 중 여부
 */
function isRunning(execStatCd: string | null | undefined): boolean {
  return !!execStatCd && RUNNING_EXEC_STAT_CDS.includes(execStatCd)
}

/**
 * statClass : 실행상태 표시 색상 CSS 클래스 반환
 * @param   execStatCd - 실행상태코드
 * @return  상태별 CSS 클래스명
 */
function statClass(execStatCd: string | null | undefined): string {
  if (execStatCd === 'COMPLETED') return 'stat-end'
  if (execStatCd === 'FAILED' || execStatCd === 'UNKNOWN') return 'stat-error'
  if (isRunning(execStatCd)) return 'stat-run'
  return 'stat-wait'
}

/**
 * toJobRow : 배치직무 응답을 그리드 행으로 변환 (코드명·일시 표시값 채움)
 * @param   job - 배치직무
 * @return  그리드 행
 */
function toJobRow(job: BatchJob): BatchJobRow {
  return {
    ...job,
    wrkMdulNm: codeName(wrkMdulOptions.value, job.wrkMdulCd),
    lastExecStatNm: codeName(EXEC_STAT_OPTIONS, job.lastExecStatCd),
    schedStatusNm: codeName(SCHED_STATUS_OPTIONS, job.schedStatus),
    lastExecStDttmText: formatDttm(job.lastExecStDttm),
    lastExecEndDttmText: formatDttm(job.lastExecEndDttm),
    nextFireDttmText: formatDttm(job.nextFireDttm),
    useYnNm: codeName(USE_YN_OPTIONS, job.useYn),
    lastDiffTm: diffSeconds(job.lastExecStDttm, job.lastExecEndDttm),
  }
}

/**
 * toExecutionRow : 실행내역 응답을 그리드 행으로 변환 (코드명·일시 표시값 채움)
 * @param   execution - 실행내역
 * @return  그리드 행
 */
function toExecutionRow(execution: BatchExecution): BatchExecutionRow {
  return {
    ...execution,
    execStatNm: codeName(EXEC_STAT_OPTIONS, execution.execStatCd),
    execClsNm: codeName(EXEC_CLS_OPTIONS, execution.execClsCd),
    execStDttmText: formatDttm(execution.execStDttm),
    execEndDttmText: formatDttm(execution.execEndDttm),
    diffTm: diffSeconds(execution.execStDttm, execution.execEndDttm),
  }
}

/**
 * focusField : 지정한 ID의 입력 필드에 포커스 이동
 * @param   id - 대상 엘리먼트 ID
 * @return  N/A
 */
function focusField(id: string): void {
  const el = document.getElementById(id)
  const target = (el?.matches('input, select, textarea') ? el : el?.querySelector('input, select, textarea')) as HTMLElement | null
  ;(target ?? el)?.focus()
}

/**
 * createEmptyForm : 배치 등록용 빈 상세 폼 생성
 * @return  기본값이 채워진 배치 상세 폼
 */
function createEmptyForm(): BatchJobForm {
  return {
    jobId: '',
    jobNm: '',
    execClassNm: '',
    wrkMdulCd: 'SY',
    scheExpr: '',
    msfrPlcyCd: '01',
    mltExecYn: 'N',
    jobParmCont: '',
    useYn: 'N',
    rmk: '',
  }
}

/**
 * toForm : 배치직무를 상세 폼 데이터로 변환
 * @param   job - 배치직무
 * @return  배치 상세 폼
 */
function toForm(job: BatchJob): BatchJobForm {
  return {
    jobId: job.jobId,
    jobNm: job.jobNm,
    execClassNm: job.execClassNm,
    wrkMdulCd: job.wrkMdulCd,
    scheExpr: job.scheExpr ?? '',
    msfrPlcyCd: job.msfrPlcyCd,
    mltExecYn: job.mltExecYn,
    jobParmCont: job.jobParmCont ?? '',
    useYn: job.useYn,
    rmk: job.rmk ?? '',
  }
}

/**
 * toBatchJobSave : 상세 폼과 알람수신자를 저장용 배치직무로 변환 (공백 정리, 빈 값은 null)
 * @param   form      - 배치 상세 폼
 * @param   rcvrRows  - 알람수신자 목록
 * @return  저장용 배치직무
 */
function toBatchJobSave(form: BatchJobForm, rcvrRows: BatchAlarmRcvr[]): BatchJobSave {
  return {
    ...form,
    alrmRcvrIdList: rcvrRows.map((row) => row.usrId),
    jobId: form.jobId.trim(),
    jobNm: form.jobNm.trim(),
    execClassNm: form.execClassNm.trim(),
    scheExpr: form.scheExpr.trim().replace(/\s+/g, ' ') || null,
    jobParmCont: form.jobParmCont.trim() || null,
    rmk: form.rmk.trim() || null,
  }
}

/**
 * checkJobId : 배치ID 형식 검증 (BH + 영문 대문자 2자리 + 숫자 3자리, 예: BHFL001)
 * @param   value - 배치ID 입력값
 * @return  오류 메시지 (올바르면 undefined)
 */
function checkJobId(value: unknown): string | undefined {
  return /^BH[A-Z]{2}[0-9]{3}$/.test(String(value).trim()) ? undefined : msg('sy.batch.validate.job.id')
}

/**
 * checkExecClassNm : 실행클래스명 형식 검증 (패키지를 포함한 Java 클래스 이름)
 * @param   value - 실행클래스명 입력값
 * @return  오류 메시지 (올바르면 undefined)
 */
function checkExecClassNm(value: unknown): string | undefined {
  return /^[A-Za-z_$][A-Za-z0-9_$]*([.][A-Za-z_$][A-Za-z0-9_$]*)+$/.test(String(value).trim())
    ? undefined
    : msg('sy.batch.validate.exec.class')
}

/**
 * checkScheExpr : 스케줄표현식 형식 검증 (Quartz cron: 초 분 시 일 월 주 [년] 6~7개)
 * @param   value - 스케줄표현식 입력값
 * @return  오류 메시지 (올바르면 undefined)
 */
function checkScheExpr(value: unknown): string | undefined {
  const tokens = String(value).trim().split(/\s+/)
  if ((tokens.length !== 6 && tokens.length !== 7) || tokens.some((token) => !/^[0-9A-Za-z*?/,#\-]+$/.test(token))) {
    return msg('sy.batch.validate.sche.plan.quartz')
  }
  return undefined
}

/**
 * checkJobParmCont : 기본 파라미터 형식 검증 (JSON 객체)
 * @param   value - 기본 파라미터 입력값
 * @return  오류 메시지 (올바르면 undefined)
 */
function checkJobParmCont(value: unknown): string | undefined {
  try {
    const parsed = JSON.parse(String(value))
    if (parsed !== null && typeof parsed === 'object' && !Array.isArray(parsed)) return undefined
  } catch {
    // 아래 오류 메시지 반환
  }
  return msg('sy.batch.validate.job.parm.json')
}

/**
 * validateDtlForm : 배치 상세 폼 필수값·형식 검증
 * @return  검증 통과 시 true
 */
async function validateDtlForm(): Promise<boolean> {
  const form = dtlForm.value
  return validateFields([
    { label: '배치ID', value: form.jobId, required: true, custom: checkJobId, focus: () => focusField('txtDtlJobId') },
    { label: '배치명', value: form.jobNm, required: true, maxBytes: 100, focus: () => focusField('txtDtlJobNm') },
    {
      label: '실행 클래스',
      value: form.execClassNm,
      required: true,
      maxBytes: 200,
      custom: checkExecClassNm,
      focus: () => focusField('txtDtlExecClassNm'),
    },
    { label: '업무모듈', value: form.wrkMdulCd, required: true, focus: () => focusField('cboDtlWrkMdulCd') },
    { label: '스케줄표현식', value: form.scheExpr, maxBytes: 100, custom: checkScheExpr, focus: () => focusField('txtDtlScheExpr') },
    { label: '기본 파라미터', value: form.jobParmCont, maxBytes: 4000, custom: checkJobParmCont, focus: () => focusField('txaDtlJobParmCont') },
    { label: '비고', value: form.rmk, maxBytes: 200, focus: () => focusField('txtDtlRmk') },
  ])
}

/**
 * toRunParmRows : 기본 파라미터(JSON)를 실행 파라미터 입력 행으로 변환 (없거나 형식이 다르면 빈 행 1개)
 * @param   jobParmCont - 배치직무 기본 파라미터 (JSON)
 * @return  실행 파라미터 입력 행 목록
 */
function toRunParmRows(jobParmCont: string | null): RunParmRow[] {
  try {
    const parsed = jobParmCont ? JSON.parse(jobParmCont) : null
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
      const rows = Object.entries(parsed).map(([key, value]) => ({ key, value: String(value ?? '') }))
      if (rows.length > 0) return rows
    }
  } catch {
    // 형식이 다르면 빈 행으로 시작
  }
  return [{ key: '', value: '' }]
}

/**
 * validateRunParms : 실행 파라미터 검증 (키 형식·예약 키·중복), 키와 값이 모두 빈 행은 제외
 * @return  실행 파라미터 맵 (검증 실패 시 null)
 */
async function validateRunParms(): Promise<Record<string, string> | null> {
  const params: Record<string, string> = {}
  for (const [idx, row] of runParmRows.value.entries()) {
    const key = row.key.trim()
    const value = row.value.trim()
    if (!key && !value) continue

    let message = ''
    if (!/^[A-Za-z][A-Za-z0-9_]*$/.test(key)) message = msg('sy.batch.validate.run.parm.key')
    else if (RESERVED_JOB_PARM_KEYS.includes(key)) message = msg('sy.batch.validate.run.parm.reserved', key)
    else if (key in params) message = msg('sy.batch.validate.run.parm.duplicate', key)
    if (message) {
      await alert({ message, icon: 'warning' })
      focusField(`txtRunParmKey${idx + 1}`)
      return null
    }
    params[key] = value
  }
  return params
}

// ---------------------------------------------------------------------
// 조회
// ---------------------------------------------------------------------
/**
 * searchJobList : 배치직무 목록 조회 (이전 선택 행 유지)
 * @return  N/A
 */
async function searchJobList(): Promise<void> {
  const prevActiveJobId = activeJobRow.value?.jobId ?? ''
  isJobLoading.value = true
  try {
    const res = await getBatchJobListApi({
      ...searchForm.value,
      keyword: searchForm.value.keyword.trim(),
      lastExecDt: dashToYmd(searchForm.value.lastExecDt),
    })
    jobRows.value = (res.data ?? []).map(toJobRow)
    renumber(jobRows.value)
    activeJobRow.value = jobRows.value.find((row) => row.jobId === prevActiveJobId) ?? null
  } finally {
    isJobLoading.value = false
  }
  if (jobRows.value.length === 0) {
    await alert({ message: msg('cm.info.no.data'), icon: 'information' })
  }
}

/**
 * searchExecutionList : 선택한 배치의 실행내역 조회
 * @return  N/A
 */
async function searchExecutionList(): Promise<void> {
  if (!execTarget.value) return
  const [stDt, endDt] = execSearchForm.value.prid ?? []
  if (!stDt || !endDt) {
    await alert({ message: msg('cm.validate.period.required'), icon: 'warning' })
    return
  }
  isExecLoading.value = true
  try {
    const res = await getBatchExecutionListApi({
      jobId: execTarget.value.jobId,
      stDt: dashToYmd(stDt),
      endDt: dashToYmd(endDt),
      execStatCd: execSearchForm.value.execStatCd,
    })
    execRows.value = (res.data?.list ?? []).map(toExecutionRow)
    isExecTruncated.value = res.data?.truncated ?? false
    renumber(execRows.value)
  } finally {
    isExecLoading.value = false
  }
}

/**
 * exportJobListToCsv : 배치직무 목록을 CSV 파일로 내보내기
 * @return  N/A
 */
function exportJobListToCsv(): void {
  const header = EXCEL_COLUMNS.map((col) => col.label)
  const lines = jobRows.value.map((row) =>
    EXCEL_COLUMNS.map((col) => {
      switch (col.key) {
        case 'lastDiffTm':
          return formatDiffTm(row.lastDiffTm)
        default:
          return String(row[col.key as keyof BatchJobRow] ?? '')
      }
    }),
  )
  downloadCsv(`배치관리_${todayYmd()}.csv`, header, lines)
}

/**
 * reloadDtlJob : 상세 팝업의 배치직무를 다시 조회 (스케줄 상태·최종 실행상태 갱신)
 * @return  N/A
 */
async function reloadDtlJob(): Promise<void> {
  const res = await getBatchJobApi(dtlForm.value.jobId)
  if (res.success && res.data) dtlJob.value = res.data
}

// ---------------------------------------------------------------------
// 팝업 열기
// ---------------------------------------------------------------------
/**
 * openExecutionPopup : 실행내역 팝업 열기 및 실행내역 조회
 * @param   row - 대상 배치 행
 * @return  N/A
 */
async function openExecutionPopup(row: BatchJobRow): Promise<void> {
  execTarget.value = row
  execSearchForm.value = { prid: [todayDash(-1), todayDash()], execStatCd: '' }
  execRows.value = []
  isExecTruncated.value = false
  isExecPopupOpen.value = true
  await searchExecutionList()
}

/**
 * openJobDtlPopup : 배치 등록/수정 팝업 열기
 * @param   row - 수정 대상 배치 행 (신규 등록이면 null)
 * @return  N/A
 */
async function openJobDtlPopup(row: BatchJobRow | null): Promise<void> {
  if (row) {
    const res = await getBatchJobApi(row.jobId)
    if (!res.success || !res.data) {
      await alert({ message: res.message ?? msg('sy.batch.not.found.retry'), icon: 'warning' })
      return
    }
    isNewJob.value = false
    dtlForm.value = toForm(res.data)
    dtlJob.value = res.data
    alrmRcvrRows.value = [...(res.data.alrmRcvrList ?? [])]
  } else {
    isNewJob.value = true
    dtlForm.value = createEmptyForm()
    dtlJob.value = null
    alrmRcvrRows.value = []
  }
  checkedAlrmRcvrIds.value = []
  isDtlPopupOpen.value = true
  await nextTick()
  focusField(isNewJob.value ? 'txtDtlJobId' : 'txtDtlJobNm')
}

// ---------------------------------------------------------------------
// 이벤트 핸들러
// ---------------------------------------------------------------------
/**
 * onBtnSrchClick : 조회 버튼 클릭
 * @return  N/A
 */
async function onBtnSrchClick(): Promise<void> {
  await searchJobList()
}

/**
 * onBtnJobRegClick : 배치등록 버튼 클릭: 배치 등록 팝업 열기
 * @return  N/A
 */
async function onBtnJobRegClick(): Promise<void> {
  await openJobDtlPopup(null)
}

/**
 * onBtnXlsDnldClick : 엑셀 버튼 클릭: 배치직무 목록 CSV 다운로드
 * @return  N/A
 */
async function onBtnXlsDnldClick(): Promise<void> {
  if (jobRows.value.length === 0) {
    await alert({ message: msg('cm.info.no.export.data'), icon: 'information' })
    return
  }
  exportJobListToCsv()
}

/**
 * onGrdJobListRowClick : 배치 그리드 행 클릭: 선택한 배치의 실행내역 팝업 표시
 * @param   row - 클릭한 배치 행
 * @return  N/A
 */
async function onGrdJobListRowClick(row: BatchJobRow): Promise<void> {
  activeJobRow.value = row
  await openExecutionPopup(row)
}

/**
 * onGrdJobListJobIdClick : 배치ID 클릭: 배치 설정 수정 팝업 열기
 * @param   row - 클릭한 배치 행
 * @return  N/A
 */
async function onGrdJobListJobIdClick(row: BatchJobRow): Promise<void> {
  activeJobRow.value = row
  await openJobDtlPopup(row)
}

/**
 * onGrdJobListExecClick : 수동실행 버튼 클릭: 실행 파라미터 입력 팝업 열기 (기본 파라미터로 채움)
 * @param   row - 대상 배치 행
 * @return  N/A
 */
async function onGrdJobListExecClick(row: BatchJobRow): Promise<void> {
  activeJobRow.value = row
  if (row.useYn !== 'Y') {
    await alert({ message: msg('sy.batch.unused.cannot.run'), icon: 'warning' })
    return
  }
  runTarget.value = row
  runParmRows.value = toRunParmRows(row.jobParmCont)
  isRunPopupOpen.value = true
  await nextTick()
  focusField('txtRunParmVal1')
}

/**
 * onBtnRunParmAddClick : 실행 파라미터 팝업의 행추가 버튼 클릭: 빈 파라미터 행 추가
 * @return  N/A
 */
async function onBtnRunParmAddClick(): Promise<void> {
  runParmRows.value.push({ key: '', value: '' })
  await nextTick()
  focusField(`txtRunParmKey${runParmRows.value.length}`)
}

/**
 * onBtnRunParmDelClick : 실행 파라미터 행의 삭제 버튼 클릭: 해당 행 삭제
 * @param   idx - 삭제할 행 인덱스
 * @return  N/A
 */
function onBtnRunParmDelClick(idx: number): void {
  runParmRows.value.splice(idx, 1)
}

/**
 * onBtnRunExecClick : 실행 파라미터 팝업의 실행 버튼 클릭: 파라미터 검증 후 배치 수동 실행 요청
 * @return  N/A
 */
async function onBtnRunExecClick(): Promise<void> {
  const target = runTarget.value
  if (!target) return
  const params = await validateRunParms()
  if (!params) return

  const res = await saveBatchJobExecutionApi(target.jobId, params)
  if (!res.success) {
    await alert({ message: res.message ?? msg('sy.batch.error.run.failed'), icon: 'error' })
    return
  }
  isRunPopupOpen.value = false
  await alert({ message: msg('sy.batch.info.run.requested'), icon: 'information' })
  await searchJobList()
}

/**
 * onBtnExecSrchClick : 실행내역 팝업의 조회 버튼 클릭
 * @return  N/A
 */
async function onBtnExecSrchClick(): Promise<void> {
  await searchExecutionList()
}

/**
 * onBtnDtlSaveClick : 배치 상세 팝업의 저장 버튼 클릭: 검증 후 배치직무 저장
 * @return  N/A
 */
async function onBtnDtlSaveClick(): Promise<void> {
  if (!(await validateDtlForm())) return
  const ok = await confirm({ message: msg('cm.confirm.save'), icon: 'question' })
  if (!ok) return

  const res = await saveBatchJobApi(toBatchJobSave(dtlForm.value, alrmRcvrRows.value), isNewJob.value)
  if (!res.success) {
    await alert({ message: res.message ?? msg('cm.error.save.failed'), icon: 'error' })
    if (res.code === 'sy.batch.job.duplicate') focusField('txtDtlJobId')
    if (res.code === 'sy.batch.alarm.receiver.not.found') focusField('btnAlrmRcvrAdd')
    if (res.code === 'sy.batch.job.bean.not.registered' || res.code === 'sy.batch.job.class.ambiguous') focusField('txtDtlExecClassNm')
    if (res.code === 'sy.batch.schedule.invalid.expression') focusField('txtDtlScheExpr')
    return
  }
  await alert({ message: msg('cm.info.saved'), icon: 'information' })
  isDtlPopupOpen.value = false
  await searchJobList()
  activeJobRow.value = jobRows.value.find((row) => row.jobId === dtlForm.value.jobId.trim()) ?? activeJobRow.value
}

/**
 * onBtnSchedPauseClick : 스케줄 일시정지/재개 버튼 클릭: 현재 스케줄 상태에 따라 일시정지 또는 재개
 * @return  N/A
 */
async function onBtnSchedPauseClick(): Promise<void> {
  const isPause = dtlSchedStatus.value !== 'PAUSED'
  const ok = await confirm({
    message: msg(isPause ? 'sy.batch.confirm.schedule.pause' : 'sy.batch.confirm.schedule.resume', dtlForm.value.jobId),
    icon: 'question',
  })
  if (!ok) return

  const res = await saveBatchJobScheduleApi(dtlForm.value.jobId, isPause ? 'PAUSED' : 'NORMAL')
  if (!res.success) {
    await alert({ message: res.message ?? msg('cm.error.save.failed'), icon: 'error' })
    return
  }
  await reloadDtlJob()
  await alert({ message: msg(isPause ? 'sy.batch.info.schedule.paused' : 'sy.batch.info.schedule.resumed'), icon: 'information' })
  await searchJobList()
}

/**
 * onBtnJobStatInitClick : 배치상태 초기화 버튼 클릭: 실행 중 상태로 멈춘 최종 실행 건을 폐기(ABANDONED)
 * @return  N/A
 */
async function onBtnJobStatInitClick(): Promise<void> {
  const dsptExecSn = dtlJob.value?.lastDsptExecSn
  if (!isDtlRunning.value || !dsptExecSn) {
    await alert({ message: msg('sy.batch.only.running.can.reset'), icon: 'warning' })
    return
  }
  const ok = await confirm({ message: msg('sy.batch.confirm.reset'), icon: 'warning' })
  if (!ok) return

  const res = await saveBatchExecutionAbandonApi(dsptExecSn)
  if (!res.success) {
    await alert({ message: res.message ?? msg('sy.batch.error.reset.failed'), icon: 'error' })
    return
  }
  await reloadDtlJob()
  await alert({ message: msg('sy.batch.info.reset'), icon: 'information' })
  await searchJobList()
}

/**
 * onBtnAlrmRcvrAddClick : 알람수신 추가 버튼 클릭: 사용자 선택 팝업 열기
 * @return  N/A
 */
function onBtnAlrmRcvrAddClick(): void {
  isAlrmRcvrPopupOpen.value = true
}

/**
 * onMdlAlrmRcvrSelect : 사용자 선택 팝업에서 선택: 선택한 사용자를 알람수신자 목록에 추가 (이미 있으면 제외)
 * @param   rows - 선택한 사용자 목록
 * @return  N/A
 */
function onMdlAlrmRcvrSelect(rows: SearchCodeRow[]): void {
  for (const usr of rows) {
    if (!alrmRcvrRows.value.some((rcvr) => rcvr.usrId === usr.CD)) {
      alrmRcvrRows.value.push({ usrId: usr.CD, usrNm: usr.NM, useYn: 'Y' })
    }
  }
}

/**
 * onBtnAlrmRcvrDelClick : 알람수신 삭제 버튼 클릭: 체크한 알람수신자를 목록에서 제외 (저장 시 반영)
 * @return  N/A
 */
async function onBtnAlrmRcvrDelClick(): Promise<void> {
  if (checkedAlrmRcvrIds.value.length === 0) {
    await alert({ message: msg('sy.batch.alarm.receiver.delete.select'), icon: 'warning' })
    return
  }
  const checkedIdSet = new Set(checkedAlrmRcvrIds.value)
  alrmRcvrRows.value = alrmRcvrRows.value.filter((rcvr) => !checkedIdSet.has(rcvr.usrId))
  checkedAlrmRcvrIds.value = []
}

/**
 * onChkAlrmRcvrAllChange : 알람수신자 전체 선택 체크박스 변경: 전체 선택 또는 해제
 * @param   event - 체크박스 변경 이벤트
 * @return  N/A
 */
function onChkAlrmRcvrAllChange(event: Event): void {
  checkedAlrmRcvrIds.value = (event.target as HTMLInputElement).checked ? alrmRcvrRows.value.map((rcvr) => rcvr.usrId) : []
}

/**
 * onChkDtlUseYnChange : 상세 팝업 사용여부 체크박스 변경
 * @param   event - 체크박스 변경 이벤트
 * @return  N/A
 */
function onChkDtlUseYnChange(event: Event): void {
  dtlForm.value.useYn = (event.target as HTMLInputElement).checked ? 'Y' : 'N'
}

/**
 * onChkDtlMltExecYnChange : 상세 팝업 다중실행여부 체크박스 변경
 * @param   event - 체크박스 변경 이벤트
 * @return  N/A
 */
function onChkDtlMltExecYnChange(event: Event): void {
  dtlForm.value.mltExecYn = (event.target as HTMLInputElement).checked ? 'Y' : 'N'
}

// ---------------------------------------------------------------------
// 페이지 라이프사이클
// ---------------------------------------------------------------------
/**
 * fetchPageData : 페이지 참조 데이터(업무모듈 공통코드) 조회
 * @return  N/A
 */
async function fetchPageData(): Promise<void> {
  wrkMdulOptions.value = await getCommCdOptions('SY', 'WRK_MDUL_CD')
}

/**
 * initPageUi : 조회 조건·그리드 초기화 및 최초 조회
 * @return  N/A
 */
function initPageUi(): void {
  searchForm.value = { keyword: '', wrkMdulCd: '', useYn: 'Y', lastExecDt: '', lastExecStatCd: '' }
  jobRows.value = []
  activeJobRow.value = null
  searchJobList()
}

onMounted(async () => {
  await fetchPageData()
  initPageUi()
})
</script>

<template>
  <section class="page-root">
    <header class="page-title-bar">
      <h2 class="page-title"><span class="page-plate">{{ PAGE_ID }}</span>{{ PAGE_TITLE }}</h2>
      <div class="page-actions">
        <Button id="btnJobReg" v-permission variant="outline" size="small" @click="onBtnJobRegClick">배치등록</Button>
        <Button id="btnXlsDnld" v-permission variant="outline" size="small" @click="onBtnXlsDnldClick">엑셀</Button>
      </div>
    </header>

    <!-- 조회 조건 영역 -->
    <form id="frmSrch" class="search-area" @submit.prevent="onBtnSrchClick">
      <div class="search-fields">
        <label class="search-field">
          <span class="field-label">배치ID/명</span>
          <Input id="txtKeyword" v-model="searchForm.keyword" @enter="onBtnSrchClick" />
        </label>
        <label class="search-field">
          <span class="field-label">업무모듈</span>
          <Select id="cboWrkMdulCd" v-model="searchForm.wrkMdulCd" :options="withAll(wrkMdulOptions)" />
        </label>
        <label class="search-field">
          <span class="field-label">사용여부</span>
          <Select id="cboUseYn" v-model="searchForm.useYn" :options="withAll(useYnOptions)" />
        </label>
        <label class="search-field">
          <span class="field-label">배치수행일</span>
          <DatePicker id="dtpLastExecDt" v-model="searchForm.lastExecDt" :presets="false" size="small" />
        </label>
        <label class="search-field">
          <span class="field-label">실행상태</span>
          <Select id="cboLastExecStatCd" v-model="searchForm.lastExecStatCd" :options="withAll(EXEC_STAT_OPTIONS)" />
        </label>
      </div>
      <div class="search-actions">
        <Button id="btnSrch" v-permission @click="onBtnSrchClick">조회</Button>
      </div>
    </form>

    <!-- 배치직무 (TB_DSPT_JOB_M) -->
    <div class="grid-area">
      <div class="grid-header">
        <span class="grid-title">배치 목록 - <strong id="lblTotCnt" class="total-count">{{ jobTotalCount }}건</strong></span>
        <span class="grid-guide">{{ msg('sy.batch.grid.guide') }}</span>
      </div>
      <DataTable
        id="grdJobList"
        :columns="jobColumns"
        :data="jobRows"
        :loading="isJobLoading"
        :active-row="activeJobRow"
        :page-size="20"
        dense
        @row-click="onGrdJobListRowClick"
      >
        <template #cell-jobId="{ row }">
          <a href="#" class="cell-link" @click.prevent.stop="onGrdJobListJobIdClick(row)">{{ row.jobId }}</a>
        </template>
        <template #cell-jobNm="{ row }">
          <span class="cell-link-text">{{ row.jobNm }}</span>
        </template>
        <template #cell-manlExec="{ row }">
          <button v-permission="'btnJobExec'" type="button" class="cell-button" @click.stop="onGrdJobListExecClick(row)">실행</button>
        </template>
        <template #cell-lastExecStatNm="{ row }">
          <span v-if="row.lastExecStatCd" class="stat-badge" :class="statClass(row.lastExecStatCd)">{{ row.lastExecStatNm }}</span>
        </template>
        <template #cell-lastDiffTm="{ row }">{{ formatDiffTm(row.lastDiffTm) }}</template>
        <template #cell-lastProcCnt="{ row }">{{ formatCount(row.lastProcCnt) }}</template>
        <template #cell-lastSuccCnt="{ row }">{{ formatCount(row.lastSuccCnt) }}</template>
        <template #cell-lastFailCnt="{ row }">
          <span :class="{ 'fail-count': (row.lastFailCnt ?? 0) > 0 }">{{ formatCount(row.lastFailCnt) }}</span>
        </template>
        <template #cell-lastErrMsgCont="{ row }">
          <span class="cell-ellipsis" :title="row.lastErrMsgCont ?? ''">{{ row.lastErrMsgCont }}</span>
        </template>
        <template #cell-execClassNm="{ row }">
          <span class="cell-ellipsis" :title="row.execClassNm">{{ row.execClassNm }}</span>
        </template>
        <template #cell-rmk="{ row }">
          <span class="cell-ellipsis" :title="row.rmk ?? ''">{{ row.rmk }}</span>
        </template>
      </DataTable>
    </div>

    <!-- 실행내역 팝업 (TB_DSPT_EXEC_L) -->
    <PopupWindow id="mdlExecList" v-model="isExecPopupOpen" :title="`배치 이력 - ${execTarget?.jobNm ?? ''}`" :width="980">
      <form id="frmExecSrch" class="search-area popup-search" @submit.prevent="onBtnExecSrchClick">
        <div class="search-fields">
          <label class="search-field">
            <span class="field-label">기간</span>
            <DatePicker id="dtpExecPrid" v-model="execSearchForm.prid" range :presets="false" size="small" />
          </label>
          <label class="search-field">
            <span class="field-label">실행상태</span>
            <Select id="cboExecStatCd" v-model="execSearchForm.execStatCd" :options="withAll(EXEC_STAT_OPTIONS)" />
          </label>
        </div>
        <div class="search-actions">
          <Button id="btnExecSrch" @click="onBtnExecSrchClick">조회</Button>
        </div>
      </form>
      <div class="grid-header">
        <span class="grid-title">
          배치 이력 <span class="sub-title">[{{ execTarget?.jobId }}]</span> -
          <strong id="lblExecTotCnt" class="total-count">{{ execRows.length }}건</strong>
          <span v-if="isExecTruncated" class="sub-title text-warn">{{ msg('cm.info.recent.rows.shown', EXEC_MAX_ROWS) }}</span>
        </span>
      </div>
      <DataTable
        id="grdExecList"
        :columns="execColumns"
        :data="execRows"
        :loading="isExecLoading"
        :page-size="10"
        :empty-text="msg('sy.batch.hist.empty')"
        dense
      >
        <template #cell-diffTm="{ row }">{{ formatDiffTm(row.diffTm) }}</template>
        <template #cell-execStatNm="{ row }">
          <span class="stat-badge" :class="statClass(row.execStatCd)">{{ row.execStatNm }}</span>
        </template>
        <template #cell-procCnt="{ row }">{{ formatCount(row.procCnt) }}</template>
        <template #cell-succCnt="{ row }">{{ formatCount(row.succCnt) }}</template>
        <template #cell-failCnt="{ row }">
          <span :class="{ 'fail-count': row.failCnt > 0 }">{{ formatCount(row.failCnt) }}</span>
        </template>
        <template #cell-errMsgCont="{ row }">
          <span class="cell-ellipsis" :title="row.errMsgCont ?? ''">{{ row.errMsgCont }}</span>
        </template>
      </DataTable>
    </PopupWindow>

    <!-- 실행 파라미터 입력 팝업 -->
    <PopupWindow id="mdlJobRun" v-model="isRunPopupOpen" :title="`배치 실행 - ${runTarget?.jobNm ?? ''}`" :width="600">
      <div class="dtl-toolbar">
        <span class="grid-title">
          실행 파라미터 <span class="sub-title">[{{ runTarget?.jobId }}]</span>
        </span>
        <div class="page-actions">
          <Button id="btnRunParmAdd" variant="outline" size="small" @click="onBtnRunParmAddClick">행추가</Button>
          <Button id="btnRunExec" variant="success" size="small" @click="onBtnRunExecClick">실행</Button>
        </div>
      </div>

      <table id="grdRunParm" class="run-parm-table">
        <thead>
          <tr>
            <th class="col-no">NO</th>
            <th>파라미터 키</th>
            <th>값</th>
            <th class="col-del">삭제</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="runParmRows.length === 0">
            <td colspan="4" class="run-parm-empty">{{ msg('sy.batch.run.parm.empty') }}</td>
          </tr>
          <tr v-for="(parm, idx) in runParmRows" :key="idx">
            <td class="col-no">{{ idx + 1 }}</td>
            <td>
              <input :id="`txtRunParmKey${idx + 1}`" v-model="parm.key" class="cell-input" maxlength="100" placeholder="baseDt" />
            </td>
            <td>
              <input
                :id="`txtRunParmVal${idx + 1}`"
                v-model="parm.value"
                class="cell-input"
                maxlength="1000"
                placeholder="20261017"
                @keydown.enter.prevent="onBtnRunExecClick"
              />
            </td>
            <td class="col-del">
              <button type="button" class="cell-button" @click="onBtnRunParmDelClick(idx)">삭제</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p class="field-hint run-parm-hint">
        {{ msg('sy.batch.run.parm.default.hint') }}
      </p>
    </PopupWindow>

    <!-- 배치 등록 / 수정 팝업 -->
    <PopupWindow id="mdlJobDtl" v-model="isDtlPopupOpen" :title="dtlPopupTitle" :width="900">
      <div class="dtl-toolbar">
        <span class="grid-title">
          배치 기본
          <span v-if="dtlJob?.lastExecStatCd" class="stat-badge" :class="statClass(dtlJob.lastExecStatCd)">
            {{ codeName(EXEC_STAT_OPTIONS, dtlJob.lastExecStatCd) }}
          </span>
        </span>
        <div class="page-actions">
          <Button
            v-if="!isNewJob"
            id="btnJobStatInit"
            variant="outline"
            size="small"
            :disabled="!isDtlRunning"
            @click="onBtnJobStatInitClick"
          >
            배치상태 초기화
          </Button>
          <Button
            v-if="!isNewJob"
            id="btnSchedPause"
            variant="outline"
            size="small"
            :disabled="dtlSchedStatus !== 'NORMAL' && dtlSchedStatus !== 'PAUSED'"
            @click="onBtnSchedPauseClick"
          >
            {{ dtlSchedStatus === 'PAUSED' ? '스케줄 재개' : '스케줄 일시정지' }}
          </Button>
          <Button id="btnDtlSave" variant="success" size="small" @click="onBtnDtlSaveClick">저장</Button>
        </div>
      </div>

      <div id="frmJobDtl" class="dtl-form">
        <div class="dtl-row">
          <span class="dtl-label">배치ID</span>
          <div class="dtl-value">
            <Input
              id="txtDtlJobId"
              v-model="dtlForm.jobId"
              required
              :readonly="!isNewJob"
              :disabled="!isNewJob"
              maxlength="7"
              placeholder="BHFL001"
            />
            <p v-if="isNewJob" class="field-hint">BH + 업무 2자리 + 순번 3자리</p>
          </div>
          <span class="dtl-label">업무모듈</span>
          <div class="dtl-value">
            <Select id="cboDtlWrkMdulCd" v-model="dtlForm.wrkMdulCd" required :options="wrkMdulOptions" />
          </div>
        </div>

        <div class="dtl-row">
          <span class="dtl-label">배치명</span>
          <div class="dtl-value is-wide">
            <Input id="txtDtlJobNm" v-model="dtlForm.jobNm" required maxlength="100" />
          </div>
        </div>

        <div class="dtl-row">
          <span class="dtl-label">실행 클래스</span>
          <div class="dtl-value is-wide">
            <Input
              id="txtDtlExecClassNm"
              v-model="dtlForm.execClassNm"
              required
              maxlength="200"
              placeholder="kr.or.coder.batch.job.system.userstatusapply.UserStatusApplyJobConfig"
            />
            <p class="field-hint">Job 빈을 정의한 클래스 (패키지 포함)</p>
          </div>
        </div>

        <div class="dtl-row">
          <span class="dtl-label">스케줄표현식</span>
          <div class="dtl-value">
            <Input id="txtDtlScheExpr" v-model="dtlForm.scheExpr" maxlength="100" placeholder="0 10 0 * * ?" />
            <p class="field-hint">초 분 시 일 월 주 [년], 비우면 수동 실행 전용</p>
          </div>
          <span class="dtl-label">미스파이어</span>
          <div id="rdoDtlMsfrPlcyCd" class="dtl-value radio-group">
            <label v-for="option in MSFR_PLCY_OPTIONS" :key="option.value" class="radio-item">
              <input v-model="dtlForm.msfrPlcyCd" type="radio" name="msfrPlcyCd" :value="option.value" />
              {{ option.label }}
            </label>
          </div>
        </div>

        <div v-if="!isNewJob" class="dtl-row">
          <span class="dtl-label">스케줄상태</span>
          <div id="lblDtlSchedStatus" class="dtl-value dtl-text">{{ codeName(SCHED_STATUS_OPTIONS, dtlSchedStatus) }}</div>
          <span class="dtl-label">다음실행일시</span>
          <div id="lblDtlNextFireDttm" class="dtl-value dtl-text">{{ formatDttm(dtlJob?.nextFireDttm) }}</div>
        </div>

        <div class="dtl-row">
          <span class="dtl-label">다중실행</span>
          <label class="dtl-value check-item">
            <input id="chkDtlMltExecYn" type="checkbox" :checked="dtlForm.mltExecYn === 'Y'" @change="onChkDtlMltExecYnChange" />
            허용
          </label>
          <span class="dtl-label">사용여부</span>
          <label class="dtl-value check-item">
            <input id="chkDtlUseYn" type="checkbox" :checked="dtlForm.useYn === 'Y'" @change="onChkDtlUseYnChange" />
            사용
          </label>
        </div>

        <div class="dtl-row">
          <span class="dtl-label">기본 파라미터</span>
          <div class="dtl-value is-wide">
            <textarea
              id="txaDtlJobParmCont"
              v-model="dtlForm.jobParmCont"
              class="dtl-textarea"
              rows="3"
              maxlength="4000"
              placeholder='{"baseDt":"20261017"}'
            ></textarea>
            <p class="field-hint">JSON 형식, 수동 실행·스케줄 실행 시 JobParameters로 전달</p>
          </div>
        </div>

        <div class="dtl-row">
          <span class="dtl-label">비고</span>
          <div class="dtl-value is-wide">
            <Input id="txtDtlRmk" v-model="dtlForm.rmk" maxlength="200" />
          </div>
        </div>
      </div>

      <!-- 알람수신: 실행이 실패하면 등록된 사용자에게 알림 센터 알람 (TB_DSPT_ALRM_RCVR_R) -->
      <div class="dtl-toolbar dtl-section">
        <span class="grid-title">
          알람수신
          <span class="sub-title">총 <span class="total-count">{{ alrmRcvrRows.length }}</span>명 · 실행 실패 시 알림</span>
        </span>
        <div class="page-actions">
          <Button id="btnAlrmRcvrAdd" variant="outline" size="small" @click="onBtnAlrmRcvrAddClick">추가</Button>
          <Button id="btnAlrmRcvrDel" variant="outline" size="small" @click="onBtnAlrmRcvrDelClick">삭제</Button>
        </div>
      </div>

      <!-- 알람수신자 선택 팝업 (사용자 코드서치, 다건 선택) -->
      <ComSearchCodePopup id="mdlAlrmRcvr" v-model="isAlrmRcvrPopupOpen" type="USR" multiple @select="onMdlAlrmRcvrSelect" />

      <table id="grdAlrmRcvr" class="run-parm-table">
        <thead>
          <tr>
            <th class="col-chk">
              <input
                id="chkAlrmRcvrAll"
                type="checkbox"
                title="전체 선택"
                :checked="isAllAlrmRcvrChecked"
                :disabled="alrmRcvrRows.length === 0"
                @change="onChkAlrmRcvrAllChange"
              />
            </th>
            <th class="col-no">NO</th>
            <th>사용자ID</th>
            <th>사용자명</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="alrmRcvrRows.length === 0">
            <td colspan="4" class="run-parm-empty">{{ msg('sy.batch.alarm.receiver.empty') }}</td>
          </tr>
          <tr v-for="(rcvr, idx) in alrmRcvrRows" :key="rcvr.usrId">
            <td class="col-chk">
              <input :id="`chkAlrmRcvr${idx + 1}`" v-model="checkedAlrmRcvrIds" type="checkbox" :value="rcvr.usrId" />
            </td>
            <td class="col-no">{{ idx + 1 }}</td>
            <td>{{ rcvr.usrId }}</td>
            <td>
              {{ rcvr.usrNm }}
              <span v-if="rcvr.useYn === 'N'" class="stat-badge stat-wait">미사용</span>
            </td>
          </tr>
        </tbody>
      </table>
      <p class="field-hint run-parm-hint">{{ msg('sy.batch.alarm.receiver.save.hint') }}</p>
    </PopupWindow>
  </section>
</template>

<style scoped src="@/assets/page.css"></style>

<style scoped>
.grid-header {
  gap: normal;
}

.grid-guide, .sub-title {
  color: var(--ds-ink-2);
}

.cell-link-text {
  text-decoration: underline;
  cursor: pointer;
}

/* 그리드 셀 버튼·상태 배지 */
.cell-button {
  padding: 2px 10px;
  font: inherit;
  font-size: 12px;
  font-weight: 600;
  color: var(--ds-ink);
  background: var(--ds-surface);
  border: 1px solid var(--ds-rule-strong);
  border-radius: var(--ds-radius-control);
  cursor: pointer;
  transition: background-color var(--ds-ease), border-color var(--ds-ease), color var(--ds-ease);
}

.cell-button:hover {
  color: var(--ds-slate-900);
  background: var(--ds-slate-050);
  border-color: var(--ds-slate-700);
}

.cell-button:focus-visible {
  outline: none;
  box-shadow: var(--ds-focus);
}

/* 컬럼 폭을 유지하고 일시·이름이 두 줄로 나뉘지 않게 한다 */
:deep(#grdJobList td), :deep(#grdJobList th), :deep(#grdExecList td), :deep(#grdExecList th) {
  white-space: nowrap;
}

.cell-ellipsis {
  max-width: 220px;
}

.fail-count {
  font-weight: 600;
  color: var(--ds-stamp);
  font-variant-numeric: tabular-nums;
}

.stat-badge {
  display: inline-block;
  min-width: 40px;
  padding: 0 7px;
  font-size: 12px;
  font-weight: 600;
  line-height: 20px;
  text-align: center;
  white-space: nowrap;
  border: 1px solid currentColor;
  border-radius: 2px;
}

.stat-end { color: var(--ds-slate-700); background: var(--ds-slate-100); }
.stat-error { color: var(--ds-stamp);    background: var(--ds-stamp-tint); }
.stat-run { color: var(--ds-slate-500); background: var(--ds-slate-050); }
.stat-wait { color: var(--ds-ink-3);    background: var(--ds-surface-muted); }

.dtl-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

/* 배치 기본 아래 두 번째 영역 (알람수신) */
.dtl-section {
  margin-top: 18px;
}

.dtl-row {
  grid-template-columns: 110px minmax(0, 1fr) 100px minmax(0, 1fr);
}

.dtl-value {
  font-size: inherit;
}

.dtl-text {
  font-size: 13px;
  color: var(--ds-ink);
}

.radio-group, .check-item {
  flex-direction: row;
  align-items: center;
  justify-content: flex-start;
  gap: 14px;
  font-size: 13px;
}

.radio-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}

/* 실행 파라미터 팝업 */
.run-parm-table {
  width: 100%;
  border-collapse: collapse;
  border-top: 1px solid var(--ds-rule-strong);
  font-size: 13px;
}

.run-parm-table th {
  padding: 6px 8px;
  font-weight: 600;
  text-align: left;
  color: var(--ds-ink-2);
  background: var(--ds-slate-050);
  border-bottom: 1px solid var(--ds-rule);
}

.run-parm-table td {
  padding: 4px 8px;
  border-bottom: 1px solid var(--ds-rule);
}

.run-parm-table .col-no {
  width: 44px;
  text-align: center;
}

.run-parm-table .col-chk {
  width: 36px;
  text-align: center;
}

.run-parm-table .col-del {
  width: 64px;
  text-align: center;
}

.run-parm-empty {
  padding: 12px 8px;
  text-align: center;
  color: var(--ds-ink-3);
}

.run-parm-hint {
  margin-top: 8px;
}

.cell-input {
  width: 100%;
}
</style>
