# MSBS010 공통코드관리 프로그램 사양서

| 항목 | 내용 |
|---|---|
| 프로그램 ID | MSBS010 |
| 프로그램명 | 공통코드관리 |
| 메뉴 경로 | 기준정보 > 기초정보 > 공통코드관리 (메뉴ID `MS010100`) |
| 라우트 | `/ms/msbs010` (name `MSBS010`) |
| 화면 파일 | `workspace/coder-frontend/src/views/master/basic/CommonCodeManageView.vue` |
| API 파일 | `workspace/coder-frontend/src/api/sy/commonCode.ts` (화면용), `src/api/common/commonCode.ts` (콤보용 공통코드 조회) |
| API 상태 | 실제 API 연동 (coder-backend `CommonCodeController`, `/api/v1/common-codes`) |
| 대상 데이터 | `TB_COMM_CD_M` 공통코드기본(분류), `TB_COMM_CD_D` 공통코드상세(코드) |
| 작성자 | ywkim |
| 작성일 | 2026-10-25 |

### 변경 이력

| 일자 | 작성자 | 내용 |
|---|---|---|
| 2026-10-25 | ywkim | 최초 작성 (현재 구현 기준) |

---

## 1. 개요

화면 콤보·코드 변환에 쓰는 공통코드를 관리한다. 위쪽 그리드에서 공통코드 분류(`TB_COMM_CD_M`)를 조회·등록·수정·삭제하고, 아래쪽 그리드에서 선택한 분류의 코드(`TB_COMM_CD_D`)를 등록·수정한다. 분류와 코드의 변경분은 저장 버튼 한 번으로 한 트랜잭션에 저장한다.

- 코드(상세)는 삭제하지 않는다. 쓰지 않는 코드는 사용여부를 미사용(`N`)으로 바꾼다.
- 코드가 등록된 분류는 삭제할 수 없다.
- 다른 화면은 공통코드를 `getCommCdOptions(업무모듈, 분류코드)`(`api/common/commonCode.ts`)로 조회해 콤보 옵션으로 쓴다. 이때는 사용여부 `Y`인 코드만 정렬순서대로 받는다.

---

## 2. 화면 구성

```text
┌ [MSBS010] 공통코드관리 ─────────────────────────────── [신규][저장][삭제] ┐
├ 검색영역 ─────────────────────────────────────────────────────────────────┤
│ 업무모듈 [전체▼]  분류코드 [      ]  분류명 [      ]               [조회] │
├ 분류 목록 - N건 ──────────────────────────────────────────────────────────┤
│ ☐│상태│NO│업무모듈│분류코드│분류명│비고│속성1..속성10│사용자관리│사용│정렬  │
│ (grdCommCtgList, 10행 페이징, 셀 편집)                                    │
├ 코드 목록 (분류코드 분류명) (삭제 예정) - N건 ──────── [행추가][행삭제] ─┤
│ ☐│상태│NO│상세코드│상세명│비고│{분류의 속성명1..10}│사용│정렬            │
│ (grdCommCdList, 10행 페이징, 셀 편집)                                     │
└───────────────────────────────────────────────────────────────────────────┘
```

- 두 그리드 모두 행 선택 체크박스(`selectable`)와 행 상태 컬럼(`flag-column`, I/U/D)을 표시한다.
- 분류 행을 클릭하면 아래 코드 목록이 그 분류의 코드로 바뀐다. 삭제 표시(D)된 분류를 고르면 코드 목록이 흐리게 잠긴다(`is-locked`, 편집 불가).

---

## 3. 검색조건

| Ref/ID | 항목 | 컴포넌트 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|---|
| `cboWrkMdulCd` | 업무모듈 | Select | | 전체 | 공통코드 `SY/WRK_MDUL_CD` |
| `txtCommCtgCd` | 분류코드 | Input | | | 부분 일치, 대소문자 구분 없음. Enter 시 조회 |
| `txtCommCtgNm` | 분류명 | Input | | | 부분 일치(대소문자 구분). Enter 시 조회 |

- 검색영역 폼 ID는 `frmSrch`이며, submit 시 조회한다.
- 조회 결과는 업무모듈 → 정렬순서(빈 값은 마지막) → 분류코드 순이다.

---

## 4. 그리드 / 상세

### 4.1 분류 목록 그리드 (`grdCommCtgList`, `TB_COMM_CD_M`)

| 컬럼 | Key | 폭 | 편집 | 필수 | 비고 |
|---|---|---|---|---|---|
| NO | `no` | 44px | | | 순번 |
| 업무모듈 | `wrkMdulCd` | 120px | 신규 행만 (select) | Y | 공통코드 `SY/WRK_MDUL_CD`. 저장된 행은 명칭 표시 |
| 분류코드 | `commCtgCd` | 130px | 신규 행만 | Y | 최대 30자. 입력 시 대문자로 변환. 영문 대문자·숫자·`_`만 허용 |
| 분류명 | `commCtgNm` | 160px | O | Y | 최대 50자 |
| 비고 | `rmk` | 180px | O | | 최대 200자 |
| 속성1~속성10 | `commPropNm1`~`commPropNm10` | 각 110px | O | | 최대 40자. 코드 목록의 속성 컬럼 제목으로 쓰인다 |
| 사용자관리여부 | `usrMngeYn` | 96px | 체크박스 | | `Y`/`N` |
| 사용여부 | `useYn` | 68px | 체크박스 | | `Y`/`N` |
| 정렬순서 | `alinSeq` | 76px | 숫자 입력 | | 0 이상, 빈 값이면 `null` |

- 10행 페이징. 건수는 `lblCtgTotCnt`에 표시한다 (삭제 표시 행 포함).
- 신규 행 기본값: 업무모듈은 검색조건의 업무모듈(전체면 첫 번째 업무모듈), 사용여부 `Y`, 사용자관리여부 `Y`, 나머지 빈 값.
- 필수 셀은 `cell-input is-required`로 왼쪽 빨간 줄을 표시한다. 분류코드는 신규 행에서만 필수 표시한다.

### 4.2 코드 목록 그리드 (`grdCommCdList`, `TB_COMM_CD_D`)

| 컬럼 | Key | 폭 | 편집 | 필수 | 비고 |
|---|---|---|---|---|---|
| NO | `no` | 44px | | | 순번 |
| 상세코드 | `commCtgDtlCd` | 110px | 신규 행만 | Y | 최대 30자 (`MAX_COMM_CD_DNUM`) |
| 상세명 | `commCtgDtlNm` | 160px | O | Y | 최대 80자 |
| 비고 | `rmk` | 180px | O | | 최대 200자 |
| {속성명1}~{속성명10} | `commPropCont1`~`commPropCont10` | 각 110px | O | | 최대 100자. 제목은 선택한 분류의 속성명, 없으면 `속성n` |
| 사용여부 | `useYn` | 68px | 체크박스 | | `Y`/`N` |
| 정렬순서 | `alinSeq` | 76px | 숫자 입력 | | 0 이상, 빈 값이면 `null` |

- 10행 페이징. 그리드 제목에 선택 분류(`lblDtlCtg`, 신규 분류는 "신규")와 건수(`lblDtlTotCnt`)를 표시하고, 삭제 표시된 분류면 "(삭제 예정)"을 붙인다.
- 빈 목록 문구: 분류를 선택하지 않았으면 `sy.common.code.empty.category.select`, 선택했는데 코드가 없으면 `sy.common.code.empty.code.none`.
- 신규 행 기본값: 사용여부 `Y`, 나머지 빈 값. 업무모듈·분류코드는 저장할 때 소속 분류 값으로 채운다.
- 분류별 편집 상태는 화면에 보관된다. 다른 분류를 클릭했다 돌아와도 저장 전 편집 내용이 유지되며, 분류의 코드는 처음 선택할 때 한 번만 조회한다.

---

## 5. 버튼 / 이벤트

| Ref/ID | 버튼/이벤트 | 핸들러 | 권한 | 처리 |
|---|---|---|---|---|
| `btnSrch` | 조회 | `onBtnSrchClick` | v-permission | 변경이 있으면 버리고 조회할지 확인 후 분류 목록 조회 (6.1) |
| `btnNew` | 신규 | `onBtnNewClick` | v-permission | 분류 목록 맨 위에 신규 분류 행 추가 후 선택 (6.2) |
| `btnSave` | 저장 | `onBtnSaveClick` | v-permission | 분류·코드 변경분 검증 후 일괄 저장 (6.5) |
| `btnDel` | 삭제 | `onBtnDelClick` | v-permission | 선택한 분류 삭제 표시 (6.3) |
| `btnDtlRowAdd` | 코드 행추가 | `onBtnDtlRowAddClick` | v-permission | 선택한 분류에 신규 코드 행 추가 (6.4) |
| `btnDtlRowDel` | 코드 행삭제 | `onBtnDtlRowDelClick` | v-permission | 신규 코드 행만 제거 (6.4) |
| `grdCommCtgList` | 행 클릭 | `onGrdCommCtgListRowClick` | | 해당 분류의 코드 목록 표시 |
| `txtCommCtgCd`, `txtCommCtgNm` | Enter | `onBtnSrchClick` | | 조회 |

- 버튼 6개는 모두 `TB_PRGM_CMPN_M`에 컴포넌트로 등록되어 있다.

---

## 6. 처리 로직

### 6.0 초기화 (`onMounted`)
1. `fetchPageData()` : 공통코드 `SY/WRK_MDUL_CD`(업무모듈)를 조회한다. 검색조건 옵션은 앞에 "전체"를 붙인다.
2. `initPageUi()` : 기본 조건(전체)으로 최초 조회한다.

### 6.1 조회 (`searchCtgList`)
1. `getCommCdListApi(검색조건)`로 분류 목록을 조회하고 순번을 매긴다.
2. 분류별 코드 편집 상태와 선택 행을 비운다.
3. 첫 번째 분류(저장 후 재조회면 저장 전에 보던 분류)를 선택하고 코드 목록을 조회한다.
4. 결과가 없으면 `cm.info.no.data`를 띄운다.

### 6.2 분류 신규
- 신규 분류 행(`I`)을 맨 위에 추가하고 바로 선택한다. 신규 분류의 코드 목록은 조회하지 않고 빈 목록으로 시작한다.

### 6.3 분류 삭제 (`onBtnDelClick`)
1. 선택한 분류가 없으면 `sy.common.code.delete.select`.
2. 저장된 분류마다 코드 목록을 확인해, 저장된 코드가 하나라도 있으면 `sy.common.code.delete.has.code`(분류코드)를 띄우고 중단한다.
3. `sy.common.code.delete.confirm` 확인 후 삭제 표시한다 (신규 분류는 바로 제거). 마스터 행 삭제라 확인 창을 띄운다 (PRD FR-04 행삭제 확인 규칙).
4. 제거된 분류의 코드 편집 상태를 지우고, 보던 분류가 사라졌으면 첫 번째 분류를 선택한다.

### 6.4 코드 행추가 / 행삭제
- 행추가: 분류를 선택하지 않았으면 `sy.common.code.select.required`, 삭제 표시된 분류면 `sy.common.code.deleted.readonly`를 띄운다. 통과하면 코드 목록 끝에 신규 코드 행을 추가한다.
- 행삭제: 선택한 코드가 없으면 `sy.common.code.dtl.delete.select`. 저장된 코드가 섞여 있으면 `sy.common.code.dtl.delete.saved`를 띄우고 중단한다. 신규 코드 행만 바로 제거한다 (확인 창 없음).

### 6.5 저장 (`onBtnSaveClick` → `useGridSave.saveChanges`)
1. 분류·코드 어디에도 변경이 없으면 `cm.info.no.changes`.
2. 분류 검증 (`validateCtgRows`, 신규·수정 행 대상, 행 라벨 `sy.common.code.row.label`):
   - 업무모듈 없음 → `sy.common.code.validate.module.required`
   - 분류코드 없음 → `sy.common.code.validate.category.required`
   - 형식 오류(영문 대문자·숫자·`_` 외) → `sy.common.code.validate.category.format`
   - 같은 업무모듈 안에서 분류코드 중복 → `sy.common.code.validate.category.duplicate`
   - 분류명 없음 → `sy.common.code.validate.category.name.required`
3. 코드 검증 (`validateDtlRows`, 변경이 있는 분류의 신규·수정 행 대상, 행 라벨 `sy.common.code.dtl.row.label`):
   - 상세코드 없음 → `sy.common.code.validate.code.required`
   - 같은 분류 안에서 상세코드 중복 → `sy.common.code.validate.code.duplicate`
   - 상세명 없음 → `sy.common.code.validate.code.name.required`
4. `cm.confirm.save` 확인 후 `saveCommCdApi`를 호출한다. 요청은 분류 `insertList`/`updateList`/`deleteList`와 분류별 코드 변경분 `dtlSaveList`(`insertList`/`updateList`)이다. 삭제 표시된 분류의 코드는 보내지 않는다. 코드·명칭은 앞뒤 공백을 지워 보낸다.
5. 성공하면 `cm.info.saved`를 띄우고, 저장 전에 보던 분류를 다시 선택해 재조회한다. 실패하면 서버 메시지(없으면 `cm.error.save.failed`)를 띄운다.

backend 처리 순서 (`CommonCodeServiceImpl.saveCommonCode`, 한 트랜잭션):
1. 분류 삭제: 코드가 있는 분류면 `sy.common.code.delete.has.code`로 거부
2. 분류 수정: 대상이 없으면 `cm.error.entity.not.found`
3. 분류 등록: 이미 있으면 `sy.common.code.save.category.exists`
4. 코드 수정 → 등록: 업무모듈·분류코드는 소속 분류 값으로 채운다. 이미 있는 상세코드면 `sy.common.code.save.code.exists`

---

## 7. 사용 API

| 함수 | Method / URL | 요청 | 응답 | 비고 |
|---|---|---|---|---|
| `getCommCdListApi` | `GET /api/v1/common-codes` | `wrkMdulCd`, `commCtgCd`, `commCtgNm` | `CommCd[]` | `@PrgmAuth("MSBS010")` |
| `getCommCdDtlListApi` | `GET /api/v1/common-codes/{wrkMdulCd}/{commCtgCd}/details` | 경로: 업무모듈, 분류코드 | `CommCdDtl[]` | 사용여부 무관 전체, 정렬순서 순 |
| `saveCommCdApi` | `POST /api/v1/common-codes` | `CommCdSaveRequest` (분류 I/U/D, 분류별 코드 I/U) | 처리 건수 | `@PrgmAuth("MSBS010")`. 업무 오류면 `success=false`와 서버 메시지 |
| `getCommCdOptions` | `GET /api/v1/common-codes/{wrkMdulCd}/{commCtgCd}/details?useYn=Y` | 업무모듈, 분류코드 | `CodeOption[]` | 콤보용 (`api/common/commonCode.ts`). 이 화면은 `SY/WRK_MDUL_CD`에 사용 |

---

## 8. 메시지

| 키 | 문구 (ko) | 사용 시점 |
|---|---|---|
| `cm.info.no.data` | 조회된 내역이 없습니다. | 분류 조회 결과 없음 |
| `cm.confirm.discard.and.search` | 저장하지 않은 변경 내용이 있습니다.\n조회하시겠습니까? | 변경이 있는 상태에서 조회 |
| `sy.common.code.delete.select` | 삭제할 분류를 선택하세요. | 분류 삭제 - 선택 없음 |
| `sy.common.code.delete.has.code` | [{0}] 분류에 등록된 코드가 있어 삭제할 수 없습니다.\n사용여부를 미사용으로 변경하세요. | 분류 삭제 - 코드 있음 (화면·backend) |
| `sy.common.code.delete.confirm` | 선택한 분류를 삭제하시겠습니까? | 분류 삭제 확인 |
| `sy.common.code.select.required` | 분류를 먼저 선택하세요. | 코드 행추가 - 분류 미선택 |
| `sy.common.code.deleted.readonly` | 삭제 표시된 분류의 코드는 변경할 수 없습니다. | 코드 행추가 - 삭제 표시 분류 |
| `sy.common.code.dtl.delete.select` | 삭제할 코드를 선택하세요. | 코드 행삭제 - 선택 없음 |
| `sy.common.code.dtl.delete.saved` | 저장된 코드는 삭제할 수 없습니다.\n사용여부를 미사용으로 변경하세요. | 코드 행삭제 - 저장된 코드 포함 |
| `sy.common.code.empty.category.select` | 분류목록에서 분류를 선택하세요. | 코드 목록 빈 문구 (분류 미선택) |
| `sy.common.code.empty.code.none` | 등록된 코드가 없습니다. | 코드 목록 빈 문구 (코드 없음) |
| `sy.common.code.row.label` | 분류 {0}행 | 분류 검증 메시지의 행 라벨 |
| `sy.common.code.dtl.row.label` | [{0}] 코드 {1}행 | 코드 검증 메시지의 행 라벨 |
| `sy.common.code.validate.module.required` | {0}: 업무모듈은 필수 입력입니다. | 저장 검증 |
| `sy.common.code.validate.category.required` | {0}: 분류코드는 필수 입력입니다. | 저장 검증 |
| `sy.common.code.validate.category.format` | {0}: 분류코드는 영문 대문자, 숫자, _ 만 입력할 수 있습니다. | 저장 검증 |
| `sy.common.code.validate.category.duplicate` | {0}: 분류코드 [{1}]가 중복되었습니다. | 저장 검증 |
| `sy.common.code.validate.category.name.required` | {0}: 분류명은 필수 입력입니다. | 저장 검증 |
| `sy.common.code.validate.code.required` | {0}: 상세코드는 필수 입력입니다. | 저장 검증 |
| `sy.common.code.validate.code.duplicate` | {0}: 상세코드 [{1}]가 중복되었습니다. | 저장 검증 |
| `sy.common.code.validate.code.name.required` | {0}: 상세명은 필수 입력입니다. | 저장 검증 |
| `cm.info.no.changes` | 저장할 변경 내용이 없습니다. | 저장 - 변경 없음 |
| `cm.confirm.save` | 저장하시겠습니까? | 저장 확인 |
| `cm.info.saved` | 저장되었습니다. | 저장 완료 |
| `cm.error.save.failed` | 저장에 실패했습니다. | 저장 실패 (서버 메시지가 없을 때) |
| `sy.common.code.save.category.exists` | [{0}] {1} 분류가 이미 등록되어 있습니다. | backend: 등록 분류 존재 |
| `sy.common.code.save.code.exists` | [{0}] 분류에 상세코드 [{1}]가 이미 등록되어 있습니다. | backend: 등록 상세코드 존재 |
| `cm.error.entity.not.found` | 요청한 리소스를 찾을 수 없습니다. | backend: 수정 대상 없음 |

---

## 9. 비고 / 미결사항

- 사용자관리여부(`usrMngeYn`)는 저장만 하고 화면 동작에는 쓰지 않는다. `N`인 분류의 코드도 이 화면에서 편집할 수 있다. 시스템 관리 코드를 화면 편집에서 막을지 결정이 필요하다.
- 분류 건수(`lblCtgTotCnt`)는 삭제 표시 행을 포함한다 (SYPG020은 삭제 표시 행을 빼고 센다).
- 검색조건 분류코드는 대소문자를 구분하지 않고, 분류명은 구분한다 (SYPG020 메뉴 검색은 둘 다 구분하지 않도록 맞춤).
- 상세코드는 길이(30자)만 제한하고 형식은 검사하지 않는다 (분류코드는 영문 대문자·숫자·`_`만 허용).
- 코드 목록 조회 API(`/details`)에는 `@PrgmAuth`가 없다. 다른 화면의 콤보 조회(`getCommCdOptions`)와 함께 쓰는 API라서 로그인 사용자는 누구나 조회할 수 있다.
- 엑셀 다운로드 기능은 없다.
