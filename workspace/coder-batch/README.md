# CODER Batch Application

`coder-batch`는 `coder-backend`와 같은 기술 기반(Spring Boot 3.3.5, Java 21, `coder-core` Composite Build, MyBatis, PostgreSQL)에서 동작하는 **Spring Batch 5 + Quartz 2.3** 배치 애플리케이션입니다. 배치직무와 스케줄은 DB(`TB_DSPT_JOB_M`)에 등록하며, 재배포 없이 관리 API로 등록·변경·수동 실행·중지·이력 조회를 합니다.

요구사항 정의: [documents/batch/PRD.md](../../documents/batch/PRD.md)

---

## 🛠️ 기술 스택

- **Java** 21 / **Spring Boot** 3.3.5 / **Gradle** 8.10 (Composite Build: `includeBuild('../coder-core')`)
- **Spring Batch** 5.1.2 (`spring-boot-starter-batch`)
- **Quartz** 2.3.2 (`spring-boot-starter-quartz`, JDBC JobStore, Cluster 모드)
- **MyBatis** 3.0.3 (`coder-core-db`) - `MyBatisCursorItemReader`, `MyBatisBatchItemWriter`
- **coder-core**: `coder-core-common`(ApiResponse, 메시지), `coder-core-db`(DataSource, SqlSessionFactory, TransactionManager), `coder-core-security`(JWT, Swagger)

---

## 📁 디렉터리 구조

```text
coder-batch/
├── src/main/java/kr/or/coder/batch/
│   ├── CoderBatchApplication.java
│   ├── common/
│   │   ├── config/        # BatchConfig(공통 Listener 일괄 등록), BatchProperties(coder.batch.*), MethodSecurityConfig
│   │   ├── quartz/        # BatchLaunchQuartzJob / SerialBatchLaunchQuartzJob, BatchLaunchExecutor, ScheduleSyncRunner
│   │   ├── listener/      # JobExecutionLogListener(TB_DSPT_EXEC_L 기록, MDC), StepMdcListener
│   │   ├── support/       # BatchConstants, JobParametersFactory, PageResponse, ...
│   │   └── exception/     # BatchErrorCode, BatchException, BatchExceptionHandler(404/409 응답)
│   ├── frame/             # 배치 관리 API (controller / service / mapper / dto / entity)
│   └── job/{업무}/{기능}/      # 업무 배치 Job
│       └── system/
│           ├── execlogpurge/     # execLogPurgeJob (Tasklet형 샘플)
│           └── inactiveuserlock/ # inactiveUserLockJob (Chunk형 샘플)
├── src/main/resources/
│   ├── application.yml           # 공통 설정 (프로필 미지정 시 local)
│   ├── application-{local,dev,prod}.yml  # 환경별 DB 접속정보, Swagger 등
│   ├── logback-spring.xml        # MDC [jobId:jobExecutionId:stepName]
│   ├── messages/                 # sy.batch.* 메시지
│   └── mapper/{frame, job/system}/   # MyBatis XML
└── src/test/                     # 단위 테스트 + PostgreSQL 통합 테스트 (E2E 흐름 포함)
```

DDL은 루트 `schema/`에서 관리합니다.

| 파일 | 내용 |
|---|---|
| `schema/batch/batch-account.sql` | `batch` 계정·스키마 생성, 권한 부여 (`coder`는 `batch` 스키마 읽기 전용) |
| `schema/batch/spring-batch-schema-postgresql.sql` | Spring Batch 메타 테이블 (`BATCH_*`) |
| `schema/batch/quartz-tables-postgresql.sql` | Quartz 테이블 (`QRTZ_*`, 원본 DROP 문 주석 처리) |
| `schema/table/TB_DSPT_JOB_M.sql` | 배치직무기본 |
| `schema/table/TB_DSPT_EXEC_L.sql` | 배치실행내역 (+ 시퀀스) |
| `schema/table/TB_DSPT_ALRM_RCVR_R.sql` | 배치알람수신자관계 (실패 알람 받을 사용자, PK 직무ID+사용자ID) |
| `schema/table/TB_ALRM_L.sql` | 알람내역 (coder-backend 알림 센터와 공유) |
| `schema/batch/batch-job-data.sql` | 샘플 배치직무 등록 |

---

## 💻 실행 방법

### 1. DB 준비 (최초 1회)

`spring.batch.jdbc.initialize-schema`와 `spring.quartz.jdbc.initialize-schema`는 `never`입니다 (Quartz 원본 스크립트에 `DROP TABLE`이 있어 자동 실행 시 스케줄이 매번 삭제됨). 아래 순서로 직접 반영합니다.

`BATCH_*`·`QRTZ_*`는 `batch` 계정이 소유한 `batch` 스키마에, `TB_DSPT_*`는 `coder` 스키마에 둡니다. coder-batch는 `batch` 계정으로 접속하고 `currentSchema=batch,coder`로 두 스키마를 접두사 없이 조회합니다.

```bash
# 1) 업무 테이블 (coder 계정, coder 스키마)
psql -h localhost -U coder -d coder -c "SET search_path TO coder" \
  -f ../../schema/table/TB_DSPT_JOB_M.sql \
  -f ../../schema/table/TB_DSPT_EXEC_L.sql \
  -f ../../schema/table/TB_ALRM_L.sql \
  -f ../../schema/table/TB_DSPT_ALRM_RCVR_R.sql

# 2) batch 계정·스키마, 권한 (슈퍼유저)
psql -h localhost -U postgres -d coder -f ../../schema/batch/batch-account.sql

# 3) 프레임워크 테이블 (batch 계정 → batch 스키마)
psql -h localhost -U batch -d coder \
  -f ../../schema/batch/spring-batch-schema-postgresql.sql \
  -f ../../schema/batch/quartz-tables-postgresql.sql

# 4) 샘플 배치직무 (coder 계정)
psql -h localhost -U coder -d coder -c "SET search_path TO coder" \
  -f ../../schema/batch/batch-job-data.sql
```

배치가 새 업무 테이블을 쓰게 되면 `batch-account.sql`처럼 `batch` 계정에 해당 테이블 권한을 추가로 부여합니다.

### 2. 실행

```bash
./gradlew bootRun          # http://localhost:8081
```

- Swagger UI: http://localhost:8081/swagger-ui.html
- 프로필: `local`(기본) / `dev` / `prod` — `SPRING_PROFILES_ACTIVE=prod` 또는 `--spring.profiles.active=prod`
- `prod`는 `CODER_BATCH_DB_URL`, `CODER_BATCH_DB_USERNAME`, `CODER_BATCH_DB_PASSWORD`, `CODER_JWT_SECRET`을 기본값 없이 받으므로 누락 시 기동에 실패하며, Swagger는 비활성화됩니다.
- 환경변수: `CODER_BATCH_DB_PASSWORD` (`batch` 계정 비밀번호), `CODER_JWT_SECRET` (`coder-backend`와 같은 값이어야 백엔드에서 발급한 토큰이 통과합니다)
- 운영 배포 시 JVM 옵션 `-Duser.timezone=Asia/Seoul`을 지정합니다 (`bootRun`에는 설정되어 있음).

### 3. 테스트

```bash
./gradlew test             # 단위 테스트 - 외부 DB 불필요
./gradlew integrationTest  # PostgreSQL 통합 테스트 (1번 DB 준비 필요)
```

---

## 🚀 관리 API

모든 API는 `ApiResponse<T>` 규격이며 JWT의 `role=ADMIN`이 필요합니다.

| Method | Endpoint | 설명 |
|---|---|---|
| `GET` | `/api/v1/batch-jobs` | 배치직무 목록 (스케줄 상태, 다음 실행 예정 일시 포함) |
| `GET` | `/api/v1/batch-jobs/{jobId}` | 배치직무 상세 |
| `POST` | `/api/v1/batch-jobs` | 배치직무 저장 + 스케줄 반영 |
| `PUT` | `/api/v1/batch-jobs/{jobId}` | 배치직무 저장 + 재스케줄 (일시정지 상태 유지) |
| `DELETE` | `/api/v1/batch-jobs/{jobId}` | 배치직무·실행내역·스케줄 삭제 |
| `PATCH` | `/api/v1/batch-jobs/{jobId}/schedule` | `{"schedStatus":"PAUSED"\|"NORMAL"}` |
| `POST` | `/api/v1/batch-jobs/{jobId}/executions` | 수동 실행 `{"params":{"baseDt":"20261008"}}` (비동기) |
| `GET` | `/api/v1/batch-executions` | 실행 이력 (`stDt`, `endDt`, `wrkMdulCd`, `jobId`, `execStatCd`, `page`, `size`) |
| `GET` | `/api/v1/batch-executions/{dsptExecSn}` | 실행 상세 (JobParameters, Step별 현황) |
| `POST` | `/api/v1/batch-executions/{dsptExecSn}/restarts` | FAILED/STOPPED 건 재시작 (비동기) |
| `PATCH` | `/api/v1/batch-executions/{dsptExecSn}` | `{"execStatCd":"STOPPED"\|"ABANDONED"}` |

---

## 🔔 실패 알람

배치 실행이 실패하면 `TB_DSPT_ALRM_RCVR_R`에 그 직무ID로 등록된 사용자(사용 중인 사용자만)에게 알람을 보낸다.

- **대상:** Job 실행 결과 `FAILED`(`JobExecutionLogListener`), Job 기동 자체 실패(`BatchLaunchExecutor`). `STOPPED`·`SKIP`·`COMPLETED`는 보내지 않는다.
- **전송:** `TB_ALRM_L`에 수신자별로 등록(유형 `DSPT`, 화면 `SYLK050`, 내용은 오류메시지) → Redis 채널 `coder:sse` 발행 → `coder-backend`가 접속 중인 사용자 화면(상단 알림 센터)에 SSE로 전달.
- **오류 처리:** 알람 처리 오류는 배치 결과에 영향을 주지 않는다 (ERROR 로그만). Redis를 쓸 수 없으면 WARN 로그만 남기고, 알람은 DB에 있으므로 다음 알림 센터 조회 때 보인다. Redis는 `actuator/health`에 포함하지 않는다.
- **수신자 등록:** 배치관리(SYLK050) 배치 등록/수정 팝업의 "알람수신" 영역에서 사용자를 검색해 추가·삭제하고 저장한다.
  API는 배치직무 저장(`POST`/`PUT /api/v1/batch-jobs`) 요청의 `alrmRcvrIdList`로 전체 교체한다 (필드를 빼면 기존 수신자 유지, `[]`이면 전체 삭제, 없는 사용자는 400 `sy.batch.alarm.receiver.not.found`). 상세 조회(`GET /api/v1/batch-jobs/{jobId}`) 응답의 `alrmRcvrList`에 사용자명·사용여부와 함께 내려준다.
- 설정: `spring.data.redis.host/port` (`CODER_REDIS_HOST`, `CODER_REDIS_PORT`, 기본 localhost:6379)

---

## 🧩 새 배치 Job 추가

1. `kr.or.coder.batch.job.{업무약어 소문자}.{기능}` 패키지에 `{기능}JobConfig`를 만들고 Job 빈 이름을 `{기능}Job`(camelCase)으로 지정합니다. 한 `JobConfig`에는 Job 빈을 1개만 정의합니다.
2. Job 전용 Mapper는 `...{기능}.mapper.{도메인}BatchMapper` + `resources/mapper/job/{업무약어}/*.xml`에 둡니다.
3. JobParameters를 쓰는 Reader/Processor/Tasklet은 `@StepScope`로 선언합니다.
4. 실행내역(`JobExecutionLogListener`)과 MDC(`StepMdcListener`)는 `BatchConfig`가 모든 Job/Step에 자동 등록하므로 따로 붙이지 않습니다.
5. 관리 API(`POST /api/v1/batch-jobs`) 또는 배치관리 화면(SYLK050)에서 `jobId` = 배치ID(`BHFL001` 형식), `execClassNm` = `{기능}JobConfig` 전체 클래스명, cron, 기본 파라미터(JSON)를 등록하면 즉시 스케줄에 반영됩니다.
