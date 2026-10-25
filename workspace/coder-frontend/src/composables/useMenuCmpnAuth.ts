/*****************************************************************************************
	Program ID   : useMenuCmpnAuth
	Program Type : ts
	Program Name : 화면/컴포넌트 권한 편집 컴포저블
	Description  : 권한 소유자(권한그룹, 사용자)별 화면 권한·컴포넌트 권한 편집 상태를 관리한다
	               - 메뉴·프로그램 참조 데이터 (메뉴 경로, 프로그램명)
	               - 소유자별 화면 권한 목록, 소유자·프로그램별 컴포넌트 권한 목록 (처음 선택할 때 한 번 조회)
	               - 컴포넌트 권한여부 변경/일괄 변경, 화면추가 팝업, 권한복사, 저장 목록 생성
	               사용 화면: SYAU010 사용자권한관리, SYAU020 그룹권한관리
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-20  ywkim     최초생성 (SYAU010, SYAU020 중복 로직 통합)
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { computed, ref, type Ref } from 'vue'
import { getMenuListApi, type Menu } from '@/api/sy/menu'
import { getPrgmCmpnListApi, getPrgmListApi } from '@/api/sy/program'
import { deleteFlaggedRows, hasRowChanges, markRowUpdated, nextRowId, renumber, type RowFlag } from '@/utils/rowFlag'

/** 권한 소유자 행 (권한그룹 행, 사용자 행) */
export interface AuthOwnerRow {
  id: number
  _flag: RowFlag
}

/** 화면 권한 1건 (소유자 키 제외) */
export interface MenuAuthItem {
  menuId: string
  prgmId: string
}

/** 컴포넌트 권한 1건 (소유자 키 제외) */
export interface CmpnAuthItem {
  prgmId: string
  cmpnId: string
  authYn: string
}

/** 화면 권한 그리드 행 */
export interface MenuAuthRow extends MenuAuthItem {
  id: number
  no?: number
  _flag: RowFlag
}

/** 프로그램 컴포넌트 + 소유자의 권한여부 */
export interface CmpnAuthRow {
  id: number
  no?: number
  _flag: RowFlag
  cmpnId: string
  cmpnNm: string
  alinSeq: number | null
  authYn: string
  /** 권한 테이블에 이미 저장된 권한인지 (없으면 저장 시 등록) */
  isSaved: boolean
}

/** 화면추가 팝업 후보 (프로그램이 연결된 메뉴) */
export interface ScrnCandidate {
  id: string
  menuId: string
  menuPath: string
  prgmId: string
  prgmNm: string
}

/** 소유자별 화면 권한 저장 목록 */
export interface MenuAuthSaveItem {
  ownerKey: string
  insertList: MenuAuthItem[]
  deleteList: MenuAuthItem[]
}

/** 소유자·프로그램별 컴포넌트 권한 저장 목록 */
export interface CmpnAuthSaveItem {
  ownerKey: string
  prgmId: string
  insertList: CmpnAuthItem[]
  updateList: CmpnAuthItem[]
}

/** 소유자 1건의 화면 권한 편집 상태 */
interface MenuAuthState {
  rows: MenuAuthRow[]
}

/** 소유자·프로그램 1건의 컴포넌트 권한 편집 상태 */
interface CmpnAuthState {
  ownerRowId: number
  prgmId: string
  rows: CmpnAuthRow[]
}

export interface MenuCmpnAuthOptions<O extends AuthOwnerRow> {
  /** 소유자 그리드 행 목록 */
  ownerRows: Ref<O[]>
  /** 현재 선택된 소유자 행 */
  activeOwnerRow: Ref<O | null>
  /** 소유자 키 (권한그룹코드, 사용자ID) */
  getOwnerKey: (owner: O) => string
  /** 소유자의 저장된 화면 권한 조회 */
  fetchMenuAuthList: (ownerKey: string) => Promise<MenuAuthItem[]>
  /** 소유자의 저장된 컴포넌트 권한 조회 (prgmId 생략 시 전체 프로그램) */
  fetchCmpnAuthList: (ownerKey: string, prgmId?: string) => Promise<CmpnAuthItem[]>
}

/**
 * menuAuthKey : 화면 권한 식별 키 생성
 * @param   item - 화면 권한
 * @return  "메뉴ID/프로그램ID" 형식의 키
 */
function menuAuthKey(item: MenuAuthItem): string {
  return `${item.menuId}/${item.prgmId}`
}

/**
 * toMenuAuthRow : 화면 권한을 그리드 행으로 변환
 * @param   item - 화면 권한
 * @param   flag - 행 상태 플래그
 * @return  화면 권한 그리드 행
 */
function toMenuAuthRow(item: MenuAuthItem, flag: RowFlag = ''): MenuAuthRow {
  return { menuId: item.menuId, prgmId: item.prgmId, id: nextRowId(), _flag: flag }
}

/**
 * useMenuCmpnAuth : 권한 소유자별 화면/컴포넌트 권한 편집 상태와 동작 제공
 * @param   options - 소유자 행·키와 권한 조회 함수
 * @return  편집 상태와 동작
 * @example const auth = useMenuCmpnAuth({ ownerRows: grpRows, activeOwnerRow: activeGrpRow, getOwnerKey: (row) => row.usrGrpCd.trim(), ... })
 */
export function useMenuCmpnAuth<O extends AuthOwnerRow>(options: MenuCmpnAuthOptions<O>) {
  const { ownerRows, activeOwnerRow, getOwnerKey, fetchMenuAuthList, fetchCmpnAuthList } = options

  // 참조 데이터 (메뉴·프로그램)
  const menuMap = ref(new Map<string, Menu>())
  const prgmNmMap = ref(new Map<string, string>())

  // 화면 권한: 소유자 행(id)별 편집 상태
  const menuAuthStateMap = ref(new Map<number, MenuAuthState>())
  const selectedMenuAuthRows = ref<MenuAuthRow[]>([])
  const activeMenuAuthRow = ref<MenuAuthRow | null>(null)
  const isMenuAuthLoading = ref(false)

  // 컴포넌트 권한: `${소유자 행 id}/${프로그램ID}` 별 편집 상태
  const cmpnAuthStateMap = ref(new Map<string, CmpnAuthState>())
  const isCmpnAuthLoading = ref(false)

  // 화면추가 팝업
  const isScrnPopupOpen = ref(false)
  const scrnKeyword = ref('')
  const selectedScrnRows = ref<ScrnCandidate[]>([])

  const activeMenuAuthState = computed(() =>
    activeOwnerRow.value ? menuAuthStateMap.value.get(activeOwnerRow.value.id) : undefined,
  )

  const menuAuthRows = computed(() => activeMenuAuthState.value?.rows ?? [])

  const cmpnAuthRows = computed(() => {
    if (!activeOwnerRow.value || !activeMenuAuthRow.value) return []
    return cmpnAuthStateMap.value.get(cmpnStateKey(activeOwnerRow.value.id, activeMenuAuthRow.value.prgmId))?.rows ?? []
  })

  const isActiveOwnerDeleted = computed(() => activeOwnerRow.value?._flag === 'D')

  const isCmpnAuthLocked = computed(() => isActiveOwnerDeleted.value || activeMenuAuthRow.value?._flag === 'D')

  const hasAuthChanges = computed(
    () =>
      [...menuAuthStateMap.value.values()].some((state) => hasRowChanges(state.rows)) ||
      [...cmpnAuthStateMap.value.values()].some((state) => hasRowChanges(state.rows)),
  )

  const isAllCmpnAuthYn = computed(
    () => cmpnAuthRows.value.length > 0 && cmpnAuthRows.value.every((row) => row.authYn === 'Y'),
  )

  const scrnCandidates = computed<ScrnCandidate[]>(() => {
    const addedKeys = new Set(menuAuthRows.value.map(menuAuthKey))
    const keyword = scrnKeyword.value.trim().toUpperCase()
    return [...menuMap.value.values()]
      .filter((menu) => menu.prgmId && !addedKeys.has(menuAuthKey(menu)))
      .map((menu) => ({
        id: menuAuthKey(menu),
        menuId: menu.menuId,
        menuPath: getMenuPath(menu.menuId),
        prgmId: menu.prgmId,
        prgmNm: getPrgmNm(menu.prgmId, menu.menuId),
      }))
      .filter((c) => !keyword || `${c.menuId} ${c.menuPath} ${c.prgmId} ${c.prgmNm}`.toUpperCase().includes(keyword))
  })

  /**
   * cmpnStateKey : 컴포넌트 권한 상태 Map 키 생성
   * @param   ownerRowId - 소유자 행 ID
   * @param   prgmId     - 프로그램ID
   * @return  "소유자행ID/프로그램ID" 형식의 키
   */
  function cmpnStateKey(ownerRowId: number, prgmId: string): string {
    return `${ownerRowId}/${prgmId}`
  }

  /**
   * fetchReferenceData : 메뉴·프로그램 참조 데이터 조회
   * @return  N/A
   */
  async function fetchReferenceData(): Promise<void> {
    const [menuRes, prgmRes] = await Promise.all([getMenuListApi(), getPrgmListApi({})])
    menuMap.value = new Map(menuRes.data.map((menu) => [menu.menuId, menu]))
    prgmNmMap.value = new Map(prgmRes.data.map((prgm) => [prgm.prgmId, prgm.prgmNm]))
  }

  /**
   * getMenuPath : 메뉴ID의 상위 메뉴 경로(메뉴명 / 구분) 조회
   * @param   menuId - 메뉴ID
   * @return  메뉴 경로 문자열
   */
  function getMenuPath(menuId: string): string {
    const names: string[] = []
    const visited = new Set<string>()
    let menu = menuMap.value.get(menuId)
    while (menu && !visited.has(menu.menuId)) {
      names.unshift(menu.menuNm)
      visited.add(menu.menuId)
      menu = menuMap.value.get(menu.upprMenuId)
    }
    return names.join(' / ')
  }

  /**
   * getPrgmNm : 프로그램명 조회 (없으면 메뉴명, 그것도 없으면 프로그램ID)
   * @param   prgmId - 프로그램ID
   * @param   menuId - 메뉴ID (프로그램명이 없을 때 대체용)
   * @return  프로그램명
   */
  function getPrgmNm(prgmId: string, menuId = ''): string {
    return prgmNmMap.value.get(prgmId) ?? menuMap.value.get(menuId)?.menuNm ?? prgmId
  }

  /**
   * clearAuthStates : 화면·컴포넌트 권한 편집 상태와 선택 초기화
   * @return  N/A
   */
  function clearAuthStates(): void {
    menuAuthStateMap.value = new Map()
    selectedMenuAuthRows.value = []
    activeMenuAuthRow.value = null
    cmpnAuthStateMap.value = new Map()
  }

  /**
   * getActiveMenuAuthKey : 현재 선택된 화면 권한의 키 (재조회 후 같은 화면을 다시 선택할 때 사용)
   * @return  "메뉴ID/프로그램ID" 키 (선택 없으면 빈 문자열)
   */
  function getActiveMenuAuthKey(): string {
    return activeMenuAuthRow.value ? menuAuthKey(activeMenuAuthRow.value) : ''
  }

  /**
   * loadMenuAuthState : 소유자의 화면 권한 목록을 처음 한 번 조회 (신규 소유자는 빈 목록)
   * @param   owner - 소유자 행
   * @return  화면 권한 편집 상태
   */
  async function loadMenuAuthState(owner: O): Promise<MenuAuthState> {
    const loaded = menuAuthStateMap.value.get(owner.id)
    if (loaded) return loaded
    menuAuthStateMap.value.set(owner.id, { rows: [] })
    const state = menuAuthStateMap.value.get(owner.id)!
    if (owner._flag === 'I') return state
    isMenuAuthLoading.value = true
    try {
      state.rows = (await fetchMenuAuthList(getOwnerKey(owner))).map((item) => toMenuAuthRow(item))
      renumber(state.rows)
    } finally {
      isMenuAuthLoading.value = false
    }
    return state
  }

  /**
   * loadCmpnAuthState : 소유자·프로그램의 컴포넌트 목록과 권한여부를 처음 한 번 조회
   * @param   owner  - 소유자 행
   * @param   prgmId - 프로그램ID
   * @return  컴포넌트 권한 편집 상태
   */
  async function loadCmpnAuthState(owner: O, prgmId: string): Promise<CmpnAuthState> {
    const key = cmpnStateKey(owner.id, prgmId)
    const loaded = cmpnAuthStateMap.value.get(key)
    if (loaded) return loaded
    cmpnAuthStateMap.value.set(key, { ownerRowId: owner.id, prgmId, rows: [] })
    const state = cmpnAuthStateMap.value.get(key)!
    isCmpnAuthLoading.value = true
    try {
      const [cmpnRes, authList] = await Promise.all([
        getPrgmCmpnListApi(prgmId),
        owner._flag === 'I' ? Promise.resolve([] as CmpnAuthItem[]) : fetchCmpnAuthList(getOwnerKey(owner), prgmId),
      ])
      const authMap = new Map(authList.map((auth) => [auth.cmpnId, auth.authYn]))
      state.rows = cmpnRes.data.map((cmpn) => ({
        id: nextRowId(),
        _flag: '' as RowFlag,
        cmpnId: cmpn.cmpnId,
        cmpnNm: cmpn.cmpnNm,
        alinSeq: cmpn.alinSeq,
        authYn: authMap.get(cmpn.cmpnId) ?? 'N',
        isSaved: authMap.has(cmpn.cmpnId),
      }))
      renumber(state.rows)
    } finally {
      isCmpnAuthLoading.value = false
    }
    return state
  }

  /**
   * activateOwnerAuth : 선택한 소유자의 화면 권한을 표시하고, 이전에 보던 화면(없으면 첫 화면)을 선택
   *                     (activeOwnerRow 는 호출하는 화면에서 먼저 바꾼다)
   * @param   owner       - 선택한 소유자 행
   * @param   prevAuthKey - 이전에 선택했던 "메뉴ID/프로그램ID" 키
   * @return  N/A
   */
  async function activateOwnerAuth(owner: O, prevAuthKey = ''): Promise<void> {
    activeMenuAuthRow.value = null
    selectedMenuAuthRows.value = []
    const state = await loadMenuAuthState(owner)
    if (activeOwnerRow.value !== owner || state.rows.length === 0) return
    const prevRow = state.rows.find((row) => menuAuthKey(row) === prevAuthKey)
    await activateMenuAuthRow(prevRow ?? state.rows[0])
  }

  /**
   * activateMenuAuthRow : 화면 권한 행을 선택하고 컴포넌트 권한 표시
   * @param   row - 선택할 화면 권한 행
   * @return  N/A
   */
  async function activateMenuAuthRow(row: MenuAuthRow): Promise<void> {
    activeMenuAuthRow.value = row
    if (activeOwnerRow.value) await loadCmpnAuthState(activeOwnerRow.value, row.prgmId)
  }

  /**
   * pruneAuthStates : 소유자 행삭제 후 남지 않은 소유자의 편집 상태 제거
   * @return  N/A
   */
  function pruneAuthStates(): void {
    const remainIds = new Set(ownerRows.value.map((row) => row.id))
    ;[...menuAuthStateMap.value.keys()].filter((id) => !remainIds.has(id)).forEach((id) => menuAuthStateMap.value.delete(id))
    ;[...cmpnAuthStateMap.value.entries()]
      .filter(([, state]) => !remainIds.has(state.ownerRowId))
      .forEach(([key]) => cmpnAuthStateMap.value.delete(key))
  }

  /**
   * deleteSelectedMenuAuthRows : 선택한 화면 권한 행삭제 (등록 행은 제거, 나머지는 D 표시)
   * @return  삭제할 행이 없으면 false
   */
  async function deleteSelectedMenuAuthRows(): Promise<boolean> {
    const state = activeMenuAuthState.value
    if (!state || selectedMenuAuthRows.value.length === 0) return false
    state.rows = deleteFlaggedRows(state.rows, selectedMenuAuthRows.value)
    selectedMenuAuthRows.value = []
    renumber(state.rows)
    if (activeMenuAuthRow.value && !state.rows.includes(activeMenuAuthRow.value)) {
      activeMenuAuthRow.value = null
      if (state.rows.length > 0) await activateMenuAuthRow(state.rows[0])
    }
    return true
  }

  /**
   * setCmpnAuthYn : 컴포넌트 권한여부 변경: 저장된 권한이면 수정(U), 없던 권한이면 등록(I)
   * @param   row    - 컴포넌트 권한 행
   * @param   authYn - 변경할 권한여부 (Y/N)
   * @return  N/A
   */
  function setCmpnAuthYn(row: CmpnAuthRow, authYn: string): void {
    if (row.authYn === authYn) return
    row.authYn = authYn
    if (row.isSaved) markRowUpdated(row)
    else if (row._flag === '') row._flag = 'I'
  }

  /**
   * setAllCmpnAuthYn : 현재 화면의 컴포넌트 권한여부 일괄 변경
   * @param   authYn - 변경할 권한여부 (Y/N)
   * @return  N/A
   */
  function setAllCmpnAuthYn(authYn: string): void {
    cmpnAuthRows.value.forEach((row) => setCmpnAuthYn(row, authYn))
  }

  /**
   * openScrnPopup : 화면추가 팝업 열기 (검색어·선택 초기화)
   * @return  N/A
   */
  function openScrnPopup(): void {
    scrnKeyword.value = ''
    selectedScrnRows.value = []
    isScrnPopupOpen.value = true
  }

  /**
   * addSelectedScrns : 화면추가 팝업에서 선택한 화면을 현재 소유자의 화면 권한에 추가하고 팝업 닫기
   * @return  추가할 화면을 선택하지 않았으면 false
   */
  async function addSelectedScrns(): Promise<boolean> {
    const state = activeMenuAuthState.value
    if (!state || selectedScrnRows.value.length === 0) return false
    const addedKeys = new Set(state.rows.map(menuAuthKey))
    const addedRows = selectedScrnRows.value
      .filter((scrn) => !addedKeys.has(menuAuthKey(scrn)))
      .map((scrn) => toMenuAuthRow(scrn, 'I'))
    state.rows.push(...addedRows)
    renumber(state.rows)
    isScrnPopupOpen.value = false
    if (addedRows.length > 0) await activateMenuAuthRow(addedRows[0])
    return true
  }

  /**
   * copyAuthFrom : 원본 소유자의 화면 권한과 컴포넌트 권한을 현재 소유자에 복사
   *                (이미 있는 화면은 유지, 삭제 표시된 화면은 되살리고, 컴포넌트 권한여부는 원본 값으로 덮어씀)
   *                원본이 화면에서 편집 중이면 편집 값을 사용한다
   * @param   owner     - 복사 대상 소유자 행
   * @param   sourceKey - 원본 소유자 키
   * @return  원본 화면 권한 건수 (0이면 복사하지 않음)
   */
  async function copyAuthFrom(owner: O, sourceKey: string): Promise<number> {
    const sourceRow = ownerRows.value.find((row) => getOwnerKey(row) === sourceKey && row._flag !== 'I')
    const sourceState = sourceRow ? menuAuthStateMap.value.get(sourceRow.id) : undefined
    const sourceMenuList: MenuAuthItem[] = sourceState
      ? sourceState.rows.filter((row) => row._flag !== 'D')
      : await fetchMenuAuthList(sourceKey)
    if (sourceMenuList.length === 0) return 0

    // 원본 컴포넌트 권한: 저장된 값 + 화면에서 편집 중인 값
    const sourceCmpnMap = new Map<string, string>()
    ;(await fetchCmpnAuthList(sourceKey)).forEach((auth) => sourceCmpnMap.set(`${auth.prgmId}/${auth.cmpnId}`, auth.authYn))
    if (sourceRow) {
      cmpnAuthStateMap.value.forEach((cmpnState) => {
        if (cmpnState.ownerRowId !== sourceRow.id) return
        cmpnState.rows.forEach((row) => sourceCmpnMap.set(`${cmpnState.prgmId}/${row.cmpnId}`, row.authYn))
      })
    }

    // 화면 권한
    const state = await loadMenuAuthState(owner)
    const rowMap = new Map(state.rows.map((row) => [menuAuthKey(row), row]))
    sourceMenuList.forEach((source) => {
      const target = rowMap.get(menuAuthKey(source))
      if (!target) state.rows.push(toMenuAuthRow(source, 'I'))
      else if (target._flag === 'D') target._flag = ''
    })
    renumber(state.rows)

    // 컴포넌트 권한
    const prgmIds = [...new Set(sourceMenuList.map((source) => source.prgmId))]
    for (const prgmId of prgmIds) {
      const cmpnState = await loadCmpnAuthState(owner, prgmId)
      cmpnState.rows.forEach((row) => {
        const sourceAuthYn = sourceCmpnMap.get(`${prgmId}/${row.cmpnId}`)
        if (sourceAuthYn) setCmpnAuthYn(row, sourceAuthYn)
      })
    }
    if (activeOwnerRow.value === owner && !activeMenuAuthRow.value && state.rows.length > 0) {
      await activateMenuAuthRow(state.rows[0])
    }
    return sourceMenuList.length
  }

  /**
   * buildMenuAuthSaveList : 변경된 소유자별 화면 권한 저장 목록 생성
   *                         (삭제 표시된 소유자의 권한은 소유자 삭제 시 함께 삭제되므로 제외)
   * @return  소유자별 화면 권한 저장 목록
   */
  function buildMenuAuthSaveList(): MenuAuthSaveItem[] {
    return ownerRows.value.flatMap((owner) => {
      const state = menuAuthStateMap.value.get(owner.id)
      if (owner._flag === 'D' || !state || !hasRowChanges(state.rows)) return []
      const toList = (flag: RowFlag) =>
        state.rows.filter((row) => row._flag === flag).map((row) => ({ menuId: row.menuId, prgmId: row.prgmId }))
      return [{ ownerKey: getOwnerKey(owner), insertList: toList('I'), deleteList: toList('D') }]
    })
  }

  /**
   * buildCmpnAuthSaveList : 변경된 소유자·프로그램별 컴포넌트 권한 저장 목록 생성
   *                         (소유자가 삭제됐거나 해당 프로그램의 화면 권한이 모두 삭제되면 함께 삭제되므로 제외)
   * @return  소유자·프로그램별 컴포넌트 권한 저장 목록
   */
  function buildCmpnAuthSaveList(): CmpnAuthSaveItem[] {
    return [...cmpnAuthStateMap.value.values()].flatMap((state) => {
      const owner = ownerRows.value.find((row) => row.id === state.ownerRowId)
      const menuState = menuAuthStateMap.value.get(state.ownerRowId)
      const hasPrgmAuth = menuState?.rows.some((row) => row.prgmId === state.prgmId && row._flag !== 'D')
      if (!owner || owner._flag === 'D' || !hasPrgmAuth || !hasRowChanges(state.rows)) return []
      const toList = (flag: RowFlag) =>
        state.rows
          .filter((row) => row._flag === flag)
          .map((row) => ({ prgmId: state.prgmId, cmpnId: row.cmpnId, authYn: row.authYn }))
      return [{ ownerKey: getOwnerKey(owner), prgmId: state.prgmId, insertList: toList('I'), updateList: toList('U') }]
    })
  }

  return {
    // 참조 데이터
    fetchReferenceData,
    getMenuPath,
    getPrgmNm,
    // 화면 권한
    menuAuthRows,
    selectedMenuAuthRows,
    activeMenuAuthRow,
    isMenuAuthLoading,
    activeMenuAuthState,
    isActiveOwnerDeleted,
    // 컴포넌트 권한
    cmpnAuthRows,
    isCmpnAuthLoading,
    isCmpnAuthLocked,
    isAllCmpnAuthYn,
    setCmpnAuthYn,
    setAllCmpnAuthYn,
    // 화면추가 팝업
    isScrnPopupOpen,
    scrnKeyword,
    selectedScrnRows,
    scrnCandidates,
    openScrnPopup,
    addSelectedScrns,
    // 상태 관리
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
  }
}
