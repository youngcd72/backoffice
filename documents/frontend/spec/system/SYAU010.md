# SYAU010 사용자권한관리 프로그램 사양서

| 항목 | 내용 |
|---|---|
| 프로그램 ID | SYAU010 |
| 프로그램명 | 사용자권한관리 |
| 메뉴 경로 | 시스템관리 > 권한관리 > 사용자권한관리 (메뉴ID `SY020100`, `TB_MENU_M`) |
| 라우트 | `/sy/syau010` (name `SYAU010`) |
| 화면 파일 | `workspace/coder-frontend/src/views/system/auths/UserAuthManageView.vue` |
| 하위 컴포넌트 | `views/system/auths/components/AuthCmpnGrid.vue` (컴포넌트목록 그리드), `views/system/auths/components/AuthScreenAddPopup.vue` (화면추가 팝업) |
| 공통 로직 | `composables/useMenuCmpnAuth.ts` (화면/컴포넌트 권한 편집), `composables/useGridSave.ts` (저장·조회 확인 흐름), `utils/rowFlag.ts` (행 상태 I/U/D) |
| API 파일 | `workspace/coder-frontend/src/api/sy/usrAuthMng.ts` |
| API 상태 | 실제 API 연동 (coder-backend `UserAuthController`, `/api/v1/user-auths`) |
| 대상 데이터 | `TB_USR_M`(사용자), `TB_USR_MENU_R`(사용자별 화면 권한), `TB_USR_CMPN_R`(사용자별 컴포넌트 권한), 참조 `TB_MENU_M`, `TB_PRGM_M`, `TB_PRGM_CMPN_M` |
| 작성자 | ywkim |
| 작성일 | 2026-10-23 |

### 변경 이력

| 일자 | 작성자 | 내용 |
|---|---|---|
| 2026-10-23 | ywkim | 최초 작성 (현재 구현 기준) |
| 2026-10-23 | ywkim | 문제점 수정 반영 |

---

## 1. 개요

사용자(`TB_USR_M`) 목록을 조회하고 사용자 정보(부서, 재직여부, 사용여부, 사용자유형, 잠김여부, 이메일, 전화번호, 사용기간)를 등록·수정·삭제한다. 선택한 사용자의 화면 권한(`TB_USR_MENU_R`)과 화면별 컴포넌트(버튼) 권한(`TB_USR_CMPN_R`)을 추가·삭제·변경하고, 다른 사용자의 권한을 복사한다. 사용자·화면 권한·컴포넌트 권한 변경분은 저장 버튼 한 번으로 한 트랜잭션에 저장한다. 협력업체(`P`) 사용자는 비밀번호를 초기화할 수 있다.

---

## 2. 화면 구성

```text
┌ [SYAU010] 사용자권한관리 ──────────────────────────────────────────────── [저장] ┐
├ 검색영역 (frmSrch) ──────────────────────────────────────────────────────────────┤
│ ID/사용자명 [ID][명]  사용자유형 [전체▼]  사용여부 [사용▼]                 [조회] │
├ 사용자 정보 - N건 ─[행추가][행삭제]┬ 상세정보 ───────────────── [비밀번호 초기화] ┤
│ NO│사용자ID│사용자명│부서명        │ frmUsrDtl (2열 고정)                          │
│ (grdUsrList, 15행 페이징,          │ 사용자명 | 사용자ID / 사원번호 | 부서          │
│  체크박스 선택)                    │ 재직여부 | 사용여부 / 사용자유형 | 잠김여부    │
│                                    │ 이메일 | 전화번호 / 입사일자 | 퇴사일자        │
│                                    │ 시작일자 | 종료일자                            │
│                                    ├ 권한 목록 (ID) - N건 ──────┬ 컴포넌트목록 ─────┤
│                                    │ 사용자[cdsCopyUsr]         │ [프로그램명] - N건│
│                                    │ [권한복사][화면추가][행삭제]│ NO│컴포넌트ID│... │
│                                    │ NO│메뉴ID│메뉴│프로그램    │ (grdUsrCmpnAuth   │
│                                    │ (grdUsrMenuAuthList, 15행)  │  List, 15행)      │
└────────────────────────────────────┴────────────────────────────┴───────────────────┘
  [화면추가] → [화면 추가] 팝업 (mdlScrnSrch, 760px)
```

- 왼쪽 사용자 정보 영역은 400px 고정, 오른쪽은 나머지 폭을 쓴다. 오른쪽 아래 권한 목록 | 컴포넌트목록은 나머지 폭 | 380px이다.
- 화면 폭 1500px 이하이면 권한 목록과 컴포넌트목록이 위아래로, 1100px 이하이면 왼쪽·오른쪽 영역도 위아래로 배치된다.
- 상세정보는 2열 고정(열 최소 260px)이고, 좁으면 가로 스크롤한다.
- 삭제(D) 표시된 사용자를 선택하면 상세정보·권한 목록 제목 옆에 "(삭제 예정)"을 표시하고, 상세정보·권한 목록·컴포넌트목록을 편집 불가(반투명, 클릭 불가)로 표시한다.

---

## 3. 검색조건

| Ref/ID | 항목 | 컴포넌트 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|---|
| `txtUsrId` | 사용자ID | Input | | | placeholder "사용자ID". Enter 시 조회 |
| `txtUsrNm` | 사용자명 | Input | | | placeholder "사용자명". Enter 시 조회 |
| `cboUsrClsCd` | 사용자유형 | Select | | 전체 | 공통코드 `SY/USR_CLS_CD` (E 직원, P 협력업체) |
| `cboUseYn` | 사용여부 | Select | | 사용(`Y`) | `USE_YN_OPTIONS`(사용 `Y`/미사용 `N`) + 전체 |

- 검색영역 폼 ID는 `frmSrch`이며, submit 시 조회한다. 사용자유형·사용여부 Select 폭은 140px이다.

---

## 4. 그리드 / 상세

### 4.1 사용자 정보 그리드 (`grdUsrList`)

| 컬럼 | Key | 폭 | 정렬 | 비고 |
|---|---|---|---|---|
| NO | `no` | 40px | 가운데 | 순번 (행추가·행삭제 시 재부여) |
| 사용자ID | `usrId` | 110px | | 신규 행에서 ID 미입력 시 "신규"(회색) 표시 |
| 사용자명 | `usrNm` | (나머지) | | |
| 부서명 | `deptNm` | 100px | | `deptCd`를 `DEPT_OPTIONS`(임시 목록)로 명칭 변환 |

- 15행 페이징, 조밀(dense) 표시, 체크박스 선택(`selectable`), 행 상태 표시 컬럼(`flag-column`, I/U/D).
- 건수는 `lblTotCnt`에 "N건"으로 표시한다.
- 조회 후 이전에 선택했던 사용자(없으면 첫 행)를 활성 행으로 선택한다.

### 4.2 상세정보 (`frmUsrDtl`)

활성 사용자 행의 값을 표시하고, 값을 바꾸면 해당 행에 바로 반영한다 (조회된 행은 `U` 표시, 값이 같으면 무시, 삭제(D) 행은 변경 불가). 선택 행이 없으면 `sy.user.auth.empty.select.user`를 표시한다.

| Ref/ID | 항목 | 컴포넌트 | 필수 | 기본값(신규) | 설명 |
|---|---|---|---|---|---|
| `txtDtlUsrNm` | 사용자명 | Input | Y | | maxlength 30 |
| `txtDtlUsrId` | 사용자ID | Input | Y | | maxlength 10. 신규(I) 행만 입력 가능, 그 외 읽기전용 |
| `txtDtlEmpNo` | 사원번호 | Input | | | 읽기전용 (`TB_USR_M.EMP_NO`) |
| `cboDtlDeptCd` | 부서 | Select | | 선택 | "선택" + `DEPT_OPTIONS`(임시 목록) |
| `cboDtlUsrStatCd` | 재직여부 | Select | | `T`(재직) | "선택" + 공통코드 `SY/USR_STAT_CD` (T 재직, L 휴직, R 퇴직) |
| `chkDtlUseYn` | 사용여부 | checkbox ("사용") | | 체크(`Y`) | 체크 시 `Y`, 해제 시 `N`. 삭제 행은 비활성 |
| `cboDtlUsrClsCd` | 사용자유형 | Select | | 선택 | "선택" + 공통코드 `SY/USR_CLS_CD` |
| `chkDtlUsrLockYn` | 잠김여부 | checkbox ("잠김") | | 해제(`N`) | 체크 시 `Y`, 해제 시 `N` (`USR_LOCK_YN`). 삭제 행은 비활성 |
| `txtDtlEmlAddr` | 이메일 | Input (email) | | | maxlength 100, placeholder "user@example.com" |
| `txtDtlMoblNo` | 전화번호 | Input (tel) | | | maxlength 13, placeholder "010-0000-0000" |
| `txtDtlEcomDt` | 입사일자 | Input | | | 읽기전용, `YYYY-MM-DD` 표시 (`ECOM_DT`) |
| `txtDtlResiDt` | 퇴사일자 | Input | | | 읽기전용, `YYYY-MM-DD` 표시 (`RESI_DT`) |
| `dtpDtlStDt` | 시작일자 | DatePicker | Y | 오늘 | 프리셋·지우기 없음. `YYYYMMDD`로 저장 |
| `dtpDtlEndDt` | 종료일자 | DatePicker | Y | `99991231` | 프리셋·지우기 없음. `YYYYMMDD`로 저장 |

- 필수 표시는 컴포넌트 `required`(입력 박스 왼쪽 빨간 줄)로만 한다. 저장 시 검증은 `validateUsrRows()`(6.5)가 한다 (`validateForm` 미사용).

### 4.3 권한 목록 그리드 (`grdUsrMenuAuthList`, `TB_USR_MENU_R`)

| 컬럼 | Key | 폭 | 정렬 | 비고 |
|---|---|---|---|---|
| NO | `no` | 40px | 가운데 | 순번 |
| 메뉴ID | `menuId` | 80px | | |
| 메뉴 | `menuPath` | (나머지) | | 상위 메뉴명을 " / "로 연결한 경로 (`getMenuPath`), 최대 220px 말줄임 + title |
| 프로그램 | `prgmNm` | 150px | | 프로그램명 (없으면 메뉴명, 그것도 없으면 프로그램ID), 최대 130px 말줄임, title "프로그램명 (프로그램ID)" |

- 15행 페이징, 조밀 표시, 체크박스 선택, 행 상태 표시 컬럼.
- 제목: "권한 목록 (사용자ID 또는 신규)" (`lblAuthUsrId`) + 건수 `lblAuthTotCnt`.
- 빈 목록 문구: 사용자 선택 시 `sy.auth.group.empty.screen.none`, 미선택 시 `sy.user.auth.empty.select.user`.
- 사용자를 선택하면 해당 사용자의 화면 권한을 처음 한 번만 조회하고(신규 사용자는 빈 목록), 이전에 보던 화면(없으면 첫 화면)을 활성 행으로 선택한다. 편집 상태는 사용자 행별로 유지된다.
- 권한 목록 헤더 오른쪽: 원본 사용자 코드검색 `cdsCopyUsr`(`ComSearchCode` type `USR`, 라벨 "사용자"), `btnAuthCopy`, `btnScrnAdd`, `btnAuthRowDel`.

### 4.4 컴포넌트목록 그리드 (`AuthCmpnGrid`, grid-id `grdUsrCmpnAuthList`)

| 컬럼 | Key | 폭 | 정렬 | 비고 |
|---|---|---|---|---|
| NO | `no` | 40px | 가운데 | 순번 |
| 컴포넌트ID | `cmpnId` | 110px | | `TB_PRGM_CMPN_M.CMPN_ID` (화면 버튼 id) |
| 컴포넌트명 | `cmpnNm` | (나머지) | | |
| 권한여부 | `authYn` | 72px | 가운데 | 체크박스 (Y/N). 헤더에 전체 선택 체크박스 |

- 15행 페이징, 조밀 표시, 행 상태 표시 컬럼 (체크박스 선택 없음).
- 제목: "컴포넌트목록 [프로그램명]" (`lblCmpnPrgmNm`, 활성 화면이 삭제(D) 표시면 "(삭제 예정)") + 건수 `lblCmpnTotCnt`.
- 활성 화면의 프로그램 컴포넌트 목록(`TB_PRGM_CMPN_M`)과 사용자의 컴포넌트 권한을 처음 한 번 조회해 합친다. 권한 행이 없는 컴포넌트는 `N`으로 표시한다.
- 권한여부 변경: 저장된 권한이면 `U`, 없던 권한이면 `I` 표시. 헤더 체크박스는 모든 행이 `Y`일 때 체크되고, 변경 시 현재 화면의 전체 행을 일괄 변경한다.
- 빈 목록 문구: 화면 선택 시 `sy.auth.group.empty.component.none`, 미선택 시 `sy.auth.group.empty.component.select.screen`.
- 사용자 또는 활성 화면이 삭제(D) 표시면 편집 불가(`locked`).

### 4.5 화면 추가 팝업 (`AuthScreenAddPopup`, `mdlScrnSrch`)

| 항목 | 내용 |
|---|---|
| 팝업 | `PopupWindow` id `mdlScrnSrch`, 제목 "화면 추가", 폭 760px |
| 검색 | `txtScrnKeyword` (placeholder "메뉴ID·메뉴·프로그램"), 입력 즉시 필터 (메뉴ID·메뉴경로·프로그램ID·프로그램명, 대소문자 무시) |
| 그리드 | `grdScrnList`, 8행 페이징, 조밀, 체크박스 선택. 빈 목록 `sy.auth.group.empty.addable.none` |
| 후보 | 프로그램이 연결된 메뉴 중 현재 사용자의 권한 목록에 없는 화면 ("메뉴ID/프로그램ID" 기준) |
| 버튼 | footer 슬롯으로 화면에서 전달: `btnScrnSrchAdd`(추가), `btnScrnSrchClose`(닫기) |

| 컬럼 | Key | 폭 | 비고 |
|---|---|---|---|
| 메뉴ID | `menuId` | 90px | |
| 메뉴 | `menuPath` | (나머지) | 메뉴 경로 |
| 프로그램ID | `prgmId` | 120px | |
| 프로그램명 | `prgmNm` | 140px | |

---

## 5. 버튼 / 이벤트

| Ref/ID | 버튼/이벤트 | 핸들러 | 권한 | 처리 |
|---|---|---|---|---|
| `btnSave` | 저장 | `onBtnSaveClick` | v-permission | 사용자·화면 권한·컴포넌트 권한 일괄 저장 (6.5) |
| `btnSrch` | 조회 | `onBtnSrchClick` | v-permission | 변경 있으면 확인 후 사용자 목록 조회 (6.1) |
| `txtUsrId`, `txtUsrNm` | Enter | `onBtnSrchClick` | | 조회 |
| `btnRowAdd` | 행추가 | `onBtnRowAddClick` | v-permission | 사용자 신규 행을 맨 위에 추가하고 활성화 (6.2) |
| `btnRowDel` | 행삭제 | `onBtnRowDelClick` | v-permission | 선택 사용자 삭제 표시 (6.2) |
| `btnPwdInit` | 비밀번호 초기화 | `onBtnPwdInitClick` | v-permission | 협력업체 사용자 비밀번호 초기화 (6.6). 조건 불충족 시 비활성 |
| `btnAuthCopy` | 권한복사 | `onBtnAuthCopyClick` | v-permission | `cdsCopyUsr` 사용자의 권한을 현재 사용자에 복사 (6.4) |
| `btnScrnAdd` | 화면추가 | `onBtnScrnAddClick` | v-permission | 화면 추가 팝업 열기 (6.3) |
| `btnAuthRowDel` | 행삭제 (권한 목록) | `onBtnAuthRowDelClick` | v-permission | 선택 화면 권한 삭제 표시 (6.3) |
| `btnScrnSrchAdd` | 추가 (팝업) | `onBtnScrnSrchAddClick` | v-permission | 선택 화면을 권한 목록에 추가 (6.3) |
| `btnScrnSrchClose` | 닫기 (팝업) | `onBtnScrnSrchCloseClick` | v-permission | 팝업 닫기 |
| `grdUsrList` | 행 클릭 | `onGrdUsrListRowClick` | | 다른 행이면 사용자 활성화 → 상세정보·권한 목록 표시 |
| `grdUsrMenuAuthList` | 행 클릭 | `onGrdUsrMenuAuthListRowClick` | | 다른 행이면 화면 활성화 → 컴포넌트목록 표시 |
| `chkDtlUseYn` | change | `onChkDtlUseYnChange` | | 사용여부 Y/N 반영 |
| `chkDtlUsrLockYn` | change | `onChkDtlUsrLockYnChange` | | 잠김여부 Y/N 반영 |
| `grdUsrCmpnAuthList` | 권한여부 체크 / 헤더 전체 체크 | `setCmpnAuthYn` / `setAllCmpnAuthYn` (`change` / `change-all` 이벤트) | | 컴포넌트 권한여부 변경 |

- 버튼 컴포넌트 10개(`btnSave`~`btnScrnSrchClose`)는 `TB_PRGM_CMPN_M` 시드(`schema/data/TB_PRGM_CMPN_M-data.sql`)에 등록되어 있다.

---

## 6. 처리 로직

### 6.0 초기화 (`onMounted`)
1. `fetchPageData()` : 아래를 병렬 조회한다.
   - `fetchReferenceData()` : 메뉴 전체(`getMenuListApi`)와 프로그램 전체(`getPrgmListApi({})`) → 메뉴 경로·프로그램명 변환용 Map
   - 공통코드 `SY/USR_CLS_CD`(사용자유형), `SY/USR_STAT_CD`(재직여부)
   - 검색조건 사용자유형은 앞에 "전체", 상세 Select는 앞에 "선택"을 붙인다.
2. `initPageUi()` : 검색조건 초기화(`resetSearchForm`, 사용여부 `Y`), 그리드 초기화(`clearGrids`) 후 최초 조회(`searchUsrList`)한다.

### 6.1 조회 (`onBtnSrchClick` → `searchUsrList`)
1. 저장하지 않은 변경(사용자 행 또는 화면·컴포넌트 권한)이 있으면 `cm.confirm.discard.and.search`로 확인한다 (`confirmDiscardChanges`).
2. 현재 활성 사용자ID와 활성 화면 키("메뉴ID/프로그램ID")를 기억한 뒤 `getUsrMngListApi(검색조건)`를 호출한다.
3. 그리드·권한 편집 상태·복사 원본 사용자를 모두 비우고, 결과에 행 ID·순번을 붙인다.
4. 결과가 없으면 `cm.info.no.data`를 띄운다.
5. 이전 활성 사용자(없으면 첫 행)를 활성화하고, 권한 목록에서 이전에 보던 화면(없으면 첫 화면)을 선택한다.

### 6.2 사용자 행추가 / 행삭제
- 행추가(`addUsrRow`): 맨 위에 `I` 행을 추가한다. 기본값은 재직여부 `T`, 잠김여부 `N`, 시작일자 오늘, 종료일자 `99991231`, 사용여부 `Y`, 나머지 빈 값. 추가한 행을 활성화한다 (권한 목록은 빈 목록).
- 행삭제: 체크된 행이 없으면 `sy.user.auth.manage.delete.select`, 있으면 `sy.user.auth.manage.delete.confirm`으로 확인한다. 신규(`I`) 행은 바로 제거하고, 나머지는 `D`로 표시한다(저장 시 삭제). 제거된 행의 권한 편집 상태도 지운다(`pruneAuthStates`). 활성 행이 제거되면 첫 행을 활성화한다.

### 6.3 화면 권한 추가 / 삭제
- 화면추가·권한 행삭제·권한복사는 먼저 `alertIfUsrLocked()`로 확인한다: 사용자 미선택이면 `sy.user.auth.manage.select.required`, 삭제(D) 표시 사용자면 `sy.user.auth.manage.deleted.readonly`를 띄우고 중단한다.
- 화면추가: 검색어·선택을 비우고 팝업을 연다. 팝업 추가 버튼에서 선택이 없으면 `sy.auth.group.screen.add.select`를 띄운다. 선택한 화면을 `I` 행으로 권한 목록 끝에 추가하고(이미 있는 화면 제외), 팝업을 닫은 뒤 처음 추가한 화면을 활성화한다. 추가한 화면의 컴포넌트 권한은 모두 `N`으로 표시된다.
- 권한 행삭제: 선택이 없으면 `sy.auth.group.screen.delete.select`. 신규(`I`) 행은 제거, 나머지는 `D` 표시. 활성 화면이 제거되면 첫 화면을 활성화한다.

### 6.4 권한복사 (`onBtnAuthCopyClick` → `copyAuthFrom`)
1. `cdsCopyUsr`의 사용자ID가 없으면 `sy.user.auth.manage.copy.select.source`, 현재 사용자와 같으면 `sy.user.auth.manage.copy.same.user`를 띄운다.
2. `sy.user.auth.manage.copy.cmpn.confirm`(원본 사용자ID)으로 확인한다.
3. 원본 화면 권한: 원본 사용자가 그리드에 있고 편집 상태가 있으면 그 값(`D` 제외)을, 아니면 `getUsrMenuAuthListApi(원본)`로 조회한다. 0건이면 `sy.user.auth.manage.copy.source.empty`를 띄우고 끝낸다.
4. 원본 컴포넌트 권한: `getUsrCmpnAuthListApi(원본)`(prgmId 생략, 전체) 결과에 화면에서 편집 중인 원본 값을 덮어쓴다.
5. 현재 사용자의 권한 목록에 없는 화면은 `I`로 추가하고, 삭제(D) 표시된 화면은 되살린다(이미 있는 화면은 유지).
6. 복사한 화면의 프로그램별 컴포넌트 목록을 불러와 원본에 값이 있는 컴포넌트의 권한여부를 원본 값으로 바꾼다.
7. 활성 화면이 없으면 첫 화면을 활성화한다. (저장은 저장 버튼으로 한다.)

### 6.5 저장 (`onBtnSaveClick`, `useGridSave.saveChanges`)
1. 변경이 없으면 `cm.info.no.changes`를 띄운다.
2. `validateUsrRows()` : 신규·수정(`I`/`U`) 행을 순서대로 검증하고, 첫 오류 행을 활성화한 뒤 경고로 띄운다. 행 라벨은 `sy.user.auth.manage.row.label`(NO)이다.
   1. 사용자ID 없음 → `sy.user.auth.manage.validate.id.required`
   2. 사용자명 없음 → `sy.user.auth.manage.validate.name.required`
   3. 화면 내 사용자ID 중복(`D` 행 제외) → `sy.user.auth.manage.validate.id.duplicate`
   4. 시작·종료일자 없음 → `sy.user.auth.manage.validate.period.required`
   5. 일자 형식 오류(`isValidYmd`) → `sy.user.auth.manage.validate.period.invalid`
   6. 시작일자 > 종료일자 → `sy.user.auth.manage.validate.period.reversed`
   7. 이메일 형식 오류 (`^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$`, 입력 시만) → `sy.user.auth.manage.validate.email.format`
   8. 전화번호 형식 오류 (`^0\d{1,2}-?\d{3,4}-?\d{4}$`, 입력 시만) → `sy.user.auth.manage.validate.mobile.format`
   9. 신규 행의 사용자ID가 이미 등록됨(`existsUsrIdApi`) → `sy.user.auth.manage.validate.id.exists`
3. `cm.confirm.save`로 확인한다.
4. `saveUsrAuthApi`를 호출한다. 요청은 다음과 같다.
   - `insertList`/`updateList`/`deleteList` : 사용자 행을 상태별로 나눈 목록 (`splitRowsByFlag`, 사용자ID·사용자명·이메일·전화번호 trim)
   - `menuAuthSaveList` : 변경된 사용자별 `{ usrId, insertList, deleteList }` (삭제(D) 사용자 제외)
   - `cmpnAuthSaveList` : 변경된 사용자·프로그램별 `{ usrId, prgmId, insertList, updateList }` (삭제 사용자, 또는 해당 프로그램의 화면 권한이 모두 삭제된 경우 제외)
5. 실패(`success=false`)면 서버 메시지(없으면 `cm.error.save.failed`)를 띄운다. 성공이면 `cm.info.saved`를 토스트로 띄우고 재조회(`searchUsrList`)한다.
- 삭제된 사용자의 권한그룹·즐겨찾기·화면·컴포넌트 권한, 그리고 삭제된 화면의 프로그램에 다른 메뉴 권한이 남지 않은 경우의 컴포넌트 권한은 backend에서 함께 삭제한다 (API 주석·삭제 확인 문구 기준).

### 6.6 비밀번호 초기화 (`onBtnPwdInitClick`)
- 활성화 조건(`isPwdInitEnabled`): 활성 사용자가 있고, 신규(`I`)·삭제(`D`) 행이 아니며, 사용자유형이 `P`(협력업체, `PWD_INIT_USR_CLS_CD`)일 때. 조건이 아니면 버튼이 비활성이고, 호출되더라도 `sy.user.auth.manage.password.init.not.allowed`를 띄운다.
- `sy.user.auth.manage.password.init.confirm`(사용자ID)으로 확인 후 `initUsrPwdApi(usrId)`를 호출한다. 실패면 서버 메시지(없으면 `cm.error.save.failed`), 성공이면 `sy.user.auth.manage.password.init.done`을 토스트로 띄운다.

---

## 7. 사용 API

| 함수 | Method / URL | 요청 | 응답 | 비고 |
|---|---|---|---|---|
| `getUsrMngListApi` | `GET /api/v1/user-auths` | `usrId`, `usrNm`, `usrClsCd`, `useYn` | `UsrMng[]` | 사용자ID 순 |
| `existsUsrIdApi` | `GET /api/v1/user-auths/{usrId}/exists` | `usrId` | `boolean` | 신규 사용자ID 중복 확인 |
| `getUsrMenuAuthListApi` | `GET /api/v1/user-auths/{usrId}/menu-auths` | `usrId` | `UsrMenuAuth[]` (`usrId`, `menuId`, `prgmId`) | 사용자 처음 선택 시, 권한복사 원본 조회 |
| `getUsrCmpnAuthListApi` | `GET /api/v1/user-auths/{usrId}/component-auths?prgmId=` | `usrId`, `prgmId`(생략 시 전체) | `UsrCmpnAuth[]` (`usrId`, `prgmId`, `cmpnId`, `authYn`) | |
| `saveUsrAuthApi` | `POST /api/v1/user-auths` | `UsrAuthSaveRequest { insertList, updateList, deleteList, menuAuthSaveList, cmpnAuthSaveList }` | `number` (저장 건수) | 한 트랜잭션. 업무 오류는 `success=false` + 서버 메시지 (`requestApi`) |
| `initUsrPwdApi` | `POST /api/v1/user-auths/{usrId}/password-resets` | `usrId` | `null` | 미등록 사용자면 `success=false` + 서버 메시지 |
| `getMenuListApi` | `GET /api/v1/menus` (`api/sy/menu.ts`) | | `Menu[]` | 메뉴 경로·화면 후보 |
| `getPrgmListApi` | `GET /api/v1/programs` (`api/sy/program.ts`) | `{}` | `Prgm[]` | 프로그램명 변환 |
| `getPrgmCmpnListApi` | `GET /api/v1/programs/{prgmId}/components` (`api/sy/program.ts`) | `prgmId` | `PrgmCmpn[]` | 컴포넌트목록 (`TB_PRGM_CMPN_M`) |
| `getCommCdOptions` | `GET /api/v1/common-codes/{업무}/{분류}/details?useYn=Y` (`api/common/commonCode.ts`) | 업무약어, 코드분류 | `CodeOption[]` | `SY/USR_CLS_CD`, `SY/USR_STAT_CD` |
| `searchCodeApi('USR')` | `GET /api/v1/users` (`getAllUsersApi`, `api/auth.ts`) | | 사용자 프로필 목록 | `cdsCopyUsr` 코드검색 (`ComSearchCode`) |

- `UsrMng` 필드: `usrId`, `usrNm`, `empNo`, `deptCd`, `emlAddr`, `moblNo`, `usrStatCd`, `usrLockYn`, `usrClsCd`, `ecomDt`, `resiDt`, `stDt`, `endDt`, `useYn`.

---

## 8. 메시지

| 키 | 문구 (ko) | 사용 시점 |
|---|---|---|
| `cm.info.no.data` | 조회된 내역이 없습니다. | 조회 결과 없음 |
| `cm.confirm.discard.and.search` | 저장하지 않은 변경 내용이 있습니다.\n조회하시겠습니까? | 변경 있는 상태에서 조회 |
| `cm.info.no.changes` | 저장할 변경 내용이 없습니다. | 변경 없이 저장 |
| `cm.confirm.save` | 저장하시겠습니까? | 저장 확인 |
| `cm.info.saved` | 저장되었습니다. | 저장 성공 (토스트) |
| `cm.error.save.failed` | 저장에 실패했습니다. | 저장·비밀번호 초기화 실패 (서버 메시지 없을 때) |
| `sy.user.auth.empty.select.user` | 왼쪽에서 사용자를 선택하세요. | 상세정보·권한 목록 (사용자 미선택) |
| `sy.user.auth.manage.row.label` | 사용자 {0}행 | 검증 메시지의 행 라벨 |
| `sy.user.auth.manage.validate.id.required` | {0}: 사용자ID는 필수 입력입니다. | 저장 검증 |
| `sy.user.auth.manage.validate.name.required` | {0}: 사용자명은 필수 입력입니다. | 저장 검증 |
| `sy.user.auth.manage.validate.id.duplicate` | {0}: 사용자ID [{1}]가 중복되었습니다. | 저장 검증 (화면 내 중복) |
| `sy.user.auth.manage.validate.period.required` | {0}: 시작일자와 종료일자는 필수 입력입니다. | 저장 검증 |
| `sy.user.auth.manage.validate.period.invalid` | {0}: 시작일자 또는 종료일자 형식이 올바르지 않습니다. | 저장 검증 |
| `sy.user.auth.manage.validate.period.reversed` | {0}: 시작일자가 종료일자보다 늦습니다. | 저장 검증 |
| `sy.user.auth.manage.validate.email.format` | {0}: 이메일 형식이 올바르지 않습니다. | 저장 검증 |
| `sy.user.auth.manage.validate.mobile.format` | {0}: 전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678) | 저장 검증 |
| `sy.user.auth.manage.validate.id.exists` | 사용자ID [{0}]는 이미 등록되어 있습니다. | 저장 검증 (기존 등록) |
| `sy.user.auth.manage.delete.select` | 삭제할 사용자를 선택하세요. | 사용자 행삭제 (선택 없음) |
| `sy.user.auth.manage.delete.confirm` | 선택한 사용자와 권한그룹·즐겨찾기·화면·컴포넌트 권한이 함께 삭제됩니다.\n삭제하시겠습니까? | 사용자 행삭제 확인 |
| `sy.user.auth.manage.select.required` | 사용자를 먼저 선택하세요. | 권한 편집 시 사용자 미선택 |
| `sy.user.auth.manage.deleted.readonly` | 삭제 표시된 사용자의 권한은 변경할 수 없습니다. | 삭제 표시 사용자 권한 편집 |
| `sy.user.auth.manage.copy.select.source` | 권한을 복사할 원본 사용자를 선택하세요. | 권한복사 원본 없음 |
| `sy.user.auth.manage.copy.same.user` | 현재 사용자와 다른 원본 사용자를 선택하세요. | 권한복사 원본 = 현재 사용자 |
| `sy.user.auth.manage.copy.cmpn.confirm` | [{0}] 사용자의 화면·컴포넌트 권한을 복사합니다.\n이미 있는 화면의 컴포넌트 권한은 원본 값으로 덮어씁니다. | 권한복사 확인 |
| `sy.user.auth.manage.copy.source.empty` | [{0}] 사용자에게 복사할 화면 권한이 없습니다. | 원본 화면 권한 0건 |
| `sy.user.auth.manage.password.init.not.allowed` | 비밀번호 초기화는 저장된 협력업체 사용자만 할 수 있습니다. | 초기화 대상 아님 |
| `sy.user.auth.manage.password.init.confirm` | [{0}] 사용자의 비밀번호를 초기 비밀번호(coder1234)로 초기화하시겠습니까? | 초기화 확인 |
| `sy.user.auth.manage.password.init.done` | [{0}] 사용자의 비밀번호가 초기화되었습니다. | 초기화 성공 (토스트) |
| `sy.auth.group.screen.add.select` | 추가할 화면을 선택하세요. | 팝업 추가 (선택 없음) |
| `sy.auth.group.screen.delete.select` | 삭제할 화면을 선택하세요. | 권한 행삭제 (선택 없음) |
| `sy.auth.group.empty.screen.none` | 등록된 화면 권한이 없습니다. | 권한 목록 빈 목록 |
| `sy.auth.group.empty.component.none` | 프로그램에 등록된 컴포넌트가 없습니다. | 컴포넌트목록 빈 목록 |
| `sy.auth.group.empty.component.select.screen` | 권한목록에서 화면을 선택하세요. | 컴포넌트목록 (화면 미선택) |
| `sy.auth.group.empty.addable.none` | 추가할 수 있는 화면이 없습니다. | 화면 추가 팝업 빈 목록 |

---

## 9. 비고 / 미결사항

- 부서는 부서 마스터가 없어 `DEPT_OPTIONS`(임시 목록 6건, `api/sy/usrAuthMng.ts`)로 선택·명칭 변환한다 (PRD Open Issue 3).
- 행삭제 확인 규칙: 마스터 행(사용자) 행삭제만 확인 메시지를 띄우고, 권한 목록 행삭제는 확인 없이 D 표시만 한다 (SYAU020·SYAU030 공통 규칙).
- 권한 목록·화면 추가·컴포넌트목록 관련 안내는 SYAU020 그룹권한관리와 공유하는 `sy.auth.group.*` 키를 쓴다 (하위 컴포넌트·컴포저블 공용).
- 권한복사 원본 코드검색(`cdsCopyUsr`, type `USR`)은 사용자 그리드 API가 아닌 `GET /api/v1/users`(사용자 프로필) 목록을 쓴다.
- 초기 비밀번호 값(`coder1234`)이 메시지 문구에 직접 들어 있다. backend 초기 비밀번호를 바꾸면 메시지도 함께 고쳐야 한다.
