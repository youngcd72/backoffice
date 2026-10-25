# SYPG020 메뉴관리 프로그램 사양서

| 항목 | 내용 |
|---|---|
| 프로그램 ID | SYPG020 |
| 프로그램명 | 메뉴관리 |
| 메뉴 경로 | 시스템관리 > 메뉴관리 > 메뉴관리 |
| 라우트 | `/sy/sypg020` (name `SYPG020`) |
| 화면 파일 | `workspace/coder-frontend/src/views/system/menu/MenuManageView.vue` |
| API 파일 | `workspace/coder-frontend/src/api/sy/menu.ts` |
| API 상태 | 실제 API 연동 (coder-backend `MenuController`) |
| 대상 데이터 | `TB_MENU_M` (메뉴), 삭제 시 `TB_PRGM_FAV_L`(즐겨찾기)·메뉴 권한 함께 삭제 |
| 작성자 | ywkim |
| 작성일 | 2026-10-23 |

### 변경 이력

| 일자 | 작성자 | 내용 |
|---|---|---|
| 2026-10-23 | ywkim | 최초 작성 (현재 구현 기준) |
| 2026-10-23 | ywkim | 문제점 수정 반영 |

---

## 1. 개요

시스템 메뉴(`TB_MENU_M`)를 트리와 그리드로 조회하고, 그리드에서 행 단위로 등록·수정·삭제한 뒤 일괄 저장한다. 메뉴 수가 많지 않아 전체 메뉴를 한 번에 조회하고, 트리 구성과 검색조건 필터링은 화면에서 처리한다. 메뉴타입은 `MENU`(메뉴, 하위 메뉴를 가짐)와 `LINK`(링크, 프로그램ID로 화면을 연다) 두 가지이다.

---

## 2. 화면 구성

```text
┌ [SYPG020] 메뉴관리 ───────────────────────────────── [신규][저장][엑셀] ┐
├ 검색영역 (frmSrch) ─────────────────────────────────────────────────────┤
│ 메뉴ID [    ]  메뉴명 [    ]  메뉴타입 [전체▼]  사용여부 [Y▼]     [조회] │
├ 메뉴 ───── [열기][닫기] ┬ 메뉴목록 - N건 ──────────── [행추가][행삭제] ┤
│ ▸ 기준정보              │ □│상태│NO│메뉴ID│메뉴명│정렬│메뉴타입│...      │
│ ▾ 시스템관리            │ (grdMenuList, 15행 페이징, 셀 편집)          │
│   ▸ 메뉴관리            │                                              │
│ (treMenu)               │                                              │
└─────────────────────────┴──────────────────────────────────────────────┘
  상위메뉴명 셀 돋보기 → [상위메뉴 검색] 팝업 (mdlUpprMenu, 640px)
```

- 왼쪽 트리 영역은 260px 고정, 오른쪽 그리드가 나머지 폭을 쓴다. 트리 박스는 최대 높이 640px(스크롤).
- 화면 폭 900px 이하에서는 트리와 그리드를 세로로 쌓고, 트리 박스 최대 높이는 280px이다.

---

## 3. 검색조건

| Ref/ID | 항목 | 컴포넌트 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|---|
| `txtMenuId` | 메뉴ID | Input | | | 부분 일치, 대소문자 무시. Enter 시 조회 |
| `txtMenuNm` | 메뉴명 | Input | | | 부분 일치, 대소문자 무시. Enter 시 조회 |
| `cboMenuTypeCd` | 메뉴타입 | Select | | 전체 | 공통코드 `SY/MENU_TYPE_CD` (`MENU` 메뉴, `LINK` 링크) |
| `cboUseYn` | 사용여부 | Select (clearable) | Y | `Y` | `YN_OPTIONS`(Y/N, 임시 상수). `field-name="사용여부"` |

- 검색영역 폼 ID는 `frmSrch`(ref `frmSrchRef`)이며, submit 시 조회한다.
- 검색조건은 서버로 보내지 않는다. 전체 메뉴를 조회한 뒤 화면에서 필터링한다 (6.1).

---

## 4. 그리드 / 상세

### 4.1 메뉴 트리 (`treMenu`)

- `TreeView`로 표시한다. 노드는 `id = menuId`, `label = menuNm`(없으면 `menuId`)이며, 삭제 표시(`D`) 행과 메뉴ID가 빈 행은 제외한다.
- 상위메뉴ID(`upprMenuId`)가 조회 목록에 없는 메뉴는 최상위로 붙인다.
- 같은 부모 안에서는 정렬순번(`alinSeq`), 메뉴ID 순으로 정렬한다 (`compareMenu`).
- 최초 조회 시 최상위 노드만 펼친다 (펼침 상태가 비어 있을 때만). 재조회 시 선택 노드가 사라졌으면 선택을 해제한다.
- 노드 선택 시 해당 메뉴와 그 하위 메뉴 전체(DFS 순서) 중 검색조건에 맞는 메뉴를 그리드에 표시한다. 선택 노드가 없으면 전체 메뉴가 대상이다.

### 4.2 메뉴목록 그리드 (`grdMenuList`)

| 컬럼 | Key | 폭 | 정렬 | 편집 | 비고 |
|---|---|---|---|---|---|
| NO | `no` | 56px | 가운데 | | 그리드 표시 순번 |
| 메뉴ID | `menuId` | 110px | | 신규 행만 | 필수(신규), maxlength 20, 셀 입력 100px |
| 메뉴명 | `menuNm` | 180px | | O | 필수(신규 표시), maxlength 50, 셀 입력 170px |
| 정렬 | `alinSeq` | 64px | 가운데 | O | number 입력(min 0, max 99), 셀 입력 56px. 빈 값이면 `null` |
| 메뉴타입 | `menuTypeCd` | 84px | 가운데 | O | select, 공통코드 `SY/MENU_TYPE_CD` |
| 상위메뉴ID | `upprMenuId` | 100px | | | 표시 전용 (팝업으로 변경) |
| 상위메뉴명 | `upprMenuNm` | 160px | | | 상위 메뉴의 `menuNm` + 돋보기 버튼(상위메뉴 검색 팝업) |
| 사용여부 | `useYn` | 72px | 가운데 | O | 체크박스 (체크 `Y` / 해제 `N`) |
| 프로그램ID | `prgmId` | 130px | | O | maxlength 100, 셀 입력 120px. 메뉴타입 `LINK`는 필수, `MENU`는 입력 불가 (저장 검증, 6.3) |
| 메뉴PATH | `menuPath` | (나머지) | | | 최상위부터 `메뉴명 > 메뉴명` 형식 |

- 15행 페이징, 행 선택(체크박스, `selectable`), 행 상태 컬럼(`flag-column`, I/U/D) 표시. 건수는 `lblTotCnt`에 그리드 표시 행 중 삭제(`D`) 표시 행을 뺀 수로 표시한다.
- 필수 셀(메뉴ID, 메뉴명)은 신규(`I`) 행에서만 `cell-input is-required`로 빨간 줄을 표시한다.
- 셀 값을 바꾸면 기존 행은 수정(`U`)으로 표시한다 (`markRowUpdated`).
- 신규 행은 검색조건과 관계없이 항상 그리드에 남는다.

### 4.3 상위메뉴 검색 팝업 (`mdlUpprMenu`)

| Ref/ID | 항목 | 내용 |
|---|---|---|
| `txtUpprMenuKeyword` | 메뉴ID/명 | 메뉴ID 또는 메뉴명 부분 일치 (대소문자 무시), 입력 즉시 필터링. placeholder "메뉴ID 또는 메뉴명" |
| `grdUpprMenuList` | 후보 그리드 | 메뉴ID(`menuId`, 100px), 메뉴명(`menuNm`, 160px), 메뉴PATH(`menuPath`). 8행 페이징 |

- 후보는 메뉴타입이 `MENU`이고 삭제 표시가 아니며, 대상 행 자신과 그 하위 메뉴가 아닌 메뉴이다 (트리 순서).
- 후보가 없으면 `sy.menu.empty.selectable.none`을 표시한다.
- 팝업 제목 "상위메뉴 검색", 폭 640px. 팝업을 열 때 검색어를 비운다.

---

## 5. 버튼 / 이벤트

| Ref/ID | 버튼/이벤트 | 핸들러 | 권한 | 처리 |
|---|---|---|---|---|
| `btnNew` | 신규 | `onBtnNewClick` | v-permission | 변경이 있으면 확인(`sy.menu.new.confirm.discard`) 후 메뉴 재조회. 검색조건·그리드를 초기화하고 신규 행 1건 추가 |
| `btnSave` | 저장 | `onBtnSaveClick` | v-permission | 검증 후 일괄 저장 (6.3) |
| `btnXlsDnld` | 엑셀 | `onBtnXlsDnldClick` | v-permission | 그리드 목록 CSV 다운로드 (6.4) |
| `btnSrch` | 조회 | `onBtnSrchClick` | v-permission | 검색영역 검증 → 미저장 변경 확인 → 조회 (6.1) |
| `txtMenuId`, `txtMenuNm` | Enter | `onBtnSrchClick` | | 조회 |
| `btnTreeOpen` | 열기 | `onBtnTreeOpenClick` | v-permission | 트리 전체 펼치기 |
| `btnTreeClose` | 닫기 | `onBtnTreeCloseClick` | v-permission | 트리 전체 접기 |
| `treMenu` | 노드 선택 | `onTreMenuSelect` | | 선택 메뉴와 하위 메뉴를 그리드에 표시 (선택 행 해제) |
| `btnRowAdd` | 행추가 | `onBtnRowAddClick` | v-permission | 신규 행 추가 (6.2) |
| `btnRowDel` | 행삭제 | `onBtnRowDelClick` | v-permission | 선택 행 삭제 표시 (6.2) |
| (상위메뉴명 셀 돋보기) | 클릭 | `onBtnUpprMenuSrchClick` | | 대상 행을 지정하고 상위메뉴 검색 팝업 열기 |
| `grdUpprMenuList` | 행 클릭 | `onGrdUpprMenuListRowClick` | | 선택 메뉴를 대상 행의 상위메뉴로 적용하고 팝업 닫기 |
| `btnUpprMenuRoot` | 최상위로 지정 | `onBtnUpprMenuRootClick` | v-permission | 상위메뉴ID를 빈 값(최상위)으로 적용하고 팝업 닫기 |
| `btnUpprMenuClose` | 닫기 | `onBtnUpprMenuCloseClick` | v-permission | 팝업 닫기 |

- 상위메뉴를 적용하면 대상 행은 수정(`U`)으로 표시한다 (`applyUpprMenu`).

---

## 6. 처리 로직

### 6.0 초기화 (`onMounted`)
1. `fetchPageData()` : 공통코드 `SY/MENU_TYPE_CD`(메뉴타입)를 조회한다. 사용여부 옵션은 `YN_OPTIONS` 상수를 쓴다 (TODO: 사용여부 공통코드 등록 시 대체). 검색조건 메뉴타입 옵션은 앞에 "전체"를 붙인다.
2. `initPageUi()` : 검색조건(`useYn = 'Y'`)과 그리드를 초기화하고 최초 조회한다.

### 6.1 조회 (`searchMenuList`)
1. `onBtnSrchClick` : `validateForm(frmSrch)`으로 검색영역을 검증한다 (사용여부 미선택 시 `cm.validate.required`). 저장하지 않은 변경이 있으면 `cm.confirm.discard.and.search`로 확인한다.
2. `loadMenuList()` : `getMenuListApi`로 전체 메뉴를 조회해 행(`_flag = ''`)으로 바꾼다. 선택 노드가 목록에 없으면 해제하고, 펼침 상태가 비어 있으면 최상위 노드를 펼친다.
3. `refreshGrid()` : 선택 노드 기준 하위 메뉴(`collectSubtree`) 중 메뉴ID·메뉴명(둘 다 대소문자 무시)·메뉴타입·사용여부 조건에 맞는 행과 신규(`I`) 행을 그리드에 표시하고, NO를 다시 매긴다.
4. 그리드가 비었으면 `cm.info.no.data`를 띄운다.

### 6.2 행추가 / 행삭제
- 행추가 (`addMenuRow`)
  - 상위메뉴ID: 선택 노드가 `MENU` 타입이면 선택 노드, 아니면 빈 값(최상위).
  - 기본값: 메뉴ID·메뉴명·프로그램ID·비고 빈 값, 정렬 = 같은 부모의 최대 정렬순번 + 1, 메뉴타입 `MENU`, 사용여부 `Y`.
  - 신규 행은 그리드 맨 위에 추가한다.
- 행삭제 (`onBtnRowDelClick`)
  - 선택 행이 없으면 `sy.menu.delete.select`.
  - 선택 행 중 하위 메뉴(삭제 표시가 아니고 이번에 함께 선택하지 않은 메뉴)가 남아 있는 행이 있으면 `sy.menu.delete.has.children`(메뉴명)을 띄우고 중단한다.
  - 신규(`I`) 행은 바로 제거하고, 나머지는 삭제(`D`)로 표시한다 (저장 시 삭제). 트리에서 선택 중인 메뉴를 삭제하면 트리 선택을 해제한다.

### 6.3 저장 (`useGridSave.saveChanges`)
1. 변경 행이 없으면 `cm.info.no.changes`.
2. `validateMenuRows()` : 등록·수정 행(전체 메뉴 대상)을 차례로 검증하고 첫 오류를 경고로 띄운다. 행 표시는 메뉴ID가 있으면 `sy.menu.row.label.id`, 없으면 `sy.menu.row.label.no`.
   - 메뉴ID 없음 → `sy.menu.validate.id.required`
   - 메뉴명 없음 → `sy.menu.validate.name.required`
   - 메뉴ID 중복(삭제 표시 제외) → `sy.menu.validate.id.duplicate`
   - 정렬순번 없음 → `sy.menu.validate.sequence.required`
   - 메뉴타입 `LINK`인데 프로그램ID 없음(trim 기준) → `sy.menu.validate.prgm.id.required`
   - 메뉴타입 `MENU`인데 프로그램ID 있음 → `sy.menu.validate.prgm.id.not.allowed`
   - 상위메뉴가 목록에 없음 → `sy.menu.validate.parent.not.found`
   - 상위메뉴가 삭제 표시 → `sy.menu.validate.parent.deleted`
   - 상위메뉴가 자기 자신·하위 메뉴(순환) → `sy.menu.validate.parent.circular`
3. `cm.confirm.save`로 확인한다.
4. `splitRowsByFlag`로 `insertList`/`updateList`/`deleteList`를 만들어 `saveMenuListApi`를 호출한다. 메뉴ID·메뉴명은 trim하고 화면용 필드(`id`, `no`, `_flag`)는 뺀다.
5. 실패(`success = false`)면 서버 메시지(없으면 `cm.error.save.failed`)를 오류로 띄운다. 성공이면 `cm.info.saved` 후 재조회한다.
- backend 처리 순서: 삭제(즐겨찾기·그룹 메뉴 권한·사용자 메뉴 권한 삭제 후 메뉴 삭제) → 남은 하위 메뉴 확인(`sy.menu.save.child.exists`) → 수정 → 등록(메뉴ID 존재 시 `sy.menu.save.id.exists`). 등록·수정 행은 처리 전에 메뉴타입별 프로그램ID 규칙을 화면과 같은 메시지(`sy.menu.validate.prgm.id.*`, `{0}`은 메뉴ID)로 다시 검사한다.

### 6.4 엑셀 다운로드
- 그리드가 비었으면 `cm.info.no.export.data`를 띄운다.
- 파일명 `메뉴목록_{YYYYMMDD}.csv`. 컬럼은 그리드 컬럼 전체(NO 포함)이며, 삭제 표시 행은 제외한다. 메뉴타입은 명칭으로, 상위메뉴명·메뉴PATH는 계산값으로 넣는다.

---

## 7. 사용 API

| 함수 | Method / URL | 요청 | 응답 | 비고 |
|---|---|---|---|---|
| `getMenuListApi` | `GET /api/v1/menus` | 없음 | `Menu[]` | `TB_MENU_M` 전체, 정렬순번 순 |
| `saveMenuListApi` | `POST /api/v1/menus` | `MenuSaveRequest { insertList, updateList, deleteList }` | `number` (저장 건수) | `requestApi`로 감싸 업무 오류면 `success=false`와 서버 메시지 반환 |
| `getCommCdOptions` | 공통코드 조회 (`api/common/commonCode.ts`) | 업무약어, 코드분류 | `CodeOption[]` | `SY/MENU_TYPE_CD` |

- `Menu` : `menuId`, `menuNm`, `upprMenuId`, `alinSeq`(number \| null), `menuTypeCd`, `useYn`, `prgmId`, `rmk`.
- `MENU_TYPE_CD` 상수(`MENU`, `LINK`)는 `api/sy/menu.ts`에 있다.

---

## 8. 메시지

| 키 | 문구 (ko) | 사용 시점 |
|---|---|---|
| `cm.validate.required` | {0} 필수 입력 항목입니다. | 사용여부 미선택 (검색영역 검증) |
| `cm.confirm.discard.and.search` | 저장하지 않은 변경 내용이 있습니다.\n조회하시겠습니까? | 미저장 변경이 있을 때 조회 |
| `cm.info.no.data` | 조회된 내역이 없습니다. | 조회 결과 없음 |
| `sy.menu.new.confirm.discard` | 저장하지 않은 변경 내용이 사라집니다.\n계속하시겠습니까? | 미저장 변경이 있을 때 신규 |
| `sy.menu.delete.select` | 삭제할 행을 선택하세요. | 행삭제 시 선택 없음 |
| `sy.menu.delete.has.children` | 메뉴 [{0}]에 하위 메뉴가 있어 삭제할 수 없습니다.\n하위 메뉴를 함께 선택하거나 먼저 삭제하세요. | 하위 메뉴가 남는 메뉴 삭제 |
| `sy.menu.empty.selectable.none` | 선택할 수 있는 메뉴가 없습니다. | 상위메뉴 후보 없음 |
| `sy.menu.row.label.id` | 메뉴 [{0}] | 검증 메시지 행 표시 (메뉴ID 있음) |
| `sy.menu.row.label.no` | {0}행 | 검증 메시지 행 표시 (메뉴ID 없음) |
| `sy.menu.validate.id.required` | {0}: 메뉴ID는 필수 입력입니다. | 저장 검증 |
| `sy.menu.validate.name.required` | {0}: 메뉴명은 필수 입력입니다. | 저장 검증 |
| `sy.menu.validate.id.duplicate` | {0}: 메뉴ID가 중복되었습니다. | 저장 검증 |
| `sy.menu.validate.sequence.required` | {0}: 정렬 순번은 필수 입력입니다. | 저장 검증 |
| `sy.menu.validate.prgm.id.required` | {0}: 링크 메뉴는 프로그램ID가 필수입니다. | 저장 검증 |
| `sy.menu.validate.prgm.id.not.allowed` | {0}: 메뉴 그룹에는 프로그램ID를 입력할 수 없습니다. | 저장 검증 |
| `sy.menu.validate.parent.not.found` | {0}: 상위메뉴 [{1}]가 존재하지 않습니다. | 저장 검증 |
| `sy.menu.validate.parent.deleted` | {0}: 상위메뉴 [{1}]가 삭제 예정입니다. | 저장 검증 |
| `sy.menu.validate.parent.circular` | {0}: 상위메뉴를 자기 자신이나 하위 메뉴로 지정할 수 없습니다. | 저장 검증 |
| `cm.info.no.changes` | 저장할 변경 내용이 없습니다. | 변경 없이 저장 |
| `cm.confirm.save` | 저장하시겠습니까? | 저장 확인 |
| `cm.error.save.failed` | 저장에 실패했습니다. | 저장 실패 (서버 메시지 없음) |
| `cm.info.saved` | 저장되었습니다. | 저장 완료 |
| `sy.menu.save.id.exists` | 메뉴ID [{0}]가 이미 등록되어 있습니다. | backend: 등록 메뉴ID 존재 |
| `sy.menu.save.child.exists` | 메뉴ID [{0}]에 하위 메뉴가 있어 삭제할 수 없습니다. 하위 메뉴를 먼저 삭제하세요. | backend: 삭제 메뉴에 하위 메뉴 남음 |
| `cm.info.no.export.data` | 내보낼 내역이 없습니다. | 엑셀 대상 없음 |

---

## 9. 비고 / 미결사항

- 사용여부 검색 옵션은 공통코드가 없어 `YN_OPTIONS` 상수를 쓴다 (코드 TODO).
- 화면 ID와 메뉴 순서가 설계서와 맞는지 확인이 필요하다 (PRD Open Issue 7).
- 프로그램ID가 `TB_PRGM_M`에 있는지는 화면·backend 모두 확인하지 않는다.
- 비고(`rmk`)는 그리드에 표시·편집하지 않는다 (신규 행은 빈 값으로 저장).
