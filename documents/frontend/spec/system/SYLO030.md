# SYLO030 시스템로그관리 프로그램 사양서

| 항목 | 내용 |
|---|---|
| 프로그램 ID | SYLO030 |
| 프로그램명 | 시스템로그관리 |
| 메뉴 경로 | 시스템관리 > LOG관리 > 시스템로그관리 |
| 라우트 | `/sy/sylo030` (name `SYLO030`) |
| 화면 파일 | `workspace/coder-frontend/src/views/system/logs/SystemLogManageView.vue` |
| API 파일 | `workspace/coder-frontend/src/api/sy/systemLog.ts` |
| API 상태 | 실제 API 연동 (coder-backend) |
| 대상 데이터 | coder-backend 에러 로그 파일 `logs/coder-backend-error.{일자}.{순번}.log` (JSON Lines), 사용자 테이블 조인 |
| 작성자 | ywkim |
| 작성일 | 2026-10-23 |

### 변경 이력

| 일자 | 작성자 | 내용 |
|---|---|---|
| 2026-10-23 | ywkim | 최초 작성 (현재 구현 기준) |

---

## 1. 개요

coder-backend가 남긴 ERROR 로그(JSON Lines 파일)를 기간·사용자·시스템유형 조건으로 조회하고, 선택한 로그의 에러내용과 상세(요청, 추적ID, 에러코드, SQL_ID, 스택트레이스)를 확인한다. 화면 오류 응답 헤더 `X-Trace-Id`로 해당 로그를 찾는 용도로도 쓴다.

---

## 2. 화면 구성

```text
┌ [SYLO030] 시스템로그관리 ─────────────────────────────────── [엑셀] ┐
├ 검색영역 ───────────────────────────────────────────────────────────┤
│ 기간 [어제 ~ 오늘]  사용자유형 [전체▼]  ID/사용자명 [ID][명]           │
│ 시스템유형 [전체▼]                                           [조회] │
├ 시스템 로그 - N건 (최근 1000건 표시) ─┬ 에러내용 ─────────────────────┤
│ NO│로그일시│시스템유형│사용자ID│...    │ ┌──────────────────────────┐ │
│ (grdSysmLogList, 20행 페이징)         │ │ txaErrMsg (읽기전용)     │ │
│                                       │ └──────────────────────────┘ │
└───────────────────────────────────────┴──────────────────────────────┘
  행 더블클릭 → [시스템 로그 상세] 팝업 (mdlSysmLogDtl, 960px)
```

- 왼쪽 그리드는 최대 740px, 오른쪽 에러내용 영역은 최소 360px로 나머지 폭을 쓴다.

---

## 3. 검색조건

| Ref/ID | 항목 | 컴포넌트 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|---|
| `dtpLogPrid` | 기간 | DatePicker (range) | Y | 어제 ~ 오늘 | 최대 31일 (`MAX_PERIOD_DAYS`). 프리셋·지우기 없음 |
| `cboUsrClsCd` | 사용자유형 | Select | | 전체 | 공통코드 `SY/USR_CLS_CD` |
| `txtUsrId` | 사용자ID | Input | | | Enter 시 조회 |
| `txtUsrNm` | 사용자명 | Input | | | Enter 시 조회 |
| `cboSysmClsCd` | 시스템유형 | Select | | 전체 | 공통코드 `SY/SYSM_CLS_CD` |

- 검색영역 폼 ID는 `frmSrch`이며, submit 시 조회한다.

---

## 4. 그리드 / 상세

### 4.1 시스템 로그 그리드 (`grdSysmLogList`)

| 컬럼 | Key | 폭 | 정렬 | 비고 |
|---|---|---|---|---|
| NO | `no` | 48px | 가운데 | 조회 순번 |
| 로그일시 | `logDttm` | 170px | 가운데 | 정렬 가능, `yyyy-MM-dd HH:mm:ss.SSS` |
| 시스템유형 | `sysmClsCd` | 80px | 가운데 | 코드 → 명칭 |
| 사용자ID | `usrId` | 100px | | 정렬 가능 |
| 사용자명 | `usrNm` | 120px | | |
| 사용자유형 | `usrClsCd` | 84px | 가운데 | 코드 → 명칭 |
| 부서명 | `deptCd` | 110px | | `DEPT_OPTIONS`(임시 목록)로 명칭 변환 |

- 20행 페이징, 조밀(dense) 표시, 줄바꿈 없음. 건수는 `lblTotCnt`에 표시한다.
- 조회 후 첫 행을 활성 행으로 선택한다.
- 그리드 상단 안내: `sy.system.log.guide.grid`

### 4.2 에러내용 (`txaErrMsg`)

- 활성 행의 `errMsg`를 읽기전용 textarea(16줄)로 표시한다. 선택 행이 없으면 placeholder "로그를 선택하면 에러내용이 표시됩니다."

### 4.3 시스템 로그 상세 팝업 (`mdlSysmLogDtl`)

| 항목 | 내용 |
|---|---|
| 로그일시 / 시스템유형 | `logDttm` / 시스템유형 명칭 |
| 사용자 / 사용자유형·부서 | `usrId usrNm` / 사용자유형 명칭 · 부서명 |
| 요청 | `method uri` |
| 추적ID / 에러코드 | `traceId` / `errCd` |
| SQL_ID | `sqlId` (`{매퍼명}.{쿼리ID}`) |
| 로거 / 스레드 | `logger` / `thread` |
| 에러내용 | `errMsg` |
| 스택트레이스 | `stackTrace` (`lblStackTrace`, 없으면 `-`) |

---

## 5. 버튼 / 이벤트

| Ref/ID | 버튼/이벤트 | 핸들러 | 권한 | 처리 |
|---|---|---|---|---|
| `btnSrch` | 조회 | `onBtnSrchClick` | v-permission | 검색조건 검증 후 목록 조회 (6.1) |
| `btnXlsDnld` | 엑셀 | `onBtnXlsDnldClick` | v-permission | 조회 결과 CSV 다운로드 (6.3) |
| `grdSysmLogList` | 행 클릭 | `onGrdSysmLogListRowClick` | | 활성 행 변경 → 오른쪽 에러내용 갱신 |
| `grdSysmLogList` | 행 더블클릭 | `onGrdSysmLogListRowDblclick` | | 상세 조회 후 팝업 열기 (6.2) |
| `txtUsrId`, `txtUsrNm` | Enter | `onBtnSrchClick` | | 조회 |

---

## 6. 처리 로직

### 6.0 초기화 (`onMounted`)
1. `fetchPageData()` : 공통코드 `SY/SYSM_CLS_CD`(시스템유형), `SY/USR_CLS_CD`(사용자유형)를 병렬 조회한다. 검색조건 옵션은 앞에 "전체"를 붙인다.
2. `initPageUi()` : 기본 조건(어제~오늘)으로 최초 조회한다.

### 6.1 조회 (`searchList`)
1. `validateSearch()` : 기간 시작·종료가 없으면 `cm.validate.period.required`, 기간이 31일을 넘으면 `cm.validate.period.max`(31)를 경고로 띄우고 중단한다.
2. 기간을 `YYYYMMDD`로 바꿔 `getSystemLogListApi`를 호출한다.
3. 결과에 순번(`no`)을 붙이고 첫 행을 활성 행으로, `totalCount`·`truncated`를 저장한다.
4. 결과가 없으면 `cm.info.no.data`, 잘렸으면(`truncated`) `sy.system.log.info.truncated`(전체 건수, 1000)를 띄운다. 잘린 경우 그리드 제목 옆에 `cm.info.recent.rows.shown`을 표시한다.

### 6.2 상세 (`openDetail`)
- `getSystemLogApi(logKey)`로 스택트레이스를 포함한 상세를 조회해 팝업에 표시한다.

### 6.3 엑셀 다운로드
- 조회 결과가 없으면 `cm.info.no.export.data`를 띄운다.
- 파일명 `시스템로그_{YYYYMMDD}.csv`. 컬럼은 그리드 컬럼(NO 제외) + 에러내용이며, 코드 컬럼은 명칭으로 변환한다. 스택트레이스는 넣지 않는다.

---

## 7. 사용 API

| 함수 | Method / URL | 요청 | 응답 | 비고 |
|---|---|---|---|---|
| `getSystemLogListApi` | `GET /api/v1/system-logs` | `stDt`, `endDt`(YYYYMMDD), `sysmClsCd`, `usrClsCd`, `usrId`, `usrNm` | `SystemLogPage { list, totalCount, truncated }` | 최근 순 최대 1000건 (`MAX_ROWS`) |
| `getSystemLogApi` | `GET /api/v1/system-logs/{logKey}` | `logKey` (`{일자}-{파일순번}-{줄번호}`) | `SystemLog` (스택트레이스 포함) | |
| `getCommCdOptions` | 공통코드 조회 (`api/common/commonCode.ts`) | 업무약어, 코드분류 | `CodeOption[]` | `SY/SYSM_CLS_CD`, `SY/USR_CLS_CD` |

- `MAX_PERIOD_DAYS`(31), `MAX_ROWS`(1000)는 backend `SystemLogServiceImpl`과 같은 값을 유지한다.

---

## 8. 메시지

| 키 | 문구 (ko) | 사용 시점 |
|---|---|---|
| `cm.validate.period.required` | 조회 기간을 입력하세요. | 기간 미입력 |
| `cm.validate.period.max` | 조회 기간은 최대 {0}일입니다. | 기간 31일 초과 |
| `cm.info.no.data` | 조회된 내역이 없습니다. | 조회 결과 없음 |
| `sy.system.log.info.truncated` | 조회 결과가 {0}건이라 최근 {1}건만 표시합니다.\n조건을 좁혀 조회하세요. | 결과 1000건 초과 |
| `cm.info.recent.rows.shown` | (최근 {0}건 표시) | 그리드 제목 옆 (잘린 경우) |
| `sy.system.log.guide.grid` | 행을 더블클릭하면 에러 상세(스택트레이스)를 볼 수 있습니다. | 그리드 안내 |
| `cm.info.no.export.data` | 내보낼 내역이 없습니다. | 엑셀 대상 없음 |

---

## 9. 비고 / 미결사항

- 부서명은 부서 마스터가 없어 `DEPT_OPTIONS`(임시 목록, `api/sy/usrAuthMng.ts`)로 변환한다 (PRD Open Issue 3).
- 사용자유형 코드가 DB 사용자(`A`/`O`/`V`)와 공통코드 SY002(`1`~`9`)가 맞지 않는다 (PRD Open Issue 2).
