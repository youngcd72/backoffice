<script setup lang="ts">
/*****************************************************************************************
	Program ID   : SYPG020
	Program Type : vue
	Program Name : 메뉴관리
	Description  : 메뉴 트리 및 메뉴 목록을 조회·등록·수정·삭제하는 화면
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
import {
  Button,
  Input,
  Select,
  DataTable,
  PopupWindow,
  TreeView,
  useDialog,
  type TreeNode,
} from '@/components/common'
import { useGridSave } from '@/composables/useGridSave'
import {
  MENU_TYPE_CD,
  getMenuListApi,
  saveMenuListApi,
  type Menu,
} from '@/api/sy/menu'
import { YN_OPTIONS } from '@/utils/code'
import { downloadCsv } from '@/utils/csv'
import { todayYmd } from '@/utils/date'
import { getCommCdOptions } from '@/api/common/commonCode'
import { deleteFlaggedRows, hasRowChanges, markRowUpdated, nextRowId, renumber, splitRowsByFlag, type RowFlag } from '@/utils/rowFlag'
import { validateForm } from '@/utils/validate'

interface MenuRow extends Menu {
  id: number
  no?: number
  _flag: RowFlag
}

interface MenuTreeNode extends TreeNode {
  children: MenuTreeNode[]
}

const PAGE_TITLE = '메뉴관리'
const PAGE_ID = 'SYPG020'

const gridColumns = [
  { key: 'no', label: 'NO', width: '56px', align: 'center' },
  { key: 'menuId', label: '메뉴ID', width: '110px' },
  { key: 'menuNm', label: '메뉴명', width: '180px' },
  { key: 'alinSeq', label: '정렬', width: '64px', align: 'center' },
  { key: 'menuTypeCd', label: '메뉴타입', width: '84px', align: 'center' },
  { key: 'upprMenuId', label: '상위메뉴ID', width: '100px' },
  { key: 'upprMenuNm', label: '상위메뉴명', width: '160px' },
  { key: 'useYn', label: '사용여부', width: '72px', align: 'center' },
  { key: 'prgmId', label: '프로그램ID', width: '130px' },
  { key: 'menuPath', label: '메뉴PATH' },
]

const upprMenuColumns = [
  { key: 'menuId', label: '메뉴ID', width: '100px' },
  { key: 'menuNm', label: '메뉴명', width: '160px' },
  { key: 'menuPath', label: '메뉴PATH' },
]

const { alert, confirm } = useDialog()
const { saveChanges, confirmDiscardChanges } = useGridSave()

// 조회 조건
/** 메뉴타입 공통코드 (SY/MENU_TYPE_CD) */
const menuTypeCdOptions = ref<Array<{ label: string; value: string }>>([])
const menuTypeOptions = computed(() => [{ label: '전체', value: '' }, ...menuTypeCdOptions.value])
const useYnOptions = ref<Array<{ label: string; value: string }>>([])
const searchForm = ref({ menuId: '', menuNm: '', menuTypeCd: '', useYn: 'Y' })
const frmSrchRef = ref<HTMLFormElement | null>(null)

// 메뉴 데이터: allRows 는 전체 메뉴(트리 원본), gridRows 는 그리드에 표시 중인 메뉴
const allRows = ref<MenuRow[]>([])
const gridRows = ref<MenuRow[]>([])
const selectedRows = ref<MenuRow[]>([])
const isLoading = ref(false)

// 트리
const selectedTreeId = ref('')
const expandedTreeIds = ref<string[]>([])

// 상위메뉴 팝업
const isUpprMenuPopupOpen = ref(false)
const upprMenuTargetRow = ref<MenuRow | null>(null)
const upprMenuKeyword = ref('')

const totalCount = computed(() => gridRows.value.filter((row) => row._flag !== 'D').length)

const hasChanges = computed(() => hasRowChanges(allRows.value))

const menuMap = computed(() => new Map(allRows.value.map((row) => [row.menuId, row])))

/**
 * compareMenu : 메뉴 정렬 비교 (정렬순번, 메뉴ID 순)
 * @param   a - 비교 대상 메뉴 행
 * @param   b - 비교 대상 메뉴 행
 * @return  정렬 비교 결과값
 */
function compareMenu(a: MenuRow, b: MenuRow): number {
  return (a.alinSeq ?? 0) - (b.alinSeq ?? 0) || a.menuId.localeCompare(b.menuId)
}

const childrenMap = computed(() => {
  const map = new Map<string, MenuRow[]>()
  allRows.value.forEach((row) => {
    const parentId = menuMap.value.has(row.upprMenuId) ? row.upprMenuId : ''
    if (!map.has(parentId)) map.set(parentId, [])
    map.get(parentId)!.push(row)
  })
  map.forEach((list) => list.sort(compareMenu))
  return map
})

const treeNodes = computed<MenuTreeNode[]>(() => {
  const build = (parentId: string): MenuTreeNode[] =>
    (childrenMap.value.get(parentId) ?? [])
      .filter((row) => row.menuId && row._flag !== 'D')
      .map((row) => ({ id: row.menuId, label: row.menuNm || row.menuId, children: build(row.menuId) }))
  return build('')
})

/**
 * collectSubtree : 메뉴와 그 하위 메뉴 전체를 트리 순서(DFS)로 수집
 * @param   menuId - 기준 메뉴ID (빈 값이면 전체)
 * @return  메뉴 행 목록
 */
function collectSubtree(menuId: string): MenuRow[] {
  const result: MenuRow[] = []
  const walk = (parentId: string) => {
    ;(childrenMap.value.get(parentId) ?? []).forEach((row) => {
      result.push(row)
      if (row.menuId) walk(row.menuId)
    })
  }
  if (menuId) {
    const self = menuMap.value.get(menuId)
    if (self) result.push(self)
  }
  walk(menuId)
  return result
}

/**
 * getAncestors : 메뉴의 상위 메뉴 목록 조회 (최상위부터 순서대로)
 * @param   row - 대상 메뉴
 * @return  상위 메뉴 행 목록
 */
function getAncestors(row: Menu): MenuRow[] {
  const ancestors: MenuRow[] = []
  const visited = new Set<string>([row.menuId])
  let parent = menuMap.value.get(row.upprMenuId)
  while (parent && !visited.has(parent.menuId)) {
    ancestors.unshift(parent)
    visited.add(parent.menuId)
    parent = menuMap.value.get(parent.upprMenuId)
  }
  return ancestors
}

/**
 * getUpprMenuNm : 상위메뉴명 조회
 * @param   row - 대상 메뉴
 * @return  상위메뉴명 (없으면 빈 문자열)
 */
function getUpprMenuNm(row: Menu): string {
  return menuMap.value.get(row.upprMenuId)?.menuNm ?? ''
}

/**
 * getMenuPath : 메뉴 경로(상위 > 하위) 조회
 * @param   row - 대상 메뉴
 * @return  메뉴 경로 문자열
 */
function getMenuPath(row: Menu): string {
  return [...getAncestors(row), row].map((menu) => menu.menuNm).join(' > ')
}

/**
 * getMenuTypeNm : 메뉴타입코드의 명칭 조회
 * @param   menuTypeCd - 메뉴타입코드
 * @return  메뉴타입명 (없으면 빈 문자열)
 */
function getMenuTypeNm(menuTypeCd: string): string {
  return menuTypeCdOptions.value.find((opt) => opt.value === menuTypeCd)?.label ?? ''
}

// 상위메뉴 팝업 후보: 메뉴 타입이면서 대상 행 자신·하위 메뉴가 아닌 메뉴
const upprMenuCandidates = computed(() => {
  const target = upprMenuTargetRow.value
  const excludeIds = new Set(target?.menuId ? collectSubtree(target.menuId).map((row) => row.menuId) : [])
  const keyword = upprMenuKeyword.value.trim().toUpperCase()
  return collectSubtree('')
    .filter(
      (row) =>
        row.menuId && row._flag !== 'D' && row.menuTypeCd === MENU_TYPE_CD.MENU && !excludeIds.has(row.menuId),
    )
    .filter((row) => !keyword || `${row.menuId} ${row.menuNm}`.toUpperCase().includes(keyword))
    .map((row) => ({ id: row.id, menuId: row.menuId, menuNm: row.menuNm, menuPath: getMenuPath(row) }))
})

/**
 * toMenuRow : 메뉴 데이터를 그리드 행으로 변환
 * @param   menu - 메뉴 데이터
 * @param   flag - 행 상태 플래그
 * @return  메뉴 그리드 행
 */
function toMenuRow(menu: Menu, flag: RowFlag = ''): MenuRow {
  return { ...menu, id: nextRowId(), _flag: flag }
}

/**
 * resetSearchForm : 조회 조건 초기화
 * @return  N/A
 */
function resetSearchForm(): void {
  searchForm.value = { menuId: '', menuNm: '', menuTypeCd: '', useYn: 'Y' }
}

/**
 * clearGrid : 그리드와 선택 행 초기화
 * @return  N/A
 */
function clearGrid(): void {
  gridRows.value = []
  selectedRows.value = []
}

/**
 * refreshGrid : 트리에서 선택한 메뉴의 하위 메뉴 중 조회 조건에 맞는 메뉴를 그리드에 표시
 * @return  N/A
 */
function refreshGrid(): void {
  const { menuId, menuNm, menuTypeCd, useYn } = searchForm.value
  const idKeyword = menuId.trim().toUpperCase()
  const nmKeyword = menuNm.trim().toUpperCase()
  gridRows.value = collectSubtree(selectedTreeId.value).filter(
    (row) =>
      row._flag === 'I' ||
      ((!idKeyword || row.menuId.toUpperCase().includes(idKeyword)) &&
        (!nmKeyword || row.menuNm.toUpperCase().includes(nmKeyword)) &&
        (!menuTypeCd || row.menuTypeCd === menuTypeCd) &&
        (!useYn || row.useYn === useYn)),
  )
  selectedRows.value = []
  renumber(gridRows.value)
}

/**
 * loadMenuList : 메뉴 목록 조회 및 트리 확장 상태 보정
 * @return  N/A
 */
async function loadMenuList(): Promise<void> {
  const res = await getMenuListApi()
  allRows.value = res.data.map((menu) => toMenuRow(menu))
  if (selectedTreeId.value && !menuMap.value.has(selectedTreeId.value)) selectedTreeId.value = ''
  if (expandedTreeIds.value.length === 0) {
    expandedTreeIds.value = treeNodes.value.map((node) => node.id)
  }
}

/**
 * searchMenuList : 메뉴 목록 조회 후 그리드 갱신
 * @return  N/A
 */
async function searchMenuList(): Promise<void> {
  isLoading.value = true
  try {
    await loadMenuList()
    refreshGrid()
  } finally {
    isLoading.value = false
  }
  if (gridRows.value.length === 0) {
    await alert({ message: msg('cm.info.no.data'), icon: 'information' })
  }
}

/**
 * addMenuRow : 메뉴 신규 행 추가
 * @return  N/A
 */
function addMenuRow(): void {
  const upprMenuId = menuMap.value.get(selectedTreeId.value)?.menuTypeCd === MENU_TYPE_CD.MENU ? selectedTreeId.value : ''
  const siblings = childrenMap.value.get(upprMenuId) ?? []
  const maxSeq = siblings.reduce((max, row) => Math.max(max, row.alinSeq ?? 0), 0)
  const row = toMenuRow(
    {
      menuId: '',
      menuNm: '',
      upprMenuId,
      alinSeq: maxSeq + 1,
      menuTypeCd: MENU_TYPE_CD.MENU,
      useYn: 'Y',
      prgmId: '',
      rmk: '',
    },
    'I',
  )
  allRows.value.push(row)
  gridRows.value.unshift(row)
  renumber(gridRows.value)
}

/**
 * setRowUseYn : 행의 사용여부 변경 및 수정 표시
 * @param   row - 메뉴 행
 * @param   event - 체크박스 변경 이벤트
 * @return  N/A
 */
function setRowUseYn(row: MenuRow, event: Event): void {
  row.useYn = (event.target as HTMLInputElement).checked ? 'Y' : 'N'
  markRowUpdated(row)
}

/**
 * setRowAlinSeq : 행의 정렬순번 변경 및 수정 표시
 * @param   row - 메뉴 행
 * @param   event - 입력 변경 이벤트
 * @return  N/A
 */
function setRowAlinSeq(row: MenuRow, event: Event): void {
  const value = (event.target as HTMLInputElement).value
  row.alinSeq = value === '' ? null : Number(value)
  markRowUpdated(row)
}

/**
 * applyUpprMenu : 선택한 상위메뉴를 대상 행에 적용하고 팝업 닫기
 * @param   upprMenuId - 상위메뉴ID (최상위는 빈 값)
 * @return  N/A
 */
function applyUpprMenu(upprMenuId: string): void {
  const row = upprMenuTargetRow.value
  if (row) {
    row.upprMenuId = upprMenuId
    markRowUpdated(row)
  }
  isUpprMenuPopupOpen.value = false
}

/**
 * validateMenuRows : 필수값·중복·메뉴타입별 프로그램ID·상위메뉴 순환 검증
 * @return  오류 메시지 (문제 없으면 빈 문자열)
 */
function validateMenuRows(): string {
  const menuIdCnt = new Map<string, number>()
  allRows.value.forEach((row) => {
    if (row._flag === 'D') return
    const menuId = row.menuId.trim()
    if (menuId) menuIdCnt.set(menuId, (menuIdCnt.get(menuId) ?? 0) + 1)
  })

  for (const row of allRows.value) {
    if (row._flag === '' || row._flag === 'D') continue
    const rowLabel = row.menuId ? msg('sy.menu.row.label.id', row.menuId) : msg('sy.menu.row.label.no', row.no ?? '')
    if (!row.menuId.trim()) return msg('sy.menu.validate.id.required', rowLabel)
    if (!row.menuNm.trim()) return msg('sy.menu.validate.name.required', rowLabel)
    if ((menuIdCnt.get(row.menuId.trim()) ?? 0) > 1) return msg('sy.menu.validate.id.duplicate', rowLabel)
    if (row.alinSeq === null || Number.isNaN(row.alinSeq)) return msg('sy.menu.validate.sequence.required', rowLabel)
    // 링크(LINK)는 프로그램ID 필수, 메뉴 그룹(MENU)은 프로그램ID 없음
    const prgmId = (row.prgmId ?? '').trim()
    if (row.menuTypeCd === MENU_TYPE_CD.LINK && !prgmId) return msg('sy.menu.validate.prgm.id.required', rowLabel)
    if (row.menuTypeCd === MENU_TYPE_CD.MENU && prgmId) return msg('sy.menu.validate.prgm.id.not.allowed', rowLabel)
    if (row.upprMenuId && !menuMap.value.has(row.upprMenuId)) {
      return msg('sy.menu.validate.parent.not.found', rowLabel, row.upprMenuId)
    }
    if (menuMap.value.get(row.upprMenuId)?._flag === 'D') {
      return msg('sy.menu.validate.parent.deleted', rowLabel, row.upprMenuId)
    }
    if (row.upprMenuId && getAncestors(row).some((menu) => menu.menuId === row.menuId)) {
      return msg('sy.menu.validate.parent.circular', rowLabel)
    }
  }
  return ''
}

/**
 * toMenu : 메뉴 행을 저장용 메뉴 데이터로 변환
 * @param   row - 메뉴 행
 * @return  저장용 메뉴 데이터
 */
function toMenu(row: MenuRow): Menu {
  const { id: _id, no: _no, _flag: _rowFlag, ...menu } = row
  return { ...menu, menuId: menu.menuId.trim(), menuNm: menu.menuNm.trim() }
}

/**
 * exportMenuListToCsv : 메뉴 목록을 CSV 파일로 내보내기
 * @return  N/A
 */
function exportMenuListToCsv(): void {
  const header = ['NO', '메뉴ID', '메뉴명', '정렬', '메뉴타입', '상위메뉴ID', '상위메뉴명', '사용여부', '프로그램ID', '메뉴PATH']
  const lines = gridRows.value
    .filter((row) => row._flag !== 'D')
    .map((row): string[] => [
      String(row.no ?? ''),
      row.menuId,
      row.menuNm,
      String(row.alinSeq ?? ''),
      getMenuTypeNm(row.menuTypeCd),
      row.upprMenuId,
      getUpprMenuNm(row),
      row.useYn,
      row.prgmId,
      getMenuPath(row),
    ])
  downloadCsv(`메뉴목록_${todayYmd()}.csv`, header, lines)
}

// ---------------------------------------------------------------------
// 이벤트 핸들러
// ---------------------------------------------------------------------
/**
 * onBtnSrchClick : 조회 버튼 클릭: 검증 후 메뉴 목록 조회
 * @return  N/A
 */
async function onBtnSrchClick(): Promise<void> {
  if (!(await validateForm(frmSrchRef.value))) return
  if (!(await confirmDiscardChanges(hasChanges.value))) return
  await searchMenuList()
}

/**
 * onBtnNewClick : 신규 버튼 클릭: 조회 조건·그리드 초기화 후 신규 행 추가
 * @return  N/A
 */
async function onBtnNewClick(): Promise<void> {
  if (hasChanges.value) {
    const ok = await confirm({ message: msg('sy.menu.new.confirm.discard') })
    if (!ok) return
    await loadMenuList()
  }
  resetSearchForm()
  clearGrid()
  addMenuRow()
}

/**
 * onTreMenuSelect : 메뉴 트리 노드 선택: 하위 메뉴를 그리드에 표시
 * @param   node - 선택한 트리 노드
 * @return  N/A
 */
function onTreMenuSelect(node: TreeNode): void {
  selectedTreeId.value = node.id
  refreshGrid()
}

/**
 * onBtnTreeOpenClick : 트리 전체 펼치기 버튼 클릭
 * @return  N/A
 */
function onBtnTreeOpenClick(): void {
  expandedTreeIds.value = allRows.value.filter((row) => row.menuId).map((row) => row.menuId)
}

/**
 * onBtnTreeCloseClick : 트리 전체 접기 버튼 클릭
 * @return  N/A
 */
function onBtnTreeCloseClick(): void {
  expandedTreeIds.value = []
}

/**
 * onBtnRowAddClick : 행추가 버튼 클릭
 * @return  N/A
 */
function onBtnRowAddClick(): void {
  addMenuRow()
}

/**
 * onBtnRowDelClick : 행삭제 버튼 클릭: 선택한 메뉴 삭제 처리 (하위 메뉴가 남아 있으면 차단)
 * @return  N/A
 */
async function onBtnRowDelClick(): Promise<void> {
  if (selectedRows.value.length === 0) {
    await alert({ message: msg('sy.menu.delete.select'), icon: 'warning' })
    return
  }
  const targets = new Set(selectedRows.value)
  const blocked = selectedRows.value.find(
    (row) =>
      row.menuId &&
      (childrenMap.value.get(row.menuId) ?? []).some((child) => child._flag !== 'D' && !targets.has(child)),
  )
  if (blocked) {
    await alert({ message: msg('sy.menu.delete.has.children', blocked.menuNm), icon: 'warning' })
    return
  }
  // 등록(I) 행은 바로 제거하고, 나머지는 D 로 표시한다 (저장 시 삭제)
  allRows.value = deleteFlaggedRows(allRows.value, selectedRows.value)
  const remainRows = new Set(allRows.value)
  gridRows.value = gridRows.value.filter((row) => remainRows.has(row))
  if (selectedRows.value.some((row) => row.menuId === selectedTreeId.value)) selectedTreeId.value = ''
  selectedRows.value = []
  renumber(gridRows.value)
}

/**
 * onBtnSaveClick : 저장 버튼 클릭: 검증 후 메뉴 저장
 * @return  N/A
 */
async function onBtnSaveClick(): Promise<void> {
  await saveChanges({
    hasChanges: hasChanges.value,
    validate: validateMenuRows,
    save: () => saveMenuListApi(splitRowsByFlag(allRows.value, toMenu)),
    afterSave: searchMenuList,
  })
}

/**
 * onBtnXlsDnldClick : 엑셀 버튼 클릭: 메뉴 목록 CSV 다운로드
 * @return  N/A
 */
async function onBtnXlsDnldClick(): Promise<void> {
  if (gridRows.value.length === 0) {
    await alert({ message: msg('cm.info.no.export.data'), icon: 'information' })
    return
  }
  exportMenuListToCsv()
}

/**
 * onBtnUpprMenuSrchClick : 상위메뉴 검색 버튼 클릭: 상위메뉴 선택 팝업 열기
 * @param   row - 대상 메뉴 행
 * @return  N/A
 */
function onBtnUpprMenuSrchClick(row: MenuRow): void {
  upprMenuTargetRow.value = row
  upprMenuKeyword.value = ''
  isUpprMenuPopupOpen.value = true
}

/**
 * onGrdUpprMenuListRowClick : 상위메뉴 팝업 그리드 행 클릭: 상위메뉴 적용
 * @param   candidate - 선택한 상위메뉴 후보
 * @return  N/A
 */
function onGrdUpprMenuListRowClick(candidate: { menuId: string }): void {
  applyUpprMenu(candidate.menuId)
}

/**
 * onBtnUpprMenuRootClick : 상위메뉴 팝업의 최상위 버튼 클릭: 상위메뉴를 최상위로 지정
 * @return  N/A
 */
function onBtnUpprMenuRootClick(): void {
  applyUpprMenu('')
}

/**
 * onBtnUpprMenuCloseClick : 상위메뉴 팝업의 닫기 버튼 클릭
 * @return  N/A
 */
function onBtnUpprMenuCloseClick(): void {
  isUpprMenuPopupOpen.value = false
}

// ---------------------------------------------------------------------
// 페이지 라이프사이클
// ---------------------------------------------------------------------
/**
 * fetchPageData : 페이지 참조 데이터(메뉴타입 공통코드, 사용여부 옵션) 조회
 * @return  N/A
 */
async function fetchPageData(): Promise<void> {
  menuTypeCdOptions.value = await getCommCdOptions('SY', 'MENU_TYPE_CD')
  // TODO: 사용여부 공통코드 등록 시 공통코드로 대체
  useYnOptions.value = YN_OPTIONS
}

/**
 * initPageUi : 조회 조건·그리드 초기화 및 최초 조회
 * @return  N/A
 */
function initPageUi(): void {
  resetSearchForm()
  clearGrid()
  searchMenuList()
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
    <form id="frmSrch" ref="frmSrchRef" class="search-area" @submit.prevent="onBtnSrchClick">
      <div class="search-fields">
        <label class="search-field">
          <span class="field-label">메뉴ID</span>
          <Input id="txtMenuId" v-model="searchForm.menuId" @enter="onBtnSrchClick" />
        </label>
        <label class="search-field">
          <span class="field-label">메뉴명</span>
          <Input id="txtMenuNm" v-model="searchForm.menuNm" @enter="onBtnSrchClick" />
        </label>
        <label class="search-field">
          <span class="field-label">메뉴타입</span>
          <Select id="cboMenuTypeCd" v-model="searchForm.menuTypeCd" :options="menuTypeOptions" />
        </label>
        <label class="search-field">
          <span class="field-label">사용여부</span>
          <Select id="cboUseYn" v-model="searchForm.useYn" :options="useYnOptions" required field-name="사용여부" clearable />
        </label>
      </div>
      <div class="search-actions">
        <Button id="btnSrch" v-permission @click="onBtnSrchClick">조회</Button>
      </div>
    </form>

    <div class="content-area">
      <!-- 메뉴 트리 -->
      <aside class="tree-area">
        <div class="area-header">
          <span class="area-title">메뉴</span>
          <div class="area-actions">
            <Button id="btnTreeOpen" v-permission variant="outline" size="small" @click="onBtnTreeOpenClick">열기</Button>
            <Button id="btnTreeClose" v-permission variant="outline" size="small" @click="onBtnTreeCloseClick">닫기</Button>
          </div>
        </div>
        <div class="tree-box">
          <TreeView
            id="treMenu"
            v-model:expanded-ids="expandedTreeIds"
            :nodes="treeNodes"
            :selected-id="selectedTreeId"
            @select="onTreMenuSelect"
          />
        </div>
      </aside>

      <!-- 메뉴 목록 그리드 -->
      <div class="grid-area">
        <div class="area-header">
          <span class="area-title">메뉴목록 - <strong id="lblTotCnt" class="total-count">{{ totalCount }}건</strong></span>
          <div class="area-actions">
            <Button id="btnRowAdd" v-permission variant="outline" size="small" @click="onBtnRowAddClick">행추가</Button>
            <Button id="btnRowDel" v-permission variant="outline" size="small" @click="onBtnRowDelClick">행삭제</Button>
          </div>
        </div>
        <DataTable
          id="grdMenuList"
          v-model:selected-rows="selectedRows"
          :columns="gridColumns"
          :data="gridRows"
          :loading="isLoading"
          :page-size="15"
          selectable
          flag-column
        >
          <template #cell-menuId="{ row }">
            <input
              v-model="row.menuId"
              class="cell-input"
              :class="{ 'is-required': row._flag === 'I' }"
              style="width: 100px"
              maxlength="20"
              :readonly="row._flag !== 'I'"
              @input="markRowUpdated(row)"
            />
          </template>
          <template #cell-menuNm="{ row }">
            <input
              v-model="row.menuNm"
              class="cell-input"
              :class="{ 'is-required': row._flag === 'I' }"
              style="width: 170px"
              maxlength="50"
              @input="markRowUpdated(row)"
            />
          </template>
          <template #cell-alinSeq="{ row }">
            <input
              type="number"
              class="cell-input"
              style="width: 56px"
              min="0"
              max="99"
              :value="row.alinSeq ?? ''"
              @input="setRowAlinSeq(row, $event)"
            />
          </template>
          <template #cell-menuTypeCd="{ row }">
            <select v-model="row.menuTypeCd" class="cell-input" @change="markRowUpdated(row)">
              <option v-for="opt in menuTypeCdOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
            </select>
          </template>
          <template #cell-upprMenuId="{ row }">{{ row.upprMenuId }}</template>
          <template #cell-upprMenuNm="{ row }">
            <span class="uppr-menu">
              <span class="uppr-menu-nm">{{ getUpprMenuNm(row) }}</span>
              <button type="button" class="cell-search-btn" aria-label="상위메뉴 검색" @click.stop="onBtnUpprMenuSrchClick(row)">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" aria-hidden="true">
                  <circle cx="11" cy="11" r="7" />
                  <line x1="16.5" y1="16.5" x2="21" y2="21" />
                </svg>
              </button>
            </span>
          </template>
          <template #cell-useYn="{ row }">
            <input type="checkbox" class="cell-check" :checked="row.useYn === 'Y'" @change="setRowUseYn(row, $event)" />
          </template>
          <template #cell-prgmId="{ row }">
            <input v-model="row.prgmId" class="cell-input" style="width: 120px" maxlength="100" @input="markRowUpdated(row)" />
          </template>
          <template #cell-menuPath="{ row }">
            <span class="menu-path">{{ getMenuPath(row) }}</span>
          </template>
        </DataTable>
      </div>
    </div>

    <!-- 상위메뉴 검색 팝업 -->
    <PopupWindow id="mdlUpprMenu" v-model="isUpprMenuPopupOpen" title="상위메뉴 검색" :width="640">
      <div class="popup-search">
        <span class="field-label">메뉴ID/명</span>
        <Input id="txtUpprMenuKeyword" v-model="upprMenuKeyword" placeholder="메뉴ID 또는 메뉴명" />
      </div>
      <DataTable
        id="grdUpprMenuList"
        :columns="upprMenuColumns"
        :data="upprMenuCandidates"
        :page-size="8"
        :empty-text="msg('sy.menu.empty.selectable.none')"
        @row-click="onGrdUpprMenuListRowClick"
      />
      <template #footer>
        <Button id="btnUpprMenuRoot" v-permission variant="outline" @click="onBtnUpprMenuRootClick">최상위로 지정</Button>
        <Button id="btnUpprMenuClose" v-permission @click="onBtnUpprMenuCloseClick">닫기</Button>
      </template>
    </PopupWindow>
  </section>
</template>

<style scoped src="@/assets/page.css"></style>

<style scoped>
.page-actions, .area-actions, .search-actions {
  display: flex;
  gap: 6px;
}

.popup-search {
  display: flex;
  align-items: center;
  gap: 8px;
}

.field-label {
  min-width: 56px;
}

.content-area {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.tree-area, .grid-area {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.tree-box {
  max-height: 640px;
  overflow: auto;
  background: var(--ds-surface);
  border: 1px solid var(--ds-rule);
  border-radius: var(--ds-radius-panel);
}

.area-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 32px;
}

.area-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--ds-ink);
}

select.cell-input option {
  color: var(--ds-ink);
}

.uppr-menu {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.uppr-menu-nm {
  white-space: nowrap;
}

.cell-search-btn {
  display: inline-flex;
  padding: 3px;
  color: var(--ds-ink-2);
  background: var(--ds-surface);
  border: 1px solid var(--ds-rule-strong);
  border-radius: var(--ds-radius-control);
  cursor: pointer;
  transition: background-color var(--ds-ease), border-color var(--ds-ease), color var(--ds-ease);
}

.cell-search-btn:hover {
  color: var(--ds-pine-900);
  background: var(--ds-pine-050);
  border-color: var(--ds-pine-700);
}

.cell-search-btn:focus-visible {
  outline: none;
  box-shadow: var(--ds-focus);
}

.menu-path {
  white-space: nowrap;
  font-size: 12px;
  color: var(--ds-ink-3);
}

@media (max-width: 900px) {
  .content-area {
    grid-template-columns: 1fr;
  }

  .tree-box {
    max-height: 280px;
  }
}
</style>
