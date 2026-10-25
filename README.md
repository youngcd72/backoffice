# CODER Backoffice Framework

Backoffice 프로젝트 진행 시 기본 Framework으로 사용하기 위한 프로젝트입니다.

공통 라이브러리(`coder-core`) 위에 관리 콘솔 백엔드·프론트엔드·배치 서버와 사용자/권한/메뉴/공통코드/배치/로그 관리 화면, 표준 DB 스키마와 개발 컨벤션을 함께 제공합니다.

---

## 구성

| 모듈 | 설명 | 포트 |
|---|---|---|
| [`workspace/coder-core`](workspace/coder-core) | 공통 라이브러리 (Composite Build로 각 애플리케이션에 포함) | - |
| [`workspace/coder-backend`](workspace/coder-backend/README.md) | 관리 콘솔 REST API 서버 | 8080 |
| [`workspace/coder-batch`](workspace/coder-batch/README.md) | Spring Batch + Quartz 배치 서버 (DB 등록형 직무·스케줄) | 8081 |
| [`workspace/coder-frontend`](workspace/coder-frontend/README.md) | 관리 콘솔 화면 (Vue 3) | 3000 |
| [`schema`](schema) | DDL, 초기 데이터, 네이밍 약어 사전 | - |
| [`documents`](documents) | PRD, 화면·배치 프로그램 사양서 | - |

`coder-core` 하위 모듈:

| 모듈 | 내용 |
|---|---|
| `coder-core-common` | `ApiResponse<T>` 응답 규격, 예외 처리(`GlobalExceptionHandler`), 다국어 메시지 |
| `coder-core-db` | HikariCP, MyBatis, `AuditInterceptor`(등록·수정자/일시 자동 입력) |
| `coder-core-redis` | `RedisTemplate` 기반 캐시 |
| `coder-core-security` | Spring Security, JWT, SpringDoc(Swagger) |
| `coder-core-kms` | 암호화 키·비밀값 조회 (`KmsClient`, `SecretResolver`, 로컬 구현 포함) |

## 기술 스택

| 구분 | 내용 |
|---|---|
| Backend | Java 21, Spring Boot 3.3.5, MyBatis 3.0.3, SpringDoc 2.6.0 |
| Batch | Spring Batch 5, Quartz 2.3.2 (JDBC JobStore, Cluster 모드) |
| Frontend | Vue 3.5, Vite 8, TypeScript 6, Vue Router 5, Pinia 4, Axios |
| Database | PostgreSQL 18, Redis |
| Build | Gradle Wrapper (backend·batch 9.7.1, core 8.10), pnpm |

## 디렉터리 구조

```text
coder/
├── CLAUDE.md                 # 개발 컨벤션 (Frontend / Backend / DB / 메시지 / 로그)
├── .claude/skills/           # Claude Code 스킬 (run-server, run-batch, save-component)
├── documents/
│   ├── backend/PRD.md
│   ├── batch/PRD.md, spec/   # 배치 프로그램 사양서 (BHFL001~003)
│   └── frontend/PRD.md, spec/ # 화면 프로그램 사양서 (SY*, MS*)
├── schema/
│   ├── table/                # 테이블 DDL (TB_*)
│   ├── data/                 # 초기 데이터 (사용자, 권한, 메뉴, 프로그램, 공통코드)
│   ├── batch/                # batch 계정·스키마, Spring Batch·Quartz 테이블, 샘플 직무
│   └── dictionary/           # 업무약어·단어약어 사전
└── workspace/
    ├── coder-core/
    ├── coder-backend/
    ├── coder-batch/
    └── coder-frontend/
```

## 제공 화면 / 배치

| 프로그램ID | 이름 |
|---|---|
| SYAU010 | 사용자권한관리 |
| SYAU020 | 그룹권한관리 |
| SYAU030 | 그룹별사용자관리 |
| SYPG010 | 프로그램관리 |
| SYPG020 | 메뉴관리 |
| SYLK050 | 배치관리 |
| SYLO030 | 시스템로그관리 |
| MSBS010 | 공통코드관리 |
| BHFL001 | (배치) 사용자상태변경 적용 |
| BHFL002 | (배치) 배치 실행이력 정리 |
| BHFL003 | (배치) 장기 미접속 사용자 잠금 |

사양서는 [`documents/frontend/spec`](documents/frontend/spec), [`documents/batch/spec`](documents/batch/spec)에 있습니다.

---

## 로컬 실행

### 1. 사전 준비

- JDK 21, Node.js 24, pnpm, Docker Desktop
- Docker 컨테이너 `postgres`(`postgres:18`, 5432)와 `redis`(6379)

| 항목 | 값 |
|---|---|
| DB | `coder` |
| backend 계정 / 스키마 | `coder` / `coder` |
| batch 계정 / 스키마 | `batch` / `batch,coder` |

### 2. DB 준비 (최초 1회)

`schema/table`의 DDL과 `schema/data`의 초기 데이터를 `coder` 스키마에 반영합니다.
배치용 `batch` 계정·스키마와 Spring Batch·Quartz 테이블은 [coder-batch README의 "DB 준비"](workspace/coder-batch/README.md)를 따릅니다.

### 3. 서버 실행

DB → backend → frontend 순서로 띄웁니다 (backend는 기동 시 DB에 접속하므로 postgres가 먼저 떠 있어야 합니다).

```bash
docker start postgres redis

# backend (8080)
cd workspace/coder-backend && ./gradlew bootRun

# frontend (3000, /api → 8080 프록시)
cd workspace/coder-frontend && pnpm install && pnpm dev

# batch (8081, 필요할 때만)
cd workspace/coder-batch && ./gradlew bootRun
```

| 주소 | 내용 |
|---|---|
| http://localhost:3000 | 관리 콘솔 (로컬 계정 `admin` / `coder1234`) |
| http://localhost:8080/swagger-ui.html | backend API 문서 |
| http://localhost:8080/actuator/health | backend 상태 (`db`, `redis` UP 확인) |
| http://localhost:8081/actuator/health | batch 상태 |

> batch 서버가 떠 있는 동안 `USE_YN='Y'`인 배치직무는 cron 스케줄대로 자동 실행됩니다.

### 환경변수

설정 파일의 기본값은 **로컬 개발용**입니다. 운영(`prod` 프로필)은 기본값 없이 환경변수가 필수입니다.

| 변수 | 용도 |
|---|---|
| `CODER_DB_PASSWORD` | backend DB(`coder`) 비밀번호 |
| `CODER_BATCH_DB_PASSWORD` | batch DB(`batch`) 비밀번호 |
| `CODER_JWT_SECRET` | JWT 서명 키 (backend·batch 동일 값) |
| `CODER_INIT_PASSWORD` | 사용자 등록·비밀번호 초기화 시 초기 비밀번호 |

---

## 개발 컨벤션

네이밍·주석·메시지·로그 규칙은 [CLAUDE.md](CLAUDE.md)에 정리되어 있습니다. 주요 내용:

- **Frontend**: 컴포넌트 PascalCase, Ref/ID는 `{접두사}{라벨명}` (`btnSave`, `grdUserList`), 핸들러는 `on{Ref}{이벤트}` (`onBtnSaveClick`)
- **Backend**: Controller/Service는 `get`/`save`/`delete`, Mapper는 `select`/`insert`/`update`/`delete`
- **DB**: 표준 단어·용어 사전 기반 대문자 약어 (`TB_USR_M`, `FIRS_REG_DT`), 신규 약어는 [`schema/dictionary`](schema/dictionary/README.md)에 먼저 등록
- **메시지**: 모든 문장형 메시지는 coder-backend `messages*.properties`에서 관리 (`{업무약어}.{식별자}`)
