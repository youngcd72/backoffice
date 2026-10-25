# CODER 관리 콘솔 백엔드 (coder-backend) PRD

| 항목 | 내용 |
|---|---|
| 문서명 | CODER 관리 콘솔 백엔드(`coder-backend`) 제품 요구사항 정의서 |
| 버전 | v0.1 (배포 전 초안 - 현재 구현 반영) |
| 작성일 | 2026-10-09 |
| 구현 | `workspace/coder-backend` (공통 프레임, SYLO030 구현. 나머지 시스템관리 화면 API는 미구현이라 frontend가 목업 데이터로 동작) |
| 대상 독자 | 백엔드/프론트엔드 개발자, 아키텍트, 운영 담당자 |
| 관련 문서 | 루트 `CLAUDE.md` (네이밍·주석·메시지·로그 규칙), `documents/batch/PRD.md`, `schema/dictionary/`, `schema/table/` |

### 변경 이력

| 버전 | 내용 |
|---|---|
| v0.1 | 최초 작성. 현재 구현(인증, 메시지, 즐겨찾기, 로그, SYLO030)과 목업으로 동작 중인 화면의 API 구현 범위, 결정 필요 사항 정리 |

---

## 1. 배경 및 목적

### 1.1 배경
- CODER는 두 개의 Spring Boot 애플리케이션으로 구성된다. 모두 `coder-core`(common / db / redis / security)를 Gradle Composite Build로 포함한다. (`coder-core-kms`는 범용 KMS 암호화 모듈로 남겨 두었으며 현재 사용처는 없다)

| 애플리케이션 | 포트 | 역할 |
|---|---|---|
| **`coder-backend`** (이 문서) | 8080 | 내부 사용자(직원·운영자)가 쓰는 관리 콘솔 `coder-frontend`의 REST API |
| `coder-batch` | 8081 | Spring Batch + Quartz 배치 실행 기반 (`documents/batch/PRD.md`) |

- `coder-frontend`의 시스템관리(SY) 화면은 대부분 만들어졌지만, `coder-backend`에는 아직 해당 API가 없다. 그래서 화면들은 `src/api/sy/*.ts`의 메모리 목업 데이터로 동작한다.
- 초기 데모 기능(모듈 현황, 캐시 조회, 시스템 상태)은 2026-10-09에 삭제했다. 헬스체크(`/api/ping`)만 남아 있다.

### 1.2 목적
- 관리 콘솔이 쓰는 **모든 업무 API와 공통 기능**(인증, 메시지, 메뉴·즐겨찾기, 로그)을 `coder-backend` 한 곳에서 제공한다.
- frontend의 목업 API를 실제 API로 하나씩 교체한다. 이때 frontend의 함수 시그니처와 응답 형식은 바꾸지 않는 것을 원칙으로 한다.
- 운영 중 오류를 화면(SYLO030)에서 추적할 수 있도록 로그 형식과 수집 기준을 표준화한다.

---

## 2. 범위

### 2.1 In Scope
1. 공통 프레임: 로그인/JWT 인증, 사용자 프로필, 다국어 메시지 제공, 즐겨찾기 메뉴, 헬스체크 (구현됨)
2. 공통 처리: 표준 응답 `ApiResponse<T>`, 전역 예외 처리, 로그 MDC·에러 로그 파일 (구현됨)
3. SYLO030 시스템로그관리 - 에러 로그 파일 조회 (구현됨)
4. 목업으로 동작 중인 시스템관리 화면의 API (미구현, 6.9 표 참고)
5. 로그인 보안 강화: 비밀번호 해시 검증, 오류 횟수·잠금, 사용기간 확인, 권한그룹 기반 권한 (미구현)

### 2.2 Out of Scope
- 배치 실행·스케줄링 → `coder-batch` (배치관리 화면 SYLK050은 `coder-batch` 관리 API를 호출)
- 대시보드 검색(MCP 서버 연동) → 별도 설계 후 진행
- 시스템관리(SY)·기준정보(MS) 외 업무 영역의 API

---

## 3. 용어 정의

| 용어 | 설명 |
|---|---|
| 화면 ID | 화면설계서의 프로그램 ID (`SYAU010` 등). frontend 라우트 이름과 같다 |
| 목업 API | frontend `src/api/sy/*.ts`에서 백엔드 대신 메모리 데이터로 응답하는 함수. 실제 API로 교체할 대상 |
| 메시지 키 | `{업무약어}.{식별자}` 형식의 다국어 메시지 키 (`cm.info.no.data`). `messages*.properties`에서 관리 |
| 업무 예외 | `BusinessException(메시지키, 인자...)`. 사용자에게 메시지를 보여줄 목적의 예상된 오류 |
| 추적ID (traceId) | 요청마다 발급하는 16자리 ID. 응답 헤더 `X-Trace-Id`와 로그에 같은 값이 남는다 |
| 에러 로그 | ERROR 레벨 로그만 JSON Lines로 남긴 파일. SYLO030의 원천 데이터 |

---

## 4. 시스템 아키텍처

### 4.1 구성도

```text
 [브라우저] coder-frontend (Vue 3, :3000, /api → 8080 프록시)
      │  Authorization: Bearer {JWT}, Accept-Language
      ▼
 ┌──────────────────────────── coder-backend (:8080) ────────────────────────────┐
 │ Spring Security (JWT HS256) → LogMdcFilter(traceId, usrId) → Controller      │
 │   ├─ common.frame : 인증·프로필 / 메시지 / 즐겨찾기 / 헬스체크               │
 │   ├─ biz.system.* : 시스템관리 화면 API (SYLO030, ...)                       │
 │   └─ GlobalExceptionHandler (ApiResponse 오류 응답 + 에러 로그)              │
 │ MyBatis (SqlIdInterceptor: 오류 쿼리 ID 기록)                                │
 └──────┬────────────────────┬─────────────────────────────────────────────────┘
        ▼                    ▼
  PostgreSQL (coder)     Redis (미사용)
  TB_USR_M, TB_MENU_M…
        │
  logs/coder-backend.log, logs/coder-backend-error.{일자}.{순번}.log  ← SYLO030 이 읽음
```

### 4.2 기술 스택

| 구분 | 내용 |
|---|---|
| 언어 / 런타임 | Java 21 |
| 프레임워크 | Spring Boot 3.3.5, Spring Security 6 (OAuth2 Resource Server, JWT HS256) |
| 데이터 접근 | MyBatis 3.0.3 (mapper XML, `map-underscore-to-camel-case`), HikariCP |
| DB | PostgreSQL 18 (DB `coder`, 스키마 `coder`) |
| 캐시 | Redis (`coder-core-redis` 포함, 업무 사용처 없음) |
| 문서 | SpringDoc OpenAPI (`/swagger-ui.html`) |
| 로그 | Logback + `logstash-logback-encoder` 7.4 (에러 로그 JSON) |
| 빌드 | Gradle Composite Build (`includeBuild('../coder-core')`) |

### 4.3 설계 원칙
- 화면용 API는 `coder-backend`가 같은 DB를 직접 조회·저장한다.
- 메시지 문구는 모두 backend 메시지 번들에서 관리하고, frontend는 `GET /api/v1/messages`로 내려받아 쓴다.
- Controller/Service는 `get`/`save`/`delete`, Mapper는 `select`/`insert`/`update`/`delete`로 명명한다 (`CLAUDE.md` Backend 2장).
- 저장 API는 화면의 행 상태(`I`/`U`/`D`)를 받아 한 트랜잭션에서 처리한다.

---

## 5. 프로젝트 구조

```text
workspace/coder-backend/
├── build.gradle                      # coder-core-*, actuator, logstash-logback-encoder
├── settings.gradle                   # includeBuild('../coder-core')
└── src/main/
    ├── java/kr/or/coder/
    │   ├── biz/
    │   │   ├── CoderBackendApplication.java
    │   │   └── system/                 # 시스템관리 업무 (메뉴 중분류 단위 패키지)
    │   │       └── logs/               # LOG관리 - SYLO030 (controller/dto/service/mapper)
    │   └── common/
    │       ├── frame/                  # 인증·메시지·즐겨찾기·헬스체크
    │       └── log/                    # LogMdcFilter, SqlIdInterceptor, LogMdcKey
    └── resources/
        ├── application.yml
        ├── logback-spring.xml
        ├── messages/messages*.properties
        └── mapper/{frame|sy}/**/*Mapper.xml
```

- 새 화면 API는 `kr.or.coder.biz.system.{중분류}` 아래에 `controller`/`dto`/`service`/`mapper`로 둔다 (중분류: `menu`, `auths`, `logs` — frontend `views/system/*`와 같은 이름). 매퍼 XML은 `resources/mapper/system/{중분류}/`에 둔다.
- 매퍼 인터페이스는 `coder-core-db`의 `@MapperScan("kr.or.coder.**.mapper")`로 자동 등록된다.

---

## 6. 기능 요구사항

### FR-01. 로그인 / 인증
| 구분 | 내용 |
|---|---|
| API | `POST /api/v1/auth/login`, `GET /api/v1/users/profile` |
| 현재 | `TB_USR_M`에서 사용자 조회 → **비밀번호를 고정값 `coder1234`와 비교(데모)** → JWT(HS256, 24시간) 발급. 권한은 역할(`ADMIN` 여부)로 하드코딩 |
| 요구사항 | ① `PWD`를 BCrypt 등 해시로 검증한다. ② 실패 시 `LGIN_ERR_CNT` 증가, 기준 횟수 초과 시 `USR_LOCK_YN='Y'`, `USR_LOCK_DTTM` 기록. ③ 잠김·미사용(`USE_YN='N'`)·사용기간(`USE_ST_DT`~`USE_END_DT`) 밖·퇴직(`USR_STAT_CD`) 사용자는 로그인 거부. ④ 성공 시 `LGIN_ERR_CNT=0`, `FINA_ACC_DTTM` 갱신. ⑤ 최초 로그인(`FIRS_LGIN_YN`)·비밀번호 변경주기(`PWD_CHG_DTTM`) 도래 시 변경 화면 유도. ⑥ 권한은 권한그룹(`TB_USR_AUTH_GRP_R` → 그룹 권한)과 사용자 개별 권한을 합쳐 계산 |
| 오류 | 인증 실패는 사유를 구분하지 않고 `cm.error.unauthorized`로 응답 (계정 존재 여부 노출 방지). HTTP 상태는 **401**이어야 한다 (현재 400, 14장 참고) |

### FR-02. 다국어 메시지
- `GET /api/v1/messages` (인증 불필요): `Accept-Language` 기준 전체 메시지를 키-값으로 반환한다.
- 원본은 `messages.properties`(기본, 영어) / `messages_ko` / `messages_en` 세 파일이고, 같은 키를 함께 추가한다.
- frontend는 앱 시작 시 한 번 받아 `msg()`로 쓴다. 규칙은 `CLAUDE.md` Backend 6장을 따른다.

### FR-03. 즐겨찾기 메뉴
- `GET /api/v1/favorites`: 로그인 사용자의 즐겨찾기(`TB_PRGM_FAV_L` + `TB_MENU_M`, 사용 중 메뉴만)를 정렬순번 순으로 반환한다.
- 후속: 즐겨찾기 추가/삭제 API (화면의 별 아이콘 토글), 메뉴 트리 API(사이드바를 DB 메뉴로 구성).

### FR-04. 헬스체크
- `GET /api/ping` (인증 불필요), `GET /actuator/health`.

### FR-05. 공통 응답 / 예외
- 모든 응답은 `ApiResponse<T>` (`success`, `code`, `message`, `data`, `timestamp`).
- `GlobalExceptionHandler`(coder-core-common)

| 예외 | HTTP | code | 로그 |
|---|---|---|---|
| `BusinessException` | 400 | 메시지 키 | ERROR, 메시지 그대로, `errCd`=메시지 키 |
| 요청 검증 실패 | 400 | `C001` | ERROR, 필드별 오류 |
| 그 외 예외 | 500 | `C004` | ERROR, 가장 안쪽 원인 메시지 + 스택트레이스 |

### FR-06. 로그
- 상세 규칙은 `CLAUDE.md` Backend 8장.

| 파일 (`LOG_PATH`, 기본 `logs/`) | 내용 | 형식 | 보관 |
|---|---|---|---|
| `coder-backend.log` | INFO 이상 | 텍스트 `[traceId\|usrId\|sqlId\|errCd]` 포함 | 30일, 100MB 분할, gzip |
| `coder-backend-error.{yyyy-MM-dd}.{N}.log` | ERROR | JSON Lines, 비압축 | 90일, 100MB 분할 |

- MDC: `LogMdcFilter`가 `traceId`, `usrId`, `uri`, `method`를 넣고 요청 종료 시 비운다. `SqlIdInterceptor`는 쿼리 오류 시 `sqlId`(`{매퍼명}.{쿼리ID}`)를, `GlobalExceptionHandler`는 `errCd`를 넣는다.
- 에러 로그 필드: `logDttm`, `errMsg`, `level`, `logger`, `thread`, `traceId`, `usrId`, `sqlId`, `errCd`, `uri`, `method`, `appNm`, `sysmClsCd`, `stackTrace`.

### FR-07. SYLO030 시스템로그관리 (구현됨)
- `GET /api/v1/system-logs?stDt&endDt&sysmClsCd&usrClsCd&usrId&usrNm`: 기간(최대 31일)의 에러 로그 파일만 읽어 조건으로 거르고, 최근 순 최대 1,000건을 반환한다 (`totalCount`, `truncated` 포함, 스택트레이스 제외).
- `GET /api/v1/system-logs/{logKey}`: `{yyyyMMdd}-{파일순번}-{줄번호}`로 해당 줄만 읽어 스택트레이스까지 반환한다. 키 형식을 정규식으로 검사해 경로 조작을 막는다.
- 사용자명·사용자유형·부서는 `TB_USR_M`에서 조인한다. 사용자명·사용자유형 조건은 조인 후 거른다.
- 로그는 **DB에 적재하지 않고 파일에서 직접 읽는다** (결정 사항).

### FR-10. 알림 센터 (SSE, 구현됨)
상단 GNB 벨 버튼(`AlarmCenter.vue`)에 미확인 알람 건수와 목록을 실시간으로 표시한다. 알람 원본은 `TB_ALRM_L`이고, SSE는 접속 중인 사용자에게 바로 보내는 통로로만 쓴다.

| Method | Endpoint | 설명 |
|---|---|---|
| GET | `/api/v1/alarms/stream` | SSE 연결 (`text/event-stream`). 이벤트 `count`(미확인 건수, 연결 직후·확인 처리 후), `alarm`(새 알람 1건) |
| GET | `/api/v1/alarms` | 최근 알람 50건 |
| GET | `/api/v1/alarms/unconfirmed-count` | 미확인 건수 |
| PATCH | `/api/v1/alarms/{alrmSn}` | 1건 확인 (`{"cfmYn":"Y"}`) |
| PATCH | `/api/v1/alarms` | 미확인 전체 확인 (`{"cfmYn":"Y"}`) |
| POST | `/api/v1/alarms` | 알람 발송 (관리자 전용, `ROLE_ADMIN`) |

- **인증:** 브라우저 `EventSource`는 헤더를 보낼 수 없어 frontend는 `@microsoft/fetch-event-source`로 `Authorization: Bearer` 헤더를 그대로 보낸다 (토큰을 URL에 넣지 않는다).
- **발송:** 업무 코드는 `AlarmService.saveAlarm(request, usrId)`를 호출한다. DB 등록 후 **커밋되면** Redis 채널 `coder:sse`로 발행하고(`SsePublisher`), 모든 backend 인스턴스가 받아 자기에게 접속한 수신자에게만 보낸다(`SseEmitterRegistry`). 그래서 서버를 여러 대로 늘려도 동작한다. Redis가 안 되면 현재 인스턴스에만 보내고, 놓친 알람은 다음 조회 때 표시된다.
- **다른 프로세스(coder-batch 등)에서 발송:** `TB_ALRM_L`에 등록한 뒤 `coder:sse` 채널에 `{"usrId":"수신자ID","eventName":"alarm","data":{AlarmResponse 필드}}`를 발행한다. coder-batch는 배치 실패 시 `TB_DSPT_ALRM_RCVR_R`에 등록된 사용자에게 이 방식으로 보낸다 (`workspace/coder-batch/README.md` "실패 알람").
- **연결 관리:** 연결 유지 30분(지나면 화면이 다시 연결), heartbeat 25초, 사용자당 최대 10개 연결(초과 시 가장 오래된 연결 종료). 끊긴 연결은 DEBUG로만 남긴다 (`SseExceptionHandler`).
- **보안 설정:** SseEmitter 응답의 ASYNC 디스패치는 최초 요청에서 인가했으므로 `OAuth2SecurityConfig`에서 `DispatcherType.ASYNC`를 허용한다.
- **프록시:** nginx 등을 앞에 두면 `proxy_buffering off`, `proxy_read_timeout`을 heartbeat보다 길게 (backend가 `X-Accel-Buffering: no` 헤더도 보낸다). HTTP/1.1이면 브라우저가 도메인당 연결을 6개로 제한하므로 운영은 HTTP/2를 권장한다.
- **후속:** 오래된 알람 정리 배치 (보관 기간 결정 필요), 업무 이벤트별 발송 연동.

### FR-09. 시스템관리 화면 API (미구현 - frontend 목업 교체 대상)

| 화면 | 화면명 | 엔드포인트 (frontend 목업 주석 기준) | 대상 테이블 |
|---|---|---|---|
| SYPG010 | 프로그램관리 | `/api/programs` → `/api/v1/programs` | TB_PRGM_M, TB_PRGM_CMPN_M |
| SYPG020 | 메뉴관리 | `/api/menus` → `/api/v1/menus` | TB_MENU_M |
| SYAU010 | 사용자권한관리 | `/api/v1/user-auths` (+ 비밀번호 초기화, 사용자ID 중복확인) | TB_USR_M, TB_USR_MENU_R, TB_USR_CMPN_R |
| SYAU020 | 그룹권한관리 | `/api/auth-groups` → `/api/v1/auth-groups` | TB_AUTH_GRP_M, TB_AUTH_GRP_MENU_R, TB_AUTH_GRP_CMPN_R |
| SYAU030 | 그룹별사용자관리 | `/api/v1/auth-groups/user-counts`, `/api/v1/auth-groups/{usrGrpCd}/users` (조회·저장) | TB_AUTH_GRP_M, TB_USR_AUTH_GRP_R, TB_USR_M |
| MSBS010 | 코드관리 | `GET/POST /api/v1/common-codes`, `GET /api/v1/common-codes/{wrkMdulCd}/{commCtgCd}/details` | TB_COMM_CD_M, TB_COMM_CD_D |
| SYLK050 | 배치관리 | `coder-batch` 관리 API 호출 (:8081) | TB_DSPT_JOB_M, TB_DSPT_EXEC_L |

공통 요구사항
- URL은 kebab-case 복수형 리소스로 통일한다 (`/api/menus`처럼 `v1`이 빠진 목업 주석은 `/api/v1/...`로 맞춘다).
- 저장은 `POST`(또는 `PUT`) 한 번에 변경 행 목록(`I`/`U`/`D`)을 받아 한 트랜잭션으로 처리하고, 시스템 컬럼(`FIRS_REGR_ID`/`FINA_REGR_ID` = 로그인 사용자)을 채운다.
- 권한: 화면·컴포넌트 권한(`TB_*_MENU_R`, `TB_*_CMPN_R`)을 API에서도 검사한다 (현재는 로그인만 확인).

---

## 7. 데이터 설계

- DDL은 `schema/table/*.sql`에서 관리하고, 명명은 `CLAUDE.md` Backend 5장(DB 표준)을 따른다.
- `coder-backend`가 직접 읽고 쓰는 테이블:

| 영역 | 테이블 |
|---|---|
| 사용자·권한 | TB_USR_M, TB_USR_AUTH_GRP_R, TB_USR_MENU_R, TB_USR_CMPN_R, TB_AUTH_GRP_M, TB_AUTH_GRP_MENU_R, TB_AUTH_GRP_CMPN_R |
| 메뉴·프로그램 | TB_MENU_M, TB_PRGM_M, TB_PRGM_CMPN_M, TB_PRGM_FAV_L |
| 알림 | TB_ALRM_L (알람유형 공통코드 SY/ALRM_TYPE_CD) |
| 공통코드·첨부 | TB_COMM_CD_M, TB_COMM_CD_D, TB_APND_FILE_M |

- 스키마에 아직 없는 마스터: 점(설계서 MS0020C), 사원(MS0210M), 부서. 화면은 임시 옵션 목록으로 대신하고 있다.
- 공통코드 테이블은 있으나 **데이터가 비어 있다**. SY002(사용자유형), SY003(시스템유형) 등은 frontend 상수로 대신하고 있다.

---

## 8. API 명세 (구현됨)

| Method | Endpoint | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/ping` | 불필요 | 헬스체크 |
| POST | `/api/v1/auth/login` | 불필요 | 로그인, JWT 발급 |
| GET | `/api/v1/users/profile` | 필요 | 로그인 사용자 프로필 |
| GET | `/api/v1/users` | 필요 | 사용자 목록 (관리용 임시, 권한 제한 필요) |
| GET | `/api/v1/messages` | 불필요 | 다국어 메시지 전체 |
| GET | `/api/v1/favorites` | 필요 | 즐겨찾기 메뉴 |
| GET | `/api/v1/alarms/stream` | 필요 | 알림 센터 SSE (FR-10) |
| GET/POST/PATCH | `/api/v1/alarms` | 필요 | 알람 목록·발송(관리자)·전체 확인 |
| GET | `/api/v1/alarms/unconfirmed-count` | 필요 | 미확인 알람 건수 |
| PATCH | `/api/v1/alarms/{alrmSn}` | 필요 | 알람 1건 확인 |
| GET | `/api/v1/system-logs` | 필요 | SYLO030 목록 |
| GET | `/api/v1/system-logs/{logKey}` | 필요 | SYLO030 상세 |
| GET | `/actuator/health`, `/swagger-ui.html` | 불필요 | 운영·문서 |

- 인증 불필요 URL은 `application.yml`의 `coder.security.permit-urls`에서 관리한다.

---

## 9. 설정 (application.yml 주요 항목)

| 키 | 값 / 설명 |
|---|---|
| `server.port` | 8080 |
| `coder.database.*`, `spring.datasource.hikari.*` | PostgreSQL 접속. 비밀번호는 `CODER_DB_PASSWORD` |
| `coder.mybatis.mapper-locations` | `classpath*:mapper/**/*.xml` |
| `coder.redis.*` | localhost:6379 |
| `coder.security.jwt.*` | HS256 비밀키, 만료 86400초, issuer `coder-auth` |
| `coder.security.permit-urls` | 인증 불필요 URL |
| `coder.message.*` | `messages/messages`, 기본 언어 ko |
| `coder.log.sysm-cls-cd` | 에러 로그의 시스템유형 (SY003, 기본 `01` ERP) |
| `coder.sse.*` | 알림 SSE: `timeout-ms` 1800000, `heartbeat-interval-ms` 25000, `max-emitters-per-user` 10 |
| `LOG_PATH` (환경변수) | 로그 폴더. SYLO030도 같은 값을 읽는다 |

---

## 10. 개발 표준

`CLAUDE.md`를 기준으로 하며, 요점은 다음과 같다.
- **명명:** Controller/Service는 `get`/`save`/`delete`, Mapper는 `select`/`insert`/`update`/`delete`, DTO는 `{도메인}Request`/`{도메인}Response`
- **주석:** 클래스·메서드 Javadoc (7.1/7.2), 매퍼 XML 헤더·쿼리 주석·SQL 주석 `/* {매퍼명}.{쿼리ID} 설명 */` (7.3)
- **메시지:** 사용자에게 보이는 문장은 메시지 키로 던진다 (`throw new BusinessException("cm.validate.period.max", 31)`). 3개 번들에 함께 추가한다.
- **로그:** 예외를 잡아 로그를 남길 때는 예외 객체를 함께 넘긴다. 예상된 상황은 WARN/INFO로 남겨 SYLO030에 섞이지 않게 한다.
- **테스트:** 새 API는 Controller 통합 테스트를 둔다 (현재 `AuthIntegrationTest`).

---

## 11. 비기능 요구사항

| 구분 | 요구사항 |
|---|---|
| 보안 | 비밀번호 해시 저장·검증, JWT 비밀키·DB 비밀번호는 환경변수/KMS로 주입 (yml 기본값 제거), 운영 환경 Swagger 비공개, 화면·컴포넌트 권한 서버 검사 |
| 성능 | 일반 조회 1초 이내. 대량 이력은 기간·건수 제한과 서버 페이징 |
| 추적성 | 모든 응답에 `X-Trace-Id`. 오류는 SYLO030에서 추적ID·사용자·SQL ID로 찾을 수 있어야 한다 |
| 가용성 | 다중 인스턴스 배포 시 에러 로그 파일이 서버별로 생기므로 `LOG_PATH`를 공유 스토리지로 맞춘다 (14장) |
| 운영 | 로그 보관 30일(전체)/90일(에러), Actuator 헬스체크 |

---

## 12. 일정 (안)

| 단계 | 내용 |
|---|---|
| Phase 1 (완료) | 공통 프레임, 메시지 이관, 로그 표준, SYLO030 |
| Phase 2 | 로그인 보안 강화(FR-01), 공통코드 API, 메뉴 트리 API |
| Phase 3 | 메뉴·프로그램·권한 화면 API (SYPG010/020, SYAU010/020/030) |

---

## 13. 결정 필요 사항 (Open Issues)

| # | 내용 |
|---|---|
| 1 | 인증 실패(`cm.error.unauthorized`)가 400으로 응답된다. 업무 예외에 HTTP 상태를 지정할 수 있게 할지(401), 인증 전용 예외를 둘지 |
| 2 | 없는 URL이 `NoResourceFoundException`으로 500(C004)이 되고 에러 로그에도 남는다. 404(C003)로 처리할지 |
| 3 | 업무 예외도 ERROR로 남아 SYLO030에 표시된다 (설계서 예시와 일치). 업무 예외를 WARN으로 낮출지 |
| 4 | 사용자유형 코드: DB 사용자는 `A`/`O`/`V`, 화면 코드(SY002)는 `1`~`9`. 어느 쪽으로 맞출지 |
| 5 | 공통코드(SY002, SY003 등) 데이터 등록과 공통코드 조회 API 제공 시점 |
| 6 | 점·사원·부서 마스터 테이블의 소유 업무(MS)와 생성 시점 |
| 7 | 다중 인스턴스에서 에러 로그 파일 조회 방식 (공유 스토리지 / 서버별 조회 / 수집기) |
| 8 | Redis 사용처 (세션·권한 캐시 등) 또는 의존성 제거 |
| 9 | `/api/v1/users`(사용자 목록) 유지 여부와 권한 제한 |

---

## 14. 구현 메모

- `workspace/coder-backend/README.md`는 삭제된 API(모듈, 캐시, 시스템 상태)를 안내하고 있어 현행화가 필요하다.
- 공통 `GlobalExceptionHandler`(coder-core)를 고치면 `coder-batch`도 영향을 받는다. 수정 후 두 앱 모두 컴파일을 확인한다.
- `SqlIdInterceptor`는 core의 `MyBatisConfig`가 `AuditInterceptor`만 등록하므로 `SqlIdInterceptorConfig`에서 `SqlSessionFactory` 생성 후 추가한다.
- 에러 로그 파일은 logback `SizeAndTimeBasedRollingPolicy`에 `<file>` 없이 설정해 처음부터 일자 파일로 쓴다. 줄번호가 바뀌지 않으므로 `logKey`(일자-순번-줄번호)를 상세 조회 키로 쓸 수 있다.
