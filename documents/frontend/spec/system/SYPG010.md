# SYPG010 프로그램관리 프로그램 사양서

| 항목 | 내용 |
|---|---|
| 프로그램 ID | SYPG010 |
| 프로그램명 | 프로그램관리 |
| 메뉴 경로 | 시스템관리 > 메뉴관리 > 프로그램관리 (메뉴ID `SY010100`) |
| 라우트 | `/sy/sypg010` (name `SYPG010`) |
| 화면 파일 | `workspace/coder-frontend/src/views/system/menu/ProgramManageView.vue` |
| 공통 로직 | `composables/useGridSave.ts` (저장·조회 확인 흐름) |
| API 파일 | `workspace/coder-frontend/src/api/sy/program.ts` |
| API 상태 | 실제 API 연동 (coder-backend `/api/v1/programs`) |
| 대상 데이터 | `TB_PRGM_M`(프로그램기본), `TB_PRGM_CMPN_M`(프로그램컴포넌트기본) |
| 작성자 | ywkim |
| 작성일 | 2026-10-23 |

### 변경 이력

| 일자 | 작성자 | 내용 |
|---|---|---|
| 2026-10-23 | ywkim | 최초 작성 (현재 구현 기준) |
| 2026-10-23 | ywkim | 문제점 수정 반영 |

---

## 1. 개요

화면(프로그램) 목록과 프로그램별 컴포넌트(버튼 등 화면의 Ref/ID)를 조회·등록·수정·삭제하고, 프로그램 목록을 CSV로 내보낸다. 컴포넌트 ID는 권한관리 화면(SYAU010/SYAU020)의 컴포넌트 권한과 버튼 권한(v-permission) 대상이 된다. 프로그램·컴포넌트 변경분은 저장 버튼 한 번으로 한 트랜잭션에 저장한다.

---

## 2. 화면 구성

```text
┌ [SYPG010] 프로그램관리 ──────────────────────────────────────────── [신규][저장][엑셀] ┐
├ 검색영역 (frmSrch) ──────────────────────────────────────────────────────────────────────┤
│ 프로그램경로 [전체▼]  프로그램ID [    ]  프로그램명 [    ]  사용 [전체▼]          [조회] │
├ 프로그램목록 - N건 [행추가][행삭제] ─────────┬ 컴포넌트목록 [프로그램ID] - N건          │
│ NO│프로그램ID│프로그램명│링크URL│프로그램경로│ │                    [행추가][행삭제]    │
│ 사용여부│시작일자│종료일자│비고               │ NO│컴포넌트ID│컴포넌트명│정렬순번│사용여부│
│ (grdPrgmList, 15행)                           │ (grdCmpnList, 15행)                     │
└───────────────────────────────────────────────┴─────────────────────────────────────────┘
```

- 왼쪽 프로그램목록은 나머지 폭, 오른쪽 컴포넌트목록은 590px. 화면 폭 1280px 이하에서는 위아래 1열로 배치한다.
- 삭제(D) 표시된 프로그램을 선택하면 컴포넌트목록이 반투명(`is-locked`)으로 바뀌고 편집할 수 없다. 제목 옆에 "(삭제 예정)"을 표시한다.

---

## 3. 검색조건

| Ref/ID | 항목 | 컴포넌트 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|---|
| `cboPrgmCrsCd` | 프로그램경로 | Select (140px) | | 전체 | 공통코드 `SY/PRGM_CRS_CD` (SY 시스템관리, MS 기준정보) |
| `txtPrgmId` | 프로그램ID | Input | | | Enter 시 조회 |
| `txtPrgmNm` | 프로그램명 | Input | | | Enter 시 조회 |
| `cboUseYn` | 사용 | Select | | 전체 | `YN_OPTIONS`(Y/N) 상수 + 전체 |

- 검색영역 폼 ID는 `frmSrch`이며, submit 시 조회한다.

---

## 4. 그리드 / 상세

### 4.1 프로그램목록 그리드 (`grdPrgmList`)

| 컬럼 | Key | 폭 | 정렬 | 필수 | 비고 |
|---|---|---|---|---|---|
| NO | `no` | 56px | 가운데 | | 순번 (행추가·삭제 시 재부여) |
| 프로그램ID | `prgmId` | 140px | | Y | 셀 입력, `maxlength` 100. 신규(I) 행만 입력 가능, 그 외 읽기전용 |
| 프로그램명 | `prgmNm` | 170px | | Y | 셀 입력, `maxlength` 50 |
| 링크URL | `lnkUrl` | 120px | | | 셀 입력, `maxlength` 1000 |
| 프로그램경로 | `prgmCrsCd` | 110px | | | 셀 select(100px), 옵션 "선택"(빈 값) + 공통코드 `SY/PRGM_CRS_CD` |
| 사용여부 | `useYn` | 84px | 가운데 | | 체크박스 (Y/N) |
| 시작일자 | `stDt` | 90px | | | 셀 입력 `YYYYMMDD`, `maxlength` 8 |
| 종료일자 | `endDt` | 90px | | | 셀 입력 `YYYYMMDD`, `maxlength` 8 |
| 비고 | `rmk` | 140px | | | 셀 입력, `maxlength` 200 |

- 15행 페이징, 조밀(dense), 체크박스 선택(selectable), 행 상태 컬럼(flag-column) 표시. 건수는 `lblTotCnt`에 표시한다.
- 텍스트 셀의 `maxlength`는 `TB_PRGM_M` 컬럼 길이 기준(`TEXT_COLUMNS`)이다. 필수 컬럼은 신규(I) 행에서만 필수 표시(`cell-input` + `is-required`)한다.
- 셀을 입력·변경하면 조회된 행은 수정(U)으로 표시한다.
- 조회 후 이전에 선택했던 프로그램(없으면 첫 행)을 활성 행으로 선택한다.
- 신규 행 기본값 (`createEmptyPrgm`): 프로그램경로 = 검색조건 `cboPrgmCrsCd` 값, `useYn` `Y`, `stDt` 오늘, `endDt` `99991231`, 나머지 빈 값. 행은 맨 위에 추가하고 바로 활성화한다.

### 4.2 컴포넌트목록 그리드 (`grdCmpnList`)

활성 프로그램의 컴포넌트 (`TB_PRGM_CMPN_M`).

| 컬럼 | Key | 폭 | 정렬 | 필수 | 비고 |
|---|---|---|---|---|---|
| NO | `no` | 48px | 가운데 | | 순번 |
| 컴포넌트ID | `cmpnId` | 110px | | Y | 셀 입력(110px), `maxlength` 50. 신규(I) 행만 입력 가능 |
| 컴포넌트명 | `cmpnNm` | (나머지) | | Y | 셀 입력(120px), `maxlength` 50 |
| 정렬순번 | `alinSeq` | 72px | 가운데 | | 숫자 입력(60px, `min` 0). 빈 값이면 `null` |
| 사용여부 | `useYn` | 72px | 가운데 | | 체크박스 (Y/N) |

- 15행 페이징, 조밀, 체크박스 선택, 행 상태 컬럼 표시.
- 제목 옆에 활성 프로그램ID(`lblCmpnPrgmId`, 신규 프로그램은 "신규"), 건수(`lblCmpnTotCnt`)를 표시한다.
- 빈 목록 안내: 프로그램 선택 시 `sy.program.empty.component.none`, 미선택 시 `sy.program.empty.select.program`.
- 프로그램을 처음 선택할 때 한 번만 서버에서 조회하고, 이후에는 화면의 편집 상태를 유지한다 (신규 프로그램은 빈 목록).
- 신규 컴포넌트 행 기본값: 정렬순번 = 현재 최대값 + 1, `useYn` `Y`. 행은 맨 아래에 추가한다.
- 컴포넌트ID는 화면 버튼 등의 Ref/ID(`btnSave` 등)와 같은 값을 쓴다.

---

## 5. 버튼 / 이벤트

| Ref/ID | 버튼/이벤트 | 핸들러 | 권한 | 처리 |
|---|---|---|---|---|
| `btnNew` | 신규 | `onBtnNewClick` | v-permission | 저장하지 않은 변경이 있으면 `cm.confirm.discard.and.search`로 확인한 뒤 검색조건·그리드를 초기화하고 신규 프로그램 행 1건 추가 |
| `btnSave` | 저장 | `onBtnSaveClick` | v-permission | 검증 후 프로그램·컴포넌트 일괄 저장 (6.4) |
| `btnXlsDnld` | 엑셀 | `onBtnXlsDnldClick` | v-permission | 프로그램 목록 CSV 다운로드 (6.5) |
| `btnSrch` | 조회 | `onBtnSrchClick` | v-permission | 변경 내용이 있으면 버릴지 확인 후 조회 (6.1) |
| `btnRowAdd` | 행추가 (프로그램) | `onBtnRowAddClick` | v-permission | 프로그램 신규 행 추가 (4.1) |
| `btnRowDel` | 행삭제 (프로그램) | `onBtnRowDelClick` | v-permission | 확인 후 선택 프로그램 삭제 처리 (6.2) |
| `btnCmpnRowAdd` | 행추가 (컴포넌트) | `onBtnCmpnRowAddClick` | v-permission | 활성 프로그램에 컴포넌트 행 추가 (6.3) |
| `btnCmpnRowDel` | 행삭제 (컴포넌트) | `onBtnCmpnRowDelClick` | v-permission | 선택 컴포넌트 삭제 처리 (6.3) |
| `grdPrgmList` | 행 클릭 | `onGrdPrgmListRowClick` | | 다른 행이면 프로그램 활성화 → 컴포넌트목록 표시 |
| `txtPrgmId`, `txtPrgmNm` | Enter | `onBtnSrchClick` | | 조회 |

- 버튼 컴포넌트는 `TB_PRGM_CMPN_M`에 SYPG010의 8개 버튼(`btnNew`, `btnSave`, `btnXlsDnld`, `btnSrch`, `btnRowAdd`, `btnRowDel`, `btnCmpnRowAdd`, `btnCmpnRowDel`)으로 등록되어 있다 (`schema/data/TB_PRGM_CMPN_M-data.sql`).

---

## 6. 처리 로직

### 6.0 초기화 (`onMounted`)
1. `fetchPageData()` : 공통코드 `SY/PRGM_CRS_CD`(프로그램경로)를 조회한다. 사용여부 옵션은 `YN_OPTIONS` 상수 앞에 "전체"를 붙여 쓴다 (TODO: 공통코드로 대체).
2. `initPageUi()` : 검색조건·그리드를 초기화하고(`resetSearchForm`, `clearGrids`) 최초 조회(`searchPrgmList`)한다.

### 6.1 조회 (`searchPrgmList`)
1. 조회 버튼(신규 버튼도 같음)은 `confirmDiscardChanges`로 저장하지 않은 변경(프로그램·컴포넌트)이 있으면 `cm.confirm.discard.and.search`를 확인한다.
2. 이전 활성 프로그램ID를 기억한 뒤 `getPrgmListApi(searchForm)`를 호출한다.
3. 그리드·편집 상태를 모두 비우고 결과를 행 상태 `''`로 넣고 NO를 부여한다.
4. 결과가 없으면 `cm.info.no.data`를 띄운다. 있으면 이전 프로그램(없으면 첫 행)을 활성화하고 컴포넌트 목록을 조회한다(`getPrgmCmpnListApi`).

### 6.2 프로그램 행삭제 (`onBtnRowDelClick`)
1. 선택한 프로그램이 없으면 `sy.program.delete.select`.
2. 프로그램을 삭제하면 하위 컴포넌트와 컴포넌트 권한이 함께 삭제되므로(backend `ProgramServiceImpl`) `cm.confirm.delete.selected`로 확인한다. 취소하면 중단한다.
3. 신규(I) 행은 바로 제거하고, 나머지는 삭제(D)로 표시한다(저장 시 삭제).
4. 제거된 프로그램의 컴포넌트 편집 상태를 지우고, 활성 프로그램이 제거됐으면 남은 첫 행을 활성화한다.

### 6.3 컴포넌트 행추가 / 행삭제
- 행추가: 활성 프로그램이 없으면 `sy.program.component.add.select.program`, 삭제(D) 표시 프로그램이면 `sy.program.component.add.deleted.program`. 아니면 `addCmpnRow`로 신규 행을 추가한다.
- 행삭제: 활성 프로그램이 없거나 선택한 컴포넌트가 없으면 `sy.program.component.delete.select`. 신규(I) 행은 바로 제거하고 나머지는 D로 표시한다. 하위 데이터가 없는 행이므로 확인 메시지는 띄우지 않는다 (행삭제 확인은 하위 데이터가 함께 지워지는 마스터 성격의 행만 한다).

### 6.4 저장 (`onBtnSaveClick` → `useGridSave.saveChanges`)
1. 변경이 없으면 `cm.info.no.changes`.
2. `validatePrgmRows()` (신규·수정 행 대상, 삭제 행 제외), 행 표시는 `sy.program.row.label`(NO):
   - 프로그램ID·프로그램명 미입력 → `sy.program.validate.required` (컬럼 라벨)
   - 프로그램ID 중복(삭제 행 제외, 화면 내) → `sy.program.validate.id.duplicate`
   - 시작일자·종료일자가 비어 있지 않은데 실제 `YYYYMMDD` 일자가 아님 → `sy.program.validate.date.format`
   - 시작일자 > 종료일자 → `sy.program.validate.date.reversed`
3. `validateCmpnRows()` (변경된 컴포넌트가 있는 프로그램, 삭제 프로그램 제외). 행 표시는 `sy.program.component.row.label`(프로그램ID, 프로그램ID가 없으면 `sy.program.row.label.no`(NO); 컴포넌트 NO):
   - 컴포넌트ID 미입력 → `sy.program.validate.component.id.required`
   - 컴포넌트명 미입력 → `sy.program.validate.component.name.required`
   - 컴포넌트ID 중복(같은 프로그램 내, 삭제 행 제외) → `sy.program.validate.component.id.duplicate`
4. `cm.confirm.save` 확인 후 `savePrgmListApi` 호출. 요청 구성:
   - `insertList` / `updateList` / `deleteList` : 프로그램 행 상태별 목록 (`splitRowsByFlag`, 프로그램ID·프로그램명 trim)
   - `cmpnSaveList` : 변경된 프로그램별 컴포넌트 `insertList`(I) / `updateList`(U) / `deleteList`(D) (컴포넌트ID·명 trim). 삭제(D) 프로그램은 제외 (프로그램 삭제 시 함께 삭제)
5. 실패(`success=false`)면 서버 메시지, 메시지가 없으면 `cm.error.save.failed`를 띄운다.
6. 성공하면 `cm.info.saved`를 띄우고 재조회(`searchPrgmList`)한다.

- backend 저장 순서: 프로그램 삭제(메뉴가 사용 중이면 오류 `sy.program.save.menu.exists`, 컴포넌트 권한 → 컴포넌트 → 프로그램 순 삭제) → 프로그램 수정 → 프로그램 등록(존재 시 `sy.program.save.id.exists`) → 컴포넌트 삭제(그룹·사용자 컴포넌트 권한 포함) → 수정 → 등록(존재 시 `sy.program.save.component.id.exists`).

### 6.5 엑셀 다운로드 (`exportPrgmListToCsv`)
- 대상은 현재 그리드 행 중 삭제(D) 행을 제외한 행이다 (저장하지 않은 신규·수정 값 포함, `getExportPrgmRows`). 대상이 없으면 `cm.info.no.export.data`를 띄운다.
- 파일명 `프로그램목록_{YYYYMMDD}.csv` (`downloadCsv`, UTF-8 BOM).
- NO는 대상 행 기준으로 1부터 다시 매긴다.
- 컬럼은 그리드 컬럼 정의(`prgmColumns`)와 같은 순서이다: NO, 프로그램ID, 프로그램명, 링크URL, 프로그램경로(코드 → 명칭), 사용여부(Y/N), 시작일자, 종료일자, 비고.

---

## 7. 사용 API

| 함수 | Method / URL | 요청 | 응답 | 비고 |
|---|---|---|---|---|
| `getPrgmListApi` | `GET /api/v1/programs` | `prgmCrsCd`, `prgmId`, `prgmNm`, `useYn` | `Prgm[]` (`prgmId`, `prgmNm`, `lnkUrl`, `prgmCrsCd`, `useYn`, `stDt`, `endDt`, `rmk`) | 정렬순번 순 |
| `getPrgmCmpnListApi` | `GET /api/v1/programs/{prgmId}/components` | `prgmId` | `PrgmCmpn[]` (`prgmId`, `cmpnId`, `cmpnNm`, `alinSeq`, `useYn`) | 정렬순번 순 |
| `savePrgmListApi` | `POST /api/v1/programs` | `PrgmSaveRequest { insertList, updateList, deleteList, cmpnSaveList }` | `number \| null` (저장 건수) | 한 트랜잭션. 업무 오류면 `success=false`와 서버 메시지 (`requestApi`) |
| `getCommCdOptions` | `GET /api/v1/common-codes/{wrkMdulCd}/{commCtgCd}/details?useYn=Y` (`api/common/commonCode.ts`) | `SY`, `PRGM_CRS_CD` | `CodeOption[]` | 프로그램경로 |

---

## 8. 메시지

| 키 | 문구 (ko) | 사용 시점 |
|---|---|---|
| `cm.confirm.discard.and.search` | 저장하지 않은 변경 내용이 있습니다.\n조회하시겠습니까? | 변경 중 조회·신규 |
| `cm.info.no.data` | 조회된 내역이 없습니다. | 조회 결과 없음 |
| `cm.confirm.delete.selected` | 선택한 항목을 삭제하시겠습니까? | 프로그램 행삭제 확인 |
| `sy.program.empty.component.none` | 등록된 컴포넌트가 없습니다. | 컴포넌트목록 빈 목록 |
| `sy.program.empty.select.program` | 왼쪽에서 프로그램을 선택하세요. | 컴포넌트목록 (프로그램 미선택) |
| `sy.program.delete.select` | 삭제할 프로그램을 선택하세요. | 프로그램 행삭제 (선택 없음) |
| `sy.program.component.add.select.program` | 컴포넌트를 추가할 프로그램을 먼저 선택하세요. | 컴포넌트 행추가 (프로그램 미선택) |
| `sy.program.component.add.deleted.program` | 삭제 표시된 프로그램에는 컴포넌트를 추가할 수 없습니다. | 컴포넌트 행추가 (삭제 프로그램) |
| `sy.program.component.delete.select` | 삭제할 컴포넌트를 선택하세요. | 컴포넌트 행삭제 (선택 없음) |
| `sy.program.row.label` | 프로그램 {0}행 | 프로그램 검증 행 표시 |
| `sy.program.row.label.no` | {0}행 | 컴포넌트 검증 (프로그램ID 없음) |
| `sy.program.component.row.label` | 프로그램 [{0}] 컴포넌트 {1}행 | 컴포넌트 검증 행 표시 |
| `sy.program.validate.required` | {0}: {1}은(는) 필수 입력입니다. | 저장 검증 (프로그램ID·명) |
| `sy.program.validate.id.duplicate` | {0}: 프로그램ID [{1}]가 중복되었습니다. | 저장 검증 |
| `sy.program.validate.date.format` | {0}: 일자는 YYYYMMDD 8자리로 입력하세요. | 저장 검증 |
| `sy.program.validate.date.reversed` | {0}: 시작일자가 종료일자보다 늦습니다. | 저장 검증 |
| `sy.program.validate.component.id.required` | {0}: 컴포넌트ID는 필수 입력입니다. | 저장 검증 |
| `sy.program.validate.component.name.required` | {0}: 컴포넌트명은 필수 입력입니다. | 저장 검증 |
| `sy.program.validate.component.id.duplicate` | {0}: 컴포넌트ID [{1}]가 중복되었습니다. | 저장 검증 |
| `cm.info.no.changes` | 저장할 변경 내용이 없습니다. | 저장 (변경 없음) |
| `cm.confirm.save` | 저장하시겠습니까? | 저장 확인 |
| `sy.program.save.id.exists` | 프로그램ID [{0}]가 이미 등록되어 있습니다. | 저장 실패 (backend) |
| `sy.program.save.component.id.exists` | 프로그램 [{0}]에 컴포넌트ID [{1}]가 이미 등록되어 있습니다. | 저장 실패 (backend) |
| `sy.program.save.menu.exists` | 프로그램ID [{0}]를 사용하는 메뉴가 있어 삭제할 수 없습니다. 메뉴관리에서 먼저 정리하세요. | 저장 실패 (backend, 삭제 프로그램을 메뉴가 사용 중) |
| `cm.error.save.failed` | 저장에 실패했습니다. | 저장 실패 (서버 메시지 없음) |
| `cm.info.saved` | 저장되었습니다. | 저장 성공 |
| `cm.info.no.export.data` | 내보낼 내역이 없습니다. | 엑셀 대상 없음 |

---

## 9. 비고 / 미결사항

- 사용여부 조회조건은 `YN_OPTIONS` 상수를 쓴다. 공통코드 API 연동 시 사용여부 공통코드로 바꾼다 (`fetchPageData` TODO).
- 시작일자·종료일자는 DatePicker가 아닌 텍스트 입력(`YYYYMMDD`)이고 필수가 아니다. 컴포넌트 정렬순번은 검증하지 않는다(빈 값 `null` 저장).
- 화면 ID·메뉴 순서(SYPG010 프로그램관리 / SYPG020 메뉴관리)가 설계서와 맞는지 확인 필요 (PRD Open Issue 7).
