# SYAU020 그룹권한관리 프로그램 사양서

| 항목 | 내용 |
|---|---|
| 프로그램 ID | SYAU020 |
| 프로그램명 | 그룹권한관리 |
| 메뉴 경로 | 시스템관리 > 권한관리 > 그룹권한관리 (메뉴ID `SY020200`) |
| 라우트 | `/sy/syau020` (name `SYAU020`) |
| 화면 파일 | `workspace/coder-frontend/src/views/system/auths/AuthGroupManageView.vue` |
| 하위 컴포넌트 | `views/system/auths/components/AuthCmpnGrid.vue` (컴포넌트 권한 그리드), `views/system/auths/components/AuthScreenAddPopup.vue` (화면추가 팝업) |
| 공통 로직 | `composables/useMenuCmpnAuth.ts` (화면/컴포넌트 권한 편집), `composables/useGridSave.ts` (저장·조회 확인 흐름) |
| API 파일 | `workspace/coder-frontend/src/api/sy/authGroup.ts` (참조: `api/sy/menu.ts`, `api/sy/program.ts`) |
| API 상태 | 실제 API 연동 (coder-backend `/api/v1/auth-groups`) |
| 대상 데이터 | `TB_AUTH_GRP_M`(권한그룹기본), `TB_AUTH_GRP_MENU_R`(그룹별권한내역), `TB_AUTH_GRP_CMPN_R`(권한그룹컴포넌트관계), 참조 `TB_MENU_M`, `TB_PRGM_M`, `TB_PRGM_CMPN_M` |
| 작성자 | ywkim |
| 작성일 | 2026-10-23 |

### 변경 이력

| 일자 | 작성자 | 내용 |
|---|---|---|
| 2026-10-23 | ywkim | 최초 작성 (현재 구현 기준) |
| 2026-10-23 | ywkim | 문제점 수정 반영 |

---

## 1. 개요

권한그룹을 조회·등록·수정·삭제하고, 그룹별로 접근할 수 있는 화면(메뉴 + 프로그램)과 화면의 컴포넌트(버튼 등) 권한여부를 관리한다. 다른 그룹의 화면·컴포넌트 권한을 현재 그룹에 복사할 수 있다. 그룹·화면 권한·컴포넌트 권한 변경분은 저장 버튼 한 번으로 한 트랜잭션에 저장한다.

---

## 2. 화면 구성

```text
┌ [SYAU020] 그룹권한관리 ─────────────────────────────────────────────────────────── [저장] ┐
├ 검색영역 (frmSrch) ──────────────────────────────────────────────────────────────────────┤
│ ID/그룹 [그룹ID][그룹명]                                                          [조회] │
├ 그룹목록 - N건 [행추가][행삭제] ┬ 권한목록 (그룹ID) - N건      ┬ 컴포넌트목록 [프로그램명] │
│ NO│그룹ID│그룹명                │ 그룹[선택▼][권한복사]        │ - N건                     │
│ (grdUsrGrpList, 10행)          │ [화면추가][행삭제]           │ NO│컴포넌트ID│컴포넌트명│ │
│                                │ NO│메뉴ID│메뉴│프로그램      │ 권한여부(☑ 전체)          │
├ 그룹 유효기간 ─────────────────┤ (grdAuthList, 15행)          │ (grdCmpnAuthList, 15행)   │
│ 유효기간 [시작일] ~            │                              │                           │
│          [종료일]              │                              │                           │
│ 비고 [txaRmk]                  │                              │                           │
└────────────────────────────────┴──────────────────────────────┴───────────────────────────┘
  [화면추가] → [화면 추가] 팝업 (mdlScrnSrch, 760px)
```

- 3열 배치: 왼쪽 380px(그룹목록 + 그룹 유효기간), 가운데 나머지 폭(권한목록), 오른쪽 360px(컴포넌트목록).
- 화면 폭 1500px 이하에서는 2열(왼쪽 380px + 권한목록)이고 컴포넌트목록이 아래 줄 전체 폭을 쓴다. 1100px 이하에서는 1열.
- 삭제(D) 표시된 그룹을 선택하면 그룹 유효기간·권한목록·컴포넌트목록이 반투명(`is-locked`)으로 바뀌고 편집할 수 없다. 권한목록 제목 옆에 "(삭제 예정)"을 표시한다.

---

## 3. 검색조건

| Ref/ID | 항목 | 컴포넌트 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|---|
| `txtUsrGrpCd` | 그룹ID | Input | | | placeholder "그룹ID". Enter 시 조회 |
| `txtUsrGrpNm` | 그룹명 | Input | | | placeholder "그룹명". Enter 시 조회 |

- 두 입력은 "ID/그룹" 라벨 하나에 나란히 둔다.
- 검색영역 폼 ID는 `frmSrch`이며, submit 시 조회한다.

---

## 4. 그리드 / 상세

### 4.1 그룹목록 그리드 (`grdUsrGrpList`)

| 컬럼 | Key | 폭 | 정렬 | 비고 |
|---|---|---|---|---|
| NO | `no` | 40px | 가운데 | 순번 (행추가·삭제 시 재부여) |
| 그룹ID | `usrGrpCd` | 140px | | 셀 입력(130px), `maxlength` 20. 신규(I) 행만 입력 가능, 그 외 읽기전용. 신규 행은 필수 표시(`is-required`) |
| 그룹명 | `usrGrpNm` | (나머지) | | 셀 입력(100%, 최소 110px), `maxlength` 30. 신규 행은 필수 표시 |

- 10행 페이징, 조밀(dense), 체크박스 선택(selectable), 행 상태 컬럼(flag-column) 표시. 건수는 `lblTotCnt`에 표시한다.
- 조회 후 이전에 선택했던 그룹(없으면 첫 행)을 활성 행으로 선택한다.
- 셀을 입력하면 조회된 행은 수정(U)으로 표시한다.
- 신규 행 기본값: `useYn` `Y`, `stDt` 빈 값, `endDt` `99991231`, `rmk` 빈 값. 행은 맨 위에 추가하고 바로 활성화한다.

### 4.2 그룹 유효기간 (상세)

활성 그룹 행의 값을 편집한다. 활성 행이 없으면 `sy.auth.group.empty.group.select` 안내를 표시한다.

| Ref/ID | 항목 | 컴포넌트 | 필수 | 설명 |
|---|---|---|---|---|
| `dtpStDt` | 유효기간 시작일 | DatePicker (small, 프리셋 없음) | | 그룹 행 `stDt`(YYYYMMDD) ↔ 화면 `YYYY-MM-DD` 변환. 변경 시 수정(U) 표시 |
| `dtpEndDt` | 유효기간 종료일 | DatePicker (small, 프리셋 없음) | | 그룹 행 `endDt`. 시작일 아래 줄에 배치 |
| `txaRmk` | 비고 | textarea (3줄) | | 그룹 행 `rmk`, `maxlength` 200. 입력 시 수정(U) 표시 |

- 시작일 오른쪽에 `~`를 두고, 종료일 줄에는 같은 폭의 빈 자리를 두어 두 입력칸 폭을 맞춘다.

### 4.3 권한목록 그리드 (`grdAuthList`)

활성 그룹에 권한을 준 화면 목록 (`TB_AUTH_GRP_MENU_R`).

| 컬럼 | Key | 폭 | 정렬 | 비고 |
|---|---|---|---|---|
| NO | `no` | 40px | 가운데 | 순번 |
| 메뉴ID | `menuId` | 90px | | |
| 메뉴 | `menuPath` | (나머지) | | 메뉴 경로 `getMenuPath(menuId)` (상위 메뉴명을 ` / `로 연결, 최대 200px 말줄임, 툴팁 전체) |
| 프로그램 | `prgmNm` | 150px | | `getPrgmNm(prgmId, menuId)` (프로그램명 → 없으면 메뉴명 → 없으면 프로그램ID). 최대 130px 말줄임, 툴팁 `프로그램명 (프로그램ID)` |

- 15행 페이징, 조밀, 체크박스 선택, 행 상태 컬럼 표시.
- 제목 옆에 활성 그룹ID(`lblAuthGrpCd`, 신규 그룹은 "신규"), 건수(`lblAuthTotCnt`)를 표시한다.
- 빈 목록 안내: 그룹 선택 시 `sy.auth.group.empty.screen.none`, 미선택 시 `sy.auth.group.empty.screen.select.group`.
- 그룹을 처음 선택할 때 한 번만 서버에서 조회하고, 이후에는 화면의 편집 상태를 유지한다 (신규 그룹은 빈 목록).
- 그룹을 선택하면 이전에 보던 화면(메뉴ID/프로그램ID)이 있으면 그 행을, 없으면 첫 행을 활성화해 컴포넌트목록을 표시한다.

| Ref/ID | 항목 | 컴포넌트 | 기본값 | 설명 |
|---|---|---|---|---|
| `cboCopyGrpCd` | 그룹 (권한복사 원본) | Select (small, 200px) | 선택(빈 값) | 옵션: 현재 그룹과 신규(I)·삭제(D) 그룹을 제외한 그룹, 표시 `그룹명 (그룹ID)`. 그룹을 바꾸면 빈 값으로 초기화 |

### 4.4 컴포넌트목록 그리드 (`AuthCmpnGrid`, `grdCmpnAuthList`)

권한목록에서 선택한 화면 프로그램의 컴포넌트(`TB_PRGM_CMPN_M`)와 그룹의 권한여부(`TB_AUTH_GRP_CMPN_R`).

| 컬럼 | Key | 폭 | 정렬 | 비고 |
|---|---|---|---|---|
| NO | `no` | 40px | 가운데 | 순번 |
| 컴포넌트ID | `cmpnId` | 110px | | |
| 컴포넌트명 | `cmpnNm` | (나머지) | | |
| 권한여부 | `authYn` | 72px | 가운데 | 체크박스. 헤더 체크박스로 전체 선택/해제 (모든 행이 `Y`이면 체크) |

- 15행 페이징, 조밀, 행 상태 컬럼 표시 (체크박스 선택 없음).
- 제목 옆에 프로그램명(`lblCmpnPrgmNm`), 선택 화면이 삭제(D) 표시면 "(삭제 예정)", 건수(`lblCmpnTotCnt`)를 표시한다.
- 빈 목록 안내: 화면 선택 시 `sy.auth.group.empty.component.none`, 미선택 시 `sy.auth.group.empty.component.select.screen`.
- 그룹·프로그램 조합별로 처음 선택할 때 한 번만 조회한다. 저장된 권한이 없는 컴포넌트는 `N`으로 표시한다.
- 권한여부 변경: 저장된 권한이면 수정(U), 저장되지 않은 권한이면 등록(I)으로 표시한다.
- 그룹 또는 선택 화면이 삭제(D) 표시면 편집할 수 없다(`locked`).

| Props / Emit | 내용 |
|---|---|
| `gridId` | 그리드 id (`grdCmpnAuthList`) |
| `rows`, `loading` | 컴포넌트 권한 행, 로딩 여부 |
| `prgmNm` | 선택 화면의 프로그램명 (선택 없으면 `null`) |
| `isPrgmDeleted`, `locked`, `allChecked` | 선택 화면 삭제 표시 여부, 편집 불가 여부, 전체 `Y` 여부 |
| `change(row, authYn)` | 행 권한여부 변경 → `setCmpnAuthYn` |
| `change-all(authYn)` | 헤더 전체 변경 → `setAllCmpnAuthYn` |

### 4.5 화면 추가 팝업 (`AuthScreenAddPopup`, `mdlScrnSrch`)

프로그램이 연결된 메뉴(`TB_MENU_M.PRGM_ID` 있음) 중 현재 그룹 권한목록에 없는 화면을 선택해 추가한다. 제목 "화면 추가", 폭 760px.

| Ref/ID | 항목 | 컴포넌트 | 설명 |
|---|---|---|---|
| `txtScrnKeyword` | 검색 | Input | placeholder "메뉴ID·메뉴·프로그램". 메뉴ID·메뉴 경로·프로그램ID·프로그램명을 대소문자 구분 없이 부분 일치로 바로 필터링 |
| `grdScrnList` | 화면 목록 | DataTable | 8행 페이징, 조밀, 체크박스 선택. 빈 목록 `sy.auth.group.empty.addable.none` |

| 컬럼 | Key | 폭 | 비고 |
|---|---|---|---|
| 메뉴ID | `menuId` | 90px | |
| 메뉴 | `menuPath` | (나머지) | 메뉴 경로 |
| 프로그램ID | `prgmId` | 120px | |
| 프로그램명 | `prgmNm` | 140px | |

- 팝업을 열 때 검색어와 선택을 초기화한다.
- 추가/닫기 버튼은 버튼 권한(v-permission) 관리를 위해 화면(`AuthGroupManageView`)에서 `footer` 슬롯으로 넘긴다.

---

## 5. 버튼 / 이벤트

| Ref/ID | 버튼/이벤트 | 핸들러 | 권한 | 처리 |
|---|---|---|---|---|
| `btnSave` | 저장 | `onBtnSaveClick` | v-permission | 검증 후 그룹·화면 권한·컴포넌트 권한 일괄 저장 (6.6) |
| `btnSrch` | 조회 | `onBtnSrchClick` | v-permission | 변경 내용이 있으면 버릴지 확인 후 조회 (6.1) |
| `btnRowAdd` | 행추가 (그룹) | `onBtnRowAddClick` | v-permission | 그룹 신규 행 추가 (4.1) |
| `btnRowDel` | 행삭제 (그룹) | `onBtnRowDelClick` | v-permission | 선택 그룹 삭제 처리 (6.2) |
| `btnAuthCopy` | 권한복사 | `onBtnAuthCopyClick` | v-permission | 원본 그룹 권한을 현재 그룹에 복사 (6.4) |
| `btnScrnAdd` | 화면추가 | `onBtnScrnAddClick` | v-permission | 그룹 선택·삭제 여부 확인 후 화면 추가 팝업 열기 |
| `btnAuthRowDel` | 행삭제 (권한목록) | `onBtnAuthRowDelClick` | v-permission | 선택한 화면 권한 삭제 처리 (6.3) |
| `btnScrnSrchAdd` | 추가 (팝업) | `onBtnScrnSrchAddClick` | v-permission | 선택한 화면을 권한목록에 추가 (6.5) |
| `btnScrnSrchClose` | 닫기 (팝업) | `onBtnScrnSrchCloseClick` | v-permission | 팝업 닫기 |
| `grdUsrGrpList` | 행 클릭 | `onGrdUsrGrpListRowClick` | | 다른 행이면 그룹 활성화 → 권한목록 표시 |
| `grdAuthList` | 행 클릭 | `onGrdAuthListRowClick` | | 다른 행이면 화면 활성화 → 컴포넌트목록 표시 |
| `txtUsrGrpCd`, `txtUsrGrpNm` | Enter | `onBtnSrchClick` | | 조회 |

- 버튼 컴포넌트는 `TB_PRGM_CMPN_M`에 SYAU020의 9개 버튼(`btnSave` ~ `btnScrnSrchClose`)으로 등록되어 있다 (`schema/data/TB_PRGM_CMPN_M-data.sql`).

---

## 6. 처리 로직

### 6.0 초기화 (`onMounted`)
1. `fetchPageData()` : `fetchReferenceData()`로 메뉴 전체(`getMenuListApi`)와 프로그램 전체(`getPrgmListApi({})`)를 병렬 조회해 메뉴 경로·프로그램명 표시용 Map을 만든다.
2. `initPageUi()` : 조회조건·그리드를 초기화하고(`resetSearchForm`, `clearGrids`) 최초 조회(`searchGrpList`)한다.

### 6.1 조회 (`searchGrpList`)
1. 조회 버튼은 `confirmDiscardChanges`로 저장하지 않은 변경(그룹·화면 권한·컴포넌트 권한)이 있으면 `cm.confirm.discard.and.search`를 확인한다.
2. 이전 활성 그룹ID와 활성 화면 키(메뉴ID/프로그램ID)를 기억한 뒤 `getAuthGrpListApi(searchForm)`를 호출한다.
3. 그리드·편집 상태를 모두 비우고(`clearGrids`) 결과를 행 상태 `''`로 넣고 NO를 부여한다.
4. 결과가 없으면 `cm.info.no.data`를 띄운다. 있으면 이전 그룹(없으면 첫 행)을 활성화하고, 이전 화면이 있으면 그 화면을 다시 선택한다.

### 6.2 그룹 행삭제 (`onBtnRowDelClick`)
1. 선택한 그룹이 없으면 `sy.auth.group.delete.select`.
2. `sy.auth.group.delete.confirm`으로 확인한다.
3. 신규(I) 행은 바로 제거하고, 나머지는 삭제(D)로 표시한다(저장 시 삭제). 제거된 그룹의 화면·컴포넌트 권한 편집 상태도 지운다(`pruneAuthStates`).
4. 활성 그룹이 제거됐으면 남은 첫 행을 활성화한다.

### 6.3 권한목록 행삭제 (`deleteSelectedMenuAuthRows`)
- 선택한 화면이 없으면 `sy.auth.group.screen.delete.select`.
- 신규(I) 화면은 바로 제거하고, 나머지는 D로 표시한다. 활성 화면이 제거됐으면 첫 행을 활성화한다. (확인 메시지 없음)

### 6.4 권한복사 (`copyAuthFrom`)
1. 그룹 미선택이면 `sy.auth.group.select.required`, 삭제 표시 그룹이면 `sy.auth.group.deleted.readonly` (`alertIfGrpLocked`).
2. 원본 그룹(`cboCopyGrpCd`)이 없으면 `sy.auth.group.copy.select.source`.
3. `sy.auth.group.copy.confirm`(원본 그룹ID)으로 확인한다.
4. 원본 화면 권한: 원본 그룹을 화면에서 이미 열어 편집 중이면 편집 값(D 제외), 아니면 `getAuthGrpMenuListApi`로 조회한다. 원본에 화면이 없으면(`copyAuthFrom`이 0 반환) `sy.auth.group.info.copy.no.auth`를 띄우고 끝낸다.
5. 원본 컴포넌트 권한: `getAuthGrpCmpnListApi(원본, 전체)`로 조회하고, 화면에서 편집 중인 원본 값이 있으면 덮어쓴다.
6. 현재 그룹 화면 권한: 없는 화면은 신규(I)로 추가, 삭제(D) 표시된 화면은 되살리고(`''`), 이미 있는 화면은 유지한다.
7. 현재 그룹 컴포넌트 권한: 원본 화면의 프로그램별로 컴포넌트 목록을 불러와 원본 권한여부로 덮어쓴다 (등록 I / 수정 U 표시).
8. 활성 화면이 없으면 첫 화면을 활성화한다. 저장은 저장 버튼으로 한다.

### 6.5 화면 추가 (`addSelectedScrns`)
- 화면추가 버튼: 그룹 미선택·삭제 표시 그룹이면 6.4-1과 같은 메시지를 띄우고, 아니면 팝업을 연다.
- 팝업 추가 버튼: 선택한 화면이 없으면 `sy.auth.group.screen.add.select`. 있으면 권한목록에 없는 화면만 신규(I)로 뒤에 추가하고 팝업을 닫은 뒤, 추가된 첫 화면을 활성화한다.

### 6.6 저장 (`onBtnSaveClick` → `useGridSave.saveChanges`)
1. 변경이 없으면 `cm.info.no.changes`.
2. `validateGrpRows()` (신규·수정 행 대상, 삭제 행 제외):
   - 그룹ID 미입력 → `sy.auth.group.validate.id.required`
   - 그룹명 미입력 → `sy.auth.group.validate.name.required`
   - 그룹ID 중복(삭제 행 제외, 화면 내) → `sy.auth.group.validate.id.duplicate`
   - 시작일·종료일이 비어 있지 않은데 실제 일자가 아님 → `sy.auth.group.validate.period.invalid`
   - 시작일 > 종료일 → `sy.auth.group.validate.period.reversed`
   - 행 표시는 `sy.auth.group.row.label`(NO)를 쓴다.
3. `cm.confirm.save` 확인 후 `saveAuthGrpListApi` 호출. 요청 구성:
   - `insertList` / `updateList` / `deleteList` : 그룹 행 상태별 목록 (`splitRowsByFlag`, 그룹ID·그룹명 trim)
   - `menuSaveList` : 변경된 그룹별 화면 권한 `insertList`(I) / `deleteList`(D). 삭제(D) 그룹은 제외
   - `cmpnSaveList` : 변경된 그룹·프로그램별 컴포넌트 권한 `insertList`(I) / `updateList`(U). 삭제 그룹, 해당 프로그램의 화면 권한이 모두 삭제된 경우는 제외
4. 실패(`success=false`)면 서버 메시지(예: `sy.auth.group.save.id.exists`), 메시지가 없으면 `cm.error.save.failed`를 띄운다.
5. 성공하면 `cm.info.saved`를 띄우고 재조회(`searchGrpList`)한다.

- backend 저장 순서: 그룹 삭제(컴포넌트 권한·화면 권한·그룹 사용자 포함) → 그룹 수정 → 그룹 등록(존재 확인) → 화면 권한 삭제·등록, 화면 권한이 없어진 프로그램의 컴포넌트 권한 삭제 → 컴포넌트 권한 등록(이미 있으면 권한여부 수정).

---

## 7. 사용 API

| 함수 | Method / URL | 요청 | 응답 | 비고 |
|---|---|---|---|---|
| `getAuthGrpListApi` | `GET /api/v1/auth-groups` | `usrGrpCd`, `usrGrpNm` | `AuthGrp[]` (`usrGrpCd`, `usrGrpNm`, `useYn`, `stDt`, `endDt`, `rmk`) | 그룹코드 순 |
| `getAuthGrpMenuListApi` | `GET /api/v1/auth-groups/{usrGrpCd}/menus` | `usrGrpCd` | `AuthGrpMenu[]` (`usrGrpCd`, `menuId`, `prgmId`) | |
| `getAuthGrpCmpnListApi` | `GET /api/v1/auth-groups/{usrGrpCd}/components?prgmId=` | `usrGrpCd`, `prgmId`(생략 시 전체) | `AuthGrpCmpn[]` (`usrGrpCd`, `prgmId`, `cmpnId`, `authYn`) | 권한복사 시 전체 조회 |
| `saveAuthGrpListApi` | `POST /api/v1/auth-groups` | `AuthGrpSaveRequest { insertList, updateList, deleteList, menuSaveList, cmpnSaveList }` | `number \| null` (저장 건수) | 한 트랜잭션. 업무 오류면 `success=false`와 서버 메시지 (`requestApi`) |
| `getMenuListApi` | `GET /api/v1/menus` (`api/sy/menu.ts`) | 없음 | `Menu[]` | 메뉴 경로, 화면 추가 후보 |
| `getPrgmListApi` | `GET /api/v1/programs` (`api/sy/program.ts`) | `{}` (전체) | `Prgm[]` | 프로그램명 |
| `getPrgmCmpnListApi` | `GET /api/v1/programs/{prgmId}/components` (`api/sy/program.ts`) | `prgmId` | `PrgmCmpn[]` | 컴포넌트목록 |

---

## 8. 메시지

| 키 | 문구 (ko) | 사용 시점 |
|---|---|---|
| `cm.confirm.discard.and.search` | 저장하지 않은 변경 내용이 있습니다.\n조회하시겠습니까? | 변경 중 조회 |
| `cm.info.no.data` | 조회된 내역이 없습니다. | 조회 결과 없음 |
| `sy.auth.group.empty.group.select` | 그룹목록에서 그룹을 선택하세요. | 그룹 유효기간 영역 (미선택) |
| `sy.auth.group.empty.screen.none` | 등록된 화면 권한이 없습니다. | 권한목록 빈 목록 |
| `sy.auth.group.empty.screen.select.group` | 왼쪽에서 그룹을 선택하세요. | 권한목록 (그룹 미선택) |
| `sy.auth.group.empty.component.none` | 프로그램에 등록된 컴포넌트가 없습니다. | 컴포넌트목록 빈 목록 |
| `sy.auth.group.empty.component.select.screen` | 권한목록에서 화면을 선택하세요. | 컴포넌트목록 (화면 미선택) |
| `sy.auth.group.empty.addable.none` | 추가할 수 있는 화면이 없습니다. | 화면 추가 팝업 빈 목록 |
| `sy.auth.group.delete.select` | 삭제할 권한그룹을 선택하세요. | 그룹 행삭제 (선택 없음) |
| `sy.auth.group.delete.confirm` | 선택한 권한그룹과 그룹 사용자·화면·컴포넌트 권한이 함께 삭제됩니다.\n삭제하시겠습니까? | 그룹 행삭제 확인 |
| `sy.auth.group.select.required` | 권한그룹을 먼저 선택하세요. | 권한복사·화면추가 (그룹 미선택) |
| `sy.auth.group.deleted.readonly` | 삭제 표시된 그룹의 권한은 변경할 수 없습니다. | 권한복사·화면추가 (삭제 그룹) |
| `sy.auth.group.copy.select.source` | 권한을 복사할 원본 그룹을 선택하세요. | 권한복사 (원본 미선택) |
| `sy.auth.group.copy.confirm` | [{0}] 그룹의 화면·컴포넌트 권한을 복사합니다.\n이미 있는 화면의 컴포넌트 권한은 원본 값으로 덮어씁니다. | 권한복사 확인 |
| `sy.auth.group.info.copy.no.auth` | 복사할 화면 권한이 없습니다. | 권한복사 (원본 화면 권한 0건) |
| `sy.auth.group.screen.delete.select` | 삭제할 화면을 선택하세요. | 권한목록 행삭제 (선택 없음) |
| `sy.auth.group.screen.add.select` | 추가할 화면을 선택하세요. | 팝업 추가 (선택 없음) |
| `sy.auth.group.row.label` | 그룹 {0}행 | 검증 메시지 행 표시 |
| `sy.auth.group.validate.id.required` | {0}: 그룹ID는 필수 입력입니다. | 저장 검증 |
| `sy.auth.group.validate.name.required` | {0}: 그룹명은 필수 입력입니다. | 저장 검증 |
| `sy.auth.group.validate.id.duplicate` | {0}: 그룹ID [{1}]가 중복되었습니다. | 저장 검증 |
| `sy.auth.group.validate.period.invalid` | {0}: 유효기간 형식이 올바르지 않습니다. | 저장 검증 |
| `sy.auth.group.validate.period.reversed` | {0}: 그룹 시작일이 종료일보다 늦습니다. | 저장 검증 |
| `cm.info.no.changes` | 저장할 변경 내용이 없습니다. | 저장 (변경 없음) |
| `cm.confirm.save` | 저장하시겠습니까? | 저장 확인 |
| `sy.auth.group.save.id.exists` | 그룹ID [{0}]는 이미 등록되어 있습니다. | 저장 실패 (backend, 그룹ID 중복) |
| `cm.error.save.failed` | 저장에 실패했습니다. | 저장 실패 (서버 메시지 없음) |
| `cm.info.saved` | 저장되었습니다. | 저장 성공 |

---

## 9. 비고 / 미결사항

- 그룹 사용여부(`useYn`)는 데이터에 있지만 화면에 편집 항목이 없다. 신규 그룹은 `Y`로 저장된다.
- 유효기간은 필수가 아니다. 신규 그룹 시작일은 빈 값, 종료일 기본값은 `99991231`이다 (SYAU010 신규 사용자, SYAU030 그룹 사용자 추가(backend `insertAuthGroupUser`), SYPG010 프로그램 종료일 기본값과 동일).
- 행삭제 확인 규칙: 마스터 행(그룹) 행삭제만 확인 메시지를 띄우고, 권한목록 행삭제는 확인 없이 D 표시만 한다 (SYAU010·SYAU030 공통 규칙).
- 화면/컴포넌트 권한 편집 로직(`useMenuCmpnAuth`), `AuthCmpnGrid`, `AuthScreenAddPopup`은 SYAU010 사용자권한관리와 함께 쓴다. 수정 시 두 화면을 함께 확인한다.
