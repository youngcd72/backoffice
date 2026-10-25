<script setup lang="ts">
/*****************************************************************************************
	Program ID   : SYPG010
	Program Type : vue
	Program Name : 프로그램관리
	Description  : 프로그램 및 프로그램별 컴포넌트를 조회/등록/수정/삭제하고 CSV로 내보낸다
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { ref, computed, onMounted } from 'vue'
import { msg } from '@/utils/message'
import { Button, Input, Select, DataTable, useDialog } from '@/components/common'
import { useGridSave } from '@/composables/useGridSave'
import {
  getPrgmListApi,
  getPrgmCmpnListApi,
  savePrgmListApi,
  type Prgm,
  type PrgmCmpn,
  type PrgmCmpnSave,
} from '@/api/sy/program'
import { getCommCdOptions } from '@/api/common/commonCode'
import { YN_OPTIONS, codeName, withAll, type CodeOption } from '@/utils/code'
import { downloadCsv } from '@/utils/csv'
import { isValidYmd } from '@/utils/validate'
import { todayYmd } from '@/utils/date'
import { deleteFlaggedRows, hasRowChanges, markRowUpdated, nextRowId, renumber, splitRowsByFlag, type RowFlag } from '@/utils/rowFlag'

interface PrgmRow extends Prgm {
  id: number
  no?: number
  _flag: RowFlag
}

interface CmpnRow extends PrgmCmpn {
  id: number
  no?: number
  _flag: RowFlag
}

/** 프로그램 1건의 컴포넌트 편집 상태 */
interface CmpnState {
  rows: CmpnRow[]
}

type YnKey = 'useYn'
type TextKey = 'prgmId' | 'prgmNm' | 'lnkUrl' | 'stDt' | 'endDt' | 'rmk'

const PAGE_TITLE = '프로그램관리'
const PAGE_ID = 'SYPG010'

// 텍스트 입력 컬럼 (maxlength 는 TB_PRGM_M 컬럼 길이 기준)
const TEXT_COLUMNS: Array<{ key: TextKey; label: string; width: string; maxlength: number; required?: boolean; placeholder?: string }> = [
  { key: 'prgmId', label: '프로그램ID', width: '140px', maxlength: 100, required: true },
  { key: 'prgmNm', label: '프로그램명', width: '170px', maxlength: 50, required: true },
  { key: 'lnkUrl', label: '링크URL', width: '120px', maxlength: 1000 },
  { key: 'stDt', label: '시작일자', width: '90px', maxlength: 8, placeholder: 'YYYYMMDD' },
  { key: 'endDt', label: '종료일자', width: '90px', maxlength: 8, placeholder: 'YYYYMMDD' },
  { key: 'rmk', label: '비고', width: '140px', maxlength: 200 },
]

const YN_COLUMNS: Array<{ key: YnKey; label: string }> = [
  { key: 'useYn', label: '사용여부' },
]

/**
 * textColumn : 텍스트 컬럼 정의에서 그리드 표시용 컬럼(key, label, width)을 추출한다
 * @param   key - 텍스트 컬럼 키
 * @return  그리드 컬럼 정의 객체
 */
const textColumn = (key: TextKey) => {
  const col = TEXT_COLUMNS.find((c) => c.key === key)!
  return { key: col.key, label: col.label, width: col.width }
}

const prgmColumns = [
  { key: 'no', label: 'NO', width: '56px', align: 'center' },
  textColumn('prgmId'),
  textColumn('prgmNm'),
  textColumn('lnkUrl'),
  { key: 'prgmCrsCd', label: '프로그램경로', width: '110px' },
  ...YN_COLUMNS.map((col) => ({ key: col.key, label: col.label, width: '84px', align: 'center' })),
  textColumn('stDt'),
  textColumn('endDt'),
  textColumn('rmk'),
]

const cmpnColumns = [
  { key: 'no', label: 'NO', width: '48px', align: 'center' },
  { key: 'cmpnId', label: '컴포넌트ID', width: '110px' },
  { key: 'cmpnNm', label: '컴포넌트명' },
  { key: 'alinSeq', label: '정렬순번', width: '72px', align: 'center' },
  { key: 'useYn', label: '사용여부', width: '72px', align: 'center' },
]

const { alert, confirm } = useDialog()
const { saveChanges, confirmDiscardChanges } = useGridSave()

// 조회 조건
const ynOptions = ref<Array<{ label: string; value: string }>>([])
/** 프로그램경로 공통코드 (SY/PRGM_CRS_CD) */
const prgmCrsOptions = ref<CodeOption[]>([])
const searchForm = ref({ prgmCrsCd: '', prgmId: '', prgmNm: '', useYn: '' })

// 프로그램 그리드
const prgmRows = ref<PrgmRow[]>([])
const selectedPrgmRows = ref<PrgmRow[]>([])
const activePrgmRow = ref<PrgmRow | null>(null)
const isPrgmLoading = ref(false)

// 컴포넌트 그리드: 프로그램 행(id)별 편집 상태
const cmpnStateMap = ref(new Map<number, CmpnState>())
const selectedCmpnRows = ref<CmpnRow[]>([])
const isCmpnLoading = ref(false)


const prgmTotalCount = computed(() => prgmRows.value.length)

const cmpnRows = computed(() =>
  activePrgmRow.value ? (cmpnStateMap.value.get(activePrgmRow.value.id)?.rows ?? []) : [],
)

const isActivePrgmDeleted = computed(() => activePrgmRow.value?._flag === 'D')

const hasChanges = computed(
  () => hasRowChanges(prgmRows.value) || [...cmpnStateMap.value.values()].some((state) => hasRowChanges(state.rows)),
)

/**
 * createEmptyPrgm : 신규 프로그램 행의 기본값을 만든다 (프로그램경로는 조회조건 값, 시작일자는 오늘)
 * @return  기본값이 채워진 프로그램 객체
 */
function createEmptyPrgm(): Prgm {
  return {
    prgmId: '',
    prgmNm: '',
    lnkUrl: '',
    prgmCrsCd: searchForm.value.prgmCrsCd,
    useYn: 'Y',
    stDt: todayYmd(),
    endDt: '99991231',
    rmk: '',
  }
}

/**
 * resetSearchForm : 검색 조건 입력값을 초기 상태로 되돌린다
 * @return  N/A
 */
function resetSearchForm(): void {
  searchForm.value = { prgmCrsCd: '', prgmId: '', prgmNm: '', useYn: '' }
}

/**
 * clearGrids : 그리드 데이터와 선택/활성 행 상태를 모두 초기화한다
 * @return  N/A
 */
function clearGrids(): void {
  prgmRows.value = []
  selectedPrgmRows.value = []
  activePrgmRow.value = null
  cmpnStateMap.value = new Map()
  selectedCmpnRows.value = []
}

/**
 * searchPrgmList : 검색 조건으로 프로그램 목록을 조회하고 이전 선택 행(없으면 첫 행)을 활성화한다
 * @return  N/A
 */
async function searchPrgmList(): Promise<void> {
  const prevActivePrgmId = activePrgmRow.value?.prgmId ?? ''
  isPrgmLoading.value = true
  try {
    const res = await getPrgmListApi(searchForm.value)
    clearGrids()
    prgmRows.value = res.data.map((prgm) => ({ ...prgm, id: nextRowId(), _flag: '' as RowFlag }))
    renumber(prgmRows.value)
  } finally {
    isPrgmLoading.value = false
  }
  if (prgmRows.value.length === 0) {
    await alert({ message: msg('cm.info.no.data'), icon: 'information' })
    return
  }
  const prevRow = prgmRows.value.find((row) => row.prgmId === prevActivePrgmId)
  await activatePrgmRow(prevRow ?? prgmRows.value[0])
}

/**
 * activatePrgmRow : 프로그램 행을 선택하고, 처음 선택하는 행이면 컴포넌트 목록을 조회한다
 * @param   row - 선택할 프로그램 행
 * @return  N/A
 */
async function activatePrgmRow(row: PrgmRow): Promise<void> {
  activePrgmRow.value = row
  selectedCmpnRows.value = []
  if (cmpnStateMap.value.has(row.id)) return

  cmpnStateMap.value.set(row.id, { rows: [] })
  if (row._flag === 'I') return

  isCmpnLoading.value = true
  try {
    const res = await getPrgmCmpnListApi(row.prgmId)
    const loadedState = cmpnStateMap.value.get(row.id)
    if (!loadedState) return
    loadedState.rows = res.data.map((cmpn) => ({ ...cmpn, id: nextRowId(), _flag: '' as RowFlag }))
    renumber(loadedState.rows)
  } finally {
    isCmpnLoading.value = false
  }
}

/**
 * addPrgmRow : 신규 프로그램 행을 그리드 맨 위에 추가하고 활성화한다
 * @return  N/A
 */
function addPrgmRow(): void {
  const row: PrgmRow = { ...createEmptyPrgm(), id: nextRowId(), _flag: 'I' }
  prgmRows.value.unshift(row)
  renumber(prgmRows.value)
  activatePrgmRow(prgmRows.value[0])
}

/**
 * addCmpnRow : 활성 프로그램의 컴포넌트 목록에 신규 행을 추가한다 (정렬순서는 최대값+1)
 * @param   state - 컴포넌트 편집 상태
 * @return  N/A
 */
function addCmpnRow(state: CmpnState): void {
  const maxSeq = state.rows.reduce((max, row) => Math.max(max, row.alinSeq ?? 0), 0)
  state.rows.push({
    prgmId: activePrgmRow.value?.prgmId ?? '',
    cmpnId: '',
    cmpnNm: '',
    alinSeq: maxSeq + 1,
    useYn: 'Y',
    id: nextRowId(),
    _flag: 'I',
  })
  renumber(state.rows)
}

/**
 * setPrgmRowYn : 체크박스 상태를 프로그램 행의 Y/N 값으로 반영하고 수정 상태로 표시한다
 * @param   row - 대상 프로그램 행
 * @param   key - Y/N 컬럼 키
 * @param   event - 체크박스 change 이벤트
 * @return  N/A
 */
function setPrgmRowYn(row: PrgmRow, key: YnKey, event: Event): void {
  row[key] = (event.target as HTMLInputElement).checked ? 'Y' : 'N'
  markRowUpdated(row)
}

/**
 * setCmpnRowUseYn : 체크박스 상태를 컴포넌트 행의 사용여부(Y/N)로 반영하고 수정 상태로 표시한다
 * @param   row - 대상 컴포넌트 행
 * @param   event - 체크박스 change 이벤트
 * @return  N/A
 */
function setCmpnRowUseYn(row: CmpnRow, event: Event): void {
  row.useYn = (event.target as HTMLInputElement).checked ? 'Y' : 'N'
  markRowUpdated(row)
}

/**
 * setCmpnRowAlinSeq : 입력값을 컴포넌트 행의 정렬순서로 반영하고 수정 상태로 표시한다 (빈 값은 null)
 * @param   row - 대상 컴포넌트 행
 * @param   event - 입력 이벤트
 * @return  N/A
 */
function setCmpnRowAlinSeq(row: CmpnRow, event: Event): void {
  const value = (event.target as HTMLInputElement).value
  row.alinSeq = value === '' ? null : Number(value)
  markRowUpdated(row)
}

/**
 * isValidYmdOrEmpty : 일자가 비어 있거나 실제 존재하는 YYYYMMDD 일자인지 판별
 * @param   value - 검사할 일자
 * @return  유효하면 true
 */
function isValidYmdOrEmpty(value: string): boolean {
  return !value || isValidYmd(value)
}

/**
 * validatePrgmRows : 프로그램 필수값·형식·중복 검증
 * @return  오류 메시지 (문제 없으면 빈 문자열)
 */
function validatePrgmRows(): string {
  const prgmIdCnt = new Map<string, number>()
  prgmRows.value.forEach((row) => {
    if (row._flag === 'D') return
    const prgmId = row.prgmId.trim()
    if (prgmId) prgmIdCnt.set(prgmId, (prgmIdCnt.get(prgmId) ?? 0) + 1)
  })

  for (const row of prgmRows.value) {
    if (row._flag === '' || row._flag === 'D') continue
    const rowLabel = msg('sy.program.row.label', row.no)
    for (const col of TEXT_COLUMNS) {
      if (col.required && !row[col.key].trim()) return msg('sy.program.validate.required', rowLabel, col.label)
    }
    if ((prgmIdCnt.get(row.prgmId.trim()) ?? 0) > 1) {
      return msg('sy.program.validate.id.duplicate', rowLabel, row.prgmId)
    }
    if (!isValidYmdOrEmpty(row.stDt) || !isValidYmdOrEmpty(row.endDt)) {
      return msg('sy.program.validate.date.format', rowLabel)
    }
    if (row.stDt && row.endDt && row.stDt > row.endDt) {
      return msg('sy.program.validate.date.reversed', rowLabel)
    }
  }
  return ''
}

/**
 * validateCmpnRows : 프로그램별 컴포넌트 필수값·중복 검증
 * @return  오류 메시지 (문제 없으면 빈 문자열)
 */
function validateCmpnRows(): string {
  for (const prgmRow of prgmRows.value) {
    const state = cmpnStateMap.value.get(prgmRow.id)
    if (prgmRow._flag === 'D' || !state || !hasRowChanges(state.rows)) continue
    const cmpnIdCnt = new Map<string, number>()
    state.rows.forEach((row) => {
      if (row._flag === 'D') return
      const cmpnId = row.cmpnId.trim()
      if (cmpnId) cmpnIdCnt.set(cmpnId, (cmpnIdCnt.get(cmpnId) ?? 0) + 1)
    })
    const prgmLabel = prgmRow.prgmId || msg('sy.program.row.label.no', prgmRow.no)
    for (const row of state.rows) {
      if (row._flag === '' || row._flag === 'D') continue
      const rowLabel = msg('sy.program.component.row.label', prgmLabel, row.no)
      if (!row.cmpnId.trim()) return msg('sy.program.validate.component.id.required', rowLabel)
      if (!row.cmpnNm.trim()) return msg('sy.program.validate.component.name.required', rowLabel)
      if ((cmpnIdCnt.get(row.cmpnId.trim()) ?? 0) > 1) return msg('sy.program.validate.component.id.duplicate', rowLabel, row.cmpnId)
    }
  }
  return ''
}

/**
 * toPrgm : 프로그램 행에서 화면용 필드를 제거하고 문자열을 trim하여 저장용 객체로 변환한다
 * @param   row - 프로그램 행
 * @return  저장용 프로그램 객체
 */
function toPrgm(row: PrgmRow): Prgm {
  const { id: _id, no: _no, _flag: _rowFlag, ...prgm } = row
  return { ...prgm, prgmId: prgm.prgmId.trim(), prgmNm: prgm.prgmNm.trim() }
}

/**
 * toPrgmCmpn : 컴포넌트 행을 저장용 객체로 변환한다
 * @param   row - 컴포넌트 행
 * @param   prgmId - 소속 프로그램ID
 * @return  저장용 컴포넌트 객체
 */
function toPrgmCmpn(row: CmpnRow, prgmId: string): PrgmCmpn {
  return {
    prgmId,
    cmpnId: row.cmpnId.trim(),
    cmpnNm: row.cmpnNm.trim(),
    alinSeq: row.alinSeq,
    useYn: row.useYn,
  }
}

/**
 * buildCmpnSaveList : 변경된 컴포넌트를 프로그램별 등록/수정/삭제 목록으로 구성한다 (삭제 프로그램 제외)
 * @return  프로그램별 컴포넌트 저장 목록
 */
function buildCmpnSaveList(): PrgmCmpnSave[] {
  return prgmRows.value.flatMap((prgmRow) => {
    const state = cmpnStateMap.value.get(prgmRow.id)
    // 삭제(D) 프로그램의 컴포넌트는 프로그램 삭제 시 함께 삭제된다
    if (prgmRow._flag === 'D' || !state || !hasRowChanges(state.rows)) return []
    const prgmId = prgmRow.prgmId.trim()
    return [
      {
        prgmId,
        insertList: state.rows.filter((row) => row._flag === 'I').map((row) => toPrgmCmpn(row, prgmId)),
        updateList: state.rows.filter((row) => row._flag === 'U').map((row) => toPrgmCmpn(row, prgmId)),
        deleteList: state.rows.filter((row) => row._flag === 'D').map((row) => toPrgmCmpn(row, prgmId)),
      },
    ]
  })
}

/**
 * getExportPrgmRows : CSV로 내보낼 프로그램 행 (삭제(D) 표시 행 제외)
 * @return  내보낼 프로그램 행 목록
 */
function getExportPrgmRows(): PrgmRow[] {
  return prgmRows.value.filter((row) => row._flag !== 'D')
}

/**
 * exportPrgmListToCsv : 프로그램 목록을 그리드 컬럼 순서대로 CSV로 내려받는다 (NO는 1부터 다시 매김)
 * @param   rows - 내보낼 프로그램 행 (삭제 행 제외)
 * @return  N/A
 */
function exportPrgmListToCsv(rows: PrgmRow[]): void {
  const header = prgmColumns.map((col) => col.label)
  const lines = rows.map((row, idx): string[] =>
    prgmColumns.map((col) => {
      if (col.key === 'no') return String(idx + 1)
      if (col.key === 'prgmCrsCd') return codeName(prgmCrsOptions.value, row.prgmCrsCd)
      return String(row[col.key as TextKey | YnKey] ?? '')
    }),
  )
  downloadCsv(`프로그램목록_${todayYmd()}.csv`, header, lines)
}

// ---------------------------------------------------------------------
// 이벤트 핸들러
// ---------------------------------------------------------------------
/**
 * onBtnSrchClick : 조회 버튼 클릭 시 변경 내용 확인 후 프로그램 목록을 조회한다
 * @return  N/A
 */
async function onBtnSrchClick(): Promise<void> {
  if (!(await confirmDiscardChanges(hasChanges.value))) return
  await searchPrgmList()
}

/**
 * onBtnNewClick : 신규 버튼 클릭 시 변경 내용 확인 후 검색조건/그리드를 초기화하고 신규 프로그램 행을 추가한다
 * @return  N/A
 */
async function onBtnNewClick(): Promise<void> {
  if (!(await confirmDiscardChanges(hasChanges.value))) return
  resetSearchForm()
  clearGrids()
  addPrgmRow()
}

/**
 * onGrdPrgmListRowClick : 프로그램 그리드 행 클릭 시 해당 행을 활성화한다
 * @param   row - 클릭한 프로그램 행
 * @return  N/A
 */
function onGrdPrgmListRowClick(row: PrgmRow): void {
  if (row !== activePrgmRow.value) activatePrgmRow(row)
}

/**
 * onBtnRowAddClick : 프로그램 행추가 버튼 클릭 시 신규 행을 추가한다
 * @return  N/A
 */
function onBtnRowAddClick(): void {
  addPrgmRow()
}

/**
 * onBtnRowDelClick : 프로그램 행삭제 버튼 클릭 시 확인 후 선택 행을 삭제(신규는 제거, 기존은 삭제 표시)한다
 * @return  N/A
 */
async function onBtnRowDelClick(): Promise<void> {
  if (selectedPrgmRows.value.length === 0) {
    await alert({ message: msg('sy.program.delete.select'), icon: 'warning' })
    return
  }
  // 프로그램을 삭제하면 하위 컴포넌트·컴포넌트 권한이 함께 삭제되므로 확인한다
  const ok = await confirm({ message: msg('cm.confirm.delete.selected'), icon: 'question' })
  if (!ok) return
  // 등록(I) 행은 바로 제거하고, 나머지는 D 로 표시한다 (저장 시 삭제)
  prgmRows.value = deleteFlaggedRows(prgmRows.value, selectedPrgmRows.value)
  const remainIds = new Set(prgmRows.value.map((row) => row.id))
  ;[...cmpnStateMap.value.keys()].filter((id) => !remainIds.has(id)).forEach((id) => cmpnStateMap.value.delete(id))
  selectedPrgmRows.value = []
  renumber(prgmRows.value)
  if (activePrgmRow.value && !remainIds.has(activePrgmRow.value.id)) {
    activePrgmRow.value = null
    if (prgmRows.value.length > 0) await activatePrgmRow(prgmRows.value[0])
  }
}

/**
 * onBtnCmpnRowAddClick : 컴포넌트 행추가 버튼 클릭 시 활성 프로그램에 컴포넌트 행을 추가한다
 * @return  N/A
 */
async function onBtnCmpnRowAddClick(): Promise<void> {
  const state = activePrgmRow.value ? cmpnStateMap.value.get(activePrgmRow.value.id) : undefined
  if (!state) {
    await alert({ message: msg('sy.program.component.add.select.program'), icon: 'warning' })
    return
  }
  if (isActivePrgmDeleted.value) {
    await alert({ message: msg('sy.program.component.add.deleted.program'), icon: 'warning' })
    return
  }
  addCmpnRow(state)
}

/**
 * onBtnCmpnRowDelClick : 컴포넌트 행삭제 버튼 클릭 시 선택한 컴포넌트 행을 삭제 처리한다
 * @return  N/A
 */
async function onBtnCmpnRowDelClick(): Promise<void> {
  const state = activePrgmRow.value ? cmpnStateMap.value.get(activePrgmRow.value.id) : undefined
  if (!state || selectedCmpnRows.value.length === 0) {
    await alert({ message: msg('sy.program.component.delete.select'), icon: 'warning' })
    return
  }
  state.rows = deleteFlaggedRows(state.rows, selectedCmpnRows.value)
  selectedCmpnRows.value = []
  renumber(state.rows)
}

/**
 * onBtnSaveClick : 저장 버튼 클릭 시 검증 후 프로그램/컴포넌트 변경 내용을 저장하고 재조회한다
 * @return  N/A
 */
async function onBtnSaveClick(): Promise<void> {
  await saveChanges({
    hasChanges: hasChanges.value,
    validate: () => validatePrgmRows() || validateCmpnRows(),
    save: () => savePrgmListApi({ ...splitRowsByFlag(prgmRows.value, toPrgm), cmpnSaveList: buildCmpnSaveList() }),
    afterSave: () => searchPrgmList(),
  })
}

/**
 * onBtnXlsDnldClick : 엑셀 다운로드 버튼 클릭 시 프로그램 목록을 CSV로 내보낸다
 * @return  N/A
 */
async function onBtnXlsDnldClick(): Promise<void> {
  const exportRows = getExportPrgmRows()
  if (exportRows.length === 0) {
    await alert({ message: msg('cm.info.no.export.data'), icon: 'information' })
    return
  }
  exportPrgmListToCsv(exportRows)
}

// ---------------------------------------------------------------------
// 페이지 라이프사이클
// ---------------------------------------------------------------------
/**
 * fetchPageData : 페이지에서 사용할 참조 데이터(프로그램경로 공통코드, 사용여부 옵션)를 조회한다
 * @return  N/A
 */
async function fetchPageData(): Promise<void> {
  prgmCrsOptions.value = await getCommCdOptions('SY', 'PRGM_CRS_CD')
  // TODO: 공통코드 API 연동 시 사용여부(Y/N) 공통코드로 대체
  ynOptions.value = withAll(YN_OPTIONS)
}

/**
 * initPageUi : 검색영역과 그리드를 초기화하고 목록을 조회한다
 * @return  N/A
 */
function initPageUi(): void {
  resetSearchForm()
  clearGrids()
  searchPrgmList()
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
        <Button id="btnXlsDnld" v-permission variant="outline" size="small" @click="onBtnXlsDnldClick">엑셀</Button>
      </div>
    </header>

    <!-- 조회 조건 영역 -->
    <form id="frmSrch" class="search-area" @submit.prevent="onBtnSrchClick">
      <div class="search-fields">
        <label class="search-field">
          <span class="field-label">프로그램경로</span>
          <Select id="cboPrgmCrsCd" v-model="searchForm.prgmCrsCd" class="srch-select" :options="withAll(prgmCrsOptions)" />
        </label>
        <label class="search-field">
          <span class="field-label">프로그램ID</span>
          <Input id="txtPrgmId" v-model="searchForm.prgmId" @enter="onBtnSrchClick" />
        </label>
        <label class="search-field">
          <span class="field-label">프로그램명</span>
          <Input id="txtPrgmNm" v-model="searchForm.prgmNm" @enter="onBtnSrchClick" />
        </label>
        <label class="search-field">
          <span class="field-label">사용</span>
          <Select id="cboUseYn" v-model="searchForm.useYn" :options="ynOptions" />
        </label>
      </div>
      <div class="search-actions">
        <Button id="btnSrch" v-permission @click="onBtnSrchClick">조회</Button>
      </div>
    </form>

    <div class="grids-area">
      <!-- 프로그램 목록 그리드 (TB_PRGM_M) -->
      <div class="grid-area">
        <div class="grid-header">
          <span class="grid-title">프로그램목록 - <strong id="lblTotCnt" class="total-count">{{ prgmTotalCount }}건</strong></span>
          <div class="grid-actions">
            <Button id="btnRowAdd" v-permission variant="outline" size="small" @click="onBtnRowAddClick">행추가</Button>
            <Button id="btnRowDel" v-permission variant="outline" size="small" @click="onBtnRowDelClick">행삭제</Button>
          </div>
        </div>
        <DataTable
          id="grdPrgmList"
          v-model:selected-rows="selectedPrgmRows"
          :columns="prgmColumns"
          :data="prgmRows"
          :loading="isPrgmLoading"
          :active-row="activePrgmRow"
          :page-size="15"
          selectable
          dense
          flag-column
          @row-click="onGrdPrgmListRowClick"
        >
          <template #cell-prgmCrsCd="{ row }">
            <select
              v-model="row.prgmCrsCd"
              class="cell-input"
              style="width: 100px"
              @click.stop
              @change="markRowUpdated(row)"
            >
              <option value="">선택</option>
              <option v-for="option in prgmCrsOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
            </select>
          </template>

          <template v-for="col in TEXT_COLUMNS" :key="col.key" #[`cell-${col.key}`]="{ row }">
            <input
              v-model="row[col.key]"
              class="cell-input"
              :class="{ 'is-required': col.required && row._flag === 'I' }"
              :style="{ width: col.width }"
              :maxlength="col.maxlength"
              :placeholder="col.placeholder"
              :readonly="col.key === 'prgmId' && row._flag !== 'I'"
              @input="markRowUpdated(row)"
            />
          </template>

          <template v-for="col in YN_COLUMNS" :key="col.key" #[`cell-${col.key}`]="{ row }">
            <input
              type="checkbox"
              class="cell-check"
              :checked="row[col.key] === 'Y'"
              @change="setPrgmRowYn(row, col.key, $event)"
            />
          </template>
        </DataTable>
      </div>

      <!-- 컴포넌트 목록 그리드 (TB_PRGM_CMPN_M) -->
      <div class="grid-area">
        <div class="grid-header">
          <span class="grid-title">
            컴포넌트목록
            <span v-if="activePrgmRow" id="lblCmpnPrgmId" class="cmpn-prgm-id">[{{ activePrgmRow.prgmId || '신규' }}]</span>
            <span v-if="isActivePrgmDeleted" class="locked-note">(삭제 예정)</span>
            - <strong id="lblCmpnTotCnt" class="total-count">{{ cmpnRows.length }}건</strong>
          </span>
          <div class="grid-actions">
            <Button id="btnCmpnRowAdd" v-permission variant="outline" size="small" @click="onBtnCmpnRowAddClick">행추가</Button>
            <Button id="btnCmpnRowDel" v-permission variant="outline" size="small" @click="onBtnCmpnRowDelClick">행삭제</Button>
          </div>
        </div>
        <DataTable
          id="grdCmpnList"
          v-model:selected-rows="selectedCmpnRows"
          :columns="cmpnColumns"
          :data="cmpnRows"
          :loading="isCmpnLoading"
          :page-size="15"
          :empty-text="activePrgmRow ? msg('sy.program.empty.component.none') : msg('sy.program.empty.select.program')"
          :class="{ 'is-locked': isActivePrgmDeleted }"
          selectable
          dense
          flag-column
        >
          <template #cell-cmpnId="{ row }">
            <input
              v-model="row.cmpnId"
              class="cell-input"
              :class="{ 'is-required': row._flag === 'I' }"
              style="width: 110px"
              maxlength="50"
              :readonly="row._flag !== 'I'"
              @input="markRowUpdated(row)"
            />
          </template>
          <template #cell-cmpnNm="{ row }">
            <input
              v-model="row.cmpnNm"
              class="cell-input"
              :class="{ 'is-required': row._flag === 'I' }"
              style="width: 120px"
              maxlength="50"
              @input="markRowUpdated(row)"
            />
          </template>
          <template #cell-alinSeq="{ row }">
            <input
              type="number"
              class="cell-input"
              style="width: 60px"
              min="0"
              :value="row.alinSeq ?? ''"
              @input="setCmpnRowAlinSeq(row, $event)"
            />
          </template>
          <template #cell-useYn="{ row }">
            <input
              type="checkbox"
              class="cell-check"
              :checked="row.useYn === 'Y'"
              @change="setCmpnRowUseYn(row, $event)"
            />
          </template>
        </DataTable>
      </div>
    </div>
  </section>
</template>

<style scoped src="@/assets/page.css"></style>

<style scoped>
.srch-select {
  width: 140px;
}

.grids-area {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 590px;
  gap: 16px;
  align-items: start;
}

.grid-header {
  gap: normal;
}

.grid-title {
  display: inline;
}

.cmpn-prgm-id {
  font-weight: 400;
  color: var(--ds-ink-2);
}

.locked-note {
  font-weight: 600;
  color: var(--ds-stamp);
}

/* 삭제(D) 표시된 프로그램의 컴포넌트는 편집 불가 */
.is-locked {
  pointer-events: none;
  opacity: 0.5;
}

@media (max-width: 1280px) {
  .grids-area {
    grid-template-columns: 1fr;
  }
}
</style>
