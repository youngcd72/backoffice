<script setup lang="ts">
/*****************************************************************************************
	Program ID   : SYAU010
	Program Type : vue
	Program Name : 사용자권한관리
	Description  : 사용자 정보(부서 포함)·화면 권한(TB_USR_MENU_R)·컴포넌트 권한(TB_USR_CMPN_R)을
	               조회·등록·수정·삭제·복사하고, 협력업체(P) 사용자의 비밀번호를 초기화하는 화면
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
import { Button, Input, Select, DataTable, DatePicker, ComSearchCode, useDialog, useToast } from '@/components/common'
import { useGridSave } from '@/composables/useGridSave'
import {
  DEPT_OPTIONS,
  getUsrMngListApi,
  existsUsrIdApi,
  getUsrMenuAuthListApi,
  getUsrCmpnAuthListApi,
  saveUsrAuthApi,
  initUsrPwdApi,
  type UsrMng,
  type UsrMenuAuthSave,
  type UsrCmpnAuthSave,
} from '@/api/sy/usrAuthMng'
import { getCommCdOptions } from '@/api/common/commonCode'
import { useMenuCmpnAuth, type MenuAuthRow } from '@/composables/useMenuCmpnAuth'
import { USE_YN_OPTIONS, withAll } from '@/utils/code'
import { dashToYmd, todayYmd, ymdToDash } from '@/utils/date'
import { isValidYmd } from '@/utils/validate'
import { deleteFlaggedRows, hasRowChanges, markRowUpdated, nextRowId, renumber, splitRowsByFlag, type RowFlag } from '@/utils/rowFlag'
import AuthCmpnGrid from './components/AuthCmpnGrid.vue'
import AuthScreenAddPopup from './components/AuthScreenAddPopup.vue'

interface UsrRow extends UsrMng {
  id: number
  no?: number
  _flag: RowFlag
}

/** 저장 전 검증 오류 (오류 행을 활성화해서 보여줌) */
interface UsrValidateError {
  message: string
  row: UsrRow
}

type UsrTextKey = keyof UsrMng

const PAGE_TITLE = '사용자권한관리'
const PAGE_ID = 'SYAU010'

/** 사용자유형: 비밀번호 초기화 대상 (P 협력업체) */
const PWD_INIT_USR_CLS_CD = 'P'

/** 이메일 형식 (utils/validate.ts 의 email 규칙과 동일) */
const EMAIL_PATTERN = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
/** 전화번호 형식: 0으로 시작, 하이픈 선택 (예: 010-1234-5678, 0212345678) */
const MOBL_NO_PATTERN = /^0\d{1,2}-?\d{3,4}-?\d{4}$/

const ALL_OPTION = { label: '전체', value: '' }
const NONE_OPTION = { label: '선택', value: '' }

/** 사용자구분 공통코드 (SY/USR_CLS_CD) */
const usrClsOptions = ref<Array<{ label: string; value: string }>>([])
const usrClsSrchOptions = computed(() => [ALL_OPTION, ...usrClsOptions.value])
const usrClsDtlOptions = computed(() => [NONE_OPTION, ...usrClsOptions.value])
const useYnSrchOptions = withAll(USE_YN_OPTIONS)
const deptDtlOptions = [NONE_OPTION, ...DEPT_OPTIONS]
/** 사용자상태(재직여부) 공통코드 (SY/USR_STAT_CD) */
const usrStatOptions = ref<Array<{ label: string; value: string }>>([])
const usrStatDtlOptions = computed(() => [NONE_OPTION, ...usrStatOptions.value])

const usrColumns = [
  { key: 'no', label: 'NO', width: '40px', align: 'center' },
  { key: 'usrId', label: '사용자ID', width: '110px' },
  { key: 'usrNm', label: '사용자명' },
  { key: 'deptNm', label: '부서명', width: '100px' },
]

const menuAuthColumns = [
  { key: 'no', label: 'NO', width: '40px', align: 'center' },
  { key: 'menuId', label: '메뉴ID', width: '80px' },
  { key: 'menuPath', label: '메뉴' },
  { key: 'prgmNm', label: '프로그램', width: '150px' },
]

const { alert, confirm } = useDialog()
const { saveChanges, confirmDiscardChanges } = useGridSave()
const toast = useToast()

// 조회 조건
const searchForm = ref({ usrId: '', usrNm: '', usrClsCd: '', useYn: 'Y' })

// 사용자 그리드
const usrRows = ref<UsrRow[]>([])
const selectedUsrRows = ref<UsrRow[]>([])
const activeUsrRow = ref<UsrRow | null>(null)
const isUsrLoading = ref(false)
const copyUsrId = ref('')
const copyUsrNm = ref('')

// 화면/컴포넌트 권한 (TB_USR_MENU_R, TB_USR_CMPN_R)
const {
  getMenuPath,
  getPrgmNm,
  menuAuthRows,
  selectedMenuAuthRows,
  activeMenuAuthRow,
  isMenuAuthLoading: isStateLoading,
  activeMenuAuthState: activeUsrState,
  isActiveOwnerDeleted: isActiveUsrDeleted,
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
  ownerRows: usrRows,
  activeOwnerRow: activeUsrRow,
  getOwnerKey: (row) => row.usrId.trim(),
  fetchMenuAuthList: async (usrId) => (await getUsrMenuAuthListApi(usrId)).data,
  fetchCmpnAuthList: async (usrId, prgmId) => (await getUsrCmpnAuthListApi(usrId, prgmId)).data,
})

const usrTotalCount = computed(() => usrRows.value.length)

const isPwdInitEnabled = computed(
  () => !!activeUsrRow.value && activeUsrRow.value._flag !== 'I' && activeUsrRow.value._flag !== 'D' && activeUsrRow.value.usrClsCd === PWD_INIT_USR_CLS_CD,
)

const hasChanges = computed(() => hasRowChanges(usrRows.value) || hasAuthChanges.value)

/**
 * getDeptNm : 부서코드의 부서명 조회
 * @param   deptCd - 부서코드
 * @return  부서명 (없으면 빈 문자열)
 */
function getDeptNm(deptCd: string): string {
  return DEPT_OPTIONS.find((dept) => dept.value === deptCd)?.label ?? ''
}

/**
 * resetSearchForm : 조회 조건 초기화 (사용여부는 사용)
 * @return  N/A
 */
function resetSearchForm(): void {
  searchForm.value = { usrId: '', usrNm: '', usrClsCd: '', useYn: 'Y' }
}

/**
 * clearGrids : 사용자·권한 목록 그리드와 선택/편집 상태 초기화
 * @return  N/A
 */
function clearGrids(): void {
  usrRows.value = []
  selectedUsrRows.value = []
  activeUsrRow.value = null
  clearAuthStates()
  copyUsrId.value = ''
  copyUsrNm.value = ''
}

/**
 * searchUsrList : 사용자 목록 조회 후 이전 선택 행(없으면 첫 행)을 활성화
 * @return  N/A
 */
async function searchUsrList(): Promise<void> {
  const prevActiveUsrId = activeUsrRow.value?.usrId.trim() ?? ''
  const prevActiveAuthKey = getActiveMenuAuthKey()
  isUsrLoading.value = true
  try {
    const res = await getUsrMngListApi(searchForm.value)
    clearGrids()
    usrRows.value = res.data.map((usr) => ({ ...usr, id: nextRowId(), _flag: '' as RowFlag }))
    renumber(usrRows.value)
  } finally {
    isUsrLoading.value = false
  }
  if (usrRows.value.length === 0) {
    await alert({ message: msg('cm.info.no.data'), icon: 'information' })
    return
  }
  const prevRow = usrRows.value.find((row) => row.usrId === prevActiveUsrId)
  await activateUsrRow(prevRow ?? usrRows.value[0], prevActiveAuthKey)
}

/**
 * activateUsrRow : 사용자 행을 선택하고 상세정보·권한 목록 표시 (이전에 보던 화면이 있으면 다시 선택)
 * @param   row - 선택할 사용자 행
 * @param   prevAuthKey - 이전에 선택했던 "메뉴ID/프로그램ID" 키
 * @return  N/A
 */
async function activateUsrRow(row: UsrRow, prevAuthKey = ''): Promise<void> {
  activeUsrRow.value = row
  copyUsrId.value = ''
  copyUsrNm.value = ''
  await activateOwnerAuth(row, prevAuthKey)
}

/**
 * addUsrRow : 사용자 신규 행 추가 후 활성화 (시작일자 오늘, 종료일자 99991231, 사용여부 Y)
 * @return  N/A
 */
function addUsrRow(): void {
  usrRows.value.unshift({
    usrId: '',
    usrNm: '',
    empNo: '',
    deptCd: '',
    emlAddr: '',
    moblNo: '',
    usrStatCd: 'T',
    usrLockYn: 'N',
    usrClsCd: '',
    ecomDt: '',
    resiDt: '',
    stDt: todayYmd(),
    endDt: '99991231',
    useYn: 'Y',
    id: nextRowId(),
    _flag: 'I',
  })
  renumber(usrRows.value)
  activateUsrRow(usrRows.value[0])
}

/**
 * setUsrField : 활성 사용자 행의 항목 값 변경 및 수정 표시 (값이 같으면 무시)
 * @param   key - 변경할 항목
 * @param   value - 변경할 값
 * @return  N/A
 */
function setUsrField(key: UsrTextKey, value: string | null | undefined): void {
  const row = activeUsrRow.value
  if (!row || row._flag === 'D') return
  const nextValue = value ?? ''
  if (row[key] === nextValue) return
  row[key] = nextValue
  markRowUpdated(row)
}

/**
 * onChkDtlUsrLockYnChange : 잠김여부 체크박스 변경 시 활성 사용자의 USR_LOCK_YN 을 Y/N 으로 변경
 * @param   event - 체크박스 change 이벤트
 * @return  N/A
 */
function onChkDtlUsrLockYnChange(event: Event): void {
  setUsrField('usrLockYn', (event.target as HTMLInputElement).checked ? 'Y' : 'N')
}

/**
 * onChkDtlUseYnChange : 사용여부 체크박스 변경 시 활성 사용자 행의 사용여부(Y/N) 변경
 * @param   event - 체크박스 change 이벤트
 * @return  N/A
 */
function onChkDtlUseYnChange(event: Event): void {
  setUsrField('useYn', (event.target as HTMLInputElement).checked ? 'Y' : 'N')
}

/**
 * setUsrDate : 활성 사용자의 시작일자/종료일자 변경
 * @param   key - 변경할 일자 항목 (stDt/endDt)
 * @param   value - DatePicker 일자
 * @return  N/A
 */
function setUsrDate(key: 'stDt' | 'endDt', value: string | null): void {
  setUsrField(key, dashToYmd(value))
}

/**
 * validateUsrRows : 사용자 필수값·일자·중복(화면 내, 기존 등록) 검증
 * @return  첫 오류 (문제 없으면 null)
 */
async function validateUsrRows(): Promise<UsrValidateError | null> {
  const usrIdCnt = new Map<string, number>()
  usrRows.value.forEach((row) => {
    if (row._flag === 'D') return
    const usrId = row.usrId.trim()
    if (usrId) usrIdCnt.set(usrId, (usrIdCnt.get(usrId) ?? 0) + 1)
  })
  for (const row of usrRows.value) {
    if (row._flag === '' || row._flag === 'D') continue
    const rowLabel = msg('sy.user.auth.manage.row.label', row.no)
    const usrId = row.usrId.trim()
    if (!usrId) return { message: msg('sy.user.auth.manage.validate.id.required', rowLabel), row }
    if (!row.usrNm.trim()) return { message: msg('sy.user.auth.manage.validate.name.required', rowLabel), row }
    if ((usrIdCnt.get(usrId) ?? 0) > 1) return { message: msg('sy.user.auth.manage.validate.id.duplicate', rowLabel, usrId), row }
    if (!row.stDt || !row.endDt) return { message: msg('sy.user.auth.manage.validate.period.required', rowLabel), row }
    if (!isValidYmd(row.stDt) || !isValidYmd(row.endDt)) return { message: msg('sy.user.auth.manage.validate.period.invalid', rowLabel), row }
    if (row.stDt > row.endDt) return { message: msg('sy.user.auth.manage.validate.period.reversed', rowLabel), row }
    if (row.emlAddr.trim() && !EMAIL_PATTERN.test(row.emlAddr.trim())) {
      return { message: msg('sy.user.auth.manage.validate.email.format', rowLabel), row }
    }
    if (row.moblNo.trim() && !MOBL_NO_PATTERN.test(row.moblNo.trim())) {
      return { message: msg('sy.user.auth.manage.validate.mobile.format', rowLabel), row }
    }
    if (row._flag === 'I' && (await existsUsrIdApi(usrId)).data) {
      return { message: msg('sy.user.auth.manage.validate.id.exists', usrId), row }
    }
  }
  return null
}

/**
 * toUsrMng : 사용자 행을 저장용 사용자 데이터로 변환
 * @param   row - 사용자 행
 * @return  저장용 사용자 데이터
 */
function toUsrMng(row: UsrRow): UsrMng {
  const { id: _id, no: _no, _flag: _rowFlag, ...usr } = row
  return { ...usr, usrId: usr.usrId.trim(), usrNm: usr.usrNm.trim(), emlAddr: usr.emlAddr.trim(), moblNo: usr.moblNo.trim() }
}

/**
 * buildMenuAuthSaveListDto : 변경된 사용자별 화면 권한 저장 목록 생성
 * @return  사용자별 화면 권한 저장 목록
 */
function buildMenuAuthSaveListDto(): UsrMenuAuthSave[] {
  return buildMenuAuthSaveList().map(({ ownerKey: usrId, insertList, deleteList }) => ({
    usrId,
    insertList: insertList.map((item) => ({ usrId, ...item })),
    deleteList: deleteList.map((item) => ({ usrId, ...item })),
  }))
}

/**
 * buildCmpnAuthSaveListDto : 변경된 사용자·프로그램별 컴포넌트 권한 저장 목록 생성
 * @return  사용자·프로그램별 컴포넌트 권한 저장 목록
 */
function buildCmpnAuthSaveListDto(): UsrCmpnAuthSave[] {
  return buildCmpnAuthSaveList().map(({ ownerKey: usrId, prgmId, insertList, updateList }) => ({
    usrId,
    prgmId,
    insertList: insertList.map((item) => ({ usrId, ...item })),
    updateList: updateList.map((item) => ({ usrId, ...item })),
  }))
}

/**
 * alertIfUsrLocked : 사용자 미선택 또는 삭제 표시된 사용자이면 안내 후 변경 불가 판단
 * @return  변경할 수 없으면 true
 */
async function alertIfUsrLocked(): Promise<boolean> {
  if (!activeUsrRow.value || !activeUsrState.value) {
    await alert({ message: msg('sy.user.auth.manage.select.required'), icon: 'warning' })
    return true
  }
  if (isActiveUsrDeleted.value) {
    await alert({ message: msg('sy.user.auth.manage.deleted.readonly'), icon: 'warning' })
    return true
  }
  return false
}

// ---------------------------------------------------------------------
// 이벤트 핸들러
// ---------------------------------------------------------------------
/**
 * onBtnSrchClick : 조회 버튼 클릭 (저장하지 않은 변경이 있으면 확인)
 * @return  N/A
 */
async function onBtnSrchClick(): Promise<void> {
  if (!(await confirmDiscardChanges(hasChanges.value))) return
  await searchUsrList()
}

/**
 * onGrdUsrListRowClick : 사용자 그리드 행 클릭: 해당 사용자 활성화
 * @param   row - 클릭한 사용자 행
 * @return  N/A
 */
function onGrdUsrListRowClick(row: UsrRow): void {
  if (row !== activeUsrRow.value) activateUsrRow(row)
}

/**
 * onGrdUsrMenuAuthListRowClick : 권한 목록 그리드 행 클릭: 해당 화면 활성화
 * @param   row - 클릭한 권한 목록 행
 * @return  N/A
 */
function onGrdUsrMenuAuthListRowClick(row: MenuAuthRow): void {
  if (row !== activeMenuAuthRow.value) activateMenuAuthRow(row)
}

/**
 * onBtnRowAddClick : 사용자 행추가 버튼 클릭
 * @return  N/A
 */
function onBtnRowAddClick(): void {
  addUsrRow()
}

/**
 * onBtnRowDelClick : 사용자 행삭제 버튼 클릭: 선택한 사용자와 화면·컴포넌트 권한 삭제 처리
 * @return  N/A
 */
async function onBtnRowDelClick(): Promise<void> {
  if (selectedUsrRows.value.length === 0) {
    await alert({ message: msg('sy.user.auth.manage.delete.select'), icon: 'warning' })
    return
  }
  const ok = await confirm({ message: msg('sy.user.auth.manage.delete.confirm'), icon: 'question' })
  if (!ok) return
  // 등록(I) 행은 바로 제거하고, 나머지는 D 로 표시한다 (저장 시 삭제)
  usrRows.value = deleteFlaggedRows(usrRows.value, selectedUsrRows.value)
  pruneAuthStates()
  selectedUsrRows.value = []
  renumber(usrRows.value)
  if (activeUsrRow.value && !usrRows.value.includes(activeUsrRow.value)) {
    activeUsrRow.value = null
    activeMenuAuthRow.value = null
    if (usrRows.value.length > 0) await activateUsrRow(usrRows.value[0])
  }
}

/**
 * onBtnPwdInitClick : 비밀번호 초기화 버튼 클릭: 사용자유형 P(협력업체) 사용자의 비밀번호 초기화
 * @return  N/A
 */
async function onBtnPwdInitClick(): Promise<void> {
  const row = activeUsrRow.value
  if (!row || !isPwdInitEnabled.value) {
    await alert({ message: msg('sy.user.auth.manage.password.init.not.allowed'), icon: 'warning' })
    return
  }
  const ok = await confirm({ message: msg('sy.user.auth.manage.password.init.confirm', row.usrId), icon: 'question' })
  if (!ok) return
  const res = await initUsrPwdApi(row.usrId)
  if (!res.success) {
    await alert({ message: res.message ?? msg('cm.error.save.failed'), icon: 'error' })
    return
  }
  toast.success(msg('sy.user.auth.manage.password.init.done', row.usrId))
}

/**
 * onBtnAuthCopyClick : 권한복사 버튼 클릭: 원본 사용자의 화면·컴포넌트 권한을 현재 사용자에 복사
 * @return  N/A
 */
async function onBtnAuthCopyClick(): Promise<void> {
  if (await alertIfUsrLocked()) return
  const sourceUsrId = copyUsrId.value.trim()
  if (!sourceUsrId) {
    await alert({ message: msg('sy.user.auth.manage.copy.select.source'), icon: 'warning' })
    return
  }
  const usrRow = activeUsrRow.value!
  if (sourceUsrId === usrRow.usrId.trim()) {
    await alert({ message: msg('sy.user.auth.manage.copy.same.user'), icon: 'warning' })
    return
  }
  const ok = await confirm({ message: msg('sy.user.auth.manage.copy.cmpn.confirm', sourceUsrId), icon: 'question' })
  if (!ok) return
  if ((await copyAuthFrom(usrRow, sourceUsrId)) === 0) {
    await alert({ message: msg('sy.user.auth.manage.copy.source.empty', sourceUsrId), icon: 'information' })
  }
}

/**
 * onBtnScrnAddClick : 화면추가 버튼 클릭: 화면 선택 팝업 열기
 * @return  N/A
 */
async function onBtnScrnAddClick(): Promise<void> {
  if (await alertIfUsrLocked()) return
  openScrnPopup()
}

/**
 * onBtnAuthRowDelClick : 권한 목록 행삭제 버튼 클릭: 선택한 화면 권한 삭제 처리
 * @return  N/A
 */
async function onBtnAuthRowDelClick(): Promise<void> {
  if (await alertIfUsrLocked()) return
  if (!(await deleteSelectedMenuAuthRows())) {
    await alert({ message: msg('sy.auth.group.screen.delete.select'), icon: 'warning' })
  }
}

/**
 * onBtnScrnSrchAddClick : 화면 선택 팝업의 추가 버튼 클릭: 선택한 화면을 권한 목록에 추가 (컴포넌트 권한은 모두 N)
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
 * onBtnSaveClick : 저장 버튼 클릭: 검증 후 사용자·화면 권한·컴포넌트 권한 저장
 * @return  N/A
 */
async function onBtnSaveClick(): Promise<void> {
  await saveChanges({
    hasChanges: hasChanges.value,
    // 오류가 난 사용자 행을 선택해서 보여준 뒤 안내한다
    validate: async () => {
      const error = await validateUsrRows()
      if (error && error.row !== activeUsrRow.value) await activateUsrRow(error.row)
      return error?.message ?? ''
    },
    save: () =>
      saveUsrAuthApi({
        ...splitRowsByFlag(usrRows.value, toUsrMng),
        menuAuthSaveList: buildMenuAuthSaveListDto(),
        cmpnAuthSaveList: buildCmpnAuthSaveListDto(),
      }),
    afterSave: searchUsrList,
    savedNotice: 'toast',
  })
}

// ---------------------------------------------------------------------
// 페이지 라이프사이클
// ---------------------------------------------------------------------
/**
 * fetchPageData : 페이지 참조 데이터(메뉴, 프로그램, 사용자구분·사용자상태 공통코드) 조회
 * @return  N/A
 */
async function fetchPageData(): Promise<void> {
  const [, usrClsCdOptions, usrStatCdOptions] = await Promise.all([
    fetchReferenceData(),
    getCommCdOptions('SY', 'USR_CLS_CD'),
    getCommCdOptions('SY', 'USR_STAT_CD'),
  ])
  usrClsOptions.value = usrClsCdOptions
  usrStatOptions.value = usrStatCdOptions
}

/**
 * initPageUi : 화면 초기화 및 최초 조회
 * @return  N/A
 */
function initPageUi(): void {
  resetSearchForm()
  clearGrids()
  searchUsrList()
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
          <span class="field-label">ID/사용자명</span>
          <Input id="txtUsrId" v-model="searchForm.usrId" placeholder="사용자ID" @enter="onBtnSrchClick" />
          <Input id="txtUsrNm" v-model="searchForm.usrNm" placeholder="사용자명" @enter="onBtnSrchClick" />
        </div>
        <div class="search-field">
          <span class="field-label">사용자유형</span>
          <span class="srch-select">
            <Select id="cboUsrClsCd" v-model="searchForm.usrClsCd" :options="usrClsSrchOptions" />
          </span>
        </div>
        <div class="search-field">
          <span class="field-label">사용여부</span>
          <span class="srch-select">
            <Select id="cboUseYn" v-model="searchForm.useYn" :options="useYnSrchOptions" />
          </span>
        </div>
      </div>
      <div class="search-actions">
        <Button id="btnSrch" v-permission @click="onBtnSrchClick">조회</Button>
      </div>
    </form>

    <div class="content-area">
      <!-- 왼쪽: 사용자 정보 -->
      <div class="grid-area">
        <div class="area-header">
          <span class="area-title">사용자 정보 - <strong id="lblTotCnt" class="total-count">{{ usrTotalCount }}건</strong></span>
          <div class="area-actions">
            <Button id="btnRowAdd" v-permission variant="outline" size="small" @click="onBtnRowAddClick">행추가</Button>
            <Button id="btnRowDel" v-permission variant="outline" size="small" @click="onBtnRowDelClick">행삭제</Button>
          </div>
        </div>
        <DataTable
          id="grdUsrList"
          v-model:selected-rows="selectedUsrRows"
          :columns="usrColumns"
          :data="usrRows"
          :loading="isUsrLoading"
          :active-row="activeUsrRow"
          :page-size="15"
          selectable
          dense
          flag-column
          @row-click="onGrdUsrListRowClick"
        >
          <template #cell-usrId="{ row }">
            <span :class="{ 'new-row-text': row._flag === 'I' && !row.usrId }">{{ row.usrId || '신규' }}</span>
          </template>
          <template #cell-deptNm="{ row }">
            {{ getDeptNm(row.deptCd) }}
          </template>
        </DataTable>
      </div>

      <div class="right-area">
        <!-- 상세정보 -->
        <div class="detail-area">
          <div class="area-header">
            <span class="area-title">
              상세정보
              <span v-if="isActiveUsrDeleted" class="locked-note">(삭제 예정)</span>
            </span>
            <div class="area-actions">
              <Button id="btnPwdInit" v-permission variant="outline" size="small" :disabled="!isPwdInitEnabled" @click="onBtnPwdInitClick">
                비밀번호 초기화
              </Button>
            </div>
          </div>
          <form
            v-if="activeUsrRow"
            id="frmUsrDtl"
            class="detail-form"
            :class="{ 'is-locked': isActiveUsrDeleted }"
            @submit.prevent
          >
            <div class="dtl-grid">
              <div class="dtl-item">
                <span class="field-label">사용자명</span>
                <Input
                  id="txtDtlUsrNm"
                  required
                  :model-value="activeUsrRow.usrNm"
                  size="small"
                  maxlength="30"
                  @update:model-value="setUsrField('usrNm', $event)"
                />
              </div>
              <div class="dtl-item">
                <span class="field-label">사용자ID</span>
                <Input
                  id="txtDtlUsrId"
                  required
                  :model-value="activeUsrRow.usrId"
                  size="small"
                  maxlength="10"
                  :readonly="activeUsrRow._flag !== 'I'"
                  @update:model-value="setUsrField('usrId', $event)"
                />
              </div>
              <div class="dtl-item">
                <span class="field-label">사원번호</span>
                <Input id="txtDtlEmpNo" :model-value="activeUsrRow.empNo" size="small" readonly />
              </div>
              <div class="dtl-item">
                <span class="field-label">부서</span>
                <Select
                  id="cboDtlDeptCd"
                  :model-value="activeUsrRow.deptCd"
                  :options="deptDtlOptions"
                  size="small"
                  @update:model-value="setUsrField('deptCd', $event)"
                />
              </div>
              <div class="dtl-item">
                <span class="field-label">재직여부</span>
                <Select
                  id="cboDtlUsrStatCd"
                  :model-value="activeUsrRow.usrStatCd"
                  :options="usrStatDtlOptions"
                  size="small"
                  @update:model-value="setUsrField('usrStatCd', $event)"
                />
              </div>
              <div class="dtl-item">
                <span class="field-label">사용여부</span>
                <label class="lock-check">
                  <input
                    id="chkDtlUseYn"
                    type="checkbox"
                    class="cell-check"
                    :checked="activeUsrRow.useYn === 'Y'"
                    :disabled="activeUsrRow._flag === 'D'"
                    @change="onChkDtlUseYnChange"
                  />
                  사용
                </label>
              </div>
              <div class="dtl-item">
                <span class="field-label">사용자유형</span>
                <Select
                  id="cboDtlUsrClsCd"
                  :model-value="activeUsrRow.usrClsCd"
                  :options="usrClsDtlOptions"
                  size="small"
                  @update:model-value="setUsrField('usrClsCd', $event)"
                />
              </div>
              <div class="dtl-item">
                <span class="field-label">잠김여부</span>
                <label class="lock-check">
                  <input
                    id="chkDtlUsrLockYn"
                    type="checkbox"
                    class="cell-check"
                    :checked="activeUsrRow.usrLockYn === 'Y'"
                    :disabled="activeUsrRow._flag === 'D'"
                    @change="onChkDtlUsrLockYnChange"
                  />
                  잠김
                </label>
              </div>
              <div class="dtl-item">
                <span class="field-label">이메일</span>
                <Input
                  id="txtDtlEmlAddr"
                  :model-value="activeUsrRow.emlAddr"
                  type="email"
                  size="small"
                  maxlength="100"
                  placeholder="user@example.com"
                  @update:model-value="setUsrField('emlAddr', $event)"
                />
              </div>
              <div class="dtl-item">
                <span class="field-label">전화번호</span>
                <Input
                  id="txtDtlMoblNo"
                  :model-value="activeUsrRow.moblNo"
                  type="tel"
                  size="small"
                  maxlength="13"
                  placeholder="010-0000-0000"
                  @update:model-value="setUsrField('moblNo', $event)"
                />
              </div>
              <div class="dtl-item">
                <span class="field-label">입사일자</span>
                <Input id="txtDtlEcomDt" :model-value="ymdToDash(activeUsrRow.ecomDt)" size="small" readonly />
              </div>
              <div class="dtl-item">
                <span class="field-label">퇴사일자</span>
                <Input id="txtDtlResiDt" :model-value="ymdToDash(activeUsrRow.resiDt)" size="small" readonly />
              </div>
              <div class="dtl-item">
                <span class="field-label">시작일자</span>
                <span class="date-box">
                  <DatePicker
                    id="dtpDtlStDt"
                    required
                    :model-value="ymdToDash(activeUsrRow.stDt)"
                    :presets="false"
                    :clearable="false"
                    size="small"
                    @update:model-value="setUsrDate('stDt', $event)"
                  />
                </span>
              </div>
              <div class="dtl-item">
                <span class="field-label">종료일자</span>
                <span class="date-box">
                  <DatePicker
                    id="dtpDtlEndDt"
                    required
                    :model-value="ymdToDash(activeUsrRow.endDt)"
                    :presets="false"
                    :clearable="false"
                    size="small"
                    @update:model-value="setUsrDate('endDt', $event)"
                  />
                </span>
              </div>
            </div>
          </form>
          <p v-else class="detail-empty">{{ msg('sy.user.auth.empty.select.user') }}</p>
        </div>

        <div class="auth-area">
          <!-- 권한 목록 (TB_USR_MENU_R) -->
          <div class="grid-area">
            <div class="area-header">
              <span class="area-title">
                권한 목록
                <span v-if="activeUsrRow" id="lblAuthUsrId" class="sub-title">({{ activeUsrRow.usrId || '신규' }})</span>
                <span v-if="isActiveUsrDeleted" class="locked-note">(삭제 예정)</span>
                - <strong id="lblAuthTotCnt" class="total-count">{{ menuAuthRows.length }}건</strong>
              </span>
              <div class="area-actions">
                <span class="field-label">사용자</span>
                <ComSearchCode id="cdsCopyUsr" v-model:code="copyUsrId" v-model:name="copyUsrNm" type="USR" />
                <Button id="btnAuthCopy" v-permission variant="outline" size="small" @click="onBtnAuthCopyClick">권한복사</Button>
                <Button id="btnScrnAdd" v-permission variant="outline" size="small" @click="onBtnScrnAddClick">화면추가</Button>
                <Button id="btnAuthRowDel" v-permission variant="outline" size="small" @click="onBtnAuthRowDelClick">행삭제</Button>
              </div>
            </div>
            <DataTable
              id="grdUsrMenuAuthList"
              v-model:selected-rows="selectedMenuAuthRows"
              :columns="menuAuthColumns"
              :data="menuAuthRows"
              :loading="isStateLoading"
              :active-row="activeMenuAuthRow"
              :page-size="15"
              :empty-text="activeUsrRow ? msg('sy.auth.group.empty.screen.none') : msg('sy.user.auth.empty.select.user')"
              :class="{ 'is-locked': isActiveUsrDeleted }"
              selectable
              dense
              flag-column
              @row-click="onGrdUsrMenuAuthListRowClick"
            >
              <template #cell-menuPath="{ row }">
                <span class="ellipsis-text" :title="getMenuPath(row.menuId)">{{ getMenuPath(row.menuId) }}</span>
              </template>
              <template #cell-prgmNm="{ row }">
                <span class="ellipsis-text prgm-nm" :title="`${getPrgmNm(row.prgmId, row.menuId)} (${row.prgmId})`">{{ getPrgmNm(row.prgmId, row.menuId) }}</span>
              </template>
            </DataTable>
          </div>

          <!-- 컴포넌트목록 (TB_PRGM_CMPN_M + TB_USR_CMPN_R) -->
          <AuthCmpnGrid
            grid-id="grdUsrCmpnAuthList"
            :rows="cmpnAuthRows"
            :loading="isCmpnAuthLoading"
            :prgm-nm="activeMenuAuthRow ? getPrgmNm(activeMenuAuthRow.prgmId, activeMenuAuthRow.menuId) : null"
            :is-prgm-deleted="activeMenuAuthRow?._flag === 'D'"
            :locked="isCmpnAuthLocked"
            :all-checked="isAllCmpnAuthYn"
            @change="setCmpnAuthYn"
            @change-all="setAllCmpnAuthYn"
          />
        </div>
      </div>
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

.srch-select {
  width: 140px;
}

.field-label {
  flex-shrink: 0;
  min-width: 56px;
}

.area-actions .field-label {
  min-width: 0;
}

.content-area {
  display: grid;
  grid-template-columns: 400px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.right-area {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

/* 권한 목록 | 컴포넌트목록 */
.auth-area {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 16px;
  align-items: start;
}

.grid-area, .detail-area {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
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

.new-row-text {
  color: var(--ds-ink-2);
}

.detail-form {
  overflow-x: auto;
  padding: 14px 16px;
  background: var(--ds-surface);
  border: 1px solid var(--ds-rule);
  border-radius: var(--ds-radius-panel);
}

/* 화면 폭이 바뀌어도 항목 위치가 바뀌지 않도록 2열 고정 (좁으면 가로 스크롤) */
.dtl-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(260px, 1fr));
  gap: 10px 20px;
}

.dtl-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.dtl-item .field-label {
  min-width: 84px;
}

.dtl-item > :not(.field-label) {
  flex: 1 1 0;
  min-width: 0;
}

.dtl-item.is-wide {
  grid-column: 1 / -1;
}

.date-box {
  flex: 1 1 0;
  min-width: 0;
}

.date-box :deep(.datepicker-input-box) {
  width: 100%;
  min-width: 0;
  gap: 6px;
  padding: 0 8px;
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

/* 삭제(D) 표시된 사용자의 하위 데이터는 편집 불가 */
.is-locked {
  pointer-events: none;
  opacity: 0.5;
}

.cell-check:disabled {
  cursor: default;
}

.lock-check {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--ds-ink);
  cursor: pointer;
}

.ellipsis-text {
  display: inline-block;
  max-width: 220px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  vertical-align: middle;
}

.ellipsis-text.prgm-nm {
  max-width: 130px;
}

@media (max-width: 1500px) {
  .auth-area {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1100px) {
  .content-area {
    grid-template-columns: 1fr;
  }
}
</style>
