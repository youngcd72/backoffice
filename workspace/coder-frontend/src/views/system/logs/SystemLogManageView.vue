<script setup lang="ts">
/*****************************************************************************************
	Program ID   : SYLO030
	Program Type : vue
	Program Name : 시스템로그관리
	Description  : 시스템 에러 로그(coder-backend 에러 로그 파일)를 조건별로 조회하고 상세(스택트레이스)를 확인한다.
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { computed, ref, onMounted } from 'vue'
import { Button, Input, Select, DataTable, DatePicker, PopupWindow, useDialog } from '@/components/common'
import { msg } from '@/utils/message'
import {
  MAX_PERIOD_DAYS,
  MAX_ROWS,
  getSystemLogApi,
  getSystemLogListApi,
  type SystemLog,
} from '@/api/sy/systemLog'
import { DEPT_OPTIONS } from '@/api/sy/usrAuthMng'
import { getCommCdOptions } from '@/api/common/commonCode'
import { codeName, withAll, type CodeOption } from '@/utils/code'
import { dashToYmd, todayDash, todayYmd } from '@/utils/date'
import { downloadCsv } from '@/utils/csv'

interface LogRow extends SystemLog {
  no?: number
}

const PAGE_TITLE = '시스템로그관리'
const PAGE_ID = 'SYLO030'

const columns = [
  { key: 'no', label: 'NO', width: '48px', align: 'center' },
  { key: 'logDttm', label: '로그일시', width: '170px', align: 'center', sortable: true },
  { key: 'sysmClsCd', label: '시스템유형', width: '80px', align: 'center' },
  { key: 'usrId', label: '사용자ID', width: '100px', sortable: true },
  { key: 'usrNm', label: '사용자명', width: '120px' },
  { key: 'usrClsCd', label: '사용자유형', width: '84px', align: 'center' },
  { key: 'deptCd', label: '부서명', width: '110px' },
]

/** 엑셀 컬럼: 그리드 컬럼 + 에러내용 (코드는 명칭으로 변환) */
const EXCEL_COLUMNS = [...columns.filter((col) => col.key !== 'no'), { key: 'errMsg', label: '에러내용' }]

const { alert } = useDialog()

/** 시스템유형 (SY/SYSM_CLS_CD), 사용자유형 (SY/USR_CLS_CD) 공통코드 */
const sysmClsCdOptions = ref<CodeOption[]>([])
const usrClsCdOptions = ref<CodeOption[]>([])
const sysmClsOptions = computed(() => withAll(sysmClsCdOptions.value))
const usrClsOptions = computed(() => withAll(usrClsCdOptions.value))

const searchForm = ref({
  prid: [todayDash(-1), todayDash()] as string[],
  sysmClsCd: '',
  usrClsCd: '',
  usrId: '',
  usrNm: '',
})
const rows = ref<LogRow[]>([])
const totalCount = ref(0)
const isTruncated = ref(false)
const isLoading = ref(false)

// 선택 행 (오른쪽 에러내용 표시)
const activeRow = ref<LogRow | null>(null)

// 상세 팝업
const isDtlPopupOpen = ref(false)
const dtlLog = ref<SystemLog | null>(null)

/**
 * getCellText : 그리드·엑셀에 표시할 값을 반환한다. (코드 컬럼은 명칭으로 변환)
 * @param   row - 시스템로그 행
 * @param   key - 컬럼 키
 * @return  표시 문자열
 */
function getCellText(row: SystemLog, key: string): string {
  if (key === 'sysmClsCd') return codeName(sysmClsCdOptions.value, row.sysmClsCd)
  if (key === 'usrClsCd') return codeName(usrClsCdOptions.value, row.usrClsCd)
  if (key === 'deptCd') return codeName(DEPT_OPTIONS, row.deptCd)
  return String(row[key as keyof SystemLog] ?? '')
}

/**
 * validateSearch : 조회 기간 필수 입력 및 최대 조회기간을 검증한다.
 * @return  검증 통과 여부
 */
async function validateSearch(): Promise<boolean> {
  const [stDt, endDt] = searchForm.value.prid ?? []
  if (!stDt || !endDt) {
    await alert({ message: msg('cm.validate.period.required'), icon: 'warning' })
    return false
  }
  const days = (new Date(endDt).getTime() - new Date(stDt).getTime()) / 86_400_000
  if (days > MAX_PERIOD_DAYS - 1) {
    await alert({ message: msg('cm.validate.period.max', MAX_PERIOD_DAYS), icon: 'warning' })
    return false
  }
  return true
}

/**
 * searchList : 검색 조건으로 시스템로그 목록을 조회하고 결과 건수를 안내한다.
 * @return  N/A
 */
async function searchList(): Promise<void> {
  if (!(await validateSearch())) return
  const { prid, ...rest } = searchForm.value
  isLoading.value = true
  try {
    const res = await getSystemLogListApi({ ...rest, stDt: dashToYmd(prid[0]), endDt: dashToYmd(prid[1]) })
    rows.value = res.data.list.map((log, idx) => ({ ...log, no: idx + 1 }))
    activeRow.value = rows.value[0] ?? null
    totalCount.value = res.data.totalCount
    isTruncated.value = res.data.truncated
  } finally {
    isLoading.value = false
  }
  if (rows.value.length === 0) {
    await alert({ message: msg('cm.info.no.data'), icon: 'information' })
  } else if (isTruncated.value) {
    await alert({ message: msg('sy.system.log.info.truncated', totalCount.value.toLocaleString(), MAX_ROWS), icon: 'warning' })
  }
}

/**
 * openDetail : 시스템로그 상세(스택트레이스 포함)를 조회해 팝업으로 연다.
 * @param   log - 상세를 볼 시스템로그
 * @return  N/A
 */
async function openDetail(log: SystemLog): Promise<void> {
  dtlLog.value = (await getSystemLogApi(log.logKey)).data
  isDtlPopupOpen.value = true
}

// ---------------------------------------------------------------------
// 이벤트 핸들러
// ---------------------------------------------------------------------
/**
 * onBtnSrchClick : 조회 버튼 클릭 시 시스템로그 목록을 조회한다.
 * @return  N/A
 */
async function onBtnSrchClick(): Promise<void> {
  await searchList()
}

/**
 * onGrdSysmLogListRowClick : 시스템로그 그리드 행 클릭 시 오른쪽에 에러내용을 표시한다.
 * @param   row - 클릭한 행
 * @return  N/A
 */
function onGrdSysmLogListRowClick(row: LogRow): void {
  activeRow.value = row
}

/**
 * onGrdSysmLogListRowDblclick : 시스템로그 그리드 행 더블클릭 시 상세 팝업을 연다.
 * @param   row - 더블클릭한 행
 * @return  N/A
 */
async function onGrdSysmLogListRowDblclick(row: LogRow): Promise<void> {
  await openDetail(row)
}

/**
 * onBtnXlsDnldClick : 엑셀 다운로드 버튼 클릭 시 조회 결과를 CSV로 내려받는다. (스택트레이스 제외)
 * @return  N/A
 */
async function onBtnXlsDnldClick(): Promise<void> {
  if (rows.value.length === 0) {
    await alert({ message: msg('cm.info.no.export.data'), icon: 'information' })
    return
  }
  downloadCsv(
    `시스템로그_${todayYmd()}.csv`,
    EXCEL_COLUMNS.map((col) => col.label),
    rows.value.map((row) => EXCEL_COLUMNS.map((col) => getCellText(row, col.key))),
  )
}

// ---------------------------------------------------------------------
// 페이지 라이프사이클
// ---------------------------------------------------------------------
/**
 * fetchPageData : 화면에서 사용할 참조 데이터를 조회한다. (공통코드 API 연동 전이라 현재는 없음)
 * @return  N/A
 */
async function fetchPageData(): Promise<void> {
  const [sysmClsCds, usrClsCds] = await Promise.all([getCommCdOptions('SY', 'SYSM_CLS_CD'), getCommCdOptions('SY', 'USR_CLS_CD')])
  sysmClsCdOptions.value = sysmClsCds
  usrClsCdOptions.value = usrClsCds
}

/**
 * initPageUi : 화면 요소를 초기화하고 최초 목록을 조회한다.
 * @return  N/A
 */
function initPageUi(): void {
  searchList()
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
        <Button id="btnXlsDnld" v-permission variant="outline" size="small" @click="onBtnXlsDnldClick">엑셀</Button>
      </div>
    </header>

    <form id="frmSrch" class="search-area" @submit.prevent="onBtnSrchClick">
      <div class="search-fields">
        <label class="search-field">
          <span class="field-label">기간</span>
          <DatePicker id="dtpLogPrid" v-model="searchForm.prid" range required :presets="false" :clearable="false" size="small" />
        </label>
        <label class="search-field">
          <span class="field-label">사용자유형</span>
          <Select id="cboUsrClsCd" v-model="searchForm.usrClsCd" :options="usrClsOptions" />
        </label>
        <div class="search-field">
          <span class="field-label">ID/사용자명</span>
          <Input id="txtUsrId" v-model="searchForm.usrId" placeholder="사용자ID" @enter="onBtnSrchClick" />
          <Input id="txtUsrNm" v-model="searchForm.usrNm" placeholder="사용자명" @enter="onBtnSrchClick" />
        </div>
        <label class="search-field">
          <span class="field-label">시스템유형</span>
          <Select id="cboSysmClsCd" v-model="searchForm.sysmClsCd" :options="sysmClsOptions" />
        </label>
      </div>
      <div class="search-actions">
        <Button id="btnSrch" v-permission @click="onBtnSrchClick">조회</Button>
      </div>
    </form>

    <div class="content-area">
      <div class="grid-area">
        <div class="grid-header">
          <span class="grid-title">
            시스템 로그 - <strong id="lblTotCnt" class="total-count">{{ totalCount.toLocaleString() }}건</strong>
            <span v-if="isTruncated" class="sub-title text-warn">{{ msg('cm.info.recent.rows.shown', MAX_ROWS) }}</span>
          </span>
          <span class="grid-guide">{{ msg('sy.system.log.guide.grid') }}</span>
        </div>
        <DataTable
          id="grdSysmLogList"
          class="nowrap-grid"
          :columns="columns"
          :data="rows"
          :loading="isLoading"
          :page-size="20"
          :active-row="activeRow"
          dense
          @row-click="onGrdSysmLogListRowClick"
          @row-dblclick="onGrdSysmLogListRowDblclick"
        >
          <template #cell-sysmClsCd="{ row }">{{ getCellText(row, 'sysmClsCd') }}</template>
          <template #cell-usrClsCd="{ row }">{{ getCellText(row, 'usrClsCd') }}</template>
          <template #cell-deptCd="{ row }">{{ getCellText(row, 'deptCd') }}</template>
        </DataTable>
      </div>

      <!-- 오른쪽: 선택한 로그의 에러내용 -->
      <div class="detail-area panel">
        <div class="grid-header">
          <span class="grid-title">에러내용</span>
        </div>
        <div class="detail-form">
          <textarea
            id="txaErrMsg"
            class="detail-textarea"
            :value="activeRow?.errMsg ?? ''"
            placeholder="로그를 선택하면 에러내용이 표시됩니다."
            rows="16"
            readonly
          />
        </div>
      </div>
    </div>

    <!-- 시스템로그 상세 -->
    <PopupWindow id="mdlSysmLogDtl" v-model="isDtlPopupOpen" title="시스템 로그 상세" :width="960">
      <template v-if="dtlLog">
        <div class="dtl-form">
          <div class="dtl-row">
            <span class="dtl-label">로그일시</span>
            <div class="dtl-value">{{ dtlLog.logDttm }}</div>
            <span class="dtl-label">시스템유형</span>
            <div class="dtl-value">{{ getCellText(dtlLog, 'sysmClsCd') }}</div>
          </div>
          <div class="dtl-row">
            <span class="dtl-label">사용자</span>
            <div class="dtl-value">{{ dtlLog.usrId ? `${dtlLog.usrId} ${dtlLog.usrNm ?? ''}` : '' }}</div>
            <span class="dtl-label">사용자유형 / 부서</span>
            <div class="dtl-value">{{ getCellText(dtlLog, 'usrClsCd') }} / {{ getCellText(dtlLog, 'deptCd') }}</div>
          </div>
          <div class="dtl-row">
            <span class="dtl-label">요청</span>
            <div class="dtl-value is-wide is-text mono">{{ dtlLog.method }} {{ dtlLog.uri }}</div>
          </div>
          <div class="dtl-row">
            <span class="dtl-label">추적ID</span>
            <div class="dtl-value mono">{{ dtlLog.traceId }}</div>
            <span class="dtl-label">에러코드</span>
            <div class="dtl-value mono">{{ dtlLog.errCd }}</div>
          </div>
          <div class="dtl-row">
            <span class="dtl-label">SQL_ID</span>
            <div class="dtl-value is-wide mono">{{ dtlLog.sqlId }}</div>
          </div>
          <div class="dtl-row">
            <span class="dtl-label">로거 / 스레드</span>
            <div class="dtl-value is-wide is-text mono">{{ dtlLog.logger }} / {{ dtlLog.thread }}</div>
          </div>
          <div class="dtl-row">
            <span class="dtl-label">에러내용</span>
            <div class="dtl-value is-wide is-text text-err">{{ dtlLog.errMsg }}</div>
          </div>
          <div class="dtl-row">
            <span class="dtl-label">스택트레이스</span>
            <div class="dtl-value is-wide">
              <pre v-if="dtlLog.stackTrace" id="lblStackTrace" class="code-block">{{ dtlLog.stackTrace }}</pre>
              <span v-else class="sub-title">-</span>
            </div>
          </div>
        </div>
      </template>
    </PopupWindow>
  </section>
</template>

<style scoped src="@/assets/page.css"></style>

<style scoped>
/* 왼쪽 그리드(컬럼 폭 합계만큼) + 오른쪽 에러내용(나머지 폭) */
.content-area {
  display: grid;
  grid-template-columns: minmax(0, 740px) minmax(360px, 1fr);
  gap: 16px;
  align-items: start;
}

.detail-textarea {
  display: block;
  width: 100%;
  padding: 8px 10px;
  font: inherit;
  font-size: 13px;
  line-height: 1.6;
  color: var(--ds-ink);
  background: var(--ds-surface);
  border: 1px solid var(--ds-rule-strong);
  border-radius: var(--ds-radius-control);
  box-sizing: border-box;
  resize: vertical;
}

.detail-textarea:focus {
  outline: none;
  border-color: var(--ds-slate-700);
  box-shadow: var(--ds-focus-field);
}
</style>
