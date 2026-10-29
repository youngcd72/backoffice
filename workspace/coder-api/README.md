# CODER API Application

`coder-api`는 외부 시스템(연계기관, 타 시스템, 앱)이 호출하는 **대외 연계 REST API 서버**입니다. `coder-backend`와 같은 기술 기반(Spring Boot 3.3.5, Java 21, `coder-core` Composite Build, MyBatis, PostgreSQL)에서 동작하고, 업무 API만 추가하면 인증·IP 허용·인터페이스 권한·호출 한도·호출 이력이 공통으로 적용됩니다.

요구사항 정의: [documents/api/PRD.md](../../documents/api/PRD.md)

---

## 🛠️ 기술 스택

- **Java** 21 / **Spring Boot** 3.3.5 / **Gradle** (Composite Build: `includeBuild('../coder-core')`)
- **Spring Security 6** OAuth2 Resource Server - 액세스 토큰 JWT **RS256**, 서명은 `coder-core-kms`(`KmsClient.sign`)
- **MyBatis** 3.0.3 (`coder-core-db`), **Redis** (`coder-core-redis`) - 호출 한도 카운터, 비밀키 실패 잠금, 캐시 삭제 채널
- **SpringDoc OpenAPI** - 연계기관 공유용 그룹 `partner` (운영에서는 UI·문서 비공개)
- **Logback** + `logstash-logback-encoder` - 에러 로그 JSON (`coder-backend`와 같은 형식, `sysmClsCd` 02)

---

## 📁 디렉터리 구조

```text
coder-api/
├── src/main/java/kr/or/coder/api/
│   ├── CoderApiApplication.java
│   ├── common/
│   │   ├── config/        # ApiProperties(coder.api.*), SecurityConfig(필터 체인, JwtDecoder)
│   │   ├── auth/          # TokenController(/oauth2/token, JWKS), TokenService, SigningKeyManager(KMS 서명키),
│   │   │                  # ClientKeyIssueService/Runner(키 발급), mapper/ClientKeyMapper
│   │   ├── client/        # LinkClient·LinkInterface, LinkClientCache(메모리 캐시 + Redis 삭제 채널)
│   │   ├── filter/        # TraceIdFilter, CallLogFilter, ApiClientFilter, IpAllowFilter, InterfaceAuthFilter, RateLimitFilter
│   │   ├── calllog/       # CallLogRecorder(큐·일괄 저장), CallLogMasker(본문 마스킹)
│   │   ├── annotation/    # @ApiInterface("INFC_ID"), ApiInterfaceResolver(요청 → 인터페이스ID, 기동 시 선언 검사)
│   │   ├── exception/     # ApiErrorCode(A001~A005), ApiErrorWriter, ApiErrorCodeAdvice
│   │   ├── support/       # ClientIpResolver, RateLimiter, ApiRequestContext, TtlCache
│   │   ├── log/           # LogMdcKey, SqlIdInterceptor
│   │   └── system/        # SystemController (/api/ping)
│   └── biz/{업무}/{기능}/  # 업무 API (controller / service / mapper / dto)
│       └── system/sample/ # 샘플 조회 API (IFSY0001)
└── src/main/resources/
    ├── application.yml, application-{local,dev,prod}.yml
    ├── logback-spring.xml
    ├── messages/messages*.properties   # coder-api 자체 번들 (기본 영어)
    └── mapper/{common, biz/system}/    # MyBatis XML
```

---

## 🔐 요청 처리 순서

```text
TraceIdFilter         X-Trace-Id 이어받기/발급, MDC, 응답 헤더
 → CallLogFilter       업무 API(@ApiInterface)면 처리 후 호출 이력을 큐에 넣음 (실패 응답 포함)
 → Spring Security     JWT(RS256) 서명·만료·발급자 검증                      → 401 A001
   → ApiClientFilter     토큰 sub(KEY_ID)의 키·연계기관이 지금도 사용 가능한지 → 401 A001
   → IpAllowFilter       요청 IP ∈ ALLW_IP_ADDR (비어 있으면 제한 없음)       → 403 A002
   → InterfaceAuthFilter INFC_ID ∈ 토큰 scope 그리고 TB_LNK_ORG_INFC_R 허용     → 403 A003
   → RateLimitFilter     분당·일·동시 한도 (Redis)                            → 429 A004 (Redis 장애 + deny 모드 503 A005)
 → Controller → Service → MyBatis
```

- IP 확인은 연계키를 알아야 하므로 토큰 검증 뒤에 둔다 (PRD 4.1 구성도와 순서가 다름).
- 연계키·인터페이스·허용 인터페이스는 인스턴스 메모리에 5분 캐시하고, Redis 채널 `coder:api:cache-evict`에 아무 메시지나 발행하면 모든 인스턴스가 캐시를 비운다 (`coder-backend` 연계 관리 API가 변경 후 발행).

---

## 💻 실행 방법

### 1. DB 준비 (최초 1회)

`coder` 스키마에 아래 DDL과 데이터를 반영합니다.

| 파일 | 내용 |
|---|---|
| `schema/table/TB_LNK_ORG_M.sql` | 연계기관기본 |
| `schema/table/TB_LNK_KEY_M.sql` | 연계키기본 (client_id, KMS 암호화 비밀키, 허용 IP, 호출 한도) |
| `schema/table/TB_LNK_KEY_H.sql` | 연계키이력 (+ 시퀀스) |
| `schema/table/TB_INFC_M.sql` | 인터페이스기본 (본문 로그 여부, 마스킹 필드) |
| `schema/table/TB_LNK_ORG_INFC_R.sql` | 연계기관인터페이스관계 |
| `schema/table/TB_INFC_CALL_L.sql` | 인터페이스호출내역 (+ 시퀀스, 월 파티션 2026-10 ~ 2027-12 + 기본 파티션) |
| `schema/table/TB_INFC_CALL_S.sql` | 인터페이스호출집계 (coder-batch 일 집계, Phase 2) |
| `schema/data/TB_COMM_CD-data.sql` | 공통코드 `CHNL_CLS_CD`, `KEY_STAT_CD`, `SYSM_CLS_CD` 02 |
| `schema/data/TB_INFC_M-data.sql` | 인터페이스 `IFSY0001`, 로컬 샘플 연계기관 `LOCAL01` + 권한 |

> 이전 대외 연계 설계(삭제됨)로 만든 `TB_LNK_ORG_M`, `TB_LNK_ORG_INFC_R`, `TB_LNK_KEY_H` 등이 로컬 DB에 남아 있으면 컬럼이 달라 DDL이 실패합니다. 이전 테이블을 정리한 뒤 반영합니다.

### 2. 클라이언트 키 발급

비밀키는 KMS로 암호화해야 하므로 SQL로 넣지 않습니다. 관리 화면(Phase 3) 전까지는 기동 인자로 발급합니다. 키를 저장하고 `client_id` / `client_secret`을 콘솔에 **한 번만** 출력한 뒤 종료합니다.

```bash
cd workspace/coder-api
./gradlew bootRun --args="--coder.api.key-issue.lnk-org-id=LOCAL01 --server.port=0"
```

로컬 KMS는 `application-local.yml`의 개발용 `master-key`로 암호화합니다. 이 값을 바꾸면 이미 발급한 키를 복호화할 수 없습니다.

### 3. 서버 실행 / 호출

```bash
docker start postgres redis
./gradlew bootRun        # 8082

# 토큰 발급 (client_secret_post 또는 -u client_id:client_secret 의 Basic)
curl -d "grant_type=client_credentials&client_id=$ID&client_secret=$SECRET" http://localhost:8082/oauth2/token
# 업무 API
curl -H "Authorization: Bearer $TOKEN" http://localhost:8082/api/v1/samples/SYSM_CLS_CD
```

| 주소 | 내용 |
|---|---|
| `POST /oauth2/token` | 액세스 토큰 발급 (OAuth2 표준 응답·오류) |
| `GET /.well-known/jwks.json` | 토큰 서명 공개키 (사용 중 + 직전 버전) |
| `GET /api/ping`, `GET /actuator/health` | 헬스체크 (`db`, `redis`) |
| http://localhost:8082/swagger-ui.html | API 문서 (`partner` 그룹 = 연계기관 공유용) |

### 4. 테스트

```bash
./gradlew test    # 단위 테스트 (DB·Redis 불필요): 마스킹, IP 판정, 토큰 서명·검증·서명키 교체, 키 사용 가능 여부
```

---

## ➕ 업무 API 추가 방법

1. `TB_INFC_M`에 인터페이스를 등록한다 (`IF` + 업무약어 2자리 + 순번 4자리, 본문 로그 여부·마스킹 필드).
2. `kr.or.coder.api.biz.{업무}.{기능}`에 Controller / Service / Mapper / DTO를 만들고 Controller 메서드에 `@ApiInterface("IF....")`를 붙인다. 선언이 없으면 기동하지 않는다.
3. 이 API를 쓸 연계기관에 `TB_LNK_ORG_INFC_R` 권한을 준다 (토큰은 다시 발급받아야 scope 에 들어간다).
4. 응답은 `ApiResponse<T>`, 업무 오류는 `BusinessException(메시지키)`로 던진다. 메시지는 `src/main/resources/messages/` 3개 파일에 함께 추가한다.
5. 개인정보 필드는 마스킹 대상(`MASK_FLD_CONT`)에 반드시 넣는다. 토큰·비밀키·비밀번호 필드는 항상 마스킹된다.

---

## ⚙️ 주요 설정 (`coder.api.*`)

| 키 | 기본값 | 설명 |
|---|---|---|
| `token.expires-in` | 3600 | 액세스 토큰 만료 (초) |
| `token.max-secret-fail-count` / `secret-fail-lock-minutes` | 5 / 10 | 비밀키 연속 실패 시 키 잠금 |
| `trusted-proxies` | (없음) | `X-Forwarded-For`를 믿을 프록시 IP/CIDR |
| `rate-limit.redis-failure-mode` | `allow` | Redis 장애 시 허용 / `deny`(503) |
| `call-log.queue-capacity` / `batch-size` / `flush-interval-ms` / `body-max-length` | 10000 / 500 / 1000 / 4000 | 호출 이력 저장 |
| `cache.ttl-seconds` | 300 | 연계 정보 캐시 |
