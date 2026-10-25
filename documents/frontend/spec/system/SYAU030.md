# SYAU030 그룹별사용자관리 프로그램 사양서

| 항목 | 내용 |
|---|---|
| 프로그램 ID | SYAU030 |
| 프로그램명 | 그룹별사용자관리 |
| 메뉴 경로 | 시스템관리 > 권한관리 > 그룹별사용자관리 (메뉴ID `SY020300`, `TB_MENU_M`) |
| 라우트 | `/sy/syau030` (name `SYAU030`) |
| 화면 파일 | `workspace/coder-frontend/src/views/system/auths/AuthGroupUserManageView.vue` |
| API 파일 | `workspace/coder-frontend/src/api/sy/authGroupUser.ts` (사용자 후보 조회는 `src/api/sy/usrAuthMng.ts`) |
| API 상태 | 실제 API 연동 (coder-backend `AuthGroupController`, `/api/v1/auth-groups`) |
| 대상 데이터 | `TB_AUTH_GRP_M`(권한그룹기본), `TB_USR_AUTH_GRP_R`(사용자권한그룹관계), `TB_USR_M`(사용자) |
| 작성자 | ywkim |
| 작성일 | 2026-10-23 |

### 변경 이력

| 일자 | 작성자 | 내용 |
|---|---|---|
| 2026-10-23 | ywkim | 최초 작성 (현재 구현 기준) |
| 2026-10-23 | ywkim | 문제점 수정 반영 |

---

## 1. 개요

권한그룹 목록과 그룹별 사용자수를 조회하고, 선택한 권한그룹에 속한 사용자를 조회한다. 사용자추가 팝업에서 사용 중인 사용자를 골라 그룹에 추가하거나, 그룹 사용자를 행삭제로 제외한 뒤 저장한다. 저장 단위는 선택한 그룹 1건이다.

---

## 2. 화면 구성

```text
┌ [SYAU030] 그룹별사용자관리 ───────────────────────────────────────── [저장] ┐
├ 검색영역 (frmSrch) ─────────────────────────────────────────────────────────┤
│ ID/그룹 [그룹ID][그룹명]  사용여부 [전체▼]                            [조회] │
├ 그룹목록 - N건   사용자 합계 N명 ─┬ 사용자 목록 (그룹명) - N건 [사용자추가][행삭제] ┤
│ NO│그룹ID│그룹명│사용자수         │ NO│부서명│사용자ID│사용자명│사용자유형       │
│ (grdAuthGrpList, 20행 페이징)     │ (grdAuthGrpUsrList, 20행 페이징,          │
│                                   │  체크박스 선택)                           │
└───────────────────────────────────┴───────────────────────────────────────────┘
  [사용자추가] → [사용자 추가] 팝업 (mdlUsrSrch, 680px)
```

- 왼쪽 그룹목록과 오른쪽 사용자 목록은 1 : 1.4 비율이다. 화면 폭 1280px 이하이면 위아래로 배치된다.
- 세 그리드 모두 줄바꿈 없음(`nowrap-grid`), 조밀(dense) 표시이다.

---

## 3. 검색조건

| Ref/ID | 항목 | 컴포넌트 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|---|
| `txtUsrGrpCd` | 그룹ID | Input | | | placeholder "그룹ID". Enter 시 조회 |
| `txtUsrGrpNm` | 그룹명 | Input | | | placeholder "그룹명". Enter 시 조회 |
| `cboUseYn` | 사용여부 | Select | | 전체 | `USE_YN_OPTIONS`(사용 `Y`/미사용 `N`) + 전체 |

- 검색영역 폼 ID는 `frmSrch`이며, submit 시 조회한다.

---

## 4. 그리드 / 상세

### 4.1 그룹목록 그리드 (`grdAuthGrpList`)

| 컬럼 | Key | 폭 | 정렬 | 비고 |
|---|---|---|---|---|
| NO | `no` | 48px | 가운데 | 조회 순번 |
| 그룹ID | `usrGrpCd` | 150px | | 정렬 가능 |
| 그룹명 | `usrGrpNm` | (나머지) | | 정렬 가능 |
| 사용자수 | `usrCnt` | 80px | 오른쪽 | 정렬 가능, 천 단위 구분 (`toLocaleString`) |

- 20행 페이징. 체크박스 선택 없음 (행 클릭으로 활성 그룹 선택).
- 건수는 `lblGrpTotCnt`("N건"), 조회된 그룹의 사용자수 합계는 `lblUsrTotCnt`("사용자 합계 N명")에 표시한다.
- 조회 후 다시 선택할 그룹(저장 후 같은 그룹, 없으면 첫 그룹)을 활성 행으로 선택한다.

### 4.2 사용자 목록 그리드 (`grdAuthGrpUsrList`)

| 컬럼 | Key | 폭 | 정렬 | 비고 |
|---|---|---|---|---|
| NO | `no` | 48px | 가운데 | 순번 (추가·행삭제 시 재부여) |
| 부서명 | `deptCd` | 130px | | `DEPT_OPTIONS`(임시 목록)로 명칭 변환 |
| 사용자ID | `usrId` | 110px | | 정렬 가능 |
| 사용자명 | `usrNm` | (나머지) | | 정렬 가능 |
| 사용자유형 | `usrClsCd` | 100px | 가운데 | 공통코드 `SY/USR_CLS_CD`로 명칭 변환 |

- 20행 페이징, 체크박스 선택(`selectable`), 행 상태 표시 컬럼(`flag-column`, I/D).
- 제목: "사용자 목록 (활성 그룹명)" + 건수 `lblUsrCnt`("N건").
- 빈 목록 문구: 그룹 선택 시 `sy.auth.group.user.empty.none`, 미선택 시 `sy.auth.group.empty.group.select`.

### 4.3 사용자 추가 팝업 (`mdlUsrSrch`)

| 항목 | 내용 |
|---|---|
| 팝업 | `PopupWindow` id `mdlUsrSrch`, 제목 "사용자 추가", 폭 680px |
| 검색 | `txtUsrKeyword` (placeholder "사용자ID·사용자명"), 입력 즉시 필터 (사용자ID·사용자명 포함, 대소문자 무시) |
| 그리드 | `grdUsrSrchList`, 8행 페이징, 조밀, 체크박스 선택. 빈 목록 `sy.auth.group.user.empty.addable.none` |
| 후보 | 팝업을 열 때 조회한 전체 사용자 중 사용여부 `Y`이고 현재 사용자 목록에 없는 사용자 + 현재 목록에서 삭제(D) 표시한 사용자 (사용여부 무관) |
| 버튼 | `btnUsrSrchAdd`(추가), `btnUsrSrchClose`(닫기) |

| 컬럼 | Key | 폭 | 정렬 | 비고 |
|---|---|---|---|---|
| 사용자ID | `usrId` | 110px | | |
| 사용자명 | `usrNm` | (나머지) | | |
| 부서명 | `deptCd` | 130px | | `DEPT_OPTIONS`로 명칭 변환 |
| 사용자유형 | `usrClsCd` | 100px | 가운데 | 공통코드 `SY/USR_CLS_CD`로 명칭 변환 |

---

## 5. 버튼 / 이벤트

| Ref/ID | 버튼/이벤트 | 핸들러 | 권한 | 처리 |
|---|---|---|---|---|
| `btnSave` | 저장 | `onBtnSaveClick` | v-permission | 선택 그룹의 사용자 추가/제외분 저장 (6.4) |
| `btnSrch` | 조회 | `onBtnSrchClick` | v-permission | 변경 있으면 확인 후 그룹목록 조회 (6.1) |
| `txtUsrGrpCd`, `txtUsrGrpNm` | Enter | `onBtnSrchClick` | | 조회 |
| `grdAuthGrpList` | 행 클릭 | `onGrdAuthGrpListRowClick` | | 다른 그룹이면 (변경 있을 때 확인 후) 그룹 사용자 조회 (6.2) |
| `btnUsrAdd` | 사용자추가 | `onBtnUsrAddClick` | v-permission | 사용자 추가 팝업 열기 (6.3) |
| `btnUsrRowDel` | 행삭제 | `onBtnUsrRowDelClick` | v-permission | 선택 사용자 삭제 표시 (6.3) |
| `btnUsrSrchAdd` | 추가 (팝업) | `onBtnUsrSrchAddClick` | v-permission | 선택 사용자를 사용자 목록에 추가 (6.3) |
| `btnUsrSrchClose` | 닫기 (팝업) | `onBtnUsrSrchCloseClick` | v-permission | 팝업 닫기 |

- 버튼 컴포넌트 6개(`btnSave`~`btnUsrSrchClose`)는 `TB_PRGM_CMPN_M` 시드(`schema/data/TB_PRGM_CMPN_M-data.sql`)에 등록되어 있다.

---

## 6. 처리 로직

### 6.0 초기화 (`onMounted`)
1. `fetchPageData()` : 공통코드 `SY/USR_CLS_CD`(사용자유형)를 조회한다 (그리드·팝업 명칭 변환용).
2. `initPageUi()` : 기본 조건(사용여부 전체)으로 최초 조회(`searchGrpList`)한다.

### 6.1 조회 (`onBtnSrchClick` → `searchGrpList`)
1. 저장하지 않은 사용자 추가/삭제가 있으면 `cm.confirm.discard.and.search`로 확인한다 (`confirmDiscardChanges`).
2. `getAuthGrpUsrCntListApi(검색조건)`로 그룹목록을 조회하고 순번(`no`)을 붙인다.
3. 다시 선택할 그룹(`keepUsrGrpCd`가 목록에 있으면 그 그룹, 없으면 첫 그룹)의 사용자 목록을 조회한다 (6.2). 그룹이 없으면 사용자 목록을 비운다.
4. 그룹 결과가 없으면 `cm.info.no.data`를 띄운다.

### 6.2 그룹 선택 (`onGrdAuthGrpListRowClick` → `searchUsrList`)
1. 같은 그룹을 클릭하면 무시한다. 저장하지 않은 변경이 있으면 `sy.auth.group.user.confirm.discard.and.select`로 확인한다.
2. 활성 그룹을 바꾸고 선택을 비운 뒤 `getAuthGrpUsrListApi(usrGrpCd)`로 그룹 사용자를 조회한다 (순번, 상태 `''`).

### 6.3 사용자 추가 / 행삭제
- 사용자추가: 활성 그룹이 없으면 `sy.auth.group.select.required`를 띄운다. `getUsrMngListApi({})`로 전체 사용자를 조회하고, 검색어·선택을 비운 뒤 팝업을 연다.
- 팝업 추가: 활성 그룹이 없거나 선택이 없으면 `sy.auth.group.user.add.select`를 띄운다. 선택한 사용자 중 삭제(D) 표시한 사용자는 새 행을 만들지 않고 원래 행의 상태를 `''`로 되돌린다(복원). 나머지는 `I` 행(`usrGrpCd`, `usrId`, `usrNm`, `usrClsCd`, `deptCd`)으로 사용자 목록 끝에 추가하고 팝업을 닫는다. 저장 시 추가된 사용자의 사용기간은 backend가 오늘~`99991231`로 넣는다.
- 행삭제: 선택이 없으면 `sy.auth.group.user.delete.select`를 띄운다. 확인 없이 신규(`I`) 행은 바로 제거하고, 나머지는 `D`로 표시한다(저장 시 그룹에서 제외).

### 6.4 저장 (`onBtnSaveClick`, `useGridSave.saveChanges`)
1. 활성 그룹이 없거나 변경이 없으면 `cm.info.no.changes`를 띄운다.
2. `cm.confirm.save`로 확인한다 (별도 검증 없음).
3. 사용자 행을 상태별로 나눠(`splitRowsByFlag`, 사용자ID만) `saveAuthGrpUsrApi({ usrGrpCd, insertList, deleteList })`를 호출한다.
4. 실패(`success=false`)면 서버 메시지(없으면 `cm.error.save.failed`)를 띄운다. 성공이면 `cm.info.saved`를 alert로 띄우고, 사용자수 갱신을 위해 그룹목록을 다시 조회해 같은 그룹을 선택한다 (`searchGrpList(usrGrpCd)`).

---

## 7. 사용 API

| 함수 | Method / URL | 요청 | 응답 | 비고 |
|---|---|---|---|---|
| `getAuthGrpUsrCntListApi` | `GET /api/v1/auth-groups/user-counts` | `usrGrpCd`, `usrGrpNm`, `useYn` | `AuthGrpUsrCnt[]` (`usrGrpCd`, `usrGrpNm`, `useYn`, `usrCnt`) | 그룹코드 순 |
| `getAuthGrpUsrListApi` | `GET /api/v1/auth-groups/{usrGrpCd}/users` | `usrGrpCd` | `AuthGrpUsr[]` (`usrGrpCd`, `usrId`, `usrNm`, `usrClsCd`, `deptCd`) | 사용자ID 순 |
| `saveAuthGrpUsrApi` | `POST /api/v1/auth-groups/{usrGrpCd}/users` | body `{ insertList: string[], deleteList: string[] }` (사용자ID) | `number` (처리 건수) | 업무 오류는 `success=false` + 서버 메시지 (`requestApi`) |
| `getUsrMngListApi` | `GET /api/v1/user-auths` (`api/sy/usrAuthMng.ts`) | `{}` (조건 없음) | `UsrMng[]` | 사용자 추가 팝업 후보 |
| `getCommCdOptions` | `GET /api/v1/common-codes/{업무}/{분류}/details?useYn=Y` (`api/common/commonCode.ts`) | 업무약어, 코드분류 | `CodeOption[]` | `SY/USR_CLS_CD` |

---

## 8. 메시지

| 키 | 문구 (ko) | 사용 시점 |
|---|---|---|
| `cm.info.no.data` | 조회된 내역이 없습니다. | 그룹 조회 결과 없음 |
| `cm.confirm.discard.and.search` | 저장하지 않은 변경 내용이 있습니다.\n조회하시겠습니까? | 변경 있는 상태에서 조회 |
| `sy.auth.group.user.confirm.discard.and.select` | 저장하지 않은 변경 내용이 있습니다.\n다른 그룹을 선택하시겠습니까? | 변경 있는 상태에서 다른 그룹 선택 |
| `sy.auth.group.select.required` | 권한그룹을 먼저 선택하세요. | 그룹 미선택 상태에서 사용자추가 |
| `sy.auth.group.user.add.select` | 추가할 사용자를 선택하세요. | 팝업 추가 (선택 없음) |
| `sy.auth.group.user.delete.select` | 삭제할 사용자를 선택하세요. | 행삭제 (선택 없음) |
| `sy.auth.group.user.empty.none` | 그룹에 속한 사용자가 없습니다. | 사용자 목록 빈 목록 |
| `sy.auth.group.empty.group.select` | 그룹목록에서 그룹을 선택하세요. | 사용자 목록 (그룹 미선택) |
| `sy.auth.group.user.empty.addable.none` | 추가할 수 있는 사용자가 없습니다. | 사용자 추가 팝업 빈 목록 |
| `cm.info.no.changes` | 저장할 변경 내용이 없습니다. | 변경 없이 저장 |
| `cm.confirm.save` | 저장하시겠습니까? | 저장 확인 |
| `cm.info.saved` | 저장되었습니다. | 저장 성공 (alert) |
| `cm.error.save.failed` | 저장에 실패했습니다. | 저장 실패 (서버 메시지 없을 때) |

---

## 9. 비고 / 미결사항

- 부서명은 부서 마스터가 없어 `DEPT_OPTIONS`(임시 목록, `api/sy/usrAuthMng.ts`)로 변환한다 (PRD Open Issue 3).
- "사용자 합계"는 조회된 그룹의 `usrCnt` 단순 합이다. 한 사용자가 여러 그룹에 속하면 중복 집계된다.
- 검색조건 사용여부 기본값은 "전체"이다 (SYAU010 사용자권한관리는 "사용").
- 행삭제 확인 규칙: 마스터 행(사용자·그룹) 행삭제만 확인하고, 나머지는 D 표시만 한다. 이 화면의 그룹 사용자 행삭제는 마스터가 아니므로 확인 없이 삭제 표시한다.
