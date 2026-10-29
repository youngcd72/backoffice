# CODER 기본 API Framework (coder-api) PRD

| 항목 | 내용 |
|---|---|
| 문서명 | CODER 기본 API Framework(`coder-api`) 제품 요구사항 정의서 |
| 버전 | v0.2 (Phase 1 구현) |
| 작성일 | 2026-10-26 |
| 구현 | Phase 1 + 호출 한도·캐시 (`workspace/coder-api`, 2026-10-27) |
| 대상 독자 | 백엔드 개발자, 아키텍트, 연계 담당자, 운영 담당자 |
| 관련 문서 | 루트 `CLAUDE.md` (네이밍·주석·메시지·로그 규칙), `documents/backend/PRD.md`, `documents/batch/PRD.md`, `schema/dictionary/word-abbreviations.md` 1-14 "대외 연계" |

### 변경 이력

| 버전 | 내용 |
|---|---|
| v0.1 | 최초 작성. 대외 연계 API 서버의 범위, 인증·호출한도·호출이력 공통 기능, 데이터 설계(안), 결정 필요 사항 정리 |
| v0.2 | `workspace/coder-api` 구현 (토큰·JWKS, 공통 필터, 호출 한도, 호출 이력, 샘플 API, DDL). 13장 결정 사항에 구현 기본값 표시 |

---

## 1. 배경 및 목적

### 1.1 배경
- CODER는 현재 두 개의 Spring Boot 애플리케이션으로 구성된다. 둘 다 `coder-core`를 Gradle Composite Build로 포함한다.

| 애플리케이션 | 포트 | 사용자 | 역할 |
|---|---|---|---|
| `coder-backend` | 8080 | 내부 직원·운영자 | 관리 콘솔(`coder-frontend`) REST API |
| `coder-batch` | 8081 | 운영자 | Spring Batch + Quartz 배치 실행 |
| **`coder-api`** (이 문서) | 8082 | **외부 시스템** (연계기관, 타 시스템, 앱) | 대외 연계 REST API |

- Backoffice 프로젝트는 거의 항상 **외부 시스템과 데이터를 주고받는 API**(거래처·기관 연계, 그룹사 시스템 연동, 모바일 앱 등)가 필요하다. 이 API를 관리 콘솔용 `coder-backend`에 함께 두면 다음 문제가 생긴다.
  - 사용자 로그인(JWT, 직원 계정)과 시스템 간 인증(클라이언트 키)이 한 보안 설정에 섞인다.
  - 외부 호출량이 관리 콘솔 응답 성능에 영향을 준다. 외부 노출 범위(방화벽·도메인)도 분리하기 어렵다.
  - 연계기관별 호출 한도, IP 제한, 호출 이력처럼 연계에만 필요한 기능이 프로젝트마다 새로 만들어진다.
- 이전에 대외 연계(연계기관·키·인터페이스·이력·집계, 시스템인증서) 테이블을 설계했다가 삭제한 이력이 있다. 그때 등록한 단어는 단어약어 사전 1-14에 남아 있어 이 문서의 데이터 설계에 그대로 쓴다.
- `coder-core-kms`(암호화·서명 키 관리)는 만들어 두었지만 사용처가 없다. 클라이언트 비밀키 암호화와 토큰 서명에 이 모듈을 쓴다.

### 1.2 목적
Backoffice 프로젝트가 대외 연계 API를 만들 때 **업무 API만 추가하면 되도록** 공통 기반을 제공한다.
- 연계기관·클라이언트 등록과 키 발급·폐기
- 시스템 간 인증 (OAuth2 Client Credentials, KMS 서명 토큰)
- IP 허용, 인터페이스별 접근 권한, 호출 한도 (분당·일·동시)
- 호출 이력(마스킹 포함)과 일별 집계, 추적ID로 오류 추적
- 표준 응답·오류 코드, API 버전 관리, API 문서

---

## 2. 범위

### 2.1 In Scope (MVP)
1. `workspace/coder-api` Gradle 프로젝트 신규 생성 (`includeBuild('../coder-core')`)
2. 연계기관·클라이언트 키·인터페이스·권한 테이블 설계 (DDL은 `schema/table`)
3. 토큰 발급 `POST /oauth2/token` (Client Credentials), 공개키 `GET /.well-known/jwks.json`
4. 공통 필터: 추적ID → IP 허용 → 토큰 검증 → 인터페이스 권한 → 호출 한도
5. 호출 이력 비동기 기록 (요청·응답 본문은 인터페이스 설정에 따라 마스킹 후 기록)
6. 표준 응답 `ApiResponse<T>`와 연계용 오류 코드 (401 / 403 / 429)
7. 샘플 업무 API 1종 (조회형)

### 2.2 Out of Scope (후속 단계)
- 연계 관리 화면 (연계기관·키·인터페이스·이력 조회) → Phase 3, `coder-frontend` SY 화면 + `coder-backend` 관리 API
- 호출 집계 배치, 호출 이력 정리 배치 → Phase 2, `coder-batch`
- 송신(아웃바운드) 공통 클라이언트 (CODER → 외부 호출, 재시도·재전송) → Phase 3
- mTLS(시스템인증서), HMAC 요청 서명 → 연계기관 요구가 있을 때 (13장 #2)
- B2C 회원 로그인, 개발자 포털, 과금, 상용 API Gateway(Kong 등) 도입

---

## 3. 용어 정의

| 용어 | 설명 |
|---|---|
| 연계기관 | API를 호출하는 외부 주체 (거래처, 기관, 그룹사 시스템, 앱). `TB_LNK_ORG_M` |
| 채널 | 연계기관 구분. `CHNL_CLS_CD`: `B` B2B, `C` B2C, `I` 내부 시스템 |
| 클라이언트 키 | 연계기관에 발급하는 `client_id`(`KEY_ID`) + `client_secret`. 한 기관에 여러 개 발급할 수 있다 (교체 기간 중첩) |
| 인터페이스 | 외부에 공개하는 API 단위. 인터페이스ID(`INFC_ID`)로 식별하고, 권한·본문 로그·마스킹을 인터페이스별로 설정한다 |
| 액세스 토큰 | `/oauth2/token`이 발급하는 JWT. KMS 서명 키(RS256)로 서명하고 헤더 `kid`로 키 버전을 표시한다 |
| 호출 한도 | 키별 분당(`MI_CALL_LMT_CNT`)·일(`DAY_CALL_LMT_CNT`)·동시(`SIMU_RQMT_LMT_CNT`) 요청 수 제한 |
| 추적ID | 요청마다 발급하는 ID. 요청 헤더 `X-Trace-Id`가 있으면 이어받고, 응답 헤더와 호출 이력에 같은 값이 남는다 |
| 마스킹 | 호출 이력 본문에서 지정 필드(`MASK_FLD_CONT`) 값을 `***`로 바꿔 저장하는 것 |

---

## 4. 시스템 아키텍처

### 4.1 구성도

```text
 [외부 시스템] ──HTTPS──▶ (L4/WAF/LB) ──▶
 ┌──────────────────────────────── coder-api (:8082) ────────────────────────────────┐
 │ TraceIdFilter (X-Trace-Id, MDC)                                                     │
 │  → IpAllowFilter        : 키의 ALLW_IP_ADDR 확인                       → 403        │
 │  → Spring Security      : JWT(RS256) 서명·만료 검증, JWKS(kid)         → 401        │
 │  → InterfaceAuthFilter  : @ApiInterface(INFC_ID) ↔ 기관 허용 인터페이스 → 403        │
 │  → RateLimitFilter      : Redis 카운터 (분당·일·동시)                  → 429        │
 │  → Controller (kr.or.coder.api.biz.*) → Service → MyBatis                           │
 │  → CallLogRecorder      : 호출 이력 큐 → 비동기 일괄 INSERT                         │
 │ POST /oauth2/token : KEY_ID/비밀키 검증(KMS 복호화) → KMS 서명 JWT 발급              │
 └──────┬───────────────────────┬──────────────────────────┬─────────────────────────┘
        ▼                       ▼                          ▼
  PostgreSQL (coder)        Redis                     coder-core-kms
  TB_LNK_*, TB_INFC_*       호출 한도 카운터,          비밀키 암·복호화,
                            키·권한 캐시               토큰 서명 키 버전
        ▲
  coder-backend (연계 관리 API) / coder-batch (집계·이력 정리)
```

### 4.2 기술 스택

| 구분 | 내용 |
|---|---|
| 언어 / 런타임 | Java 21 |
| 프레임워크 | Spring Boot 3.3.5, Spring Security 6 (OAuth2 Resource Server, JWT **RS256**) |
| 데이터 접근 | MyBatis 3.0.3, HikariCP (`coder-core-db`) |
| DB | PostgreSQL 18 (DB `coder`, 스키마 `coder`, 계정은 13장 #6) |
| 캐시 / 카운터 | Redis (`coder-core-redis`) - 호출 한도 카운터, 키·인터페이스 권한 캐시 |
| 키 관리 | `coder-core-kms` (`KmsClient.encrypt/decrypt/sign`, `SigningKeyVersion`) |
| 문서 | SpringDoc OpenAPI (연계기관 공유용 스펙 그룹 분리, 운영 Swagger UI 비공개) |
| 로그 | Logback + `logstash-logback-encoder` (에러 로그 JSON, `coder-backend`와 같은 형식) |

### 4.3 coder-backend와 비교

| 구분 | coder-backend | coder-api |
|---|---|---|
| 호출자 | 사람 (브라우저) | 시스템 |
| 인증 | 아이디/비밀번호 로그인 → JWT(HS256, 24시간) | Client Credentials → JWT(RS256, KMS 서명, 1시간) |
| 권한 단위 | 메뉴·컴포넌트 (`TB_*_MENU_R`, `TB_*_CMPN_R`) | 인터페이스 (`TB_LNK_ORG_INFC_R`) |
| 추가 통제 | - | IP 허용, 호출 한도 |
| 이력 | 에러 로그 파일 (SYLO030) | 모든 호출을 DB에 기록 (`TB_INFC_CALL_L`) + 에러 로그 파일 |
| 메시지 언어 | 화면 언어 (`Accept-Language`) | `Accept-Language`, 기본 영어 (13장 #8) |

### 4.4 설계 원칙
- **외부 노출 최소화:** `coder-api`에는 연계기관이 호출하는 API만 둔다. 연계기관·키·인터페이스 등록 같은 관리 기능은 `coder-backend`(관리 콘솔)에 둔다.
- **비밀값은 한 번만 노출:** `client_secret`은 발급 응답에서 한 번만 보여 주고, DB에는 KMS로 암호화한 값(`ENC_KEY_VAL`)만 저장한다. 로그·호출 이력에 남기지 않는다.
- **무상태(stateless):** 인스턴스 간 공유 상태는 Redis(카운터)와 DB에만 둔다. 인스턴스를 늘려도 한도가 정확해야 한다.
- **응답 지연 최소화:** 호출 이력은 요청 처리 스레드에서 쓰지 않고 큐에 넣어 모아서 저장한다.
- **업무 API는 같은 규칙:** 명명·주석·매퍼 규칙은 `CLAUDE.md`를 그대로 따른다.

---

## 5. 프로젝트 구조

```text
workspace/coder-api/
├── build.gradle                      # coder-core-common/db/redis/security/kms, actuator, logstash-logback-encoder
├── settings.gradle                   # includeBuild('../coder-core')
└── src/main/
    ├── java/kr/or/coder/api/
    │   ├── CoderApiApplication.java
    │   ├── common/
    │   │   ├── auth/                  # 토큰 발급(TokenController), 클라이언트 검증, JWKS
    │   │   ├── filter/                # TraceIdFilter, IpAllowFilter, InterfaceAuthFilter, RateLimitFilter
    │   │   ├── calllog/               # CallLogRecorder(큐·일괄 저장), 본문 마스킹
    │   │   ├── annotation/            # @ApiInterface("INFC_ID")
    │   │   └── exception/             # ApiErrorCode, 401/403/429 응답 처리
    │   └── biz/{업무}/{기능}/          # 업무 API (controller / service / mapper / dto)
    └── resources/
        ├── application.yml, application-{local,dev,prod}.yml
        ├── logback-spring.xml
        ├── messages/messages*.properties
        └── mapper/{common|biz}/**/*Mapper.xml
```

- 업무 API 패키지는 `kr.or.coder.api.biz.{업무}.{기능}`, 매퍼 XML은 `resources/mapper/biz/{업무}/`에 둔다.
- 매퍼 인터페이스는 `coder-core-db`의 `@MapperScan("kr.or.coder.**.mapper")`로 자동 등록된다.

---

## 6. 기능 요구사항

### FR-01. 연계기관 / 클라이언트 키 관리
| 구분 | 내용 |
|---|---|
| 위치 | 관리 API는 `coder-backend` (`/api/v1/link-orgs/**`), `coder-api`는 조회만 한다 |
| 연계기관 | 등록·수정·사용중지. 채널(`CHNL_CLS_CD`), 사용기간(`USE_ST_DT`~`USE_END_DT`) |
| 키 발급 | `KEY_ID`(client_id)와 비밀키(32바이트 이상 난수, `KmsClient.generateRandom`)를 만든다. 비밀키는 `KmsClient.encrypt`로 암호화해 `ENC_KEY_VAL`에 저장하고, 원문은 **발급 응답에서 한 번만** 반환한다 |
| 키 상태 | `KEY_STAT_CD`: 사용 / 사용중지 / 폐기. 만료일시(`EXPI_DTTM`)가 지나면 사용할 수 없다. 교체할 때는 새 키를 발급해 기간을 겹친 뒤 이전 키를 폐기한다 |
| 키별 통제 | 허용 IP(`ALLW_IP_ADDR`, CIDR 여러 개), 분당·일·동시 호출 한도 |
| 이력 | 발급·상태 변경·폐기를 `TB_LNK_KEY_H`에 남긴다 (변경자, 일시, KMS 감사ID) |

### FR-02. 토큰 발급 (OAuth2 Client Credentials)
- `POST /oauth2/token` (`application/x-www-form-urlencoded`, `grant_type=client_credentials`, `client_id`, `client_secret`, 선택 `scope`)
- 검증 순서: 키 존재 → 상태·만료 → 연계기관 사용 여부·사용기간 → 허용 IP → 비밀키 일치 (KMS 복호화 후 **상수 시간 비교**)
- 발급 토큰: JWT RS256, 서명은 `KmsClient.sign`, 헤더 `kid` = `SigningKeyVersion.kid`. 클레임 `sub`=`KEY_ID`, `org`=연계기관ID, `chnl`=채널, `scope`=허용 인터페이스(요청 `scope`가 있으면 그 범위로 좁힘), 만료 기본 3600초
- 응답: `{"access_token","token_type":"Bearer","expires_in","scope"}` (OAuth2 표준 형식, `ApiResponse`로 감싸지 않음)
- 실패는 OAuth2 표준 오류(`invalid_client`, `invalid_grant`, `invalid_scope`)로 응답하고 사유를 구분하지 않는다 (키 존재 여부 노출 방지)
- 비밀키 실패가 연속되면 해당 키를 일정 시간 막는다 (기준 13장 #5)

### FR-03. 토큰 서명 키 / JWKS
- `GET /.well-known/jwks.json` (인증 불필요): 사용 중인 서명 키 버전과 직전 버전의 공개키를 JWKS로 반환한다. 연계기관이 토큰을 직접 검증할 필요는 없지만 표준으로 제공한다.
- 서명 키 교체: `KmsClient.createSigningKeyVersion`으로 새 버전을 만들고, 이전 버전은 발급 토큰의 최대 만료 시간 동안 검증용으로 유지한다.

### FR-04. 요청 인증 / IP 허용 / 인터페이스 권한
- 모든 업무 API는 `Authorization: Bearer {access_token}`이 필요하다. 서명·만료·발급자 검증 실패는 401.
- IP 허용: 요청 IP가 키의 `ALLW_IP_ADDR`에 없으면 403. 프록시 뒤에서는 신뢰하는 프록시의 `X-Forwarded-For`만 인정한다 (`coder.api.trusted-proxies`).
- 인터페이스 권한: 각 업무 API는 `@ApiInterface("INFC_ID")`로 인터페이스를 선언한다. 토큰의 `scope`에 그 인터페이스가 없거나 `TB_LNK_ORG_INFC_R`에서 허용되지 않았으면 403. 선언이 없는 업무 API는 기동 시 오류로 막는다.
- 키·연계기관·권한 정보는 Redis에 캐시하고(기본 5분), `coder-backend`에서 변경하면 캐시를 지운다 (Redis 채널 `coder:api:cache-evict`).

### FR-05. 호출 한도 (Rate Limit)
| 한도 | 기준 | 방식 |
|---|---|---|
| 분당 | `MI_CALL_LMT_CNT` | Redis 고정 윈도 카운터 `api:rl:{KEY_ID}:mi:{yyyyMMddHHmm}` (TTL 2분) |
| 일 | `DAY_CALL_LMT_CNT` | `api:rl:{KEY_ID}:day:{yyyyMMdd}` (TTL 2일) |
| 동시 | `SIMU_RQMT_LMT_CNT` | 요청 시작 시 증가, 종료 시 감소 (비정상 종료 대비 TTL) |

- 값이 비어 있으면 제한하지 않는다. 인터페이스별 한도는 MVP에서 제외한다.
- 초과 시 429와 `Retry-After` 헤더를 보낸다. 모든 응답에 `X-RateLimit-Limit`, `X-RateLimit-Remaining` 헤더를 준다.
- Redis 장애 시 동작(허용 / 거부)은 13장 #4.

### FR-06. 호출 이력
- 모든 업무 API 호출(인증·권한·한도 실패 포함)을 `TB_INFC_CALL_L`에 1건씩 남긴다: 추적ID, 키, 연계기관, 인터페이스, HTTP 메서드·URL·상태코드, 클라이언트 IP, 요청·응답 일시, 소요시간, 오류코드.
- 요청·응답 본문은 인터페이스의 `BODY_LOG_YN='Y'`일 때만 남긴다. `MASK_FLD_CONT`(JSON 경로 목록)에 지정한 필드는 `***`로 바꾸고, 최대 길이(기본 4,000자)를 넘으면 자른다. 토큰·비밀키·`Authorization` 헤더는 설정과 관계없이 남기지 않는다.
- 저장은 메모리 큐 → 일정 건수·시간마다 일괄 INSERT로 한다. 큐가 가득 차면 이력을 버리고 WARN 로그를 남긴다 (응답을 막지 않는다).
- 보관 기간이 지난 이력은 `coder-batch` 정리 배치가 삭제한다 (보관 기간 13장 #7).

### FR-07. 호출 집계
- `coder-batch` 일 배치가 전일 `TB_INFC_CALL_L`을 연계기관·인터페이스별로 집계해 `TB_INFC_CALL_S`에 저장한다: 호출·성공·실패 건수, 평균·최대 소요시간.
- 관리 콘솔에서 일별 사용량 조회에 쓴다 (Phase 3).

### FR-08. 표준 응답 / 오류
- 업무 API 응답은 `coder-core-common`의 `ApiResponse<T>` (`success`, `code`, `message`, `data`, `timestamp`)를 쓴다. 토큰 발급(FR-02)만 OAuth2 표준 형식이다.
- 모든 응답에 `X-Trace-Id` 헤더를 준다. 연계기관이 문의할 때 이 값으로 이력을 찾는다.

| 상황 | HTTP | code (안) | 메시지 키 (안) |
|---|---|---|---|
| 토큰 없음·만료·서명 오류 | 401 | `A001` | `sy.api.token.invalid` |
| 허용되지 않은 IP | 403 | `A002` | `sy.api.ip.not.allowed` |
| 허용되지 않은 인터페이스 | 403 | `A003` | `sy.api.interface.not.allowed` |
| 호출 한도 초과 | 429 | `A004` | `sy.api.rate.limit.exceeded` |
| 업무 예외 | 400 | 메시지 키 | 업무별 |
| 요청 검증 실패 | 400 | `C001` | 공통 |
| 그 외 | 500 | `C004` | 공통 (상세 원인은 응답에 넣지 않음) |

### FR-09. API 버전 / 문서
- URL은 `/api/v{n}/{리소스}` (kebab-case, 복수형 명사, `CLAUDE.md` Backend 3장). 호환되지 않는 변경은 새 버전으로 추가하고, 이전 버전은 공지 후 일정 기간 유지한다.
- SpringDoc으로 연계기관 공유용 OpenAPI 스펙을 만든다. 운영에서는 Swagger UI를 끄고, 스펙 파일만 전달한다.

### FR-10. 운영
- `GET /api/ping`, `GET /actuator/health` (인증 불필요, `db`·`redis` 상태 포함).
- 에러 로그는 `coder-backend`와 같은 JSON 형식(`coder-api-error.{yyyy-MM-dd}.{N}.log`)으로 남기고, `sysmClsCd`로 시스템을 구분해 SYLO030에서 함께 조회할 수 있게 한다.

---

## 7. 데이터 설계 (안)

- 명명은 `CLAUDE.md` Backend 5장과 단어약어 사전을 따른다. 아래 컬럼은 사전 1-14 "대외 연계" 단어를 쓴 안이며, 사전에 없는 단어는 13장 #9에 모았다.
- 모든 테이블에 시스템 컬럼(`FIRS_REGR_ID`, `FIRS_REG_DT`, `FINA_REGR_ID`, `FINA_REG_DT`)을 둔다 (이력·내역 테이블은 최초 등록 컬럼만).

| 테이블 | 논리명 | 주요 컬럼 |
|---|---|---|
| `TB_LNK_ORG_M` | 연계기관기본 | `LNK_ORG_ID`(PK), `LNK_ORG_NM`, `CHNL_CLS_CD`, `USE_ST_DT`, `USE_END_DT`, `USE_YN`, `RMK` |
| `TB_LNK_KEY_M` | 연계키기본 | `KEY_ID`(PK, client_id), `LNK_ORG_ID`, `ENC_KEY_VAL`, `KMS_KEY_ID`, `KEY_STAT_CD`, `ISSU_DTTM`, `EXPI_DTTM`, `ALLW_IP_ADDR`, `MI_CALL_LMT_CNT`, `DAY_CALL_LMT_CNT`, `SIMU_RQMT_LMT_CNT` |
| `TB_LNK_KEY_H` | 연계키이력 | `KEY_HIST_SN`(PK), `KEY_ID`, `KEY_STAT_CD`, `CHG_CONT`, `KMS_AUDIT_ID` |
| `TB_INFC_M` | 인터페이스기본 | `INFC_ID`(PK), `INFC_NM`, `HTTP_METH_NM`, `URL`, `BODY_LOG_YN`, `MASK_FLD_CONT`, `USE_YN` |
| `TB_LNK_ORG_INFC_R` | 연계기관인터페이스관계 | `LNK_ORG_ID` + `INFC_ID`(PK), `USE_YN` |
| `TB_INFC_CALL_L` | 인터페이스호출내역 | `CALL_SN`(PK), `TRC_ID`, `KEY_ID`, `LNK_ORG_ID`, `INFC_ID`, `HTTP_METH_NM`, `URL`, `HTTP_STAT_CD`, `CLNT_IP_ADDR`, `RQMT_DTTM`, `RSPN_DTTM`, (소요시간), `ERR_CD`, `RQMT_CONT`, `RSPN_CONT` |
| `TB_INFC_CALL_S` | 인터페이스호출집계 | `STD_DT`(기준일자) + `LNK_ORG_ID` + `INFC_ID`(PK), `CALL_CNT`, `SUCC_CNT`, `FAIL_CNT`, (평균·최대 소요시간) |

- `TB_INFC_CALL_L`은 건수가 많으므로 `RQMT_DTTM` 기준 월 파티션을 둔다. 조회 인덱스는 (`LNK_ORG_ID`, `RQMT_DTTM`), (`TRC_ID`).
- 공통코드 (`WRK_MDUL_CD='SY'`): 채널구분(`CHNL_CLS_CD`), 키상태(`KEY_STAT_CD`).
- 인터페이스ID 형식 (안): `IF` + 업무약어 2자리 + 순번 4자리 (`IFSY0001`, `IFMS0001`).

---

## 8. API 명세 (안)

### 8.1 coder-api (외부 공개)

| Method | Endpoint | 인증 | 설명 |
|---|---|---|---|
| POST | `/oauth2/token` | client_id/secret | 액세스 토큰 발급 (FR-02) |
| GET | `/.well-known/jwks.json` | 불필요 | 토큰 서명 공개키 (FR-03) |
| GET | `/api/ping` | 불필요 | 헬스체크 |
| GET | `/actuator/health` | 불필요 (내부망 제한) | 상태 확인 |
| GET | `/api/v1/samples/{id}` | Bearer | 샘플 업무 API (`@ApiInterface("IFSY0001")`) |

### 8.2 coder-backend (관리, Phase 2~3)

| Method | Endpoint | 설명 |
|---|---|---|
| GET/POST | `/api/v1/link-orgs` | 연계기관 목록·저장 |
| GET/POST | `/api/v1/link-orgs/{lnkOrgId}/keys` | 키 목록·발급 (발급 응답에만 비밀키 원문) |
| PATCH | `/api/v1/link-orgs/{lnkOrgId}/keys/{keyId}` | 키 상태 변경 (사용중지·폐기), 허용 IP·한도 수정 |
| GET/POST | `/api/v1/interfaces` | 인터페이스 목록·저장 |
| GET/POST | `/api/v1/link-orgs/{lnkOrgId}/interfaces` | 기관별 허용 인터페이스 |
| GET | `/api/v1/interface-calls` | 호출 이력 조회 (기간·기관·인터페이스·추적ID) |
| GET | `/api/v1/interface-call-stats` | 일별 호출 집계 |

---

## 9. 설정 (application.yml 안)

| 키 | 값 / 설명 |
|---|---|
| `server.port` | 8082 |
| `coder.database.*`, `spring.datasource.hikari.*` | PostgreSQL 접속. 비밀번호는 `CODER_API_DB_PASSWORD` |
| `coder.redis.*` | 호출 한도·캐시용 Redis |
| `coder.kms.provider` | `local`(개발, `coder.kms.local.master-key`) / 운영은 클라우드 KMS 구현 |
| `coder.api.token.*` | `issuer`, `expires-in`(3600), `signing-key-id` |
| `coder.api.trusted-proxies` | `X-Forwarded-For`를 믿을 프록시 IP 목록 |
| `coder.api.rate-limit.redis-failure-mode` | Redis 장애 시 `allow` / `deny` (13장 #4) |
| `coder.api.call-log.*` | `queue-capacity`(10000), `batch-size`(500), `flush-interval-ms`(1000), `body-max-length`(4000) |
| `coder.api.cache.ttl-seconds` | 키·권한 캐시 TTL (300) |
| `coder.log.sysm-cls-cd` | 에러 로그 시스템유형 (SY003, `coder-api`용 코드 신규 등록 필요) |

---

## 10. 개발 표준 (업무 API 추가 방법)

1. `TB_INFC_M`에 인터페이스를 등록하고 인터페이스ID를 받는다 (본문 로그 여부·마스킹 필드 포함).
2. `kr.or.coder.api.biz.{업무}.{기능}`에 Controller / Service / Mapper / DTO를 만들고, Controller 메서드에 `@ApiInterface("IF....")`를 붙인다.
3. 이 API를 쓸 연계기관에 `TB_LNK_ORG_INFC_R` 권한을 준다.
4. 응답은 `ApiResponse<T>`, 업무 오류는 `BusinessException(메시지키)`로 던진다. 메시지는 `coder-api` 메시지 번들 3개 파일에 함께 추가한다.
5. 명명·주석·매퍼 XML 규칙은 `CLAUDE.md`를 따른다. 개인정보 필드는 마스킹 대상(`MASK_FLD_CONT`)에 반드시 넣는다.
6. Controller 통합 테스트에서 토큰 없음(401), 권한 없음(403), 정상(200)을 확인한다.

---

## 11. 비기능 요구사항

| 구분 | 요구사항 |
|---|---|
| 보안 | HTTPS 전용. 비밀키는 KMS 암호화 저장·발급 시 1회 노출. 토큰·비밀키·개인정보는 로그와 이력에 남기지 않음(마스킹). 운영 Swagger UI 비공개. 비밀번호·KMS 마스터키는 환경변수/KMS로만 주입 |
| 성능 | 공통 필터(인증·IP·권한·한도) 처리 시간 p95 20ms 이내. 호출 이력 저장은 응답 시간에 포함하지 않음 |
| 확장성 | 무상태. 인스턴스를 늘려도 호출 한도가 Redis로 정확히 적용됨 |
| 추적성 | 모든 응답에 `X-Trace-Id`. 추적ID로 호출 이력과 에러 로그를 함께 찾을 수 있어야 함 |
| 가용성 | Redis·KMS 장애 시 동작 기준을 설정으로 명시 (13장 #4) |
| 운영 | 호출 이력 보관 기간 후 자동 삭제, 일별 집계, Actuator 헬스체크 |

---

## 12. 일정 (안)

| 단계 | 내용 |
|---|---|
| Phase 1 | `coder-api` 골격, 테이블 DDL, 토큰 발급·JWKS(KMS), 인증·IP·인터페이스 권한 필터, 호출 이력, 샘플 API |
| Phase 2 | 호출 한도(Redis), 키·권한 캐시, 집계·이력 정리 배치(`coder-batch`), `coder-backend` 연계 관리 API |
| Phase 3 | 관리 콘솔 화면 (연계기관·키·인터페이스·호출 이력·사용량), 송신(아웃바운드) 공통 클라이언트 (재시도·재전송, 송신 이력) |
| Phase 4 | 필요 시 mTLS(시스템인증서), HMAC 요청 서명, 인터페이스별 한도 |

---

## 13. 결정 필요 사항 (Open Issues)

| # | 내용 |
|---|---|
| 1 | 인증 방식: OAuth2 Client Credentials(이 문서 안)로 확정할지, 단순 API Key 헤더 방식도 함께 허용할지 — **구현: Client Credentials만** (client_secret_post / client_secret_basic) |
| 2 | B2B 연계기관이 mTLS나 HMAC 요청 서명을 요구할 경우 지원 범위와 시점. HMAC을 지원하려면 비밀키를 해시가 아닌 KMS 암호화로 보관해야 함 (이 문서 안은 암호화 보관) |
| 3 | 운영 KMS 제공자 (AWS KMS / Azure Key Vault / 사내 HSM 등)와 `KmsClient` 구현 시점. 현재는 `LocalKmsClient`만 있음 |
| 4 | Redis 장애 시 호출 한도 처리: 허용(fail-open, 가용성 우선) / 거부(fail-closed, 보호 우선) — **구현: 설정 `redis-failure-mode`, 기본 allow (deny면 503 A005)** |
| 5 | 토큰 발급 비밀키 오류 시 잠금 기준 (연속 실패 횟수, 잠금 시간) — **구현 기본값: 10분 안에 5회 실패 시 10분 잠금 (설정)** |
| 6 | DB 계정: `coder` 계정을 같이 쓸지, `batch`처럼 전용 계정(`api`)과 테이블별 권한을 둘지 — **구현: `coder` 계정 공유 (분리 시 설정만 변경)** |
| 7 | 호출 이력 보관 기간 (예: 90일)과 본문 로그 최대 길이 |
| 8 | 외부 응답 메시지 언어 기본값 (영어 / 한국어)과 메시지 번들 위치 (`coder-api` 자체 번들 / `coder-backend`와 공유) — **구현: 기본 영어, `coder-api` 자체 번들** |
| 9 | 사전 미등록 단어 등록: 소요(시간), 최대, 버전, 범위(scope), 변경(`CHG`는 사전상 "정정"이라 `TB_LNK_KEY_H.CHG_CONT`를 쓸지 검토). 등록 후 7장 컬럼명 확정 — **구현: 사전 1-14에 밀리초 `MS`, 최대 `MAX` 등록(검토 필요). 소요시간 `ELAPSED_MS`, 최대·평균 `MAX_ELAPSED_MS`·`AVG_ELAPSED_MS`, `CHG_CONT` 유지. 버전·범위는 컬럼에 쓰지 않아 미등록** |
| 10 | 인터페이스ID 형식 (`IF` + 업무약어 + 순번 안)과 채번 방식 — **구현: 안대로 (`IFSY0001`), 채번은 수동 등록** |
| 11 | 송신(아웃바운드) 이력을 `TB_INFC_CALL_L`에 송수신구분 컬럼으로 함께 둘지, 별도 테이블로 둘지 |
