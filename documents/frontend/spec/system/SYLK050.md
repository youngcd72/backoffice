# SYLK050 배치관리 프로그램 사양서

| 항목 | 내용 |
|---|---|
| 프로그램 ID | SYLK050 |
| 프로그램명 | 배치관리 |
| 메뉴 경로 | 시스템관리 > 배치관리 > 배치관리 (메뉴ID `SY040100`) |
| 라우트 | `/sy/sylk050` (name `SYLK050`) |
| 화면 파일 | `workspace/coder-frontend/src/views/system/links/BatchManageView.vue` |
| API 파일 | `workspace/coder-frontend/src/api/sy/batch.ts` |
| API 상태 | 실제 API 연동 (coder-batch :8081 직접 호출. 로컬은 vite proxy `/api/v1/batch-jobs`, `/api/v1/batch-executions` → :8081, 서버는 `VITE_BATCH_API_BASE_URL`) |
| 대상 데이터 | `TB_DSPT_JOB_M`(배치직무), `TB_DSPT_EXEC_L`(배치실행내역), `TB_DSPT_ALRM_RCVR_R`(실패 알람수신자), Quartz Trigger 상태 |
| 작성자 | ywkim |
| 작성일 | 2026-10-23 |

### 변경 이력

| 일자 | 작성자 | 내용 |
|---|---|---|
| 2026-10-23 | ywkim | 최초 작성 (현재 구현 기준) |
| 2026-10-23 | ywkim | 문제점 수정 반영 |

---

## 1. 개요

coder-batch에 등록된 배치직무를 조건별로 조회하고, 배치를 등록·수정하며 실행 실패 시 알림을 받을 사용자(알람수신자)를 지정한다. 행 단위로 수동 실행(실행 파라미터 입력)을 요청하고, 배치별 실행 이력을 조회한다. 상세 팝업에서 스케줄 일시정지/재개와 실행 중 상태로 멈춘 최종 실행 건의 폐기(배치상태 초기화)를 할 수 있다.

---

## 2. 화면 구성

```text
┌ [SYLK050] 배치관리 ──────────────────────────────────── [배치등록][엑셀] ┐
├ 검색영역 (frmSrch) ──────────────────────────────────────────────────────┤
│ 배치ID/명 [    ]  업무모듈 [전체▼]  사용여부 [사용▼]                      │
│ 배치수행일 [    ]  실행상태 [전체▼]                               [조회] │
├ 배치 목록 - N건   행을 클릭하면 배치 이력, 배치ID를 클릭하면 배치 설정 ──┤
│ NO│배치ID│배치명│수동실행[실행]│실행상태│최종수행시작일시│...            │
│ (grdJobList, 20행 페이징, 가로 스크롤)                                   │
└──────────────────────────────────────────────────────────────────────────┘
  행 클릭      → [배치 이력 - {배치명}] 팝업 (mdlExecList, 980px)
  배치ID 클릭  → [배치 관리 - {배치명}] 팝업 (mdlJobDtl, 900px)
  [배치등록]   → [배치 등록] 팝업 (mdlJobDtl, 900px)
  [실행] 클릭  → [배치 실행 - {배치명}] 팝업 (mdlJobRun, 600px)
  상세 팝업 [추가] → 사용자 선택 팝업 (mdlAlrmRcvr, ComSearchCodePopup type USR, 다건)
```

- 배치 그리드와 실행내역 그리드의 셀은 줄바꿈하지 않는다 (`white-space: nowrap`). 긴 텍스트(에러메시지, 실행클래스, 비고)는 말줄임(최대 220px)하고 마우스오버(title)로 전체를 보여 준다.

---

## 3. 검색조건

| Ref/ID | 항목 | 컴포넌트 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|---|
| `txtKeyword` | 배치ID/명 | Input | | | 배치ID·배치명 부분 일치 (앞뒤 공백 제거). Enter 시 조회 |
| `cboWrkMdulCd` | 업무모듈 | Select | | 전체 | 공통코드 `SY/WRK_MDUL_CD` |
| `cboUseYn` | 사용여부 | Select | | 사용(`Y`) | `USE_YN_OPTIONS`(사용 `Y` / 미사용 `N`) + 전체 |
| `dtpLastExecDt` | 배치수행일 | DatePicker (단일) | | | 최종 실행시작일자. `YYYYMMDD`로 변환해 전송. 프리셋 없음 |
| `cboLastExecStatCd` | 실행상태 | Select | | 전체 | 최종 실행상태. `EXEC_STAT_OPTIONS`(화면 상수, 6.6) |

- 검색영역 폼 ID는 `frmSrch`이며, submit 시 조회한다.

---

## 4. 그리드 / 상세

### 4.1 배치 목록 그리드 (`grdJobList`)

| 컬럼 | Key | 폭 | 정렬 | 비고 |
|---|---|---|---|---|
| NO | `no` | 48px | 가운데 | 조회 순번 (`renumber`) |
| 배치ID | `jobId` | 90px | | 정렬 가능. 링크 → 클릭 시 상세 팝업 (4.4) |
| 배치명 | `jobNm` | 200px | | 정렬 가능. 밑줄 표시 (행 클릭 → 실행내역 팝업) |
| 수동실행 | `manlExec` | 70px | 가운데 | [실행] 셀 버튼 (`v-permission="'btnJobExec'"`) |
| 실행상태 | `lastExecStatNm` | 72px | 가운데 | 최종 실행상태 배지 (6.7). 실행 이력이 없으면 빈 칸 |
| 최종수행시작일시 | `lastExecStDttmText` | 150px | 가운데 | 정렬 가능, `YYYY-MM-DD HH:mm:ss` |
| 최종수행종료일시 | `lastExecEndDttmText` | 150px | 가운데 | `YYYY-MM-DD HH:mm:ss` |
| 최종수행시간 | `lastDiffTm` | 84px | 오른쪽 | 시작~종료 초 → `N분 NN초` / `NN초` (종료 없으면 빈 칸) |
| 처리건수 | `lastProcCnt` | 72px | 오른쪽 | 천 단위 구분 (`formatCount`) |
| 성공건수 | `lastSuccCnt` | 72px | 오른쪽 | 천 단위 구분 |
| 실패건수 | `lastFailCnt` | 72px | 오른쪽 | 천 단위 구분. 0보다 크면 빨간 굵은 글씨 |
| 에러메시지 | `lastErrMsgCont` | 220px | | 말줄임 + title |
| 업무모듈 | `wrkMdulNm` | 80px | 가운데 | `wrkMdulCd` → 공통코드 명칭 |
| 스케줄표현식 | `scheExpr` | 120px | | Quartz cron |
| 스케줄상태 | `schedStatusNm` | 76px | 가운데 | `schedStatus` → `SCHED_STATUS_OPTIONS` 명칭 |
| 다음실행일시 | `nextFireDttmText` | 150px | 가운데 | `YYYY-MM-DD HH:mm:ss` |
| 사용여부 | `useYnNm` | 64px | 가운데 | `useYn` → `USE_YN_OPTIONS` 명칭 (사용/미사용) |
| 실행클래스 | `execClassNm` | 260px | | 말줄임 + title |
| 비고 | `rmk` | 200px | | 말줄임 + title |

- 20행 페이징, 조밀(dense) 표시. 건수는 `lblTotCnt`에 표시한다.
- 그리드 상단 안내: `sy.batch.grid.guide`
- 재조회 시 이전에 선택한 배치ID의 행을 활성 행으로 유지한다 (없으면 선택 해제).

### 4.2 배치 이력 팝업 (`mdlExecList`, 980px)

제목: `배치 이력 - {배치명}`

| Ref/ID | 항목 | 컴포넌트 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|---|
| `dtpExecPrid` | 기간 | DatePicker (range) | Y | 어제 ~ 오늘 | 실행시작 기간. 프리셋 없음, 최대 기간 제한 없음 |
| `cboExecStatCd` | 실행상태 | Select | | 전체 | `EXEC_STAT_OPTIONS` |
| `btnExecSrch` | 조회 | Button | | | `onBtnExecSrchClick` (v-permission 없음) |

- 검색 폼 ID는 `frmExecSrch`이며, submit 시 조회한다.
- 그리드 제목: `배치 이력 [{배치ID}] - N건` (`lblExecTotCnt`). 최대 조회 건수(`EXEC_MAX_ROWS`=500)를 넘어 잘리면 건수 뒤에 `cm.info.recent.rows.shown`("(최근 500건 표시)")을 경고색(`text-warn`)으로 표시한다.

실행내역 그리드 (`grdExecList`)

| 컬럼 | Key | 폭 | 정렬 | 비고 |
|---|---|---|---|---|
| NO | `no` | 44px | 가운데 | 조회 순번 |
| 시작일시 | `execStDttmText` | 150px | 가운데 | `YYYY-MM-DD HH:mm:ss` |
| 종료일시 | `execEndDttmText` | 150px | 가운데 | `YYYY-MM-DD HH:mm:ss` |
| 소요시간 | `diffTm` | 70px | 오른쪽 | `N분 NN초` / `NN초` |
| 실행상태 | `execStatNm` | 72px | 가운데 | 상태 배지 (6.7) |
| 실행구분 | `execClsNm` | 64px | 가운데 | `execClsCd` → `EXEC_CLS_OPTIONS` (S 스케줄 / M 수동 / R 재시작) |
| 처리 | `procCnt` | 64px | 오른쪽 | 천 단위 구분 |
| 성공 | `succCnt` | 64px | 오른쪽 | 천 단위 구분 |
| 실패 | `failCnt` | 64px | 오른쪽 | 0보다 크면 빨간 굵은 글씨 |
| 요청자 | `firsRegrId` | 80px | 가운데 | |
| 에러메시지 | `errMsgCont` | (나머지) | | 말줄임 + title |

- 10행 페이징, 조밀 표시. 데이터가 없으면 `sy.batch.hist.empty`.

### 4.3 배치 실행 팝업 (`mdlJobRun`, 600px)

제목: `배치 실행 - {배치명}`, 영역 제목 `실행 파라미터 [{배치ID}]`

실행 파라미터 테이블 (`grdRunParm`, 네이티브 table)

| 컬럼 | Ref/ID | 설명 |
|---|---|---|
| NO | | 행 순번 |
| 파라미터 키 | `txtRunParmKey{n}` | `cell-input`, maxlength 100, placeholder `baseDt` |
| 값 | `txtRunParmVal{n}` | `cell-input`, maxlength 1000, placeholder `20261017`. Enter 시 실행 |
| 삭제 | | 셀 버튼 → `onBtnRunParmDelClick(idx)` |

- 열 때 배치직무의 기본 파라미터(`jobParmCont`, JSON 객체)를 키·값 행으로 채운다. 없거나 JSON 객체가 아니면 빈 행 1개로 시작한다. 열린 뒤 `txtRunParmVal1`로 포커스.
- 행이 0개이면 `sy.batch.run.parm.empty`를 표시한다.
- 하단 안내: `sy.batch.run.parm.default.hint`

### 4.4 배치 등록/수정 팝업 (`mdlJobDtl`, 900px)

제목: 신규 `배치 등록`, 수정 `배치 관리 - {배치명}`. 영역 제목 `배치 기본` 옆에 최종 실행상태 배지(있을 때).

배치 기본 폼 (`frmJobDtl`)

| Ref/ID | 항목 | 컴포넌트 | 필수 | 기본값(신규) | 설명 |
|---|---|---|---|---|---|
| `txtDtlJobId` | 배치ID | Input | Y | | maxlength 7, placeholder `BHFL001`. 수정 시 읽기전용·비활성. 신규일 때 안내 "BH + 업무 2자리 + 순번 3자리" |
| `cboDtlWrkMdulCd` | 업무모듈 | Select | Y | `SY` | 공통코드 `SY/WRK_MDUL_CD` ("전체" 없음) |
| `txtDtlJobNm` | 배치명 | Input | Y | | maxlength 100 (검증 100byte) |
| `txtDtlExecClassNm` | 실행 클래스 | Input | Y | | maxlength 200 (검증 200byte). 안내 "Job 빈을 정의한 클래스 (패키지 포함)" |
| `txtDtlScheExpr` | 스케줄표현식 | Input | | | maxlength 100, placeholder `0 10 0 * * ?`. 안내 "초 분 시 일 월 주 [년], 비우면 수동 실행 전용" |
| `rdoDtlMsfrPlcyCd` | 미스파이어 | 라디오 그룹 | | `01` | `MSFR_PLCY_OPTIONS` (01 건너뜀 / 02 즉시 1회 실행) |
| `lblDtlSchedStatus` | 스케줄상태 | 텍스트 | | | 수정 시에만 표시. `SCHED_STATUS_OPTIONS` 명칭 |
| `lblDtlNextFireDttm` | 다음실행일시 | 텍스트 | | | 수정 시에만 표시. `YYYY-MM-DD HH:mm:ss` |
| `chkDtlMltExecYn` | 다중실행 | Checkbox ("허용") | | `N` | 체크 시 `Y` |
| `chkDtlUseYn` | 사용여부 | Checkbox ("사용") | | `N` | 체크 시 `Y` |
| `txaDtlJobParmCont` | 기본 파라미터 | textarea (3줄) | | | maxlength 4000, placeholder `{"baseDt":"20261017"}`. 안내 "JSON 형식, 수동 실행·스케줄 실행 시 JobParameters로 전달" |
| `txtDtlRmk` | 비고 | Input | | | maxlength 200 (검증 200byte) |

- 필수 항목은 컴포넌트 `required`로 입력 박스 왼쪽 빨간 줄을 표시한다. 검증은 `validateFields`(6.3)로 하며 `validateForm`은 쓰지 않는다.
- 팝업이 열리면 신규는 `txtDtlJobId`, 수정은 `txtDtlJobNm`으로 포커스.

툴바 버튼 (6.4 ~ 6.6)

| Ref/ID | 버튼 | 표시 / 활성 조건 |
|---|---|---|
| `btnJobStatInit` | 배치상태 초기화 | 수정 시에만 표시. 최종 실행상태가 실행 중(`STARTING`/`STARTED`/`STOPPING`)일 때만 활성 |
| `btnSchedPause` | 스케줄 일시정지 / 스케줄 재개 | 수정 시에만 표시. 스케줄상태가 `PAUSED`이면 "스케줄 재개", 그 외 "스케줄 일시정지". `NORMAL`/`PAUSED`일 때만 활성 |
| `btnDtlSave` | 저장 | 항상 |

알람수신 영역 (실행 실패 시 알림 센터 알람, `TB_DSPT_ALRM_RCVR_R`)

- 제목: `알람수신  총 N명 · 실행 실패 시 알림`
- 버튼: `btnAlrmRcvrAdd`(추가), `btnAlrmRcvrDel`(삭제)
- 알람수신자 테이블 (`grdAlrmRcvr`, 네이티브 table)

| 컬럼 | Ref/ID | 설명 |
|---|---|---|
| 체크 | `chkAlrmRcvrAll` / `chkAlrmRcvr{n}` | 헤더는 전체 선택 (행이 없으면 비활성) |
| NO | | 행 순번 |
| 사용자ID | | `usrId` |
| 사용자명 | | `usrNm`. 사용자 `useYn='N'`이면 "미사용" 배지 |

- 행이 없으면 `sy.batch.alarm.receiver.empty`, 하단 안내 `sy.batch.alarm.receiver.save.hint`.
- 추가·삭제는 화면 목록에만 반영되고, [저장] 시 배치 기본과 함께 전체 교체로 저장된다.

### 4.5 알람수신자 선택 팝업 (`mdlAlrmRcvr`)

- 공통 `ComSearchCodePopup` (`type="USR"`, `multiple`). 선택한 사용자(`CD`→`usrId`, `NM`→`usrNm`, `useYn='Y'`)를 목록에 추가하며, 이미 있는 사용자ID는 제외한다.

---

## 5. 버튼 / 이벤트

| Ref/ID | 버튼/이벤트 | 핸들러 | 권한 | 처리 |
|---|---|---|---|---|
| `btnSrch` | 조회 | `onBtnSrchClick` | v-permission | 배치 목록 조회 (6.1) |
| `btnJobReg` | 배치등록 | `onBtnJobRegClick` | v-permission | 빈 폼으로 등록 팝업 열기 (4.4) |
| `btnXlsDnld` | 엑셀 | `onBtnXlsDnldClick` | v-permission | 배치 목록 CSV 다운로드 (6.8) |
| `btnJobExec` (그리드 셀 [실행]) | 수동실행 | `onGrdJobListExecClick` | v-permission `'btnJobExec'` | 실행 파라미터 팝업 열기 (6.5) |
| `grdJobList` | 행 클릭 | `onGrdJobListRowClick` | | 활성 행 변경 후 배치 이력 팝업 열기 (6.2) |
| `grdJobList` 배치ID | 링크 클릭 | `onGrdJobListJobIdClick` | | 활성 행 변경 후 상세 조회·수정 팝업 열기 (4.4) |
| `txtKeyword` | Enter | `onBtnSrchClick` | | 조회 |
| `btnExecSrch` | 조회 (이력 팝업) | `onBtnExecSrchClick` | | 실행내역 조회 (6.2) |
| `btnRunParmAdd` | 행추가 (실행 팝업) | `onBtnRunParmAddClick` | | 빈 파라미터 행 추가 후 새 행 키로 포커스 |
| (셀 버튼) | 삭제 (실행 팝업) | `onBtnRunParmDelClick` | | 해당 파라미터 행 삭제 |
| `btnRunExec` | 실행 (실행 팝업) | `onBtnRunExecClick` | | 파라미터 검증 후 수동 실행 요청 (6.5) |
| `txtRunParmVal{n}` | Enter | `onBtnRunExecClick` | | 실행 |
| `btnDtlSave` | 저장 (상세 팝업) | `onBtnDtlSaveClick` | | 검증 후 배치 저장 (6.3) |
| `btnSchedPause` | 스케줄 일시정지/재개 | `onBtnSchedPauseClick` | | 스케줄 상태 변경 (6.4) |
| `btnJobStatInit` | 배치상태 초기화 | `onBtnJobStatInitClick` | | 최종 실행 건 폐기 (6.6) |
| `btnAlrmRcvrAdd` | 추가 (알람수신) | `onBtnAlrmRcvrAddClick` | | 사용자 선택 팝업 열기 |
| `mdlAlrmRcvr` | 선택 | `onMdlAlrmRcvrSelect` | | 선택 사용자 추가 (중복 제외) |
| `btnAlrmRcvrDel` | 삭제 (알람수신) | `onBtnAlrmRcvrDelClick` | | 체크한 수신자 목록에서 제외. 선택이 없으면 `sy.batch.alarm.receiver.delete.select` |
| `chkAlrmRcvrAll` | 변경 | `onChkAlrmRcvrAllChange` | | 전체 선택 / 해제 |
| `chkDtlMltExecYn` | 변경 | `onChkDtlMltExecYnChange` | | `mltExecYn` = `Y`/`N` |
| `chkDtlUseYn` | 변경 | `onChkDtlUseYnChange` | | `useYn` = `Y`/`N` |

- 컴포넌트 권한(`TB_PRGM_CMPN_M`) 등록 대상은 화면 버튼 `btnJobReg`, `btnXlsDnld`, `btnSrch`, `btnJobExec` 4건이다. 팝업 안 버튼은 권한 제어하지 않는다.

---

## 6. 처리 로직

### 6.0 초기화 (`onMounted`)
1. `fetchPageData()` : 공통코드 `SY/WRK_MDUL_CD`(업무모듈)를 조회한다. 검색조건 옵션은 앞에 "전체"를 붙인다.
2. `initPageUi()` : 검색조건을 기본값(사용여부 `Y`, 나머지 전체/빈 값)으로 초기화하고 그리드·활성 행을 비운 뒤 최초 조회한다.

### 6.1 배치 목록 조회 (`searchJobList`)
1. 검색어는 앞뒤 공백을 제거하고, 배치수행일은 `YYYYMMDD`로 바꿔 `getBatchJobListApi`를 호출한다. 별도 검색조건 검증은 없다.
2. 응답을 그리드 행으로 변환한다 (`toJobRow`: 업무모듈·실행상태·스케줄상태·사용여부 명칭, 일시 표시값, 최종 수행시간(초)). 순번을 다시 매긴다.
3. 이전 활성 행의 배치ID가 결과에 있으면 다시 활성 행으로 선택한다.
4. 결과가 없으면 `cm.info.no.data`를 띄운다 (최초 조회 포함).

### 6.2 배치 이력 (`openExecutionPopup`, `searchExecutionList`)
1. 행 클릭 시 대상 배치를 저장하고 기간을 어제~오늘, 실행상태를 전체로 초기화한 뒤 팝업을 열고 바로 조회한다.
2. 기간 시작·종료가 없으면 `cm.validate.period.required`를 경고로 띄우고 중단한다.
3. `getBatchExecutionListApi`(배치ID, 기간 `YYYYMMDD`, 실행상태)를 호출한다. API 함수가 `page=1`, `size=EXEC_MAX_ROWS`(500)를 붙이므로 실행시작일시 내림차순 최대 500건만 조회된다.
4. 응답의 전체 건수(`totalCount`)가 받은 건수보다 많으면 잘림(`truncated`)으로 보고 그리드 제목에 `cm.info.recent.rows.shown`을 표시한다 (4.2). 팝업을 열 때 잘림 표시를 초기화한다.

### 6.3 배치 저장 (`onBtnDtlSaveClick`)
1. `validateDtlForm()` : 아래 순서로 검사하고, 첫 오류를 경고로 띄운 뒤 해당 필드로 포커스한다. 형식 검사(custom)는 값이 있을 때만 한다.

| 항목 | 필수 | 최대 | 형식 검사 | 오류 메시지 |
|---|---|---|---|---|
| 배치ID | Y | | `^BH[A-Z]{2}[0-9]{3}$` (`checkJobId`) | `sy.batch.validate.job.id` |
| 배치명 | Y | 100byte | | |
| 실행 클래스 | Y | 200byte | 패키지 포함 Java 클래스명 (`checkExecClassNm`) | `sy.batch.validate.exec.class` |
| 업무모듈 | Y | | | |
| 스케줄표현식 | | 100byte | 공백 구분 6~7개 토큰, 토큰은 `[0-9A-Za-z*?/,#-]` (`checkScheExpr`) | `sy.batch.validate.sche.plan.quartz` |
| 기본 파라미터 | | 4000byte | JSON 객체 (`checkJobParmCont`) | `sy.batch.validate.job.parm.json` |
| 비고 | | 200byte | | |

   - 필수 누락은 `cm.validate.required`, 바이트 초과는 `cm.validate.bytes.exceeded`(공통 `validateFields`).
2. `cm.confirm.save` 확인 후 `toBatchJobSave`로 요청을 만든다: 앞뒤 공백 제거, 스케줄표현식의 연속 공백을 한 칸으로 정리, 빈 스케줄표현식·기본 파라미터·비고는 `null`, 알람수신자ID 목록(`alrmRcvrIdList`, 전체 교체)을 함께 넣는다.
3. 신규면 `POST`, 수정이면 `PUT`으로 `saveBatchJobApi`를 호출한다. coder-batch는 저장과 함께 Quartz 스케줄을 반영한다. 신규 등록(`POST`)인데 이미 있는 배치ID면 coder-batch가 기존 배치를 덮어쓰지 않고 `sy.batch.job.duplicate`(409) 오류를 돌려준다.
4. 실패 시 서버 메시지(없으면 `cm.error.save.failed`)를 띄우고 오류 코드별로 포커스한다.

| 오류 코드 | 포커스 |
|---|---|
| `sy.batch.job.duplicate` | `txtDtlJobId` |
| `sy.batch.alarm.receiver.not.found` | `btnAlrmRcvrAdd` |
| `sy.batch.job.bean.not.registered`, `sy.batch.job.class.ambiguous` | `txtDtlExecClassNm` |
| `sy.batch.schedule.invalid.expression` | `txtDtlScheExpr` |

5. 성공 시 `cm.info.saved`를 띄우고 팝업을 닫은 뒤 목록을 재조회하고, 저장한 배치ID 행을 활성 행으로 선택한다.

### 6.4 스케줄 일시정지/재개 (`onBtnSchedPauseClick`)
1. 현재 스케줄상태가 `PAUSED`가 아니면 일시정지, `PAUSED`이면 재개로 처리한다.
2. `sy.batch.confirm.schedule.pause` / `sy.batch.confirm.schedule.resume`(배치ID) 확인 후 `saveBatchJobScheduleApi(jobId, 'PAUSED' | 'NORMAL')`를 호출한다.
3. 실패 시 서버 메시지(없으면 `cm.error.save.failed`). 성공 시 상세를 다시 조회(`reloadDtlJob`)해 스케줄상태·다음실행일시를 갱신하고, `sy.batch.info.schedule.paused` / `sy.batch.info.schedule.resumed`를 띄운 뒤 목록을 재조회한다. 팝업은 열린 채로 둔다.

### 6.5 수동 실행 (`onGrdJobListExecClick`, `onBtnRunExecClick`)
1. [실행] 클릭 시 사용여부가 `Y`가 아니면 `sy.batch.unused.cannot.run`을 경고로 띄우고 중단한다.
2. 기본 파라미터로 실행 파라미터 행을 채워 팝업을 연다 (4.3).
3. [실행] 시 `validateRunParms()` : 키·값이 모두 빈 행은 제외하고, 행마다 아래를 검사해 첫 오류에서 경고 후 해당 키 입력(`txtRunParmKey{n}`)으로 포커스한다. 값은 앞뒤 공백을 제거한다.
   - 키 형식 `^[A-Za-z][A-Za-z0-9_]*$` 아님 → `sy.batch.validate.run.parm.key`
   - 예약 키(`RESERVED_JOB_PARM_KEYS`: `jobId`, `requestDttm`, `fireId`, `execClsCd`, `reqUsrId`, `execSrvNm`) → `sy.batch.validate.run.parm.reserved`(키)
   - 중복 키 → `sy.batch.validate.run.parm.duplicate`(키)
4. 확인 없이 `saveBatchJobExecutionApi(jobId, params)`를 호출한다 (비동기 실행, 결과는 실행내역에서 확인).
5. 실패 시 서버 메시지(없으면 `sy.batch.error.run.failed`, 예: 이미 실행 중 `sy.batch.job.already.running`). 성공 시 팝업을 닫고 `sy.batch.info.run.requested`를 띄운 뒤 목록을 재조회한다.

### 6.6 배치상태 초기화 (`onBtnJobStatInitClick`)
1. 최종 실행상태가 실행 중(`RUNNING_EXEC_STAT_CDS`: `STARTING`, `STARTED`, `STOPPING`)이 아니거나 최종 실행일련번호(`lastDsptExecSn`)가 없으면 `sy.batch.only.running.can.reset`을 띄우고 중단한다.
2. `sy.batch.confirm.reset` 확인 후 `saveBatchExecutionAbandonApi(lastDsptExecSn)`로 해당 실행 건을 `ABANDONED` 처리한다 (coder-batch는 클러스터에서 실제로 실행 중이면 거부, `sy.batch.execution.still.running`).
3. 실패 시 서버 메시지(없으면 `sy.batch.error.reset.failed`). 성공 시 상세를 다시 조회하고 `sy.batch.info.reset`을 띄운 뒤 목록을 재조회한다.

### 6.7 코드 / 표시 규칙
- 실행상태 배지 색: `COMPLETED` 완료(초록), `FAILED`·`UNKNOWN` 오류(빨강), 실행 중(`STARTING`/`STARTED`/`STOPPING`) 진행(연초록), 그 외 대기(회색).
- 화면 상수 코드 (`api/sy/batch.ts`)

| 상수 | 값 |
|---|---|
| `EXEC_STAT_OPTIONS` | `COMPLETED` 완료, `FAILED` 실패, `STARTED` 실행중, `STARTING` 시작중, `STOPPING` 중지중, `STOPPED` 중지, `ABANDONED` 폐기, `SKIP` 건너뜀, `UNKNOWN` 알수없음 |
| `EXEC_CLS_OPTIONS` | `S` 스케줄, `M` 수동, `R` 재시작 |
| `SCHED_STATUS_OPTIONS` | `NORMAL` 정상, `PAUSED` 일시정지, `BLOCKED` 실행중, `ERROR` 오류, `COMPLETE` 종료, `NONE` 미등록 |
| `MSFR_PLCY_OPTIONS` | `01` 건너뜀, `02` 즉시 1회 실행 |

### 6.8 엑셀 다운로드
- 조회 결과가 없으면 `cm.info.no.export.data`를 띄운다.
- 파일명 `배치관리_{YYYYMMDD}.csv`. 컬럼은 그리드 컬럼에서 수동실행(`manlExec`)만 뺀 것(NO 포함)이고, 최종수행시간은 `N분 NN초` 형식, 나머지는 행 값(명칭 컬럼은 명칭, 사용여부는 사용/미사용) 그대로 넣는다.

---

## 7. 사용 API

모든 호출은 coder-batch(:8081)로 간다 (`BATCH_API = { baseURL: VITE_BATCH_API_BASE_URL || '' }`).

| 함수 | Method / URL | 요청 | 응답 | 비고 |
|---|---|---|---|---|
| `getBatchJobListApi` | `GET /api/v1/batch-jobs` | `keyword`, `wrkMdulCd`, `useYn`, `lastExecDt`(YYYYMMDD), `lastExecStatCd` | `BatchJob[]` | 스케줄 상태·최종 실행내역 포함 |
| `getBatchJobApi` | `GET /api/v1/batch-jobs/{jobId}` | `jobId` | `BatchJob` (`alrmRcvrList` 포함) | 없으면 `success=false` → `sy.batch.not.found.retry` |
| `saveBatchJobApi` | `POST /api/v1/batch-jobs` (신규) / `PUT /api/v1/batch-jobs/{jobId}` (수정) | `BatchJobSave` (배치 기본 + `alrmRcvrIdList`) | `null` | 스케줄 즉시 반영. `POST`에서 이미 있는 배치ID면 `sy.batch.job.duplicate` |
| `saveBatchJobExecutionApi` | `POST /api/v1/batch-jobs/{jobId}/executions` | `{ params: { 키: 값 } }` | `null` | 비동기 실행 |
| `saveBatchJobScheduleApi` | `PATCH /api/v1/batch-jobs/{jobId}/schedule` | `{ schedStatus: 'PAUSED' \| 'NORMAL' }` | `null` | |
| `getBatchExecutionListApi` | `GET /api/v1/batch-executions` | `jobId`, `stDt`, `endDt`(YYYYMMDD), `execStatCd`, `page=1`, `size=500` | `BatchExecutionPage` (`list`, `totalCount`, `truncated` = `totalCount` > `list` 건수) | 실행시작일시 내림차순, 최대 500건 (서버 `@Max(500)`) |
| `saveBatchExecutionAbandonApi` | `PATCH /api/v1/batch-executions/{dsptExecSn}` | `{ execStatCd: 'ABANDONED' }` | `null` | 실제로 실행 중이면 `success=false` |
| `getCommCdOptions` | 공통코드 조회 (`api/common/commonCode.ts`) | 업무약어, 코드분류 | `CodeOption[]` | `SY/WRK_MDUL_CD` |

- 저장 계열 API는 `requestApi` 공통 함수로 오류 응답을 `ApiResponse`(`success=false`, `code`, `message`)로 받는다.
- coder-batch에 있는 `DELETE /api/v1/batch-jobs/{jobId}`, `GET /api/v1/batch-executions/{execSn}`, `POST .../restarts`, 실행 중지(`STOPPED`)는 화면에서 쓰지 않는다.

---

## 8. 메시지

| 키 | 문구 (ko) | 사용 시점 |
|---|---|---|
| `sy.batch.grid.guide` | 행을 클릭하면 배치 이력, 배치ID를 클릭하면 배치 설정을 엽니다. | 배치 그리드 안내 |
| `cm.info.no.data` | 조회된 내역이 없습니다. | 배치 목록 결과 없음 |
| `cm.info.no.export.data` | 내보낼 내역이 없습니다. | 엑셀 대상 없음 |
| `cm.validate.period.required` | 조회 기간을 입력하세요. | 이력 팝업 기간 미입력 |
| `sy.batch.hist.empty` | 조회된 배치 이력이 없습니다. | 실행내역 그리드 빈 목록 |
| `cm.info.recent.rows.shown` | (최근 {0}건 표시) | 배치 이력이 500건을 넘어 잘렸을 때 |
| `sy.batch.not.found.retry` | 배치 정보를 찾을 수 없습니다. 다시 조회하세요. | 상세 조회 실패 (서버 메시지 없을 때) |
| `sy.batch.validate.job.id` | 배치ID는 BH + 영문 대문자 2자리 + 숫자 3자리로 입력하세요.\n(예: BHFL001) | 배치ID 형식 오류 |
| `sy.batch.validate.exec.class` | 실행 클래스는 패키지를 포함한 Java 클래스 이름으로 입력하세요. | 실행 클래스 형식 오류 |
| `sy.batch.validate.sche.plan.quartz` | 스케줄표현식은 "초 분 시 일 월 주 [년]" 형식으로 입력하세요.\n(예: 0 0 1 * * ?) | 스케줄표현식 형식 오류 |
| `sy.batch.validate.job.parm.json` | 기본 파라미터는 JSON 객체 형식으로 입력하세요. | 기본 파라미터 형식 오류 |
| `cm.validate.required` | {0} 필수 입력 항목입니다. | 필수 누락 |
| `cm.validate.bytes.exceeded` | {0} {1}byte를 넘을 수 없습니다. (한글 1자 = 3byte) | 바이트 초과 |
| `cm.confirm.save` | 저장하시겠습니까? | 저장 확인 |
| `cm.info.saved` | 저장되었습니다. | 저장 성공 |
| `cm.error.save.failed` | 저장에 실패했습니다. | 저장·스케줄 변경 실패 (서버 메시지 없을 때) |
| `sy.batch.confirm.schedule.pause` | [{0}] 스케줄을 일시정지하시겠습니까? | 일시정지 확인 |
| `sy.batch.confirm.schedule.resume` | [{0}] 스케줄을 재개하시겠습니까? | 재개 확인 |
| `sy.batch.info.schedule.paused` | 스케줄을 일시정지했습니다. | 일시정지 성공 |
| `sy.batch.info.schedule.resumed` | 스케줄을 재개했습니다. | 재개 성공 |
| `sy.batch.only.running.can.reset` | 최종 실행이 실행 중 상태인 배치만 초기화할 수 있습니다. | 초기화 대상 아님 |
| `sy.batch.confirm.reset` | 최종 실행 건을 폐기(ABANDONED) 처리합니다.\n실제로 실행 중인 배치가 아닌지 확인하셨습니까? | 초기화 확인 |
| `sy.batch.info.reset` | 배치상태를 초기화했습니다. | 초기화 성공 |
| `sy.batch.error.reset.failed` | 배치상태 초기화에 실패했습니다. | 초기화 실패 (서버 메시지 없을 때) |
| `sy.batch.unused.cannot.run` | 미사용 배치는 수동 실행할 수 없습니다. | 미사용 배치 실행 |
| `sy.batch.run.parm.empty` | 파라미터 없이 실행합니다. | 실행 파라미터 행 없음 |
| `sy.batch.run.parm.default.hint` | 기본 파라미터 값이 채워져 있습니다. 값을 바꾸거나 행을 추가해 실행하면 이번 실행에만 적용됩니다. | 실행 팝업 안내 |
| `sy.batch.validate.run.parm.key` | 파라미터 키는 영문으로 시작하고 영문·숫자·_만 입력할 수 있습니다. | 파라미터 키 형식 오류 |
| `sy.batch.validate.run.parm.reserved` | [{0}]은(는) 배치 시스템이 사용하는 키라서 파라미터로 쓸 수 없습니다. | 예약 키 사용 |
| `sy.batch.validate.run.parm.duplicate` | 파라미터 키 [{0}]이(가) 중복되었습니다. | 파라미터 키 중복 |
| `sy.batch.info.run.requested` | 배치 실행을 요청했습니다.\n결과는 배치 이력에서 확인하세요. | 수동 실행 요청 성공 |
| `sy.batch.error.run.failed` | 수동 실행에 실패했습니다. | 수동 실행 실패 (서버 메시지 없을 때) |
| `sy.batch.alarm.receiver.empty` | 등록된 알람수신자가 없습니다. 추가 버튼으로 사용자를 선택하세요. | 알람수신자 없음 |
| `sy.batch.alarm.receiver.delete.select` | 삭제할 알람수신자를 선택하세요. | 삭제 대상 미선택 |
| `sy.batch.alarm.receiver.save.hint` | 추가·삭제한 내용은 저장 버튼을 눌러야 반영됩니다. 미사용 사용자는 알람을 받지 않습니다. | 알람수신 안내 |

coder-batch 응답 메시지 (`coder-batch/src/main/resources/messages/messages_ko.properties`, 서버가 `message`로 내려줌)

| 키 | 문구 (ko) | 발생 |
|---|---|---|
| `sy.batch.job.duplicate` | 배치ID [{0}]이(가) 이미 등록되어 있습니다. | 신규 등록 (`POST`) |
| `sy.batch.job.bean.not.registered` | 실행 클래스 [{0}]에 정의된 Spring Batch Job 빈이 없습니다. | 저장 |
| `sy.batch.job.class.ambiguous` | 실행 클래스 [{0}]에 Job 빈이 {1}개 정의되어 있습니다. 클래스당 Job은 1개만 정의하세요. | 저장 |
| `sy.batch.schedule.invalid.expression` | 스케줄 표현식(cron)이 올바르지 않습니다: {0} | 저장 |
| `sy.batch.alarm.receiver.not.found` | [{0}] 사용자가 없어 알람수신자로 등록할 수 없습니다. | 저장 |
| `sy.batch.job.already.running` | 배치직무 [{0}]이(가) 이미 실행 중입니다. | 수동 실행 |
| `sy.batch.execution.still.running` | 배치 실행내역 [{0}]이(가) 스케줄러 노드에서 아직 실행 중입니다. | 배치상태 초기화 |

---

## 9. 비고 / 미결사항

- 실행상태·실행구분·미스파이어정책·스케줄상태는 화면 상수(`api/sy/batch.ts`)로 두고 있다. batch PRD 7.3은 실행구분·실행상태·미스파이어정책을 공통코드(`WRK_MDUL_CD='SY'`)로 정의하고 있어 공통코드 등록 후 `fetchPageData()` 조회로 바꿀지 결정이 필요하다.
- 신규 등록 시 사용여부 기본값은 `N`(미사용)이다. 등록 직후에는 스케줄이 돌지 않고 수동 실행도 막힌다.
- 배치 삭제, 실패/중지 건 재시작, 실행 중지, 실행 상세(Step별 현황)는 coder-batch API는 있으나 화면 기능이 없다.
