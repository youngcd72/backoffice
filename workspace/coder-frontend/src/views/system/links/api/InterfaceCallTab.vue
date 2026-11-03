<script setup lang="ts">
/*****************************************************************************************
	Program ID   : InterfaceCallTab
	Program Type : vue
	Program Name : API관리 - 호출이력 탭
	Description  : SYLK010 ③ 호출이력 탭. 인터페이스 호출내역(TB_INFC_CALL_L) 조회와 요청·응답 본문(마스킹) 상세
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-11-03  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { ref } from 'vue'
import { Button, Input, Select, DataTable, DatePicker, PopupWindow, useDialog, useToast } from '@/components/common'
import { msg } from '@/utils/message'
import {
  CALL_MAX_DAYS,
  CALL_MAX_ROWS,
  CALL_RSLT_OPTIONS,
  getInterfaceCallApi,
  getInterfaceCallListApi,
  getInterfaceListApi,
  getLinkOrgListApi,
  type InterfaceCall,
} from '@/api/sy/link'
import { withAll, type CodeOption } from '@/utils/code'
import { dashToYmd, todayDash } from '@/utils/date'
import { formatCount } from '@/utils/format'
import { renumber } from '@/utils/rowFlag'

/** 호출이력 그리드 행 (표시용 값 포함) */
interface InterfaceCallRow extends InterfaceCall {
  no?: number
  rqmtDttmText: string
}

const DAY_MS = 86400000

const callColumns = [
  { key: 'no', label: 'NO', width: '48px', align: 'center' },
  { key: 'rqmtDttmText', label: '요청일시', width: '170px', align: 'center' },
  { key: 'lnkOrgNm', label: '연계기관', width: '120px' },
  { key: 'infcNm', label: '인터페이스', width: '150px' },
  { key: 'httpMethNm', label: '메서드', width: '60px', align: 'center' },
  { key: 'url', label: 'URL', width: '200px' },
  { key: 'httpStatCd', label: 'HTTP상태', width: '72px', align: 'center' },
  { key: 'elapsedMs', label: '소요(ms)', width: '76px', align: 'right' },
  { key: 'errCd', label: '오류코드', width: '72px', align: 'center' },
  { key: 'clntIpAddr', label: 'IP', width: '120px' },
  { key: 'keyId', label: 'Client ID', width: '130px' },
  { key: 'trcId', label: '추적ID', width: '170px' },
]

/** 기간 프리셋 (오늘 / 최근 7일 / 최근 31일) */
const PRID_PRESETS = [
  { label: '오늘', value: () => [new Date(), new Date()] },
  { label: '최근 7일', value: () => [new Date(Date.now() - 6 * DAY_MS), new Date()] },
  { label: `최근 ${CALL_MAX_DAYS}일`, value: () => [new Date(Date.now() - (CALL_MAX_DAYS - 1) * DAY_MS), new Date()] },
]

const { alert } = useDialog()
const toast = useToast()

// 조회 조건 옵션 (연계기관·인터페이스, 사용여부 무관)
const orgOptions = ref<CodeOption[]>([])
const infcOptions = ref<CodeOption[]>([])

// 조회 조건
const searchForm = ref(createDefaultSearch())

// 호출이력
const callRows = ref<InterfaceCallRow[]>([])
const isCallLoading = ref(false)
const isCallTruncated = ref(false)

// 호출 상세 팝업
const isCallDtlOpen = ref(false)
const callDtl = ref<InterfaceCall | null>(null)

// ---------------------------------------------------------------------
// 공통 함수
// ---------------------------------------------------------------------
/**
 * createDefaultSearch : 기본 조회 조건 생성 (기간 오늘)
 * @return  조회 조건
 */
function createDefaultSearch(): { prid: string[]; lnkOrgId: string; infcId: string; rsltCd: string; trcId: string } {
  return { prid: [todayDash(), todayDash()], lnkOrgId: '', infcId: '', rsltCd: '', trcId: '' }
}

/**
 * formatDttmMs : ISO 일시를 YYYY-MM-DD HH:mm:ss.SSS 형식으로 변환
 * @param   dttm - ISO 일시 문자열
 * @return  밀리초까지 표시한 일시 (값이 없으면 빈 문자열)
 */
function formatDttmMs(dttm: string | null | undefined): string {
  return dttm ? dttm.replace('T', ' ').slice(0, 23) : ''
}

/**
 * httpStatBadgeClass : HTTP 상태코드 배지 CSS 클래스 반환
 * @param   httpStatCd - HTTP 상태코드
 * @return  배지 CSS 클래스명 (2xx 성공, 4xx 경고, 5xx 오류)
 */
function httpStatBadgeClass(httpStatCd: string): string {
  if (httpStatCd.startsWith('2')) return 'badge-ok'
  if (httpStatCd.startsWith('4')) return 'badge-warn'
  return 'badge-err'
}

/**
 * formatBody : 본문이 JSON이면 들여쓰기해 표시
 * @param   body - 요청·응답 본문
 * @return  표시할 본문 (없으면 빈 문자열)
 */
function formatBody(body: string | null | undefined): string {
  if (!body) return ''
  try {
    return JSON.stringify(JSON.parse(body), null, 2)
  } catch {
    return body
  }
}

/**
 * periodDays : 기간(YYYY-MM-DD 범위)의 일수 계산 (시작·종료일 포함)
 * @param   stDt  - 시작일 (YYYY-MM-DD)
 * @param   endDt - 종료일 (YYYY-MM-DD)
 * @return  일수
 */
function periodDays(stDt: string, endDt: string): number {
  return Math.round((new Date(endDt).getTime() - new Date(stDt).getTime()) / DAY_MS) + 1
}

// ---------------------------------------------------------------------
// 조회
// ---------------------------------------------------------------------
/**
 * reloadOptions : 연계기관·인터페이스 조회 조건 옵션 조회 (다른 탭에서 저장한 뒤에도 호출)
 * @return  N/A
 */
async function reloadOptions(): Promise<void> {
  const [orgRes, infcRes] = await Promise.all([
    getLinkOrgListApi({ keyword: '', chnlClsCd: '', useYn: '' }),
    getInterfaceListApi({ keyword: '', useYn: '' }),
  ])
  orgOptions.value = (orgRes.data ?? []).map((org) => ({ label: `${org.lnkOrgNm} (${org.lnkOrgId})`, value: org.lnkOrgId }))
  infcOptions.value = (infcRes.data ?? []).map((infc) => ({ label: `${infc.infcNm} (${infc.infcId})`, value: infc.infcId }))
}

/**
 * searchCallList : 호출이력 조회 (기간 필수, 최대 31일, 최근 500건)
 * @return  N/A
 */
async function searchCallList(): Promise<void> {
  const [stDt, endDt] = searchForm.value.prid ?? []
  if (!stDt || !endDt) {
    await alert({ message: msg('cm.validate.period.required'), icon: 'warning' })
    return
  }
  if (periodDays(stDt, endDt) > CALL_MAX_DAYS) {
    await alert({ message: msg('cm.validate.period.max', CALL_MAX_DAYS), icon: 'warning' })
    return
  }
  isCallLoading.value = true
  try {
    const res = await getInterfaceCallListApi({
      stDt: dashToYmd(stDt),
      endDt: dashToYmd(endDt),
      lnkOrgId: searchForm.value.lnkOrgId,
      infcId: searchForm.value.infcId,
      rsltCd: searchForm.value.rsltCd,
      trcId: searchForm.value.trcId.trim(),
      size: CALL_MAX_ROWS,
    })
    if (!res.success) {
      await alert({ message: res.message ?? msg('cm.error.entity.not.found'), icon: 'warning' })
      return
    }
    callRows.value = (res.data?.list ?? []).map((call) => ({ ...call, rqmtDttmText: formatDttmMs(call.rqmtDttm) }))
    isCallTruncated.value = res.data?.truncated ?? false
    renumber(callRows.value)
  } finally {
    isCallLoading.value = false
  }
}

/**
 * searchByOrg : 연계기관 탭의 호출이력 보기: 연계기관 조건을 지정하고 오늘 기간으로 조회
 * @param   lnkOrgId - 연계기관ID
 * @return  N/A
 */
async function searchByOrg(lnkOrgId: string): Promise<void> {
  searchForm.value = { ...createDefaultSearch(), lnkOrgId }
  await searchCallList()
}

// ---------------------------------------------------------------------
// 이벤트 핸들러
// ---------------------------------------------------------------------
/**
 * onBtnCallSrchClick : 조회 버튼 클릭
 * @return  N/A
 */
async function onBtnCallSrchClick(): Promise<void> {
  await searchCallList()
}

/**
 * onGrdCallListRowClick : 호출이력 행 클릭: 호출 상세 팝업 열기 (요청·응답 본문 조회)
 * @param   row - 클릭한 호출이력 행
 * @return  N/A
 */
async function onGrdCallListRowClick(row: InterfaceCallRow): Promise<void> {
  const res = await getInterfaceCallApi(row.callSn, row.rqmtDttm)
  if (!res.success || !res.data) {
    await alert({ message: res.message ?? msg('cm.error.entity.not.found'), icon: 'warning' })
    return
  }
  callDtl.value = res.data
  isCallDtlOpen.value = true
}

/**
 * onBtnCallDtlTrcIdCopyClick : 호출 상세 팝업의 추적ID 복사 버튼 클릭
 * @return  N/A
 */
async function onBtnCallDtlTrcIdCopyClick(): Promise<void> {
  if (!callDtl.value) return
  await navigator.clipboard.writeText(callDtl.value.trcId)
  toast.success(msg('cm.info.copied'))
}

// ---------------------------------------------------------------------
// 탭 초기화 (화면에서 처음 활성화될 때 호출)
// ---------------------------------------------------------------------
/**
 * initTab : 조회 조건 옵션을 조회하고 오늘 기간으로 최초 조회
 * @return  N/A
 */
async function initTab(): Promise<void> {
  searchForm.value = createDefaultSearch()
  await reloadOptions()
  await searchCallList()
}

defineExpose({ initTab, searchByOrg, reloadOptions })
</script>

<template>
  <div class="tab-root">
    <!-- 조회 조건 영역 -->
    <form id="frmCallSrch" class="search-area" @submit.prevent="onBtnCallSrchClick">
      <div class="search-fields">
        <label class="search-field">
          <span class="field-label">기간</span>
          <DatePicker id="dtpCallPrid" v-model="searchForm.prid" range required :presets="PRID_PRESETS" size="small" />
        </label>
        <label class="search-field">
          <span class="field-label">연계기관</span>
          <Select id="cboCallLnkOrgId" v-model="searchForm.lnkOrgId" :options="withAll(orgOptions)" />
        </label>
        <label class="search-field">
          <span class="field-label">인터페이스</span>
          <Select id="cboCallInfcId" v-model="searchForm.infcId" :options="withAll(infcOptions)" />
        </label>
        <label class="search-field">
          <span class="field-label">결과</span>
          <Select id="cboCallRsltCd" v-model="searchForm.rsltCd" :options="withAll(CALL_RSLT_OPTIONS)" />
        </label>
        <label class="search-field">
          <span class="field-label">추적ID</span>
          <Input id="txtCallTrcId" v-model="searchForm.trcId" placeholder="X-Trace-Id" @enter="onBtnCallSrchClick" />
        </label>
      </div>
      <div class="search-actions">
        <Button id="btnCallSrch" v-permission @click="onBtnCallSrchClick">조회</Button>
      </div>
    </form>

    <!-- 호출이력 (TB_INFC_CALL_L) -->
    <div class="grid-area">
      <div class="grid-header">
        <span class="grid-title">
          호출이력 - <strong id="lblCallTotCnt" class="total-count">{{ callRows.length }}건</strong>
          <span v-if="isCallTruncated" class="sub-title text-warn">{{ msg('cm.info.recent.rows.shown', CALL_MAX_ROWS) }}</span>
        </span>
        <span class="grid-guide">{{ msg('sy.link.call.grid.guide') }}</span>
      </div>
      <DataTable
        id="grdCallList"
        :columns="callColumns"
        :data="callRows"
        :loading="isCallLoading"
        :page-size="20"
        :empty-text="msg('cm.info.no.data')"
        class="nowrap-grid"
        dense
        @row-click="onGrdCallListRowClick"
      >
        <template #cell-lnkOrgNm="{ row }">
          <span v-if="row.lnkOrgNm" class="cell-ellipsis" :title="row.lnkOrgId ?? ''">{{ row.lnkOrgNm }}</span>
          <span v-else class="sub-title">-</span>
        </template>
        <template #cell-infcNm="{ row }">
          <span class="cell-ellipsis" :title="row.infcId">{{ row.infcNm ?? row.infcId }}</span>
        </template>
        <template #cell-url="{ row }">
          <span class="cell-ellipsis mono" :title="row.url">{{ row.url }}</span>
        </template>
        <template #cell-httpStatCd="{ row }">
          <span class="badge" :class="httpStatBadgeClass(row.httpStatCd)">{{ row.httpStatCd }}</span>
        </template>
        <template #cell-elapsedMs="{ row }">
          <span :class="{ 'text-warn': row.elapsedMs >= 1000 }">{{ formatCount(row.elapsedMs) }}</span>
        </template>
        <template #cell-keyId="{ row }">
          <span class="cell-ellipsis mono" :title="row.keyId ?? ''">{{ row.keyId }}</span>
        </template>
        <template #cell-trcId="{ row }">
          <span class="cell-ellipsis mono" :title="row.trcId">{{ row.trcId }}</span>
        </template>
      </DataTable>
    </div>

    <!-- 호출 상세 팝업 -->
    <PopupWindow id="mdlCallDtl" v-model="isCallDtlOpen" :title="`호출 상세 - ${callDtl?.trcId ?? ''}`" :width="900">
      <template v-if="callDtl">
        <div class="dtl-form">
          <div class="dtl-row">
            <span class="dtl-label">추적ID</span>
            <div class="dtl-value is-wide dtl-inline">
              <span class="mono">{{ callDtl.trcId }}</span>
              <button type="button" class="icon-button" title="추적ID 복사" @click="onBtnCallDtlTrcIdCopyClick">복사</button>
            </div>
          </div>
          <div class="dtl-row">
            <span class="dtl-label">요청일시</span>
            <div class="dtl-value is-text">{{ formatDttmMs(callDtl.rqmtDttm) }}</div>
            <span class="dtl-label">응답일시</span>
            <div class="dtl-value is-text">{{ formatDttmMs(callDtl.rspnDttm) }} ({{ formatCount(callDtl.elapsedMs) }}ms)</div>
          </div>
          <div class="dtl-row">
            <span class="dtl-label">HTTP상태</span>
            <div class="dtl-value is-text dtl-inline">
              <span class="badge" :class="httpStatBadgeClass(callDtl.httpStatCd)">{{ callDtl.httpStatCd }}</span>
              <span v-if="callDtl.errCd" class="text-err">{{ callDtl.errCd }}</span>
            </div>
            <span class="dtl-label">IP</span>
            <div class="dtl-value is-text mono">{{ callDtl.clntIpAddr }}</div>
          </div>
          <div class="dtl-row">
            <span class="dtl-label">연계기관</span>
            <div class="dtl-value is-text">{{ callDtl.lnkOrgNm ? `${callDtl.lnkOrgNm} (${callDtl.lnkOrgId})` : '- (인증 실패)' }}</div>
            <span class="dtl-label">Client ID</span>
            <div class="dtl-value is-text mono">{{ callDtl.keyId ?? '-' }}</div>
          </div>
          <div class="dtl-row">
            <span class="dtl-label">인터페이스</span>
            <div class="dtl-value is-wide is-text">
              {{ callDtl.infcNm }} ({{ callDtl.infcId }}) · <span class="mono">{{ callDtl.httpMethNm }} {{ callDtl.url }}</span>
            </div>
          </div>
        </div>

        <div class="body-split">
          <div>
            <div class="grid-title body-title">요청 본문</div>
            <pre v-if="callDtl.rqmtCont" id="txaCallDtlRqmtCont" class="code-block body-block">{{ formatBody(callDtl.rqmtCont) }}</pre>
            <p v-else class="empty-panel body-empty">{{ msg('sy.link.call.body.not.logged') }}</p>
          </div>
          <div>
            <div class="grid-title body-title">응답 본문</div>
            <pre v-if="callDtl.rspnCont" id="txaCallDtlRspnCont" class="code-block body-block">{{ formatBody(callDtl.rspnCont) }}</pre>
            <p v-else class="empty-panel body-empty">{{ msg('sy.link.call.body.not.logged') }}</p>
          </div>
        </div>
      </template>
    </PopupWindow>
  </div>
</template>

<style scoped src="@/assets/page.css"></style>

<style scoped>
.tab-root {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.grid-header {
  gap: normal;
}

.grid-guide, .sub-title {
  color: var(--ds-ink-2);
}


.dtl-inline {
  flex-direction: row;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
}

.icon-button {
  padding: 1px 8px;
  font: inherit;
  font-size: 12px;
  color: var(--ds-ink-2);
  background: var(--ds-surface);
  border: 1px solid var(--ds-rule-strong);
  border-radius: var(--ds-radius-control);
  cursor: pointer;
}

.icon-button:hover {
  color: var(--ds-slate-900);
  border-color: var(--ds-slate-700);
}

.body-split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 16px;
}

.body-title {
  margin-bottom: 6px;
}

.body-block {
  max-height: 280px;
}

.body-empty {
  padding: 24px 12px;
}
</style>
