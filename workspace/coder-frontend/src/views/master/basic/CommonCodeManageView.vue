<script setup lang="ts">
/*****************************************************************************************
	Program ID   : MSBS010
	Program Type : vue
	Program Name : 공통코드관리
	Description  : 공통코드 분류(TB_COMM_CD_M)와 분류별 코드(TB_COMM_CD_D)를 조회·등록·수정한다.
	               (설계서 MSBS010 공통코드관리) 코드는 삭제하지 않고 사용여부로 미사용 처리한다.
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-12  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { ref, computed, onMounted } from 'vue'
import { msg } from '@/utils/message'
import { Button, Input, Select, DataTable, useDialog } from '@/components/common'
import { useGridSave } from '@/composables/useGridSave'
import {
  COMM_PROP_CONT_KEYS,
  COMM_PROP_NM_KEYS,
  MAX_COMM_CD_DNUM,
  createCommCd,
  createCommCdDtl,
  getCommCdDtlListApi,
  getCommCdListApi,
  saveCommCdApi,
  type CommCd,
  type CommCdDtl,
  type CommCdDtlSave,
} from '@/api/sy/commonCode'
import { codeName, withAll, type CodeOption } from '@/utils/code'
import { getCommCdOptions } from '@/api/common/commonCode'
import { deleteFlaggedRows, hasRowChanges, markRowUpdated, nextRowId, renumber, splitRowsByFlag, type RowFlag } from '@/utils/rowFlag'

interface CtgRow extends CommCd {
  id: number
  no?: number
  _flag: RowFlag
}

interface DtlRow extends CommCdDtl {
  id: number
  no?: number
  _flag: RowFlag
}

/** 분류 1건의 코드목록 편집 상태 */
interface DtlState {
  rows: DtlRow[]
}

const PAGE_TITLE = '공통코드관리'
const PAGE_ID = 'MSBS010'

/** 분류코드는 영문 대문자·숫자만 사용 */
const CTG_CD_PATTERN = /^[A-Z0-9_]+$/

const ctgColumns = [
  { key: 'no', label: 'NO', width: '44px', align: 'center' },
  { key: 'wrkMdulCd', label: '업무모듈', width: '120px' },
  { key: 'commCtgCd', label: '분류코드', width: '130px' },
  { key: 'commCtgNm', label: '분류명', width: '160px' },
  { key: 'rmk', label: '비고', width: '180px' },
  ...COMM_PROP_NM_KEYS.map((key, idx) => ({ key, label: `속성${idx + 1}`, width: '110px' })),
  { key: 'usrMngeYn', label: '사용자관리여부', width: '96px', align: 'center' },
  { key: 'useYn', label: '사용여부', width: '68px', align: 'center' },
  { key: 'alinSeq', label: '정렬순서', width: '76px' },
]

const { alert, confirm } = useDialog()
const { saveChanges, confirmDiscardChanges } = useGridSave()

/** 업무모듈 공통코드 (SY/WRK_MDUL_CD) */
const wrkMdulCdOptions = ref<CodeOption[]>([])
const wrkMdulOptions = computed(() => withAll(wrkMdulCdOptions.value))

// 조회 조건
const searchForm = ref({ wrkMdulCd: '', commCtgCd: '', commCtgNm: '' })

// 분류목록 그리드
const ctgRows = ref<CtgRow[]>([])
const selectedCtgRows = ref<CtgRow[]>([])
const activeCtgRow = ref<CtgRow | null>(null)
const isCtgLoading = ref(false)

// 코드목록 그리드: 분류 행(id)별 편집 상태
const dtlStateMap = ref(new Map<number, DtlState>())
const selectedDtlRows = ref<DtlRow[]>([])
const isDtlLoading = ref(false)


const activeDtlState = computed(() => (activeCtgRow.value ? dtlStateMap.value.get(activeCtgRow.value.id) : undefined))

const dtlRows = computed(() => activeDtlState.value?.rows ?? [])

const isActiveCtgDeleted = computed(() => activeCtgRow.value?._flag === 'D')

/** 코드목록 컬럼: 속성 헤더는 선택한 분류의 속성명을 사용 (없으면 속성n) */
const dtlColumns = computed(() => [
  { key: 'no', label: 'NO', width: '44px', align: 'center' },
  { key: 'commCtgDtlCd', label: '상세코드', width: '110px' },
  { key: 'commCtgDtlNm', label: '상세명', width: '160px' },
  { key: 'rmk', label: '비고', width: '180px' },
  ...COMM_PROP_CONT_KEYS.map((key, idx) => ({
    key,
    label: activeCtgRow.value?.[COMM_PROP_NM_KEYS[idx]]?.trim() || `속성${idx + 1}`,
    width: '110px',
  })),
  { key: 'useYn', label: '사용여부', width: '68px', align: 'center' },
  { key: 'alinSeq', label: '정렬순서', width: '76px' },
])

const hasChanges = computed(
  () => hasRowChanges(ctgRows.value) || [...dtlStateMap.value.values()].some((state) => hasRowChanges(state.rows)),
)

/**
 * toNumberOrNull : 숫자 입력값을 정수 또는 null로 변환
 * @param   value - 입력값
 * @return  정수 (빈 값·숫자가 아니면 null)
 */
function toNumberOrNull(value: string): number | null {
  const num = Number.parseInt(value, 10)
  return Number.isNaN(num) ? null : num
}

/**
 * setRowNumber : 행의 숫자 필드를 변경하고 수정 표시
 * @param   row   - 분류 또는 코드 행
 * @param   key   - 숫자 필드 (정렬순서)
 * @param   event - 입력 이벤트
 * @return  N/A
 */
function setRowNumber<T extends { _flag: RowFlag }>(row: T, key: keyof T, event: Event): void {
  ;(row[key] as number | null) = toNumberOrNull((event.target as HTMLInputElement).value)
  markRowUpdated(row)
}

/**
 * setRowYn : 행의 여부(Y/N) 필드를 체크박스 값으로 변경하고 수정 표시
 * @param   row   - 분류 또는 코드 행
 * @param   key   - 여부 필드 (사용여부/사용자관리여부)
 * @param   event - 체크박스 변경 이벤트
 * @return  N/A
 */
function setRowYn<T extends { _flag: RowFlag }>(row: T, key: keyof T, event: Event): void {
  ;(row[key] as string) = (event.target as HTMLInputElement).checked ? 'Y' : 'N'
  markRowUpdated(row)
}

/**
 * loadDtlState : 분류의 코드목록 편집 상태를 준비 (이미 불러온 분류는 편집 내용 유지)
 * @param   ctgRow - 분류 행
 * @return  코드목록 편집 상태
 */
async function loadDtlState(ctgRow: CtgRow): Promise<DtlState> {
  const cached = dtlStateMap.value.get(ctgRow.id)
  if (cached) return cached
  const state: DtlState = { rows: [] }
  if (ctgRow._flag !== 'I') {
    isDtlLoading.value = true
    try {
      const res = await getCommCdDtlListApi(ctgRow.wrkMdulCd, ctgRow.commCtgCd)
      state.rows = res.data.map((dtl, idx) => ({ ...dtl, id: nextRowId(), no: idx + 1, _flag: '' as RowFlag }))
    } finally {
      isDtlLoading.value = false
    }
  }
  dtlStateMap.value.set(ctgRow.id, state)
  return state
}

/**
 * activateCtgRow : 분류 행을 선택하고 코드목록 표시
 * @param   row - 선택할 분류 행 (없으면 선택 해제)
 * @return  N/A
 */
async function activateCtgRow(row: CtgRow | null): Promise<void> {
  activeCtgRow.value = row
  selectedDtlRows.value = []
  if (row) await loadDtlState(row)
}

/**
 * searchCtgList : 검색 조건으로 분류 목록을 조회하고 첫 번째 분류의 코드목록을 표시한다.
 * @param   keepKey - 조회 후 다시 선택할 분류 (업무모듈코드·분류코드, 없으면 첫 번째 분류)
 * @return  N/A
 */
async function searchCtgList(keepKey?: { wrkMdulCd: string; commCtgCd: string }): Promise<void> {
  isCtgLoading.value = true
  try {
    const res = await getCommCdListApi(searchForm.value)
    ctgRows.value = res.data.map((ctg, idx) => ({ ...ctg, id: nextRowId(), no: idx + 1, _flag: '' as RowFlag }))
  } finally {
    isCtgLoading.value = false
  }
  dtlStateMap.value = new Map()
  selectedCtgRows.value = []
  const keepRow = keepKey
    ? ctgRows.value.find((row) => row.wrkMdulCd === keepKey.wrkMdulCd && row.commCtgCd === keepKey.commCtgCd)
    : undefined
  await activateCtgRow(keepRow ?? ctgRows.value[0] ?? null)
  if (ctgRows.value.length === 0) {
    await alert({ message: msg('cm.info.no.data'), icon: 'information' })
  }
}

/**
 * validateCtgRows : 분류 필수값·형식·중복 검증
 * @return  오류 메시지 (문제 없으면 빈 문자열)
 */
function validateCtgRows(): string {
  const keyCnt = new Map<string, number>()
  ctgRows.value.forEach((row) => {
    if (row._flag === 'D') return
    const key = `${row.wrkMdulCd}/${row.commCtgCd.trim()}`
    keyCnt.set(key, (keyCnt.get(key) ?? 0) + 1)
  })
  for (const row of ctgRows.value) {
    if (row._flag === '' || row._flag === 'D') continue
    const rowLabel = msg('sy.common.code.row.label', row.no)
    const ctgCd = row.commCtgCd.trim()
    if (!row.wrkMdulCd) return msg('sy.common.code.validate.module.required', rowLabel)
    if (!ctgCd) return msg('sy.common.code.validate.category.required', rowLabel)
    if (!CTG_CD_PATTERN.test(ctgCd)) return msg('sy.common.code.validate.category.format', rowLabel)
    if ((keyCnt.get(`${row.wrkMdulCd}/${ctgCd}`) ?? 0) > 1) return msg('sy.common.code.validate.category.duplicate', rowLabel, ctgCd)
    if (!row.commCtgNm.trim()) return msg('sy.common.code.validate.category.name.required', rowLabel)
  }
  return ''
}

/**
 * validateDtlRows : 분류별 코드 필수값·중복 검증
 * @return  오류 메시지 (문제 없으면 빈 문자열)
 */
function validateDtlRows(): string {
  for (const ctgRow of ctgRows.value) {
    const state = dtlStateMap.value.get(ctgRow.id)
    if (ctgRow._flag === 'D' || !state || !hasRowChanges(state.rows)) continue
    const ctgCd = ctgRow.commCtgCd.trim()
    const dtlCdCnt = new Map<string, number>()
    state.rows.forEach((row) => {
      const dtlCd = row.commCtgDtlCd.trim()
      if (dtlCd) dtlCdCnt.set(dtlCd, (dtlCdCnt.get(dtlCd) ?? 0) + 1)
    })
    for (const row of state.rows) {
      if (row._flag === '') continue
      const rowLabel = msg('sy.common.code.dtl.row.label', ctgCd, row.no)
      const dtlCd = row.commCtgDtlCd.trim()
      if (!dtlCd) return msg('sy.common.code.validate.code.required', rowLabel)
      if ((dtlCdCnt.get(dtlCd) ?? 0) > 1) return msg('sy.common.code.validate.code.duplicate', rowLabel, dtlCd)
      if (!row.commCtgDtlNm.trim()) return msg('sy.common.code.validate.code.name.required', rowLabel)
    }
  }
  return ''
}

/**
 * toCommCd : 분류 행을 저장용 공통코드 분류로 변환
 * @param   row - 분류 행
 * @return  저장용 공통코드 분류
 */
function toCommCd(row: CtgRow): CommCd {
  const { id: _id, no: _no, _flag: _rowFlag, ...ctg } = row
  return { ...ctg, commCtgCd: ctg.commCtgCd.trim(), commCtgNm: ctg.commCtgNm.trim() }
}

/**
 * toCommCdDtl : 코드 행을 저장용 공통코드 상세로 변환 (분류 키는 현재 분류 값으로 채움)
 * @param   row    - 코드 행
 * @param   ctgRow - 코드가 속한 분류 행
 * @return  저장용 공통코드 상세
 */
function toCommCdDtl(row: DtlRow, ctgRow: CtgRow): CommCdDtl {
  const { id: _id, no: _no, _flag: _rowFlag, ...dtl } = row
  return {
    ...dtl,
    wrkMdulCd: ctgRow.wrkMdulCd,
    commCtgCd: ctgRow.commCtgCd.trim(),
    commCtgDtlCd: dtl.commCtgDtlCd.trim(),
    commCtgDtlNm: dtl.commCtgDtlNm.trim(),
  }
}

/**
 * buildDtlSaveList : 분류별 코드 등록/수정분 생성 (삭제 표시된 분류는 제외)
 * @return  분류별 코드 변경분 목록
 */
function buildDtlSaveList(): CommCdDtlSave[] {
  return ctgRows.value.flatMap((ctgRow) => {
    const state = dtlStateMap.value.get(ctgRow.id)
    if (ctgRow._flag === 'D' || !state || !hasRowChanges(state.rows)) return []
    return [
      {
        wrkMdulCd: ctgRow.wrkMdulCd,
        commCtgCd: ctgRow.commCtgCd.trim(),
        insertList: state.rows.filter((row) => row._flag === 'I').map((row) => toCommCdDtl(row, ctgRow)),
        updateList: state.rows.filter((row) => row._flag === 'U').map((row) => toCommCdDtl(row, ctgRow)),
      },
    ]
  })
}

/**
 * alertIfCtgLocked : 분류 미선택 또는 삭제 표시된 분류이면 안내 후 변경 불가 판단
 * @return  변경할 수 없으면 true
 */
async function alertIfCtgLocked(): Promise<boolean> {
  if (!activeCtgRow.value || !activeDtlState.value) {
    await alert({ message: msg('sy.common.code.select.required'), icon: 'warning' })
    return true
  }
  if (isActiveCtgDeleted.value) {
    await alert({ message: msg('sy.common.code.deleted.readonly'), icon: 'warning' })
    return true
  }
  return false
}

// ---------------------------------------------------------------------
// 이벤트 핸들러
// ---------------------------------------------------------------------
/**
 * onBtnSrchClick : 조회 버튼 클릭 시 분류 목록을 조회한다.
 * @return  N/A
 */
async function onBtnSrchClick(): Promise<void> {
  if (!(await confirmDiscardChanges(hasChanges.value))) return
  await searchCtgList()
}

/**
 * onGrdCommCtgListRowClick : 분류목록 그리드 행 클릭 시 해당 분류의 코드목록을 표시한다.
 * @param   row - 클릭한 분류 행
 * @return  N/A
 */
async function onGrdCommCtgListRowClick(row: CtgRow): Promise<void> {
  if (row !== activeCtgRow.value) await activateCtgRow(row)
}

/**
 * onBtnNewClick : 신규 버튼 클릭 시 분류목록 맨 위에 새 분류 행을 추가한다.
 * @return  N/A
 */
async function onBtnNewClick(): Promise<void> {
  ctgRows.value.unshift({
    ...createCommCd(searchForm.value.wrkMdulCd || (wrkMdulCdOptions.value[0]?.value ?? '')),
    id: nextRowId(),
    _flag: 'I',
  })
  renumber(ctgRows.value)
  await activateCtgRow(ctgRows.value[0])
}

/**
 * onBtnDelClick : 삭제 버튼 클릭 시 선택한 분류를 삭제 표시한다. (코드가 등록된 분류는 삭제 불가)
 * @return  N/A
 */
async function onBtnDelClick(): Promise<void> {
  if (selectedCtgRows.value.length === 0) {
    await alert({ message: msg('sy.common.code.delete.select'), icon: 'warning' })
    return
  }
  for (const row of selectedCtgRows.value) {
    if (row._flag === 'I') continue
    const state = await loadDtlState(row)
    if (state.rows.some((dtl) => dtl._flag !== 'I')) {
      await alert({ message: msg('sy.common.code.delete.has.code', row.commCtgCd), icon: 'warning' })
      return
    }
  }
  const ok = await confirm({ message: msg('sy.common.code.delete.confirm'), icon: 'question' })
  if (!ok) return
  ctgRows.value = deleteFlaggedRows(ctgRows.value, selectedCtgRows.value)
  const remainIds = new Set(ctgRows.value.map((row) => row.id))
  ;[...dtlStateMap.value.keys()].filter((id) => !remainIds.has(id)).forEach((id) => dtlStateMap.value.delete(id))
  selectedCtgRows.value = []
  renumber(ctgRows.value)
  if (activeCtgRow.value && !remainIds.has(activeCtgRow.value.id)) {
    await activateCtgRow(ctgRows.value[0] ?? null)
  }
}

/**
 * onBtnDtlRowAddClick : 코드목록 행추가 버튼 클릭 시 선택한 분류에 새 코드 행을 추가한다.
 * @return  N/A
 */
async function onBtnDtlRowAddClick(): Promise<void> {
  if (await alertIfCtgLocked()) return
  const ctgRow = activeCtgRow.value!
  const state = activeDtlState.value!
  state.rows.push({ ...createCommCdDtl(ctgRow.wrkMdulCd, ctgRow.commCtgCd), id: nextRowId(), _flag: 'I' })
  renumber(state.rows)
}

/**
 * onBtnDtlRowDelClick : 코드목록 행삭제 버튼 클릭 시 새로 추가한 코드 행을 제거한다. (저장된 코드는 삭제 불가)
 * @return  N/A
 */
async function onBtnDtlRowDelClick(): Promise<void> {
  const state = activeDtlState.value
  if (!state || selectedDtlRows.value.length === 0) {
    await alert({ message: msg('sy.common.code.dtl.delete.select'), icon: 'warning' })
    return
  }
  if (selectedDtlRows.value.some((row) => row._flag !== 'I')) {
    await alert({ message: msg('sy.common.code.dtl.delete.saved'), icon: 'warning' })
    return
  }
  state.rows = deleteFlaggedRows(state.rows, selectedDtlRows.value)
  selectedDtlRows.value = []
  renumber(state.rows)
}

/**
 * onBtnSaveClick : 저장 버튼 클릭 시 검증 후 분류·코드 변경분을 저장하고 다시 조회한다.
 * @return  N/A
 */
async function onBtnSaveClick(): Promise<void> {
  // 저장 후 재조회 시 보고 있던 분류를 다시 선택한다
  const keepRow = activeCtgRow.value && activeCtgRow.value._flag !== 'D' ? toCommCd(activeCtgRow.value) : undefined
  await saveChanges({
    hasChanges: hasChanges.value,
    validate: () => validateCtgRows() || validateDtlRows(),
    save: () => saveCommCdApi({ ...splitRowsByFlag(ctgRows.value, toCommCd), dtlSaveList: buildDtlSaveList() }),
    afterSave: () => searchCtgList(keepRow),
  })
}

// ---------------------------------------------------------------------
// 페이지 라이프사이클
// ---------------------------------------------------------------------
/**
 * fetchPageData : 화면에서 사용할 참조 데이터(업무모듈 공통코드)를 조회한다.
 * @return  N/A
 */
async function fetchPageData(): Promise<void> {
  wrkMdulCdOptions.value = await getCommCdOptions('SY', 'WRK_MDUL_CD')
}

/**
 * initPageUi : 화면 요소를 초기화하고 최초 목록을 조회한다.
 * @return  N/A
 */
function initPageUi(): void {
  searchCtgList()
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
        <Button id="btnNew" v-permission variant="outline" size="small" @click="onBtnNewClick">신규</Button>
        <Button id="btnSave" v-permission variant="outline" size="small" @click="onBtnSaveClick">저장</Button>
        <Button id="btnDel" v-permission variant="outline" size="small" @click="onBtnDelClick">삭제</Button>
      </div>
    </header>

    <form id="frmSrch" class="search-area" @submit.prevent="onBtnSrchClick">
      <div class="search-fields">
        <label class="search-field">
          <span class="field-label">업무모듈</span>
          <Select id="cboWrkMdulCd" v-model="searchForm.wrkMdulCd" :options="wrkMdulOptions" />
        </label>
        <label class="search-field">
          <span class="field-label">분류코드</span>
          <Input id="txtCommCtgCd" v-model="searchForm.commCtgCd" placeholder="분류코드" @enter="onBtnSrchClick" />
        </label>
        <label class="search-field">
          <span class="field-label">분류명</span>
          <Input id="txtCommCtgNm" v-model="searchForm.commCtgNm" placeholder="분류명" @enter="onBtnSrchClick" />
        </label>
      </div>
      <div class="search-actions">
        <Button id="btnSrch" v-permission @click="onBtnSrchClick">조회</Button>
      </div>
    </form>

    <!-- 분류목록 (TB_COMM_CD_M) -->
    <div class="grid-area">
      <div class="grid-header">
        <span class="grid-title">
          분류 목록 - <strong id="lblCtgTotCnt" class="total-count">{{ ctgRows.length.toLocaleString() }}건</strong>
        </span>
      </div>
      <DataTable
        id="grdCommCtgList"
        v-model:selected-rows="selectedCtgRows"
        class="nowrap-grid edit-grid"
        :columns="ctgColumns"
        :data="ctgRows"
        :loading="isCtgLoading"
        :active-row="activeCtgRow"
        :page-size="10"
        selectable
        flag-column
        dense
        @row-click="onGrdCommCtgListRowClick"
      >
        <template #cell-wrkMdulCd="{ row }">
          <select
            v-if="row._flag === 'I'"
            v-model="row.wrkMdulCd"
            class="cell-select"
            style="width: 110px"
            @click.stop
          >
            <option v-for="option in wrkMdulCdOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
          <span v-else>{{ codeName(wrkMdulCdOptions, row.wrkMdulCd) }}</span>
        </template>
        <template #cell-commCtgCd="{ row }">
          <input
            v-model="row.commCtgCd"
            class="cell-input"
            :class="{ 'is-required': row._flag === 'I' }"
            style="width: 120px"
            maxlength="30"
            :readonly="row._flag !== 'I'"
            @input="row.commCtgCd = row.commCtgCd.toUpperCase()"
          />
        </template>
        <template #cell-commCtgNm="{ row }">
          <input v-model="row.commCtgNm" class="cell-input is-required" style="width: 150px" maxlength="50" @input="markRowUpdated(row)" />
        </template>
        <template #cell-rmk="{ row }">
          <input v-model="row.rmk" class="cell-input" style="width: 170px" maxlength="200" @input="markRowUpdated(row)" />
        </template>
        <template v-for="key in COMM_PROP_NM_KEYS" :key="key" #[`cell-${key}`]="{ row }">
          <input v-model="row[key]" class="cell-input" style="width: 100px" maxlength="40" @input="markRowUpdated(row)" />
        </template>
        <template #cell-usrMngeYn="{ row }">
          <input type="checkbox" class="cell-check" :checked="row.usrMngeYn === 'Y'" @click.stop @change="setRowYn(row, 'usrMngeYn', $event)" />
        </template>
        <template #cell-useYn="{ row }">
          <input type="checkbox" class="cell-check" :checked="row.useYn === 'Y'" @click.stop @change="setRowYn(row, 'useYn', $event)" />
        </template>
        <template #cell-alinSeq="{ row }">
          <input
            type="number"
            class="cell-input num-input"
            :value="row.alinSeq ?? ''"
            min="0"
            @input="setRowNumber(row, 'alinSeq', $event)"
          />
        </template>
      </DataTable>
    </div>

    <!-- 코드목록 (TB_COMM_CD_D) -->
    <div class="grid-area">
      <div class="grid-header">
        <span class="grid-title">
          코드 목록
          <span v-if="activeCtgRow" id="lblDtlCtg" class="sub-title">({{ activeCtgRow.commCtgCd || '신규' }} {{ activeCtgRow.commCtgNm }})</span>
          <span v-if="isActiveCtgDeleted" class="sub-title text-err">(삭제 예정)</span>
          - <strong id="lblDtlTotCnt" class="total-count">{{ dtlRows.length.toLocaleString() }}건</strong>
        </span>
        <div class="grid-actions">
          <Button id="btnDtlRowAdd" v-permission variant="outline" size="small" @click="onBtnDtlRowAddClick">행추가</Button>
          <Button id="btnDtlRowDel" v-permission variant="outline" size="small" @click="onBtnDtlRowDelClick">행삭제</Button>
        </div>
      </div>
      <DataTable
        id="grdCommCdList"
        v-model:selected-rows="selectedDtlRows"
        class="nowrap-grid edit-grid"
        :class="{ 'is-locked': isActiveCtgDeleted }"
        :columns="dtlColumns"
        :data="dtlRows"
        :loading="isDtlLoading"
        :page-size="10"
        :empty-text="activeCtgRow ? msg('sy.common.code.empty.code.none') : msg('sy.common.code.empty.category.select')"
        selectable
        flag-column
        dense
      >
        <template #cell-commCtgDtlCd="{ row }">
          <input
            v-model="row.commCtgDtlCd"
            class="cell-input"
            :class="{ 'is-required': row._flag === 'I' }"
            style="width: 100px"
            :maxlength="MAX_COMM_CD_DNUM"
            :readonly="row._flag !== 'I'"
          />
        </template>
        <template #cell-commCtgDtlNm="{ row }">
          <input v-model="row.commCtgDtlNm" class="cell-input is-required" style="width: 150px" maxlength="80" @input="markRowUpdated(row)" />
        </template>
        <template #cell-rmk="{ row }">
          <input v-model="row.rmk" class="cell-input" style="width: 170px" maxlength="200" @input="markRowUpdated(row)" />
        </template>
        <template v-for="key in COMM_PROP_CONT_KEYS" :key="key" #[`cell-${key}`]="{ row }">
          <input v-model="row[key]" class="cell-input" style="width: 100px" maxlength="100" @input="markRowUpdated(row)" />
        </template>
        <template #cell-useYn="{ row }">
          <input type="checkbox" class="cell-check" :checked="row.useYn === 'Y'" @click.stop @change="setRowYn(row, 'useYn', $event)" />
        </template>
        <template #cell-alinSeq="{ row }">
          <input
            type="number"
            class="cell-input num-input"
            :value="row.alinSeq ?? ''"
            min="0"
            @input="setRowNumber(row, 'alinSeq', $event)"
          />
        </template>
      </DataTable>
    </div>
  </section>
</template>

<style scoped src="@/assets/page.css"></style>

<style scoped>
.num-input {
  width: 64px;
  text-align: right;
}

/* 삭제 표시된 분류의 코드목록은 편집 불가 */
.is-locked :deep(td) {
  pointer-events: none;
  opacity: 0.6;
}
</style>
