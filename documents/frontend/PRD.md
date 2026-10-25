# CODER 관리 콘솔 프론트엔드 (coder-frontend) PRD

| 항목 | 내용 |
|---|---|
| 문서명 | CODER 관리 콘솔 프론트엔드(`coder-frontend`) 제품 요구사항 정의서 |
| 버전 | v0.2 (배포 전 초안 - 현재 구현 반영) |
| 작성일 | 2026-10-09 |
| 구현 | `workspace/coder-frontend` (프레임·공통 컴포넌트·시스템관리 화면 7개 구현. 메뉴·권한·시스템로그·배치·공통코드 화면은 실제 API 연동) |
| 대상 독자 | 프론트엔드/백엔드 개발자, 화면 설계 담당자, 아키텍트 |
| 관련 문서 | 루트 `CLAUDE.md` (Frontend 네이밍·주석·필수입력 규칙, 메시지 관리), `documents/backend/PRD.md`, `documents/batch/PRD.md`, 화면설계서(`[HDD_ST05]-화면설계서_*`) |

### 변경 이력

| 버전 | 내용 |
|---|---|
| v0.1 | 최초 작성. 프레임(MDI 탭·사이드바·즐겨찾기·홈 검색), 공통 컴포넌트, 메시지 backend 이관, 화면별 구현 현황과 목업 교체 범위, 결정 필요 사항 정리 |
| v0.2 | 현재 구현 반영 (2026-10-23): 화면별 API 연동 상태, 권한 메뉴(DB) 구성·컴포넌트 권한(v-permission), 행삭제 확인·종료일자 기본값 규칙, 프로그램 사양서(`documents/frontend/spec/`) 추가 |

---

## 1. 배경 및 목적

### 1.1 배경
- 화면은 화면설계서(pptx)의 레이아웃, 항목, 이벤트를 기준으로 만든다. 화면 ID(`SYAU010` 등)는 설계서의 프로그램 ID를 그대로 쓴다.
- 메뉴·권한·시스템로그·배치·공통코드 화면은 실제 API에 연동되어 있다.
- 화면별 상세 사양은 프로그램 사양서 `documents/frontend/spec/{대분류}/{프로그램 ID}.md`에 있다.

### 1.2 목적
- 내부 사용자(직원·운영자)가 쓰는 **단일 관리 콘솔**을 제공한다. 메뉴 → MDI 탭 → 조회/입력 화면의 일관된 사용 흐름을 갖춘다.
- 공통 컴포넌트, 검증, 메시지, 다이얼로그, 그리드 행 상태를 표준화해 화면마다 같은 방식으로 구현되게 한다.
- 화면과 API의 경계를 `src/api/**`로 고정한다. 백엔드 API가 준비되면 화면 코드 변경 없이 API 모듈만 교체한다.

---

## 2. 범위

### 2.1 In Scope
1. 앱 프레임: 로그인, JWT 보관·만료 처리, 상단 메뉴, 사이드바(메뉴/즐겨찾기 탭), MDI 탭, 홈(검색) 탭 (구현됨)
2. 공통 컴포넌트·컴포저블·유틸 (구현됨, 4.4장)
3. 메시지: backend 메시지 번들을 내려받아 `msg()`로 표시 (구현됨)
4. 시스템관리(SY) 화면 7개 (구현됨, 6.3장 표)
5. 목업 API를 실제 백엔드 API로 교체 (메뉴·권한·시스템로그·배치·공통코드 완료)
6. DB 기반 권한 메뉴 구성, 컴포넌트 권한(`v-permission`) 적용 (구현됨). URL 직접 접근 차단은 미구현

### 2.2 Out of Scope
- 대시보드 검색의 MCP 서버 연동 → 별도 설계 후 진행 (현재는 검색창만 있음)
- 시스템관리(SY)·기준정보(MS) 외 업무 영역 화면
- 외부 고객용(B2C) 화면
- 모바일 전용 레이아웃 (데스크톱 1366px 이상 기준)

---

## 3. 용어 정의

| 용어 | 설명 |
|---|---|
| 화면 ID | 설계서의 프로그램 ID. 라우트 이름·페이지 제목 앞 배지(`page-plate`)·파일 헤더 `Program ID`에 쓴다 |
| MDI 탭 | 메뉴를 열 때마다 생기는 화면 탭. 여러 화면을 동시에 열어 두고 오간다 |
| 홈 탭 | 닫을 수 없는 고정 탭. 대시보드(검색) 화면을 연다 |
| 목업 API | 백엔드 대신 메모리 데이터로 응답하는 `src/api/**` 함수. 실제 API와 같은 `ApiResponse<T>` 형태를 반환한다 |
| 행 상태 플래그 | 그리드 행의 `_flag` (`I` 등록, `U` 수정, `D` 삭제). 저장 시 변경 행만 보낸다 |
| Ref/ID 접두사 | `btn`, `cbo`, `txt`, `grd` 등 컨트롤 종류 접두사 (`CLAUDE.md` Frontend 2장) |
| 코드서치 | 코드 입력 + 돋보기 + 명칭 표시 컴포넌트 (`ComSearchCode`/`BaseSearchCode`, 접두사 `cds`) |

---

## 4. 시스템 아키텍처

### 4.1 구성도

```text
 브라우저
  └─ coder-frontend (Vite dev :3000 / 빌드 정적 파일)
       main.ts : 앱 생성 → Pinia·Router → GET /api/v1/messages → mount
       ├─ router      : JWT 유효성 가드 (없거나 만료 → /login)
       ├─ MainLayout  : 상단 메뉴 · 사이드바(메뉴/즐겨찾기) · MdiTabBar · <router-view>
       ├─ views/system/{menu,auths,logs,links}/* : 시스템관리 화면 (중분류 단위)
       ├─ views/master/basic/* : 기준정보 > 기초정보 화면
       ├─ api/**      : API 모듈 (실제 호출 또는 목업)
       │     └─ client.ts (axios) : Authorization Bearer, Accept-Language, 401 → 로그인 이동
       └─ components/common : 공통 컴포넌트
              │  /api, /actuator (Vite 프록시 또는 VITE_API_BASE_URL)
              ▼
        coder-backend (:8080)          coder-batch (:8081, 배치관리 화면 직접 호출)
```

### 4.2 기술 스택

| 구분 | 내용 |
|---|---|
| 프레임워크 | Vue 3.5 (`<script setup>` + TypeScript) |
| 빌드 | Vite 8, vue-tsc (타입 검사 후 빌드) |
| 상태 관리 | Pinia (`auth`, `tabs`) |
| 라우팅 | vue-router 5 (history 모드) |
| HTTP | axios (공통 인스턴스 `api/client.ts`, 타임아웃 10초) |
| 다국어 | 라이브러리 없음. backend 메시지 번들 + `utils/message.ts`의 `msg()` |
| 스타일 | 자체 디자인 시스템 (`assets/main.css`, `--ds-*` 토큰, 다크모드 미지원) |
| 테스트 | 단위 테스트 도구 없음. headless Chrome 스크립트로 수동 검증 중 |

### 4.3 환경 설정

| 파일 | `VITE_APP_ENV` | `VITE_API_BASE_URL` |
|---|---|---|
| `.env.local` | local | (빈 값 → Vite 프록시로 `localhost:8080`) |
| `.env.develop` | develop | `https://dev-api.coder.example.com` (임시값) |
| `.env.production` | production | `https://api.coder.example.com` (임시값) |

### 4.4 공통 모듈

| 구분 | 모듈 | 설명 |
|---|---|---|
| 입력 | `Button`, `Input`, `Select`, `DatePicker`(단일/기간), `Listbox` | `required`/`validate` 속성으로 필수 표시(왼쪽 2px 빨간 줄)와 DOM 검증 대상 지정 |
| 그리드 | `DataTable` | 정렬, 페이징, 선택(`selectable`), 행 상태 색상(`flag-column`), 활성 행(`active-row`), `row-click`/`row-dblclick` |
| 트리 | `TreeView` | 메뉴 트리 등 |
| 팝업 | `PopupWindow`, `Modal`, `MessageDialog` + `useDialog` | `alert`/`confirm`을 Promise로 사용 |
| 알림 | `ToastHost` + `useToast` | `toast.success(msg('cm.info.saved'))` |
| 코드서치 | `ComSearchCode`, `BaseSearchCode`, `ComSearchCodePopup` | 업무구분(type)별 조회 정의 (`api/common/searchCode.ts`) |
| 첨부 | `ComFileUpload` | 업로드·목록·삭제·다운로드 (`api/common/file.ts`, 현재 목업) |
| 기타 | `Tabs`, `Card`, `Badge`, `ProgressBar` | |
| 유틸 | `utils/validate.ts` | `validateForm(영역)`, `validateFields(규칙)`, 날짜·바이트·주민번호 검사, 조사 처리 |
| 유틸 | `utils/rowFlag.ts` | `markRowUpdated`, `deleteFlaggedRows`, `hasRowChanges`, `isDeletedRow` |
| 유틸 | `utils/csv.ts` | 조회 결과 CSV(엑셀) 다운로드 |
| 유틸 | `utils/message.ts` | `loadMessages`, `msg(key, ...args)`, `getLang` |

---

## 5. 프로젝트 구조

```text
workspace/coder-frontend/src/
├── main.ts                 # 앱 생성, 메시지 로드 후 mount
├── App.vue                 # DialogHost, ToastHost 포함
├── router/index.ts         # 라우트(화면 ID = 라우트 이름), 인증 가드
├── layouts/MainLayout.vue  # 상단 메뉴, 사이드바, MDI 탭, 홈 탭
├── components/
│   ├── common/             # 공통 컴포넌트 (index.ts 에서 일괄 export)
│   └── frame/MdiTabBar.vue
├── composables/            # useDialog, useToast
├── stores/                 # auth(JWT·사용자), tabs(MDI 탭)
├── api/
│   ├── client.ts           # axios 공통 인스턴스
│   ├── auth.ts             # 로그인·프로필
│   ├── common/             # message, favorite, searchCode, file
│   └── sy/                 # 시스템관리 화면별 API (대부분 목업)
├── utils/                  # message, validate, rowFlag, csv
├── views/
│   ├── main/               # 로그인(LoginView), 홈(DashboardView)
│   ├── errors/             # 오류 화면 (NotFoundView)
│   ├── system/             # 시스템관리 화면 (menu, auths, logs, links)
│   ├── master/basic/       # 기준정보 > 기초정보 화면
│   └── template/ComponentExample.vue  # 공통 컴포넌트 사용 예제
└── assets/main.css         # 디자인 시스템 토큰·공통 스타일
```

- 화면 파일명은 `{도메인}{Manage|Inquiry}View.vue` (PascalCase, 두 단어 이상)이다.
- 화면별 API 파일은 `api/{업무약어}/{도메인}.ts`이다. 파일 헤더에 대상 테이블과 연동할 엔드포인트를 적는다.

---

## 6. 기능 요구사항

### FR-01. 로그인 / 인증
- `/login`에서 아이디·비밀번호로 `POST /api/v1/auth/login`을 호출하고, JWT와 사용자 정보를 `localStorage`(`coder_token`, `coder_user`)에 저장한다.
- 라우터 가드: JWT가 없거나 만료되면 `/login`으로, 로그인 상태에서 `/login`에 오면 `/dashboard`로 보낸다.
- API가 401을 반환하면 저장 정보를 지우고 `/login`으로 이동한다.
- 로그인 화면에 브랜드 "Toshiba Global Commerce Solutions"를 표시한다.

### FR-02. 앱 프레임
| 요소 | 요구사항 |
|---|---|
| 상단 메뉴 | 대분류 `시스템관리`(sy), `템플릿`(template). 메뉴명 검색창 |
| 사이드바 | `메뉴` / `즐겨찾기` 탭. 햄버거 버튼으로 **완전히** 접고 편다 (아이콘만 남기지 않음) |
| 메뉴 | 대분류별 그룹(메뉴관리·권한관리·LOG관리·배치관리)과 화면 목록. 로그인 사용자의 권한 메뉴(`GET /api/v1/auth-menus`, `TB_MENU_M` + 권한그룹·사용자 메뉴 권한)로 상단 메뉴·사이드바·메뉴 검색을 구성한다 |
| 즐겨찾기 | `GET /api/v1/favorites`로 로그인 사용자의 즐겨찾기를 표시하고 클릭 시 화면을 연다 |
| MDI 탭 | 메뉴 클릭 시 탭을 열거나 기존 탭으로 이동한다. 탭 닫기·좌우 이동·전체 닫기를 제공한다. 새로고침 시 현재 경로의 탭을 다시 연다 |
| 홈 탭 | 닫기 버튼 없는 고정 탭. 클릭하면 대시보드(검색) 화면을 연다 |
| 대시보드 | Google처럼 검색창만 있는 화면. 검색 처리는 MCP 서버 연동 예정 (`search()`의 TODO) |

### FR-03. 메시지 (다국어)
- 앱 시작 시 `GET /api/v1/messages`로 현재 언어의 메시지를 모두 받는다. 언어는 `localStorage.coder_lang`(기본 `ko`)이고, API 요청에 `Accept-Language`로 보낸다.
- alert/confirm/toast, 검증 오류, 빈 목록 안내 같은 **문장형 메시지**는 `msg('키', 인자...)`로 표시한다. 버튼·컬럼명·제목·placeholder 같은 라벨은 대상이 아니다.
- `msg()`는 표시 시점에 호출한다 (모듈 최상위 상수에서 호출 금지).
- 새 메시지는 backend `messages*.properties` 세 파일에 추가한다 (`CLAUDE.md` Backend 6.1).

### FR-04. 화면 공통 동작
| 구분 | 요구사항 |
|---|---|
| 초기화 | `onMounted`에서 `fetchPageData()`(공통코드 등 참조 데이터) → `initPageUi()`(검색영역·그리드 초기화, 최초 조회) 순서로 호출한다 |
| 조회 | 필수 조건을 검증한 뒤 조회한다. 결과가 없으면 `cm.info.no.data`, 최대 건수를 넘으면 잘림 안내를 띄운다. 건수는 `lblTotCnt`에 표시한다 |
| 입력 | 필수 항목은 컴포넌트 `required`로 왼쪽 빨간 줄을 표시한다 (라벨 `*` 금지). 저장 전 `validateForm`/`validateFields`로 검증한다 |
| 그리드 편집 | 행추가 → `I`, 수정 → `U`, 행삭제 → `D`(신규 행은 바로 제거). 저장 대상이 없으면 `cm.info.no.changes`를 띄운다 |
| 행삭제 확인 | 하위 데이터가 함께 지워지는 마스터 행(사용자·권한그룹·프로그램 등)의 행삭제만 확인 창(`cm.confirm.delete.selected`)을 띄운다. 그 밖의 행은 `D` 표시만 하고, 저장할 때 다시 확인한다 |
| 기간 기본값 | 신규 행의 종료일자 기본값(무기한)은 `99991231`이다 (공통 SQL의 `COALESCE(END_DT, '99991231')`과 같은 값) |
| 저장 | 변경 확인(confirm) → 저장 API → 성공 toast → 재조회 |
| 엑셀 | 조회 결과를 CSV로 내려받는다. 비밀키·본문·스택트레이스 같은 민감·대용량 항목은 제외한다 |
| 상세 | 행 클릭은 선택·상세 영역 갱신, 행 더블클릭은 상세 팝업 (설계서 이벤트 기준) |

### FR-05. 공통 컴포넌트 표준
- 화면은 공통 컴포넌트를 `@/components/common`에서 가져와 쓴다. 같은 기능을 화면에서 따로 구현하지 않는다.
- 공통 컴포넌트를 고치면 `views/template/ComponentExample.vue`(사용 예제)도 함께 고친다.
- 하드코딩 색상 대신 `--ds-*` 토큰만 쓴다.

### FR-06. 시스템관리 화면

| 메뉴 그룹 | 화면 ID | 화면명 | 파일 | API 상태 |
|---|---|---|---|---|
| 메뉴관리 | SYPG010 | 프로그램관리 | `ProgramManageView.vue` | 실제 API |
| 메뉴관리 | SYPG020 | 메뉴관리 | `MenuManageView.vue` | 실제 API (`/api/v1/menus`) |
| 권한관리 | SYAU010 | 사용자권한관리 | `UserAuthManageView.vue` | 실제 API (`/api/v1/user-auths`) |
| 권한관리 | SYAU020 | 그룹권한관리 | `AuthGroupManageView.vue` | 실제 API (`/api/v1/auth-groups`) |
| 권한관리 | SYAU030 | 그룹별사용자관리 | `AuthGroupUserManageView.vue` | 실제 API (`/api/v1/auth-groups`) |
| LOG관리 | SYLO030 | 시스템로그관리 | `SystemLogManageView.vue` | 실제 API |
| 배치관리 | SYLK050 | 배치관리 | `BatchManageView.vue` | 연동 (coder-batch :8081 직접 호출) |
| 기준정보 > 기초정보 | MSBS010 | 공통코드관리 | `CommonCodeManageView.vue` | 실제 API (`/api/v1/common-codes`) |

- 화면별 상세(검색조건, 그리드, 이벤트, 처리 로직, API, 메시지)는 프로그램 사양서 `documents/frontend/spec/{대분류}/{프로그램 ID}.md`(시스템관리 `system/`, 기준정보 `master/`)를 따른다.

주요 화면 요구사항

- **SYAU010 사용자권한관리**
  - 왼쪽: 사용자 그리드 (사용자ID, 사용자명, 부서명).
  - 상세정보: 2열 고정 배치이고, 좁으면 가로 스크롤.
    - 필수: 사용자명, 사용자ID, 시작·종료일자
    - 선택: 사원번호, 부서, 이메일, 전화번호, 재직여부(`USR_STAT_CD`), 잠김여부(`USR_LOCK_YN`), 입사·퇴사일자
  - 권한 목록 그리드: 권한복사, 화면추가, 행삭제.
  - 컴포넌트목록 그리드 (`TB_USR_CMPN_R`).
  - 협력업체(사용자유형 `P`) 사용자는 비밀번호 초기화.
  - 검증: 이메일·휴대폰 형식, 사용자ID 중복, 기간 역전.
- **SYAU020 그룹권한관리:** 그룹목록, 그룹 유효기간(시작일·종료일 2줄)·비고, 권한목록, 컴포넌트 권한.
- **SYAU030 그룹별사용자관리:** 그룹목록(사용자수·합계)과 선택한 그룹의 사용자 조회·추가·삭제 (점포 대신 부서명 표시).
- **SYLO030 시스템로그관리**
  - 검색조건: 기간(필수, 어제~오늘, 최대 31일), 사용자유형, ID/사용자명, 시스템유형.
  - 그리드: NO, 로그일시, 시스템유형, 사용자ID, 사용자명, 사용자유형, 부서명.
  - 오른쪽 "에러내용" textarea: 선택 행의 에러 문구.
  - 행 더블클릭: 상세 팝업(요청, 추적ID, 에러코드, SQL_ID, 스택트레이스).
  - 엑셀에는 에러내용 포함.
- **MSBS010 코드관리:** 분류목록(TB_COMM_CD_M) 신규·수정·삭제와 분류별 코드목록(TB_COMM_CD_D) 행추가·수정. 코드는 삭제하지 않고 사용여부로 미사용 처리, 코드가 있는 분류는 삭제 불가.

### FR-07. 목업 → 실제 API 교체
- 목업 함수와 실제 API는 **같은 이름·파라미터·반환 타입**(`Promise<ApiResponse<T>>`)을 유지한다. 교체는 API 파일 안에서만 한다.
- 연동할 엔드포인트는 각 API 파일 헤더 주석과 `documents/backend/PRD.md` FR-09를 따른다.
- 교체 후에는 목업 데이터·목업 전용 옵션 목록을 지우고, 파일 헤더의 "목업으로 동작" 문구를 고친다 (Program History는 최초생성 한 줄만 두고 이력을 추가하지 않는다).
- 화면 상수 옵션은 공통코드가 등록되는 대로 `fetchPageData()`의 공통코드 조회(`getCommCdOptions`)로 바꾼다. 남은 임시 상수: `YN_OPTIONS`, `batch.ts`의 실행상태·실행구분·미스파이어정책, `DEPT_OPTIONS`(부서 마스터 부재).

### FR-08. 권한
- 로그인 사용자의 화면 권한으로 메뉴를 구성한다 (권한 없는 화면은 숨김). `TB_MENU_M` 기반 권한 메뉴 API(`GET /api/v1/auth-menus`)를 쓴다 (구현됨).
- 컴포넌트 권한(`TB_*_CMPN_R`)으로 버튼(`btnSave`, `btnJobReg` 등)의 표시를 제어한다 (`v-permission` 디렉티브, 구현됨). 컴포넌트 ID는 화면의 Ref/ID와 같은 값을 쓰고, `TB_PRGM_CMPN_M`에 등록한다.
- URL 직접 접근 차단(라우터 가드에서 화면 권한 확인)은 미구현이다.

---

## 7. 화면 개발 표준

`CLAUDE.md` Frontend 1~7장을 기준으로 하며, 요점은 다음과 같다.
- **Ref/ID:** `{접두사}{라벨약어}` camelCase (`btnSrch`, `grdUsrList`, `cboUsrClsCd`). 표에 없는 접두사는 먼저 문서에 등록한다.
- **핸들러:** `on{Ref}{이벤트}` (`onBtnSaveClick`, `onGrdSysmLogListRowDblclick`). 일반 함수는 동사로 시작한다.
- **주석:** 파일 헤더(Program ID/Type/Name, Description, Author, Program History)와 함수 주석(`함수명 : 설명`, `@param`, `@return`).
- **필수입력:** 컴포넌트 `required`로 빨간 줄을 표시하고, 라벨에 `*`를 붙이지 않는다.
- **메시지:** 문장형 메시지는 `msg()`로 쓰고 하드코딩하지 않는다.
- **API:** 화면에서 axios를 직접 쓰지 않고 `api/**` 모듈 함수만 호출한다.
- **검증:** 커밋 전 `npx vue-tsc --noEmit -p tsconfig.app.json`으로 타입 검사한다.

---

## 8. 비기능 요구사항

| 구분 | 요구사항 |
|---|---|
| 지원 환경 | 데스크톱 Chrome/Edge 최신, 해상도 1366px 이상 (1920px 기준 설계) |
| 성능 | 메뉴 클릭 후 화면 표시 1초 이내. 그리드는 페이징(기본 20행)으로 렌더링한다 |
| 보안 | 비밀키·개인정보는 화면 상태·콘솔·엑셀에 남기지 않는다. JWT는 현재 `localStorage`에 보관한다 (13장 결정 필요) |
| 접근성 | 버튼·입력에 라벨을 둔다. 포커스 링은 디자인 시스템 토큰(`--ds-focus-field`)을 쓴다 |
| 일관성 | 모든 화면은 제목바(화면 ID 배지 + 화면명) → 검색영역 → 그리드/상세 순서의 배치를 따른다 |

---

## 9. 일정 (안)

| 단계 | 내용 |
|---|---|
| Phase 1 (완료) | 프레임, 공통 컴포넌트, 메시지 backend 이관, 시스템관리 화면(목업), SYLO030 실제 연동 |
| Phase 2 (완료, 즐겨찾기 추가/삭제·URL 접근 차단 제외) | 공통코드 API 연동, DB 메뉴 트리, 화면·컴포넌트 권한 적용 |
| Phase 3 (완료) | 메뉴·프로그램·권한 화면 실제 API 연동 (SYPG010/020, SYAU010/020/030) |
| Phase 4 | 첨부파일 API 연동 |

---

## 10. 결정 필요 사항 (Open Issues)

| # | 내용 |
|---|---|
| 1 | JWT 보관 위치: `localStorage`(XSS에 취약) 유지 또는 HttpOnly 쿠키 전환 |
| 2 | 사용자유형 코드 불일치: DB 사용자 `A`/`O`/`V` vs 화면 SY002 `1`~`9` |
| 3 | 점·사원·부서 마스터 부재. SYAU010 등은 임시 목록(`DEPT_OPTIONS`)과 목업 필드로 대신하고 있음 (설계서의 점 항목은 부서로 대체) |
| 4 | 대시보드 검색(MCP 서버)의 범위·연동 방식 |
| 5 | ~~배치관리(SYLK050)가 `coder-batch`(:8081)를 직접 호출할지, `coder-backend`가 중계할지~~ → 직접 호출로 결정 (2026-10-17). 로컬은 vite proxy, 서버는 `VITE_BATCH_API_BASE_URL` |
| 6 | 단위·E2E 테스트 도구 도입 여부 (Vitest, Playwright 등) |
| 7 | SYPG010(프로그램관리)·SYPG020(메뉴관리)의 화면 ID와 메뉴 순서가 설계서와 맞는지 확인 |
| 8 | `.env.develop`/`.env.production`의 API 주소는 임시값. 실제 도메인 확정 필요 |

---

## 11. 구현 메모

- 공통 컴포넌트는 `index.ts`에서 `as any`로 export하고 있어 props 타입 검사가 되지 않는다. 컴포넌트를 TypeScript로 옮길 때 함께 정리한다.
- 오류 문의 시 응답 헤더 `X-Trace-Id`로 SYLO030에서 해당 로그를 찾을 수 있다. 오류 다이얼로그에 추적ID를 보여 줄지는 후속 검토한다.
