<script setup lang="ts">
/*****************************************************************************************
	Program ID   : SYAU030
	Program Type : vue
	Program Name : 그룹별사용자관리
	Description  : 권한그룹 목록(그룹별 사용자수)을 조회하고, 선택한 그룹에 속한 사용자를 조회·추가·제외한다.
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-12  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { computed, ref, onMounted } from 'vue'
import { Button, Input, Select, DataTable, PopupWindow, useDialog } from '@/components/common'
import { useGridSave } from '@/composables/useGridSave'
import { msg } from '@/utils/message'
import {
  getAuthGrpUsrCntListApi,
  getAuthGrpUsrListApi,
  saveAuthGrpUsrApi,
  type AuthGrpUsr,
  type AuthGrpUsrCnt,
} from '@/api/sy/authGroupUser'
import { DEPT_OPTIONS, getUsrMngListApi, type UsrMng } from '@/api/sy/usrAuthMng'
import { getCommCdOptions } from '@/api/common/commonCode'
import { USE_YN_OPTIONS, codeName, withAll, type CodeOption } from '@/utils/code'
import { deleteFlaggedRows, hasRowChanges, isDeletedRow, renumber, splitRowsByFlag, type RowFlag } from '@/utils/rowFlag'

interface GrpRow extends AuthGrpUsrCnt {
  no?: number
}

interface UsrRow extends AuthGrpUsr {
  no?: number
  _flag: RowFlag
}

const PAGE_TITLE = '그룹별사용자관리'
const PAGE_ID = 'SYAU030'

const grpColumns = [
  { key: 'no', label: 'NO', width: '48px', align: 'center' },
  { key: 'usrGrpCd', label: '그룹ID', width: '150px', sortable: true },
  { key: 'usrGrpNm', label: '그룹명', sortable: true },
  { key: 'usrCnt', label: '사용자수', width: '80px', align: 'right', sortable: true },
]

const usrColumns = [
  { key: 'no', label: 'NO', width: '48px', align: 'center' },
  { key: 'deptCd', label: '부서명', width: '130px' },
  { key: 'usrId', label: '사용자ID', width: '110px', sortable: true },
  { key: 'usrNm', label: '사용자명', sortable: true },
  { key: 'usrClsCd', label: '사용자유형', width: '100px', align: 'center' },
]

/** 사용자추가 팝업 컬럼 */
const usrSrchColumns = [
  { key: 'usrId', label: '사용자ID', width: '110px' },
  { key: 'usrNm', label: '사용자명' },
  { key: 'deptCd', label: '부서명', width: '130px' },
  { key: 'usrClsCd', label: '사용자유형', width: '100px', align: 'center' },
]

const { alert, confirm } = useDialog()
const { saveChanges, confirmDiscardChanges } = useGridSave()

const useYnOptions = withAll(USE_YN_OPTIONS)
/** 사용자유형 공통코드 (SY/USR_CLS_CD) */
const usrClsOptions = ref<CodeOption[]>([])

const searchForm = ref({
  usrGrpCd: '',
  usrGrpNm: '',
  useYn: '',
})
const grpRows = ref<GrpRow[]>([])
const usrRows = ref<UsrRow[]>([])
const activeGrp = ref<GrpRow | null>(null)
const isGrpLoading = ref(false)
const isUsrLoading = ref(false)
const selectedUsrRows = ref<UsrRow[]>([])

// 사용자추가 팝업
const isUsrPopupOpen = ref(false)
const usrKeyword = ref('')
const allUsrList = ref<UsrMng[]>([])
const selectedUsrCandidates = ref<UsrMng[]>([])

/** 저장하지 않은 사용자 추가/삭제가 있는지 여부 */
const hasChanges = computed(() => hasRowChanges(usrRows.value))

/**
 * 사용자추가 팝업 후보: 사용 중인 사용자 중 현재 그룹에 없는 사용자 + 삭제(D) 표시한 사용자 (검색어로 ID·이름 필터)
 * 삭제 표시한 사용자를 다시 고르면 새 행을 만들지 않고 원래 행의 삭제 표시를 되돌린다 (onBtnUsrSrchAddClick)
 */
const usrCandidates = computed(() => {
  const grpUsrIds = new Set(usrRows.value.filter((row) => !isDeletedRow(row)).map((row) => row.usrId))
  const deletedUsrIds = new Set(usrRows.value.filter((row) => isDeletedRow(row)).map((row) => row.usrId))
  const keyword = usrKeyword.value.trim().toUpperCase()
  return allUsrList.value.filter(
    (usr) =>
      (usr.useYn === 'Y' || deletedUsrIds.has(usr.usrId)) &&
      !grpUsrIds.has(usr.usrId) &&
      (!keyword || usr.usrId.toUpperCase().includes(keyword) || usr.usrNm.toUpperCase().includes(keyword)),
  )
})

/** 조회된 그룹의 사용자수 합계 */
const totUsrCnt = computed(() => grpRows.value.reduce((sum, grp) => sum + grp.usrCnt, 0))

/**
 * searchUsrList : 선택한 권한그룹에 속한 사용자 목록을 조회한다.
 * @param   grp - 선택한 권한그룹 (없으면 사용자 목록을 비운다)
 * @return  N/A
 */
async function searchUsrList(grp: GrpRow | null): Promise<void> {
  activeGrp.value = grp
  selectedUsrRows.value = []
  if (!grp) {
    usrRows.value = []
    return
  }
  isUsrLoading.value = true
  try {
    const res = await getAuthGrpUsrListApi(grp.usrGrpCd)
    usrRows.value = res.data.map((usr, idx) => ({ ...usr, no: idx + 1, _flag: '' as RowFlag }))
  } finally {
    isUsrLoading.value = false
  }
}

/**
 * searchGrpList : 검색 조건으로 권한그룹 목록을 조회하고 사용자 목록을 표시한다.
 * @param   keepUsrGrpCd - 조회 후 다시 선택할 권한그룹코드 (없거나 목록에 없으면 첫 번째 그룹)
 * @return  N/A
 */
async function searchGrpList(keepUsrGrpCd?: string): Promise<void> {
  isGrpLoading.value = true
  try {
    const res = await getAuthGrpUsrCntListApi(searchForm.value)
    grpRows.value = res.data.map((grp, idx) => ({ ...grp, no: idx + 1 }))
  } finally {
    isGrpLoading.value = false
  }
  const keepGrp = grpRows.value.find((grp) => grp.usrGrpCd === keepUsrGrpCd)
  await searchUsrList(keepGrp ?? grpRows.value[0] ?? null)
  if (grpRows.value.length === 0) {
    await alert({ message: msg('cm.info.no.data'), icon: 'information' })
  }
}

// ---------------------------------------------------------------------
// 이벤트 핸들러
// ---------------------------------------------------------------------
/**
 * onBtnSrchClick : 조회 버튼 클릭 시 권한그룹 목록을 조회한다.
 * @return  N/A
 */
async function onBtnSrchClick(): Promise<void> {
  if (!(await confirmDiscardChanges(hasChanges.value))) return
  await searchGrpList()
}

/**
 * onGrdAuthGrpListRowClick : 그룹목록 그리드 행 클릭 시 해당 그룹에 속한 사용자 목록을 조회한다.
 * @param   row - 클릭한 권한그룹 행
 * @return  N/A
 */
async function onGrdAuthGrpListRowClick(row: GrpRow): Promise<void> {
  if (row === activeGrp.value) return
  if (hasChanges.value) {
    const ok = await confirm({ message: msg('sy.auth.group.user.confirm.discard.and.select') })
    if (!ok) return
  }
  await searchUsrList(row)
}

/**
 * onBtnUsrAddClick : 사용자추가 버튼 클릭 시 사용자 선택 팝업을 연다.
 * @return  N/A
 */
async function onBtnUsrAddClick(): Promise<void> {
  if (!activeGrp.value) {
    await alert({ message: msg('sy.auth.group.select.required'), icon: 'warning' })
    return
  }
  allUsrList.value = (await getUsrMngListApi({})).data
  usrKeyword.value = ''
  selectedUsrCandidates.value = []
  isUsrPopupOpen.value = true
}

/**
 * onBtnUsrRowDelClick : 행삭제 버튼 클릭 시 선택한 사용자를 삭제 표시한다. (추가한 행은 바로 제거)
 * @return  N/A
 */
async function onBtnUsrRowDelClick(): Promise<void> {
  if (selectedUsrRows.value.length === 0) {
    await alert({ message: msg('sy.auth.group.user.delete.select'), icon: 'warning' })
    return
  }
  usrRows.value = deleteFlaggedRows(usrRows.value, selectedUsrRows.value)
  selectedUsrRows.value = []
  renumber(usrRows.value)
}

/**
 * onBtnUsrSrchAddClick : 사용자 선택 팝업의 추가 버튼 클릭 시 선택한 사용자를 사용자 목록에 추가한다.
 *                        (삭제(D) 표시한 사용자는 새 행을 만들지 않고 원래 행의 삭제 표시를 되돌린다)
 * @return  N/A
 */
async function onBtnUsrSrchAddClick(): Promise<void> {
  if (!activeGrp.value || selectedUsrCandidates.value.length === 0) {
    await alert({ message: msg('sy.auth.group.user.add.select'), icon: 'warning' })
    return
  }
  const usrGrpCd = activeGrp.value.usrGrpCd
  const deletedRowMap = new Map(usrRows.value.filter((row) => isDeletedRow(row)).map((row) => [row.usrId, row]))
  const newUsrList = selectedUsrCandidates.value.filter((usr) => {
    const deletedRow = deletedRowMap.get(usr.usrId)
    if (!deletedRow) return true
    deletedRow._flag = ''
    return false
  })
  usrRows.value.push(
    ...newUsrList.map((usr) => ({
      usrGrpCd,
      usrId: usr.usrId,
      usrNm: usr.usrNm,
      usrClsCd: usr.usrClsCd,
      deptCd: usr.deptCd,
      _flag: 'I' as RowFlag,
    })),
  )
  renumber(usrRows.value)
  isUsrPopupOpen.value = false
}

/**
 * onBtnUsrSrchCloseClick : 사용자 선택 팝업의 닫기 버튼 클릭
 * @return  N/A
 */
function onBtnUsrSrchCloseClick(): void {
  isUsrPopupOpen.value = false
}

/**
 * onBtnSaveClick : 저장 버튼 클릭 시 선택한 그룹의 사용자 추가/삭제분을 저장한다.
 * @return  N/A
 */
async function onBtnSaveClick(): Promise<void> {
  const usrGrpCd = activeGrp.value?.usrGrpCd ?? ''
  await saveChanges({
    hasChanges: !!activeGrp.value && hasChanges.value,
    save: () => {
      const { insertList, deleteList } = splitRowsByFlag(usrRows.value, (row) => row.usrId)
      return saveAuthGrpUsrApi({ usrGrpCd, insertList, deleteList })
    },
    // 사용자수 갱신을 위해 그룹목록을 다시 조회하고 같은 그룹을 선택한다
    afterSave: () => searchGrpList(usrGrpCd),
  })
}

// ---------------------------------------------------------------------
// 페이지 라이프사이클
// ---------------------------------------------------------------------
/**
 * fetchPageData : 화면에서 사용할 참조 데이터(사용자유형 공통코드 SY/USR_CLS_CD)를 조회한다.
 * @return  N/A
 */
async function fetchPageData(): Promise<void> {
  usrClsOptions.value = await getCommCdOptions('SY', 'USR_CLS_CD')
}

/**
 * initPageUi : 화면 요소를 초기화하고 최초 목록을 조회한다.
 * @return  N/A
 */
function initPageUi(): void {
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

    <form id="frmSrch" class="search-area" @submit.prevent="onBtnSrchClick">
      <div class="search-fields">
        <div class="search-field">
          <span class="field-label">ID/그룹</span>
          <Input id="txtUsrGrpCd" v-model="searchForm.usrGrpCd" placeholder="그룹ID" @enter="onBtnSrchClick" />
          <Input id="txtUsrGrpNm" v-model="searchForm.usrGrpNm" placeholder="그룹명" @enter="onBtnSrchClick" />
        </div>
        <label class="search-field">
          <span class="field-label">사용여부</span>
          <Select id="cboUseYn" v-model="searchForm.useYn" :options="useYnOptions" />
        </label>
      </div>
      <div class="search-actions">
        <Button id="btnSrch" v-permission @click="onBtnSrchClick">조회</Button>
      </div>
    </form>

    <div class="content-area">
      <!-- 왼쪽: 그룹목록 -->
      <div class="grid-area">
        <div class="grid-header">
          <span class="grid-title">
            그룹목록 - <strong id="lblGrpTotCnt" class="total-count">{{ grpRows.length.toLocaleString() }}건</strong>
          </span>
          <span class="grid-guide">
            사용자 합계 <strong id="lblUsrTotCnt" class="total-count">{{ totUsrCnt.toLocaleString() }}명</strong>
          </span>
        </div>
        <DataTable
          id="grdAuthGrpList"
          class="nowrap-grid"
          :columns="grpColumns"
          :data="grpRows"
          :loading="isGrpLoading"
          :page-size="20"
          :active-row="activeGrp"
          dense
          @row-click="onGrdAuthGrpListRowClick"
        >
          <template #cell-usrCnt="{ row }">{{ row.usrCnt.toLocaleString() }}</template>
        </DataTable>
      </div>

      <!-- 오른쪽: 선택한 그룹의 사용자 목록 -->
      <div class="grid-area">
        <div class="grid-header">
          <span class="grid-title">
            사용자 목록
            <span v-if="activeGrp" class="sub-title">({{ activeGrp.usrGrpNm }})</span>
            - <strong id="lblUsrCnt" class="total-count">{{ usrRows.length.toLocaleString() }}건</strong>
          </span>
          <div class="grid-actions">
            <Button id="btnUsrAdd" v-permission variant="outline" size="small" @click="onBtnUsrAddClick">사용자추가</Button>
            <Button id="btnUsrRowDel" v-permission variant="outline" size="small" @click="onBtnUsrRowDelClick">행삭제</Button>
          </div>
        </div>
        <DataTable
          id="grdAuthGrpUsrList"
          v-model:selected-rows="selectedUsrRows"
          class="nowrap-grid"
          :columns="usrColumns"
          :data="usrRows"
          :loading="isUsrLoading"
          :page-size="20"
          :empty-text="activeGrp ? msg('sy.auth.group.user.empty.none') : msg('sy.auth.group.empty.group.select')"
          selectable
          flag-column
          dense
        >
          <template #cell-deptCd="{ row }">{{ codeName(DEPT_OPTIONS, row.deptCd) }}</template>
          <template #cell-usrClsCd="{ row }">{{ codeName(usrClsOptions, row.usrClsCd) }}</template>
        </DataTable>
      </div>
    </div>

    <!-- 사용자추가 팝업: 현재 그룹에 없는 사용 중인 사용자 + 삭제(D) 표시한 사용자 -->
    <PopupWindow id="mdlUsrSrch" v-model="isUsrPopupOpen" title="사용자 추가" :width="680">
      <div class="popup-search">
        <span class="field-label">검색</span>
        <Input id="txtUsrKeyword" v-model="usrKeyword" placeholder="사용자ID·사용자명" />
      </div>
      <DataTable
        id="grdUsrSrchList"
        v-model:selected-rows="selectedUsrCandidates"
        class="nowrap-grid"
        :columns="usrSrchColumns"
        :data="usrCandidates"
        :page-size="8"
        :empty-text="msg('sy.auth.group.user.empty.addable.none')"
        selectable
        dense
      >
        <template #cell-deptCd="{ row }">{{ codeName(DEPT_OPTIONS, row.deptCd) }}</template>
        <template #cell-usrClsCd="{ row }">{{ codeName(usrClsOptions, row.usrClsCd) }}</template>
      </DataTable>
      <template #footer>
        <Button id="btnUsrSrchAdd" v-permission @click="onBtnUsrSrchAddClick">추가</Button>
        <Button id="btnUsrSrchClose" v-permission variant="outline" @click="onBtnUsrSrchCloseClick">닫기</Button>
      </template>
    </PopupWindow>
  </section>
</template>

<style scoped src="@/assets/page.css"></style>

<style scoped>
/* 왼쪽 그룹목록 + 오른쪽 사용자 목록 */
.content-area {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.4fr);
  gap: 16px;
  align-items: start;
}

@media (max-width: 1280px) {
  .content-area {
    grid-template-columns: 1fr;
  }
}
</style>
