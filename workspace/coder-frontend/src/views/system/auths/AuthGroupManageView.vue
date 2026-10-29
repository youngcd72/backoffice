<script setup lang="ts">
/*****************************************************************************************
	Program ID   : SYAU020
	Program Type : vue
	Program Name : 그룹권한관리
	Description  : 권한그룹 및 그룹별 화면/컴포넌트 권한을 조회·등록·수정·삭제·복사하는 화면
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
import { Button, Input, Select, DataTable, DatePicker, useDialog } from '@/components/common'
import { useGridSave } from '@/composables/useGridSave'
import {
  getAuthGrpListApi,
  getAuthGrpMenuListApi,
  getAuthGrpCmpnListApi,
  saveAuthGrpListApi,
  type AuthGrp,
  type AuthGrpMenuSave,
  type AuthGrpCmpnSave,
} from '@/api/sy/authGroup'
import { useMenuCmpnAuth, type MenuAuthRow } from '@/composables/useMenuCmpnAuth'
import { dashToYmd, ymdToDash } from '@/utils/date'
import { isValidYmd } from '@/utils/validate'
import { deleteFlaggedRows, hasRowChanges, markRowUpdated, nextRowId, renumber, splitRowsByFlag, type RowFlag } from '@/utils/rowFlag'
import AuthCmpnGrid from './components/AuthCmpnGrid.vue'
import AuthScreenAddPopup from './components/AuthScreenAddPopup.vue'

interface GrpRow extends AuthGrp {
  id: number
  no?: number
  _flag: RowFlag
}

const PAGE_TITLE = '그룹권한관리'
const PAGE_ID = 'SYAU020'

const grpColumns = [
  { key: 'no', label: 'NO', width: '40px', align: 'center' },
  { key: 'usrGrpCd', label: '그룹ID', width: '140px' },
  { key: 'usrGrpNm', label: '그룹명' },
]

const authColumns = [
  { key: 'no', label: 'NO', width: '40px', align: 'center' },
  { key: 'menuId', label: '메뉴ID', width: '90px' },
  { key: 'menuPath', label: '메뉴' },
  { key: 'prgmNm', label: '프로그램', width: '150px' },
]

const { alert, confirm } = useDialog()
const { saveChanges, confirmDiscardChanges } = useGridSave()

// 조회 조건
const searchForm = ref({ usrGrpCd: '', usrGrpNm: '' })

// 그룹 그리드
const grpRows = ref<GrpRow[]>([])
const selectedGrpRows = ref<GrpRow[]>([])
const activeGrpRow = ref<GrpRow | null>(null)
const isGrpLoading = ref(false)
const copyGrpCd = ref('')

// 화면/컴포넌트 권한 (TB_AUTH_GRP_MENU_R, TB_AUTH_GRP_CMPN_R)
const {
  getMenuPath,
  getPrgmNm,
  menuAuthRows: authRows,
  selectedMenuAuthRows: selectedAuthRows,
  activeMenuAuthRow: activeAuthRow,
  isMenuAuthLoading: isAuthLoading,
  activeMenuAuthState: activeAuthState,
  isActiveOwnerDeleted: isActiveGrpDeleted,
  cmpnAuthRows,
  isCmpnAuthLoading,
  isCmpnAuthLocked,
  isAllCmpnAuthYn,
  setCmpnAuthYn,
  setAllCmpnAuthYn,
  isScrnPopupOpen,
  scrnKeyword,
  selectedScrnRows,
  scrnCandidates,
  openScrnPopup,
  addSelectedScrns,
  hasAuthChanges,
  clearAuthStates,
  getActiveMenuAuthKey,
  activateOwnerAuth,
  activateMenuAuthRow,
  pruneAuthStates,
  deleteSelectedMenuAuthRows,
  copyAuthFrom,
  buildMenuAuthSaveList,
  buildCmpnAuthSaveList,
  fetchReferenceData,
} = useMenuCmpnAuth({
  ownerRows: grpRows,
  activeOwnerRow: activeGrpRow,
  getOwnerKey: (row) => row.usrGrpCd.trim(),
  fetchMenuAuthList: async (usrGrpCd) => (await getAuthGrpMenuListApi(usrGrpCd)).data,
  fetchCmpnAuthList: async (usrGrpCd, prgmId) => (await getAuthGrpCmpnListApi(usrGrpCd, prgmId)).data,
})

const grpTotalCount = computed(() => grpRows.value.length)

const hasChanges = computed(() => hasRowChanges(grpRows.value) || hasAuthChanges.value)

// 권한복사 대상: 현재 그룹을 제외한 저장된 그룹
const copyGrpOptions = computed(() => [
  { label: '선택', value: '' },
  ...grpRows.value
    .filter((row) => row !== activeGrpRow.value && row._flag !== 'I' && row._flag !== 'D')
    .map((row) => ({ label: `${row.usrGrpNm} (${row.usrGrpCd})`, value: row.usrGrpCd })),
])

/**
 * resetSearchForm : 조회 조건 초기화
 * @return  N/A
 */
function resetSearchForm(): void {
  searchForm.value = { usrGrpCd: '', usrGrpNm: '' }
}

/**
 * clearGrids : 그룹·권한·컴포넌트 권한 그리드와 선택/편집 상태 초기화
 * @return  N/A
 */
function clearGrids(): void {
  grpRows.value = []
  selectedGrpRows.value = []
  activeGrpRow.value = null
  clearAuthStates()
  copyGrpCd.value = ''
}

/**
 * searchGrpList : 권한그룹 목록 조회 후 이전 선택 행(없으면 첫 행)을 활성화
 * @return  N/A
 */
async function searchGrpList(): Promise<void> {
  const prevActiveGrpCd = activeGrpRow.value?.usrGrpCd ?? ''
  const prevActiveAuthKey = getActiveMenuAuthKey()
  isGrpLoading.value = true
  try {
    const res = await getAuthGrpListApi(searchForm.value)
    clearGrids()
    grpRows.value = res.data.map((grp) => ({ ...grp, id: nextRowId(), _flag: '' as RowFlag }))
    renumber(grpRows.value)
  } finally {
    isGrpLoading.value = false
  }
  if (grpRows.value.length === 0) {
    await alert({ message: msg('cm.info.no.data'), icon: 'information' })
    return
  }
  const prevRow = grpRows.value.find((row) => row.usrGrpCd === prevActiveGrpCd)
  await activateGrpRow(prevRow ?? grpRows.value[0], prevActiveAuthKey)
}

/**
 * activateGrpRow : 그룹 행을 선택하고 권한목록 표시 (이전에 보던 화면이 있으면 다시 선택)
 * @param   row - 선택할 권한그룹 행
 * @param   prevAuthKey - 이전에 선택했던 "메뉴ID/프로그램ID" 키
 * @return  N/A
 */
async function activateGrpRow(row: GrpRow, prevAuthKey = ''): Promise<void> {
  activeGrpRow.value = row
  copyGrpCd.value = ''
  await activateOwnerAuth(row, prevAuthKey)
}

/**
 * addGrpRow : 권한그룹 신규 행 추가 후 활성화 (종료일자 99991231, 사용여부 Y)
 * @return  N/A
 */
function addGrpRow(): void {
  grpRows.value.unshift({
    usrGrpCd: '',
    usrGrpNm: '',
    useYn: 'Y',
    stDt: '',
    endDt: '99991231',
    rmk: '',
    id: nextRowId(),
    _flag: 'I',
  })
  renumber(grpRows.value)
  activateGrpRow(grpRows.value[0])
}

/**
 * setGrpRowDate : 그룹 행의 시작일/종료일 변경 및 수정 표시
 * @param   row - 권한그룹 행
 * @param   key - 변경할 일자 필드 (stDt/endDt)
 * @param   value - DatePicker 일자
 * @return  N/A
 */
function setGrpRowDate(row: GrpRow, key: 'stDt' | 'endDt', value: string | null): void {
  row[key] = dashToYmd(value)
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
 * validateGrpRows : 그룹 필수값·형식·중복 검증
 * @return  오류 메시지 (문제 없으면 빈 문자열)
 */
function validateGrpRows(): string {
  const grpCdCnt = new Map<string, number>()
  grpRows.value.forEach((row) => {
    if (row._flag === 'D') return
    const grpCd = row.usrGrpCd.trim()
    if (grpCd) grpCdCnt.set(grpCd, (grpCdCnt.get(grpCd) ?? 0) + 1)
  })
  for (const row of grpRows.value) {
    if (row._flag === '' || row._flag === 'D') continue
    const rowLabel = msg('sy.auth.group.row.label', row.no)
    if (!row.usrGrpCd.trim()) return msg('sy.auth.group.validate.id.required', rowLabel)
    if (!row.usrGrpNm.trim()) return msg('sy.auth.group.validate.name.required', rowLabel)
    if ((grpCdCnt.get(row.usrGrpCd.trim()) ?? 0) > 1) return msg('sy.auth.group.validate.id.duplicate', rowLabel, row.usrGrpCd)
    if (!isValidYmdOrEmpty(row.stDt) || !isValidYmdOrEmpty(row.endDt)) return msg('sy.auth.group.validate.period.invalid', rowLabel)
    if (row.stDt && row.endDt && row.stDt > row.endDt) return msg('sy.auth.group.validate.period.reversed', rowLabel)
  }
  return ''
}

/**
 * toAuthGrp : 그룹 행을 저장용 권한그룹 데이터로 변환
 * @param   row - 권한그룹 행
 * @return  저장용 권한그룹 데이터
 */
function toAuthGrp(row: GrpRow): AuthGrp {
  const { id: _id, no: _no, _flag: _rowFlag, ...grp } = row
  return { ...grp, usrGrpCd: grp.usrGrpCd.trim(), usrGrpNm: grp.usrGrpNm.trim() }
}

/**
 * buildMenuSaveList : 변경된 그룹별 화면 권한 저장 목록 생성
 * @return  그룹별 화면 권한 저장 목록
 */
function buildMenuSaveList(): AuthGrpMenuSave[] {
  return buildMenuAuthSaveList().map(({ ownerKey: usrGrpCd, insertList, deleteList }) => ({
    usrGrpCd,
    insertList: insertList.map((item) => ({ usrGrpCd, ...item })),
    deleteList: deleteList.map((item) => ({ usrGrpCd, ...item })),
  }))
}

/**
 * buildCmpnSaveList : 변경된 그룹·프로그램별 컴포넌트 권한 저장 목록 생성
 * @return  그룹·프로그램별 컴포넌트 권한 저장 목록
 */
function buildCmpnSaveList(): AuthGrpCmpnSave[] {
  return buildCmpnAuthSaveList().map(({ ownerKey: usrGrpCd, prgmId, insertList, updateList }) => ({
    usrGrpCd,
    prgmId,
    insertList: insertList.map((item) => ({ usrGrpCd, ...item })),
    updateList: updateList.map((item) => ({ usrGrpCd, ...item })),
  }))
}

/**
 * alertIfGrpLocked : 권한그룹 미선택 또는 삭제 표시된 그룹이면 안내 후 변경 불가 판단
 * @return  변경할 수 없으면 true
 */
async function alertIfGrpLocked(): Promise<boolean> {
  if (!activeGrpRow.value || !activeAuthState.value) {
    await alert({ message: msg('sy.auth.group.select.required'), icon: 'warning' })
    return true
  }
  if (isActiveGrpDeleted.value) {
    await alert({ message: msg('sy.auth.group.deleted.readonly'), icon: 'warning' })
    return true
  }
  return false
}

// ---------------------------------------------------------------------
// 이벤트 핸들러
// ---------------------------------------------------------------------
/**
 * onBtnSrchClick : 조회 버튼 클릭
 * @return  N/A
 */
async function onBtnSrchClick(): Promise<void> {
  if (!(await confirmDiscardChanges(hasChanges.value))) return
  await searchGrpList()
}

/**
 * onGrdUsrGrpListRowClick : 권한그룹 그리드 행 클릭: 해당 그룹 활성화
 * @param   row - 클릭한 권한그룹 행
 * @return  N/A
 */
function onGrdUsrGrpListRowClick(row: GrpRow): void {
  if (row !== activeGrpRow.value) activateGrpRow(row)
}

/**
 * onGrdAuthListRowClick : 권한목록 그리드 행 클릭: 해당 화면 활성화
 * @param   row - 클릭한 권한목록 행
 * @return  N/A
 */
function onGrdAuthListRowClick(row: MenuAuthRow): void {
  if (row !== activeAuthRow.value) activateMenuAuthRow(row)
}

/**
 * onBtnRowAddClick : 권한그룹 행추가 버튼 클릭
 * @return  N/A
 */
function onBtnRowAddClick(): void {
  addGrpRow()
}

/**
 * onBtnRowDelClick : 권한그룹 행삭제 버튼 클릭: 선택한 그룹과 권한목록 삭제 처리
 * @return  N/A
 */
async function onBtnRowDelClick(): Promise<void> {
  if (selectedGrpRows.value.length === 0) {
    await alert({ message: msg('sy.auth.group.delete.select'), icon: 'warning' })
    return
  }
  const ok = await confirm({ message: msg('sy.auth.group.delete.confirm'), icon: 'question' })
  if (!ok) return
  // 등록(I) 행은 바로 제거하고, 나머지는 D 로 표시한다 (저장 시 삭제)
  grpRows.value = deleteFlaggedRows(grpRows.value, selectedGrpRows.value)
  pruneAuthStates()
  selectedGrpRows.value = []
  renumber(grpRows.value)
  if (activeGrpRow.value && !grpRows.value.includes(activeGrpRow.value)) {
    activeGrpRow.value = null
    activeAuthRow.value = null
    if (grpRows.value.length > 0) await activateGrpRow(grpRows.value[0])
  }
}

/**
 * onBtnAuthCopyClick : 권한복사 버튼 클릭: 원본 그룹의 권한을 현재 그룹에 복사
 * @return  N/A
 */
async function onBtnAuthCopyClick(): Promise<void> {
  if (await alertIfGrpLocked()) return
  if (!copyGrpCd.value) {
    await alert({ message: msg('sy.auth.group.copy.select.source'), icon: 'warning' })
    return
  }
  const ok = await confirm({
    message: msg('sy.auth.group.copy.confirm', copyGrpCd.value),
    icon: 'question',
  })
  if (!ok) return
  if ((await copyAuthFrom(activeGrpRow.value!, copyGrpCd.value)) === 0) {
    await alert({ message: msg('sy.auth.group.info.copy.no.auth'), icon: 'information' })
  }
}

/**
 * onBtnScrnAddClick : 화면추가 버튼 클릭: 화면 선택 팝업 열기
 * @return  N/A
 */
async function onBtnScrnAddClick(): Promise<void> {
  if (await alertIfGrpLocked()) return
  openScrnPopup()
}

/**
 * onBtnAuthRowDelClick : 권한목록 행삭제 버튼 클릭: 선택한 화면 권한 삭제 처리
 * @return  N/A
 */
async function onBtnAuthRowDelClick(): Promise<void> {
  if (!(await deleteSelectedMenuAuthRows())) {
    await alert({ message: msg('sy.auth.group.screen.delete.select'), icon: 'warning' })
  }
}

/**
 * onBtnScrnSrchAddClick : 화면 선택 팝업의 추가 버튼 클릭: 선택한 화면을 권한목록에 추가
 * @return  N/A
 */
async function onBtnScrnSrchAddClick(): Promise<void> {
  if (!(await addSelectedScrns())) {
    await alert({ message: msg('sy.auth.group.screen.add.select'), icon: 'warning' })
  }
}

/**
 * onBtnScrnSrchCloseClick : 화면 선택 팝업의 닫기 버튼 클릭
 * @return  N/A
 */
function onBtnScrnSrchCloseClick(): void {
  isScrnPopupOpen.value = false
}

/**
 * onBtnSaveClick : 저장 버튼 클릭: 검증 후 권한그룹/화면/컴포넌트 권한 저장
 * @return  N/A
 */
async function onBtnSaveClick(): Promise<void> {
  await saveChanges({
    hasChanges: hasChanges.value,
    validate: validateGrpRows,
    save: () =>
      saveAuthGrpListApi({
        ...splitRowsByFlag(grpRows.value, toAuthGrp),
        menuSaveList: buildMenuSaveList(),
        cmpnSaveList: buildCmpnSaveList(),
      }),
    afterSave: searchGrpList,
  })
}

// ---------------------------------------------------------------------
// 페이지 라이프사이클
// ---------------------------------------------------------------------
/**
 * fetchPageData : 페이지 참조 데이터(메뉴, 프로그램) 조회
 * @return  N/A
 */
async function fetchPageData(): Promise<void> {
  await fetchReferenceData()
}

/**
 * initPageUi : 화면 초기화 및 최초 조회
 * @return  N/A
 */
function initPageUi(): void {
  resetSearchForm()
  clearGrids()
  searchGrpList()
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
        <Button id="btnSave" v-permission variant="outline" size="small" @click="onBtnSaveClick">저장</Button>
      </div>
    </header>

    <!-- 조회 조건 영역 -->
    <form id="frmSrch" class="search-area" @submit.prevent="onBtnSrchClick">
      <div class="search-fields">
        <div class="search-field">
          <span class="field-label">ID/그룹</span>
          <Input id="txtUsrGrpCd" v-model="searchForm.usrGrpCd" placeholder="그룹ID" @enter="onBtnSrchClick" />
          <Input id="txtUsrGrpNm" v-model="searchForm.usrGrpNm" placeholder="그룹명" @enter="onBtnSrchClick" />
        </div>
      </div>
      <div class="search-actions">
        <Button id="btnSrch" v-permission @click="onBtnSrchClick">조회</Button>
      </div>
    </form>

    <div class="content-area">
      <!-- 왼쪽: 그룹목록 + 그룹 유효기간 -->
      <div class="left-area">
        <div class="grid-area">
          <div class="area-header">
            <span class="area-title">그룹목록 - <strong id="lblTotCnt" class="total-count">{{ grpTotalCount }}건</strong></span>
            <div class="area-actions">
              <Button id="btnRowAdd" v-permission variant="outline" size="small" @click="onBtnRowAddClick">행추가</Button>
              <Button id="btnRowDel" v-permission variant="outline" size="small" @click="onBtnRowDelClick">행삭제</Button>
            </div>
          </div>
          <DataTable
            id="grdUsrGrpList"
            v-model:selected-rows="selectedGrpRows"
            :columns="grpColumns"
            :data="grpRows"
            :loading="isGrpLoading"
            :active-row="activeGrpRow"
            :page-size="10"
            selectable
            dense
            flag-column
            @row-click="onGrdUsrGrpListRowClick"
          >
            <template #cell-usrGrpCd="{ row }">
              <input
                v-model="row.usrGrpCd"
                class="cell-input"
                :class="{ 'is-required': row._flag === 'I' }"
                style="width: 130px"
                maxlength="20"
                :readonly="row._flag !== 'I'"
                @input="markRowUpdated(row)"
              />
            </template>
            <template #cell-usrGrpNm="{ row }">
              <input
                v-model="row.usrGrpNm"
                class="cell-input"
                :class="{ 'is-required': row._flag === 'I' }"
                style="width: 100%; min-width: 110px"
                maxlength="30"
                @input="markRowUpdated(row)"
              />
            </template>
          </DataTable>
        </div>

        <!-- 그룹 유효기간 -->
        <div class="detail-area">
          <div class="area-header">
            <span class="area-title">그룹 유효기간</span>
          </div>
          <div v-if="activeGrpRow" class="detail-form" :class="{ 'is-locked': isActiveGrpDeleted }">
            <!-- 유효기간: 시작일 / 종료일 2줄 -->
            <div class="detail-row is-top">
              <span class="field-label">유효기간</span>
              <div class="date-stack">
                <div class="date-line">
                  <span class="date-box">
                    <DatePicker
                      id="dtpStDt"
                      :model-value="ymdToDash(activeGrpRow.stDt)"
                      :presets="false"
                      size="small"
                      @update:model-value="setGrpRowDate(activeGrpRow, 'stDt', $event)"
                    />
                  </span>
                  <span class="range-sep">~</span>
                </div>
                <div class="date-line">
                  <span class="date-box">
                    <DatePicker
                      id="dtpEndDt"
                      :model-value="ymdToDash(activeGrpRow.endDt)"
                      :presets="false"
                      size="small"
                      @update:model-value="setGrpRowDate(activeGrpRow, 'endDt', $event)"
                    />
                  </span>
                  <!-- 윗줄 '~' 폭만큼 비워 두 날짜 입력칸 너비를 맞춘다 -->
                  <span class="range-sep is-placeholder" aria-hidden="true">~</span>
                </div>
              </div>
            </div>
            <div class="detail-col">
              <span class="field-label">비고</span>
              <textarea
                id="txaRmk"
                v-model="activeGrpRow.rmk"
                class="detail-textarea"
                maxlength="200"
                rows="3"
                @input="markRowUpdated(activeGrpRow)"
              />
            </div>
          </div>
          <p v-else class="detail-empty">{{ msg('sy.auth.group.empty.group.select') }}</p>
        </div>
      </div>

      <!-- 가운데: 권한목록 (TB_AUTH_GRP_MENU_R) -->
      <div class="grid-area">
        <div class="area-header">
          <span class="area-title">
            권한목록
            <span v-if="activeGrpRow" id="lblAuthGrpCd" class="sub-title">({{ activeGrpRow.usrGrpCd || '신규' }})</span>
            <span v-if="isActiveGrpDeleted" class="locked-note">(삭제 예정)</span>
            - <strong id="lblAuthTotCnt" class="total-count">{{ authRows.length }}건</strong>
          </span>
          <div class="area-actions">
            <span class="field-label">그룹</span>
            <span class="copy-grp-select">
              <Select id="cboCopyGrpCd" v-model="copyGrpCd" :options="copyGrpOptions" size="small" />
            </span>
            <Button id="btnAuthCopy" v-permission variant="outline" size="small" @click="onBtnAuthCopyClick">권한복사</Button>
            <Button id="btnScrnAdd" v-permission variant="outline" size="small" @click="onBtnScrnAddClick">화면추가</Button>
            <Button id="btnAuthRowDel" v-permission variant="outline" size="small" @click="onBtnAuthRowDelClick">행삭제</Button>
          </div>
        </div>
        <DataTable
          id="grdAuthList"
          v-model:selected-rows="selectedAuthRows"
          :columns="authColumns"
          :data="authRows"
          :loading="isAuthLoading"
          :active-row="activeAuthRow"
          :page-size="15"
          :empty-text="activeGrpRow ? msg('sy.auth.group.empty.screen.none') : msg('sy.auth.group.empty.screen.select.group')"
          :class="{ 'is-locked': isActiveGrpDeleted }"
          selectable
          dense
          flag-column
          @row-click="onGrdAuthListRowClick"
        >
          <template #cell-menuPath="{ row }">
            <span class="ellipsis-text" :title="getMenuPath(row.menuId)">{{ getMenuPath(row.menuId) }}</span>
          </template>
          <template #cell-prgmNm="{ row }">
            <span class="ellipsis-text prgm-nm" :title="`${getPrgmNm(row.prgmId, row.menuId)} (${row.prgmId})`">{{ getPrgmNm(row.prgmId, row.menuId) }}</span>
          </template>
        </DataTable>
      </div>

      <!-- 오른쪽: 컴포넌트 권한 (TB_PRGM_CMPN_M + TB_AUTH_GRP_CMPN_R) -->
      <AuthCmpnGrid
        grid-id="grdCmpnAuthList"
        :rows="cmpnAuthRows"
        :loading="isCmpnAuthLoading"
        :prgm-nm="activeAuthRow ? getPrgmNm(activeAuthRow.prgmId, activeAuthRow.menuId) : null"
        :is-prgm-deleted="activeAuthRow?._flag === 'D'"
        :locked="isCmpnAuthLocked"
        :all-checked="isAllCmpnAuthYn"
        @change="setCmpnAuthYn"
        @change-all="setAllCmpnAuthYn"
      />
    </div>

    <!-- 화면추가 팝업: 프로그램이 연결된 메뉴 목록 -->
    <AuthScreenAddPopup
      v-model="isScrnPopupOpen"
      v-model:keyword="scrnKeyword"
      v-model:selected-rows="selectedScrnRows"
      :candidates="scrnCandidates"
    >
      <template #footer>
        <Button id="btnScrnSrchAdd" v-permission @click="onBtnScrnSrchAddClick">추가</Button>
        <Button id="btnScrnSrchClose" v-permission variant="outline" @click="onBtnScrnSrchCloseClick">닫기</Button>
      </template>
    </AuthScreenAddPopup>
  </section>
</template>

<style scoped src="@/assets/page.css"></style>

<style scoped>
.page-actions, .area-actions, .search-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.field-label {
  min-width: 56px;
}

.area-actions .field-label {
  min-width: 0;
}

.content-area {
  display: grid;
  grid-template-columns: 380px minmax(0, 1fr) 360px;
  gap: 16px;
  align-items: start;
}

.left-area, .grid-area, .detail-area {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.left-area {
  gap: 16px;
}

.area-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 32px;
  flex-wrap: wrap;
}

.area-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--ds-ink);
}

.sub-title {
  color: var(--ds-ink-2);
  font-size: inherit;
}

.locked-note {
  font-weight: 600;
  color: var(--ds-stamp);
}

.detail-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px 16px;
  background: var(--ds-surface);
  border: 1px solid var(--ds-rule);
  border-radius: var(--ds-radius-panel);
}

.detail-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-col {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.detail-row.is-top {
  align-items: flex-start;
}

.detail-row.is-top > .field-label {
  line-height: 32px;
}

.date-stack {
  flex: 1 1 0;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.date-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 날짜 입력칸은 내용 폭만큼만 쓰고 '~' 를 시작일 바로 옆에 둔다 */
.date-line .date-box {
  flex: 0 0 auto;
}

.date-box {
  flex: 1 1 0;
  min-width: 0;
}

/* 상세 영역 폭에 맞춰 날짜 입력칸을 늘인다 */
.date-box :deep(.datepicker-input-box) {
  width: 100%;
  min-width: 0;
  gap: 6px;
  padding: 0 8px;
}

.range-sep {
  color: var(--ds-ink-2);
}

.range-sep.is-placeholder {
  visibility: hidden;
}

.detail-textarea {
  width: 100%;
  padding: 8px 10px;
  font: inherit;
  font-size: 13px;
  color: var(--ds-ink);
  background: var(--ds-surface);
  border: 1px solid var(--ds-rule-strong);
  border-radius: var(--ds-radius-control);
  box-sizing: border-box;
  resize: vertical;
  transition: border-color var(--ds-ease);
}

.detail-textarea:focus {
  outline: none;
  border-color: var(--ds-slate-700);
  box-shadow: var(--ds-focus-field);
}

.detail-empty {
  margin: 0;
  padding: 16px;
  font-size: 13px;
  color: var(--ds-ink-2);
  background: var(--ds-surface);
  border: 1px solid var(--ds-rule);
  border-radius: var(--ds-radius-panel);
}

/* 삭제(D) 표시된 그룹·화면의 하위 데이터는 편집 불가 */
.is-locked {
  pointer-events: none;
  opacity: 0.5;
}

.copy-grp-select {
  width: 200px;
}

.ellipsis-text {
  display: inline-block;
  max-width: 200px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  vertical-align: middle;
}

.ellipsis-text.prgm-nm {
  max-width: 130px;
}

@media (max-width: 1500px) {
  .content-area {
    grid-template-columns: 380px minmax(0, 1fr);
  }

  .content-area > .grid-area:last-child {
    grid-column: 1 / -1;
  }
}

@media (max-width: 1100px) {
  .content-area {
    grid-template-columns: 1fr;
  }
}
</style>
