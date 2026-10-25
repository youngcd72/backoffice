# CODER Batch (Spring Batch + Quartz) PRD

| 항목 | 내용 |
|---|---|
| 문서명 | CODER 배치 실행 기반(`coder-batch`) 제품 요구사항 정의서 |
| 버전 | v0.1 (배포 전 초안 - Phase 1 MVP 구현 반영) |
| 작성일 | 2026-10-09 |
| 구현 | `workspace/coder-batch` (Phase 1-1 ~ 1-3 완료, PostgreSQL 실DB 검증은 미수행) |
| 대상 독자 | 백엔드/배치 개발자, 아키텍트, 운영 담당자 |
| 관련 문서 | 루트 `CLAUDE.md` (네이밍 컨벤션), `workspace/coder-backend/README.md`, `schema/dictionary/`, 배치 프로그램 사양서 `documents/batch/spec/{대분류}/{배치ID}.md` |

---

## 1. 배경 및 목적

### 1.1 배경
- 현재 CODER는 `coder-backend`(Spring Boot 3.3.5, Java 21)가 `coder-core`(common / db / redis / security)를 Composite Build로 포함해 온라인 REST API만 제공한다.
- 업무 영역(SY 시스템, MS 기준정보)이 늘면서 정산·집계·데이터 정리 같은 **대량/주기성 작업**이 필요해졌다. 이런 작업을 온라인 WAS 안의 `@Scheduled`로 처리하면 다음 문제가 생긴다.
  - 다중 인스턴스 배포 시 중복 실행
  - 실행 이력·재시작(restart)·실패 지점 복구 불가
  - 대량 처리 때문에 온라인 응답 성능 저하
  - 스케줄을 바꾸려면 재배포 필요

### 1.2 목적
`coder-backend`와 **같은 기술 기반**(Spring Boot 3.3.5, Java 21, Gradle Composite Build, `coder-core`, MyBatis, PostgreSQL)을 쓰는 독립 배치 애플리케이션 **`coder-batch`**를 만든다.
- **Spring Batch 5**: Job/Step 구조, chunk 처리, 실행 메타데이터, 재시작
- **Quartz 2.3 (JDBC JobStore, Cluster)**: cron 스케줄링, 다중 노드에서 한 번만 실행 보장, misfire 처리
- **DB 기반 배치 관리**: 배치 Job·스케줄을 업무 테이블에 등록하고, 재배포 없이 관리 API로 등록·변경·수동 실행·중지·이력 조회를 한다.

---

## 2. 범위

### 2.1 In Scope (MVP)
1. `workspace/coder-batch` Gradle 프로젝트 신규 생성 (`includeBuild('../coder-core')`)
2. Spring Batch 메타 테이블(`BATCH_*`)과 Quartz 테이블(`QRTZ_*`)의 PostgreSQL DDL 관리
3. 배치 관리 업무 테이블(배치직무기본, 배치실행내역) 설계·생성
4. Quartz → Spring Batch 실행 브리지 (공통 QuartzJobBean 하나로 모든 Batch Job 실행)
5. 기동 시 DB 등록 정보와 Quartz 스케줄 동기화
6. 배치 관리 REST API (Job 목록·등록·수정·삭제, 수동 실행, 스케줄 일시정지/재개, 실행 이력 조회, 실행 중지)
7. 공통 Listener (실행내역 기록, MDC 로깅, 처리 건수 집계)
8. 표준 Reader/Writer 가이드 (MyBatis Cursor/Paging Reader, MyBatis Batch Writer)
9. 샘플 Job 2종 (Tasklet형, Chunk형)

### 2.2 Out of Scope (후속 단계)
- 프론트엔드 배치 관리 화면 (`coder-frontend`) → Phase 2
- 실패 알림 (메일/메신저) → Phase 2
- Job 간 선후행 의존성(Job Chain) 및 워크플로 UI → Phase 3
- 원격 파티셔닝 / 원격 청킹 (멀티 노드 분산 처리) → 필요 시 검토

---

## 3. 용어 정의

| 용어 | 설명 |
|---|---|
| Batch Job | Spring Batch `Job` 빈. 하나 이상의 `Step`으로 구성된다. |
| Step | Tasklet 또는 Chunk(Reader → Processor → Writer)로 구성된 처리 단위 |
| Quartz Job | Quartz `JobDetail`. CODER에서는 공통 브리지 클래스 하나(`BatchLaunchQuartzJob`)만 쓴다. |
| Trigger | Quartz `CronTrigger`. 배치직무기본의 스케줄 표현식(cron)으로 생성한다. |
| 배치직무 | DB(`TB_DSPT_JOB_M`)에 등록된 배치 관리 단위. Batch Job 1개와 Quartz Job/Trigger 1쌍에 대응한다. |
| 실행 구분 | 스케줄 실행(S), 수동 실행(M), 재시작(R) |

---

## 4. 시스템 아키텍처

### 4.1 구성도

```text
┌────────────────────┐      REST (JWT)       ┌───────────────────────────────────────────┐
│  coder-frontend     │ ───────────────────▶ │ coder-batch  (Spring Boot 3.3.5, :8081)       │
│  (Phase 2 관리화면) │                       │                                             │
└────────────────────┘                       │  BatchJobController ──▶ BatchJobService      │
                                             │        │                  │                 │
                                             │        ▼                  ▼                 │
                                             │  ScheduleService ──▶ Quartz Scheduler        │
                                             │                       (JDBC JobStore,       │
                                             │                        isClustered=true)    │
                                             │                            │ fire           │
                                             │                            ▼                │
                                             │              BatchLaunchQuartzJob            │
                                             │                            │ JobLauncher.run│
                                             │                            ▼                │
                                             │              Spring Batch Job / Step         │
                                             │              (MyBatis Reader/Writer)         │
                                             └──────────────┬──────────────────────────────┘
                                                            │ HikariCP (coder-core-db)
                                                            ▼
                                   ┌─────────────────────────────────────────────┐
                                   │ PostgreSQL 18  (DB: coder, Schema: coder)       │
                                   │  - 업무 테이블   TB_*                          │
                                   │  - 배치 관리     TB_DSPT_JOB_M, TB_DSPT_EXEC_L │
                                   │  - Batch 메타    BATCH_*                       │
                                   │  - Quartz       QRTZ_*                        │
                                   └─────────────────────────────────────────────┘
```

### 4.2 기술 스택

| 구분 | 기술 / 버전 | 비고 |
|---|---|---|
| Language | Java 21 (LTS) | `coder-backend`와 동일 |
| Framework | Spring Boot 3.3.5 | BOM으로 버전 관리 |
| Batch | Spring Batch 5.1.x (`spring-boot-starter-batch`) | Boot 3.3 BOM 관리 버전 |
| Scheduler | Quartz 2.3.x (`spring-boot-starter-quartz`) | JDBC JobStore, Cluster 모드 |
| Persistence | MyBatis 3.0.3 (`coder-core-db`), HikariCP | `MyBatisCursorItemReader`, `MyBatisPagingItemReader`, `MyBatisBatchItemWriter` |
| Core 라이브러리 | `coder-core-common`, `coder-core-db`, `coder-core-security` | `coder-core-redis`는 선택 (분산락이 필요한 Job만) |
| DB | PostgreSQL 18 | 온라인과 같은 DB/스키마 사용 |
| 모니터링 | Spring Boot Actuator | `health`, `info`, `metrics` |
| Build | Gradle 8.x, Composite Build | `includeBuild('../coder-core')` |

### 4.3 coder-backend와 비교

| 항목 | coder-backend | coder-batch |
|---|---|---|
| 역할 | 온라인 REST API | 주기/대량 처리 + 배치 관리 API |
| 포트 | 8080 | 8081 |
| group | `kr.or.coder.biz` | `kr.or.coder.batch` |
| 진입점 | `CoderBackendApplication` | `CoderBatchApplication` |
| 트랜잭션 | `@Transactional` (요청 단위) | Step/Chunk 단위 (`commit-interval`) |
| 배포 | N개 인스턴스 (무상태) | 2개 이상 인스턴스 (Quartz Cluster로 중복 실행 방지) |

---

## 5. 프로젝트 구조

### 5.1 디렉터리 구조

```text
workspace/
├── coder-core/                         # 기존 (변경 최소화)
├── coder-backend/                      # 기존
└── coder-batch/                        # 신규
    ├── build.gradle
    ├── settings.gradle                # rootProject.name = 'coder-batch'; includeBuild('../coder-core')
    ├── gradle.properties
    ├── lombok.config
    ├── README.md
    └── src/
        ├── main/
        │   ├── java/kr/or/coder/batch/
        │   │   ├── CoderBatchApplication.java
        │   │   ├── common/
        │   │   │   ├── config/            # BatchConfig(공통 Listener 일괄 등록), BatchProperties, MethodSecurityConfig
        │   │   │   ├── quartz/            # BatchLaunchQuartzJob, SerialBatchLaunchQuartzJob, BatchLaunchExecutor, ScheduleSyncRunner
        │   │   │   ├── listener/          # JobExecutionLogListener, StepMdcListener
        │   │   │   ├── support/           # JobParametersFactory, BatchConstants, BatchTextUtil, PageResponse, SecurityUserUtil
        │   │   │   └── exception/         # BatchErrorCode, BatchException, BatchExceptionHandler
        │   │   ├── frame/                 # 배치 관리 API (Controller/Service/Mapper/DTO)
        │   │   │   ├── controller/        # BatchJobController, BatchExecutionController
        │   │   │   ├── service/           # BatchJobService(Impl), ScheduleService(Impl), BatchExecutionService(Impl)
        │   │   │   ├── mapper/            # BatchJobMapper, BatchExecutionMapper
        │   │   │   ├── entity/            # BatchJob(TB_DSPT_JOB_M), BatchExecution(TB_DSPT_EXEC_L)
        │   │   │   └── dto/               # BatchJobRequest/Response, BatchExecutionResponse ...
        │   │   └── job/                   # 업무 배치 Job 정의 (업무 패키지: system 등)
        │   │       ├── system/
        │   │       │   ├── execlogpurge/      # 샘플: execLogPurgeJob (Tasklet)
        │   │       │   └── inactiveuserlock/  # 샘플: inactiveUserLockJob (Chunk, Cursor Reader + Batch Writer)
        │   │       └── {업무}/{기능}/          # Job 전용 Mapper는 {기능}/mapper/ 하위 (MyBatisConfig 스캔 대상)
        │   └── resources/
        │       ├── application.yml
        │       ├── application-{local|dev|prd}.yml
        │       ├── logback-spring.xml
        │       ├── messages/messages*.properties
        │       └── mapper/
        │           ├── frame/             # BatchJobMapper.xml, BatchExecutionMapper.xml
        │           └── job/{업무}/          # 업무 Job용 Reader/Writer SQL
        └── test/java/kr/or/coder/batch/      # @SpringBatchTest 기반 Job 테스트
```

DDL은 기존 규칙대로 루트 `schema/table/`에서 관리한다.

```text
schema/
├── table/
│   ├── TB_DSPT_JOB_M.sql              # 배치직무기본 (신규)
│   └── TB_DSPT_EXEC_L.sql             # 배치실행내역 (신규)
└── batch/                             # 프레임워크 메타 테이블 (신규 폴더)
    ├── spring-batch-schema-postgresql.sql
    ├── quartz-tables-postgresql.sql   # 원본 DROP TABLE 문은 주석 처리
    └── batch-job-data.sql             # 샘플 배치직무 등록
```

### 5.2 build.gradle (안)

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.5'
    id 'io.spring.dependency-management' version '1.1.6'
}

group = 'kr.or.coder.batch'
version = '1.0.0-SNAPSHOT'

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

repositories { mavenCentral() }

dependencyManagement {
    imports { mavenBom "org.springframework.boot:spring-boot-dependencies:3.3.5" }
    dependencies {
        dependency 'org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.3'
        dependency 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0'
    }
}

dependencies {
    implementation 'kr.or.coder.core:coder-core-common'
    implementation 'kr.or.coder.core:coder-core-db'
    implementation 'kr.or.coder.core:coder-core-security'

    implementation 'org.springframework.boot:spring-boot-starter-batch'
    implementation 'org.springframework.boot:spring-boot-starter-quartz'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'

    runtimeOnly 'org.postgresql:postgresql'

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    testCompileOnly 'org.projectlombok:lombok'
    testAnnotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.batch:spring-batch-test'
}

tasks.named('test') { useJUnitPlatform() }
```

> 루트 `package.json`에 `"dev:coder-batch": "cd workspace/coder-batch && .\\gradlew.bat bootRun"` 스크립트를 추가하고, `coder-workspace.code-workspace`에 폴더를 등록한다.

### 5.3 진입점

```java
@SpringBootApplication(scanBasePackages = {"kr.or.coder.batch", "kr.or.coder.core"})
public class CoderBatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoderBatchApplication.class, args);
    }
}
```

- `@EnableBatchProcessing`은 **붙이지 않는다**. Boot 3에서 이 어노테이션을 쓰면 `BatchAutoConfiguration`(JobRepository, JobLauncher 자동 구성, `spring.batch.*` 프로퍼티)이 꺼진다.
- `@MapperScan`은 `coder-core-db`의 `MyBatisConfig`(`kr.or.coder.**.mapper`)가 이미 처리하므로 따로 선언하지 않는다.
- `DataSource`, `SqlSessionFactory`, `PlatformTransactionManager`(`transactionManager`)는 `coder-core-db`가 제공하는 빈을 Spring Batch와 Quartz가 함께 쓴다.

---

## 6. 기능 요구사항

### FR-01. 배치직무 등록/관리
| ID | 요구사항 | 우선순위 |
|---|---|---|
| FR-01-1 | 배치직무를 `TB_DSPT_JOB_M`에 등록·수정·삭제할 수 있다. | 필수 |
| FR-01-2 | 배치직무 ID(`JOB_ID`)는 배치ID(`BH` + 업무 2자리 + 순번 3자리, 예: `BHFL001`)이고, 실행클래스명(`EXEC_CLASS_NM`)에 Job 빈을 정의한 클래스(패키지 포함)를 저장한다. 등록 시 해당 클래스에 Job 빈이 정확히 1개 정의되어 있지 않으면 저장을 거부한다 (`BatchJobLocator`). | 필수 |
| FR-01-3 | cron 표현식은 저장 전에 `CronExpression.isValidExpression()`으로 검증한다. | 필수 |
| FR-01-4 | 사용여부(`USE_YN`)가 `N`이면 Quartz Trigger를 등록하지 않는다 (이미 있으면 삭제). | 필수 |
| FR-01-5 | 배치직무를 수정하면 Quartz Trigger를 즉시 재스케줄(`rescheduleJob`)한다. 재배포는 필요 없다. | 필수 |
| FR-01-6 | 업무모듈코드(`WRK_MDUL_CD`)로 업무별 조회·필터링을 지원한다. | 필수 |

### FR-02. 스케줄링 (Quartz)
| ID | 요구사항 | 우선순위 |
|---|---|---|
| FR-02-1 | 애플리케이션 기동 시(`ApplicationReadyEvent`) `USE_YN='Y'`인 배치직무를 읽어 Quartz JobDetail/CronTrigger와 동기화한다 (추가·변경·삭제). | 필수 |
| FR-02-2 | Quartz JobDetail은 `JobKey(JOB_ID, WRK_MDUL_CD)`, Trigger는 `TriggerKey(JOB_ID, WRK_MDUL_CD)`로 식별한다. | 필수 |
| FR-02-3 | JobStore는 JDBC(`job-store-type: jdbc`), `isClustered=true`로 구성해 여러 노드 중 한 곳에서만 실행되게 한다. | 필수 |
| FR-02-4 | misfire 정책 기본값은 `MISFIRE_INSTRUCTION_DO_NOTHING`(놓친 실행은 건너뜀)이며, 배치직무별로 `FIRE_ONCE_NOW`를 선택할 수 있다. | 선택 |
| FR-02-5 | 스케줄 일시정지(`pauseJob`)와 재개(`resumeJob`)를 API로 제공한다. | 필수 |
| FR-02-6 | 다음 실행 예정 일시(`Trigger.getNextFireTime()`)를 조회 API 응답에 포함한다. | 필수 |

### FR-03. Quartz → Spring Batch 실행 브리지
| ID | 요구사항 | 우선순위 |
|---|---|---|
| FR-03-1 | 모든 Quartz Job은 공통 클래스(`BatchLaunchQuartzJob`, 다중실행 불가용 하위 클래스 `SerialBatchLaunchQuartzJob`)로 실행한다. `JobDataMap`의 `jobId`로 배치직무를 읽고, 실행클래스명으로 Batch Job 빈을 찾아 `JobLauncher.run()`을 호출한다 (`BatchLaunchExecutor`). 배치ID는 non-identifying JobParameter `jobId`로 넘겨 실행내역(`TB_DSPT_EXEC_L.JOB_ID`)에 기록한다. 직무파라미터·다중실행여부는 실행 시점에 DB에서 다시 읽는다. | 필수 |
| FR-03-2 | JobParameters는 다음 규칙으로 만든다. ① `requestDttm`, `fireId`(identifying, 실행 요청 일시와 Quartz fireInstanceId - 실행마다 새 JobInstance) ② `TB_DSPT_JOB_M.JOB_PARM_CONT`에 정의한 기본 파라미터(identifying) ③ 수동 실행 시 요청 본문 파라미터(identifying, ②보다 우선) ④ `execClsCd`, `reqUsrId`, `execSrvNm`(non-identifying). 재시작은 이전 실행의 identifying 파라미터를 그대로 쓰고 ④만 교체한다. | 필수 |
| FR-03-3 | 다중실행여부(`MLT_EXEC_YN`)가 `N`이면 실행 전에 `JobExplorer.findRunningJobExecutions(Job 이름)`을 확인하고, 실행 중이면 이번 실행을 건너뛰고 내역에 `SKIP`으로 기록한다. Job 기동 자체가 실패한 경우(파라미터 오류 등)는 `FAILED`로 기록한다. | 필수 |
| FR-03-4 | `MLT_EXEC_YN='N'`이면 `@DisallowConcurrentExecution`이 붙은 `SerialBatchLaunchQuartzJob`으로 JobDetail을 등록해 같은 JobKey가 클러스터 안에서 동시에 실행되지 않게 한다. `Y`이면 `BatchLaunchQuartzJob`으로 등록한다. | 필수 |
| FR-03-5 | Batch Job은 동기 `JobLauncher`로 실행해 Quartz 워커 스레드가 Job 종료까지 잡고 있게 한다 (클러스터 failover와 동시 실행 제어의 정합성 확보). 장시간 Job이 많아지면 Quartz 스레드 풀 크기로 동시 실행 수를 제어한다. | 필수 |

### FR-04. 수동 실행 / 재시작 / 중지
| ID | 요구사항 | 우선순위 |
|---|---|---|
| FR-04-1 | 관리 API로 배치직무를 즉시 실행할 수 있다. 내부적으로 Quartz `triggerJob(jobKey, jobDataMap)`을 호출해 스케줄 실행과 같은 경로(클러스터 단일 실행 보장)를 탄다. | 필수 |
| FR-04-2 | 수동 실행 시 JobParameters를 추가로 넘길 수 있다 (예: `baseDt=20261008`). | 필수 |
| FR-04-3 | `FAILED` 또는 `STOPPED` 상태의 JobExecution을 같은 JobParameters로 재시작할 수 있다 (`JobOperator.restart`). Chunk Step은 마지막 커밋 지점부터 이어서 처리한다. | 필수 |
| FR-04-4 | 실행 중인 JobExecution을 중지할 수 있다 (`JobOperator.stop`). 다음 chunk 경계에서 `STOPPED`로 끝난다. | 필수 |
| FR-04-5 | 비정상 종료(노드 다운)로 `STARTED` 상태에 남은 실행 건은 관리 API로 `ABANDONED` 처리할 수 있다. 클러스터 어디에서도 실행 중이 아닐 때(`QRTZ_FIRED_TRIGGERS`에 해당 JobKey의 `EXECUTING` 행이 없을 때)만 허용한다. | 선택 |

### FR-05. 실행 이력 및 모니터링
| ID | 요구사항 | 우선순위 |
|---|---|---|
| FR-05-1 | 공통 `JobExecutionListener`가 Job 시작/종료 시 `TB_DSPT_EXEC_L`에 실행내역을 기록한다 (실행구분, 상태, 시작/종료 일시, 처리/성공/실패 건수, 오류메시지, 실행 서버명). Listener는 `BatchConfig`가 모든 Job/Step 빈에 자동 등록하며, 기록 실패는 로그만 남기고 업무 Job을 실패시키지 않는다. | 필수 |
| FR-05-2 | 처리 건수는 모든 Step의 `readCount`, `writeCount`, `skipCount`를 합산한다. | 필수 |
| FR-05-3 | 오류메시지는 최상위 예외 메시지와 root cause를 4000byte 이내로 잘라 저장한다. | 필수 |
| FR-05-4 | 실행 이력 조회 API는 기간·업무모듈·배치직무ID·상태 조건과 페이징을 지원한다. | 필수 |
| FR-05-5 | 실행 상세 조회 시 `BATCH_STEP_EXECUTION` 기반 Step별 처리 현황을 함께 반환한다. | 필수 |
| FR-05-6 | 로그에는 MDC로 `jobId`, `jobExecutionId`, `stepName`을 넣는다 (`logback-spring.xml` 패턴에 포함). | 필수 |

### FR-06. 데이터 보관 주기
| ID | 요구사항 | 우선순위 |
|---|---|---|
| FR-06-1 | 샘플 Job `execLogPurgeJob`(SY)이 매일 새벽에 보관 기간(기본 90일)이 지난 `BATCH_*` 메타 데이터와 `TB_DSPT_EXEC_L`을 삭제한다. | 필수 |
| FR-06-2 | 보관 기간은 `coder.batch.retention-days`로 설정한다. | 필수 |

---

## 7. 데이터 설계

### 7.1 프레임워크 메타 테이블 (네이밍 표준 예외)

Spring Batch(`BATCH_*`)와 Quartz(`QRTZ_*`) 테이블은 프레임워크가 테이블명을 고정하므로 `CLAUDE.md` 5장 DB 네이밍 표준의 **예외**로 등록한다.

| 구분 | 테이블 | DDL 출처 |
|---|---|---|
| Spring Batch | `BATCH_JOB_INSTANCE`, `BATCH_JOB_EXECUTION`, `BATCH_JOB_EXECUTION_PARAMS`, `BATCH_JOB_EXECUTION_CONTEXT`, `BATCH_STEP_EXECUTION`, `BATCH_STEP_EXECUTION_CONTEXT`, 시퀀스 3종 | `org/springframework/batch/core/schema-postgresql.sql` |
| Quartz | `QRTZ_JOB_DETAILS`, `QRTZ_TRIGGERS`, `QRTZ_CRON_TRIGGERS`, `QRTZ_SIMPLE_TRIGGERS`, `QRTZ_SIMPROP_TRIGGERS`, `QRTZ_BLOB_TRIGGERS`, `QRTZ_CALENDARS`, `QRTZ_PAUSED_TRIGGER_GRPS`, `QRTZ_FIRED_TRIGGERS`, `QRTZ_SCHEDULER_STATE`, `QRTZ_LOCKS` | `org/quartz/impl/jdbcjobstore/tables_postgres.sql` |

- 모든 환경에서 자동 생성을 끄고(`spring.batch.jdbc.initialize-schema: never`, `spring.quartz.jdbc.initialize-schema: never`), `schema/batch/`의 DDL을 최초 1회 직접 반영한다.
- Quartz 원본 스크립트는 `DROP TABLE`로 시작하므로 `always`로 두면 기동할 때마다 등록된 스케줄이 삭제된다. 저장소의 스크립트는 DROP 문을 주석 처리했다.
- 통합 테스트(`application-test.yml`, `BatchFlowIntegrationTest`)에서만 PostgreSQL 임시 스키마에 `always`를 쓴다 (컨텍스트 종료 시 스키마 삭제).

### 7.2 업무 테이블

> 단어약어는 `schema/dictionary/word-abbreviations.md` 기준이다. 배치는 `DSPT`, 직무는 `JOB`, 실행은 `EXEC`, 스케줄은 `SCHE`, 파라미터는 `PARM`을 쓴다.
> ⚠ 표시 단어는 기존 사전에 없어 `word-abbreviations.md` 1-13절에 등록했다 (DB 표준 담당자 검토 대상).

#### TB_DSPT_JOB_M (배치직무기본)

| 컬럼명 | 논리명 | 타입 | NULL | 기본값 | 비고 |
|---|---|---|---|---|---|
| `JOB_ID` | 직무ID | VARCHAR(100) | N | | PK. 배치ID (`BH` + 업무 2자리 + 순번 3자리, 예: `BHFL001`) |
| `JOB_NM` | 직무명 | VARCHAR(100) | N | | |
| `EXEC_CLASS_NM` | 실행클래스명 | VARCHAR(200) | N | | Job 빈을 정의한 클래스, 패키지 포함 (예: `kr.or.coder.batch.job.system.execlogpurge.ExecLogPurgeJobConfig`). 클래스당 Job 빈 1개 |
| `WRK_MDUL_CD` | 업무모듈코드 | VARCHAR(2) | N | | `SY`, `MS` (업무약어 사전) |
| `SCHE_EXPR` | 스케줄표현식 ⚠ | VARCHAR(100) | Y | | Quartz cron (초 분 시 일 월 요일 [년]). NULL이면 수동 실행 전용 |
| `MSFR_PLCY_CD` | 미스파이어정책코드 ⚠ | VARCHAR(2) | N | `'01'` | 공통코드: `01` 건너뜀(DO_NOTHING), `02` 즉시 1회 실행(FIRE_ONCE_NOW) |
| `MLT_EXEC_YN` | 다중실행여부 | CHAR(1) | N | `'N'` | 기존 `TB_PRGM_M` 용어 재사용 |
| `JOB_PARM_CONT` | 직무파라미터내용 | VARCHAR(4000) | Y | | 기본 JobParameters (JSON, 예: `{"retentionDays":"90"}`) |
| `USE_YN` | 사용여부 | CHAR(1) | N | `'N'` | |
| `RMK` | 비고 | VARCHAR(200) | Y | | |
| `REFL_PRGM_ID` | 반영프로그램ID | VARCHAR(50) | Y | | |
| `FIRS_REGR_ID` | 최초등록자ID | VARCHAR(10) | N | | |
| `FIRS_REG_DT` | 최초등록일시 | TIMESTAMP | N | `CURRENT_TIMESTAMP` | |
| `FINA_REGR_ID` | 최종등록자ID | VARCHAR(10) | N | | |
| `FINA_REG_DT` | 최종등록일시 | TIMESTAMP | N | `CURRENT_TIMESTAMP` | |

- PK: `TB_DSPT_JOB_M_PK (JOB_ID)`
- Index: `TB_DSPT_JOB_M_IX01 (WRK_MDUL_CD, USE_YN)`

#### TB_DSPT_EXEC_L (배치실행내역)

| 컬럼명 | 논리명 | 타입 | NULL | 기본값 | 비고 |
|---|---|---|---|---|---|
| `DSPT_EXEC_SN` | 배치실행일련번호 | BIGINT | N | | PK. 시퀀스 `SQ_TB_DSPT_EXEC_L_DSPT_EXEC_SN` |
| `JOB_ID` | 직무ID | VARCHAR(100) | N | | FK → `TB_DSPT_JOB_M` |
| `JOB_EXEC_ID` | 직무실행ID | BIGINT | Y | | `BATCH_JOB_EXECUTION.JOB_EXECUTION_ID`. SKIP 건은 NULL |
| `EXEC_CLS_CD` | 실행구분코드 | VARCHAR(1) | N | | `S` 스케줄, `M` 수동, `R` 재시작 |
| `EXEC_STAT_CD` | 실행상태코드 | VARCHAR(20) | N | | `STARTED`, `COMPLETED`, `FAILED`, `STOPPED`, `ABANDONED`, `SKIP` |
| `EXEC_ST_DTTM` | 실행시작일시 | TIMESTAMP | N | | |
| `EXEC_END_DTTM` | 실행종료일시 | TIMESTAMP | Y | | |
| `PROC_CNT` | 처리건수 | BIGINT | N | `0` | Σ readCount (Batch 5 건수는 long) |
| `SUCC_CNT` | 성공건수 | BIGINT | N | `0` | Σ writeCount |
| `FAIL_CNT` | 실패건수 | BIGINT | N | `0` | Σ skipCount |
| `ERR_MSG_CONT` | 오류메시지내용 | VARCHAR(4000) | Y | | |
| `EXEC_SRV_NM` | 실행서버명 | VARCHAR(100) | Y | | Quartz `instanceId` |
| `FIRS_REGR_ID` | 최초등록자ID | VARCHAR(10) | N | | 스케줄 실행은 `BATCH`, 수동 실행은 요청 사용자 ID |
| `FIRS_REG_DT` | 최초등록일시 | TIMESTAMP | N | `CURRENT_TIMESTAMP` | |
| `FINA_REGR_ID` | 최종등록자ID | VARCHAR(10) | N | | |
| `FINA_REG_DT` | 최종등록일시 | TIMESTAMP | N | `CURRENT_TIMESTAMP` | |

- PK: `TB_DSPT_EXEC_L_PK (DSPT_EXEC_SN)`
- FK: `TB_DSPT_EXEC_L_FK01 (JOB_ID) → TB_DSPT_JOB_M`
- Index: `TB_DSPT_EXEC_L_IX01 (JOB_ID, EXEC_ST_DTTM)`, `TB_DSPT_EXEC_L_IX02 (JOB_EXEC_ID)`, `TB_DSPT_EXEC_L_IX03 (EXEC_ST_DTTM)`
- 배치직무를 삭제하면 FK 때문에 해당 직무의 실행내역도 함께 삭제한다 (운영 중 중단은 삭제 대신 `USE_YN='N'` 권장).

#### 신규 등록 단어 (⚠)

| 한글 | 제안 약어 | 사용 컬럼 |
|---|---|---|
| 표현식 (Expression) | `EXPR` | `SCHE_EXPR` |
| 미스파이어 (Misfire) | `MSFR` | `MSFR_PLCY_CD` |
| 정책 (Policy) | `PLCY` | `MSFR_PLCY_CD` |

> `TB_PRGM_M`처럼 기존 DDL은 일시 컬럼에 `_DT`(TIMESTAMP)를 쓰지만, 사전은 일시를 `DTTM`으로 정의한다. 공통 컬럼(`FIRS_REG_DT`, `FINA_REG_DT`)은 기존 표준을 따르고, 업무 일시 컬럼은 사전대로 `DTTM`을 쓴다.

### 7.3 공통코드 (TB_COMM_CD_M / TB_COMM_CD_D, `WRK_MDUL_CD='SY'`)

| 공통분류 | 코드 | 코드명 |
|---|---|---|
| 실행구분 | `S` / `M` / `R` | 스케줄 / 수동 / 재시작 |
| 실행상태 | `STARTED` / `COMPLETED` / `FAILED` / `STOPPED` / `ABANDONED` / `SKIP` | 실행중 / 완료 / 실패 / 중지 / 폐기 / 건너뜀 |
| 미스파이어정책 | `01` / `02` | 건너뜀 / 즉시 1회 실행 |

공통분류코드(`COMM_CTG_CD`, 5자리) 값은 공통코드 관리 담당자가 채번한다.

---

## 8. 관리 API 명세

- Base URL: `http://{host}:8081`
- 모든 응답은 `coder-core-common`의 `ApiResponse<T>` 규격을 따른다.
- 인증: `coder-core-security` JWT (온라인과 같은 `coder.security.jwt.secret`을 공유해 `coder-backend`에서 발급한 토큰을 그대로 검증한다). 관리자 권한 사용자만 호출할 수 있다 (권한 코드는 권한그룹 체계에 맞춰 확정).
- URL은 kebab-case 복수형 명사를 쓰고 동사를 넣지 않는다 (`CLAUDE.md` Backend 3장).

| Method | Endpoint | 설명 | Controller/Service 메서드 |
|---|---|---|---|
| `GET` | `/api/v1/batch-jobs` | 배치직무 목록 (업무모듈, 사용여부, 다음 실행 예정 일시, 스케줄 상태) | `getBatchJobList` |
| `GET` | `/api/v1/batch-jobs/{jobId}` | 배치직무 상세 | `getBatchJob` |
| `POST` | `/api/v1/batch-jobs` | 배치직무 등록 + 스케줄 등록 | `saveBatchJob` |
| `PUT` | `/api/v1/batch-jobs/{jobId}` | 배치직무 수정 + 재스케줄 | `saveBatchJob` |
| `DELETE` | `/api/v1/batch-jobs/{jobId}` | 배치직무 삭제 + 스케줄 삭제 | `deleteBatchJob` |
| `PATCH` | `/api/v1/batch-jobs/{jobId}/schedule` | 스케줄 일시정지/재개 (`{"schedStatus":"PAUSED"\|"NORMAL"}`) | `saveBatchJobSchedule` |
| `POST` | `/api/v1/batch-jobs/{jobId}/executions` | 수동 실행 (`{"params":{"baseDt":"20261008"}}`) | `saveBatchJobExecution` |
| `GET` | `/api/v1/batch-executions` | 실행 이력 목록 (기간·업무모듈·직무ID·상태, 페이징) | `getBatchExecutionList` |
| `GET` | `/api/v1/batch-executions/{execSn}` | 실행 상세 (Step별 현황 포함) | `getBatchExecution` |
| `POST` | `/api/v1/batch-executions/{execSn}/restarts` | 실패/중지 건 재시작 | `saveBatchExecutionRestart` |
| `PATCH` | `/api/v1/batch-executions/{execSn}` | 실행 중지/폐기 (`{"execStatCd":"STOPPED"\|"ABANDONED"}`) | `saveBatchExecutionStatus` |
| `GET` | `/actuator/health` | 헬스체크 (Quartz Scheduler 상태 포함) | |

Mapper 메서드는 SQL 태그에 맞춰 `selectBatchJobList`, `selectBatchJob`, `insertBatchJob`, `updateBatchJob`, `deleteBatchJob`, `insertBatchExecution`, `updateBatchExecution`, `selectBatchExecutionList` 등으로 나눈다.

### 8.1 오류 응답 / 메시지 키

| 상황 | HTTP | 메시지 키 (`messages_ko.properties`) |
|---|---|---|
| 배치직무 없음 | 404 | `sy.batch.job.not.found` |
| Job 빈 미등록 | 400 | `sy.batch.job.bean.not.registered` |
| cron 표현식 오류 | 400 | `sy.batch.schedule.invalid.expression` |
| 이미 실행 중 (다중실행 불가) | 409 | `sy.batch.job.already.running` |
| 재시작 불가 상태 | 409 | `sy.batch.execution.not.restartable` |
| 중지 불가 상태 | 409 | `sy.batch.execution.not.stoppable` |

---

## 9. 설정 (application.yml 안)

```yaml
server:
  port: 8081
  shutdown: graceful

spring:
  application:
    name: coder-batch
  lifecycle:
    timeout-per-shutdown-phase: 60s

  datasource:
    hikari:
      driver-class-name: org.postgresql.Driver
      jdbc-url: jdbc:postgresql://localhost:5432/coder?currentSchema=coder
      username: coder
      password: ${CODER_DB_PASSWORD:coder1234!}
      maximum-pool-size: 20          # Quartz 스레드 + Step 병렬 + 관리 API 여유분
      minimum-idle: 5

  batch:
    job:
      enabled: false                 # 기동 시 Job 자동 실행 금지 (Quartz가 실행)
    jdbc:
      initialize-schema: never       # DDL은 schema/batch/*.sql 로 관리 (7.1 참고)

  quartz:
    job-store-type: jdbc
    jdbc:
      initialize-schema: never
    wait-for-jobs-to-complete-on-shutdown: true
    overwrite-existing-jobs: true
    properties:
      org.quartz.scheduler.instanceName: coder-batch-scheduler
      org.quartz.scheduler.instanceId: AUTO
      org.quartz.jobStore.driverDelegateClass: org.quartz.impl.jdbcjobstore.PostgreSQLDelegate
      org.quartz.jobStore.tablePrefix: QRTZ_
      org.quartz.jobStore.isClustered: true
      org.quartz.jobStore.clusterCheckinInterval: 15000
      org.quartz.jobStore.misfireThreshold: 60000
      org.quartz.threadPool.threadCount: 10

coder:
  mybatis:
    mapper-locations: classpath*:mapper/**/*.xml
    type-aliases-package: kr.or.coder.batch.frame.dto,kr.or.coder.core.db.model
    map-underscore-to-camel-case: true
    call-setters-on-nulls: true
    use-generated-keys: true
    default-statement-timeout: 0     # 배치는 문장 타임아웃 미적용 (Step별로 개별 지정)

  security:
    enabled: true
    csrf-enabled: false
    cors-enabled: true
    jwt:
      secret: ${CODER_JWT_SECRET:...} # coder-backend와 동일 값 (로컬 기본값은 백엔드와 같음, 운영은 환경변수 필수)
      issuer: "coder-auth"
    permit-urls:
      - "/actuator/health"
      - "/swagger-ui/**"
      - "/v3/api-docs/**"

  message:
    basename: messages/messages
    encoding: UTF-8
    default-locale: ko

  batch:
    system-user-id: BATCH            # 스케줄 실행 시 FIRS_REGR_ID / FINA_REGR_ID
    retention-days: 90               # 실행 이력 보관 기간
    default-chunk-size: 1000

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

> 현재 `coder-backend/application.yml`에는 JWT secret과 DB 비밀번호 기본값이 평문으로 들어 있다. 배치는 처음부터 환경변수로 주입하고, 백엔드도 같은 방식으로 바꿀 것을 권장한다.

---

## 10. 배치 Job 개발 표준

### 10.1 명명 규칙

| 대상 | 규칙 | 예시 |
|---|---|---|
| Job 패키지 | `kr.or.coder.batch.job.{업무}.{기능}` (시스템관리는 `system`, backend `biz.system`과 동일) | `kr.or.coder.batch.job.system.execlogpurge` |
| Job 설정 클래스 | `{기능}JobConfig` | `ExecLogPurgeJobConfig` |
| Job 빈 이름 | `{기능}Job` (camelCase). 배치직무에는 `{기능}JobConfig` 클래스 전체 이름을 `EXEC_CLASS_NM`으로 등록 | `execLogPurgeJob` |
| 배치ID (`JOB_ID`) | `BH` + 업무 2자리 + 순번 3자리 | `BHFL001` |
| Step 빈 이름 | `{기능}Step` 또는 `{기능}{세부}Step` | `execLogPurgeStep`, `memberGradeCalculateStep` |
| Tasklet | `{기능}Tasklet` | `ExecLogPurgeTasklet` |
| Processor | `{기능}Processor` | `MemberGradeProcessor` |
| Mapper | `{도메인}BatchMapper` (온라인 Mapper와 분리) | `MemberGradeBatchMapper` |
| Mapper SQL ID | `select~` / `insert~` / `update~` / `delete~` | `selectMemberGradeTargetList` |
| JobParameter 키 | camelCase, DB 용어를 풀어 쓴 형태 | `baseDt`, `wrkMdulCd`, `requestDttm` |

### 10.2 구현 원칙
1. **Job 설정**: Boot 3 / Batch 5 방식대로 `JobBuilder(name, jobRepository)`, `StepBuilder(name, jobRepository)`를 쓰고, `.chunk(size, transactionManager)`에 `coder-core-db`의 `transactionManager`를 명시해서 넘긴다.
2. **Reader**
   - 대량 순차 처리: `MyBatisCursorItemReader` (커서 유지. PostgreSQL에서는 `fetchSize`를 지정해야 하고 커넥션 `autoCommit=false` 조건을 확인한다)
   - 재시작 정합성이 중요한 경우: `MyBatisPagingItemReader` (`ORDER BY` 고정 키 필수, 처리 중 대상 데이터가 바뀌면 누락될 수 있음에 유의)
3. **Writer**: `MyBatisBatchItemWriter` (JDBC batch). `assertUpdates=true`를 기본으로 둔다.
4. **재시작 가능성**: Reader에 `saveState=true`를 유지하고, 같은 JobParameters로 다시 실행해도 결과가 같도록(멱등) 작성한다 (예: 대상 일자 데이터를 DELETE 후 INSERT).
5. **Skip / Retry**: 데이터 오류는 `faultTolerant().skip(...).skipLimit(n)`, 일시적 DB 오류는 `retry(TransientDataAccessException.class).retryLimit(3)`로 처리한다. skip 건은 `SkipListener`로 로그를 남긴다.
6. **감사 컬럼**: 배치가 쓰는 업무 테이블의 `FIRS_REGR_ID` / `FINA_REGR_ID`에는 `coder.batch.system-user-id`(`BATCH`)를 넣는다. 최초 INSERT 시 변경자 정보에도 같은 값을 넣는다 (`CLAUDE.md` 5.7).
7. **@StepScope / @JobScope**: JobParameters를 쓰는 Reader/Processor/Tasklet은 반드시 `@StepScope`로 선언한다.
8. **트랜잭션**: Tasklet 안에서 `@Transactional`을 중복 선언하지 않는다 (Step이 트랜잭션 경계).

### 10.3 샘플 코드 골격

```java
@Configuration
@RequiredArgsConstructor
public class ExecLogPurgeJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    // 실행내역/MDC Listener는 BatchConfig가 모든 Job/Step에 자동 등록하므로 따로 붙이지 않는다.
    @Bean
    public Job execLogPurgeJob(Step execLogPurgeStep) {
        return new JobBuilder("execLogPurgeJob", jobRepository)
                .start(execLogPurgeStep)
                .build();
    }

    @Bean
    public Step execLogPurgeStep(ExecLogPurgeTasklet execLogPurgeTasklet) {
        return new StepBuilder("execLogPurgeStep", jobRepository)
                .tasklet(execLogPurgeTasklet, transactionManager)
                .build();
    }
}
```

```java
// MLT_EXEC_YN = 'Y'
public class BatchLaunchQuartzJob extends QuartzJobBean {

    private final BatchLaunchExecutor batchLaunchExecutor;

    public BatchLaunchQuartzJob(BatchLaunchExecutor batchLaunchExecutor) {
        this.batchLaunchExecutor = batchLaunchExecutor;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        // DB 조회 → 다중실행 검사(SKIP) → JobParameters 생성 → JobLauncher.run (동기)
        batchLaunchExecutor.launch(context);
    }
}

// MLT_EXEC_YN = 'N'
@DisallowConcurrentExecution
public class SerialBatchLaunchQuartzJob extends BatchLaunchQuartzJob { ... }
```

> Spring Batch 5.1의 `DefaultBatchConfiguration`이 `JobRegistryBeanPostProcessor`를 이미 선언하므로 Job 빈은 `JobRegistry`에 자동 등록된다. 같은 역할의 빈을 추가로 선언하면 `DuplicateJobException`이 발생한다.

---

## 11. 비기능 요구사항

| ID | 구분 | 요구사항 |
|---|---|---|
| NFR-01 | 가용성 | 2개 이상 노드로 Quartz Cluster를 구성한다. 노드 하나가 다운되면 `clusterCheckinInterval`(15초) 안에 다른 노드가 Trigger를 넘겨받는다. 실행 중이던 Batch Job은 `STARTED`로 남으므로 운영자가 `ABANDONED` 처리 후 재실행한다. |
| NFR-02 | 정합성 | 같은 배치직무는 클러스터 전체에서 동시에 한 번만 실행된다 (`@DisallowConcurrentExecution` + `MLT_EXEC_YN` 검사). |
| NFR-03 | 성능 | 기본 chunk size는 1,000이며, 100만 건 처리 기준 Job별 목표 처리시간을 설계 단계에서 정의한다. 온라인 피크 시간대(09~18시)에는 대량 Job 스케줄을 피한다. |
| NFR-04 | 자원 격리 | 배치는 온라인과 별도 프로세스·별도 커넥션 풀로 실행해 온라인 응답에 영향을 주지 않는다. |
| NFR-05 | 무중단 배포 | `server.shutdown=graceful`, `wait-for-jobs-to-complete-on-shutdown=true`로 실행 중 Job을 마친 뒤 종료한다. 롤링 배포 시 한 노드씩 내린다. |
| NFR-06 | 관측성 | Actuator `health`에 Quartz 상태를 포함한다. 로그에 MDC(`jobId`, `jobExecutionId`)를 넣어 Job별로 추적할 수 있게 한다. |
| NFR-07 | 보안 | 관리 API는 JWT 인증과 관리자 권한을 요구한다. DB 비밀번호와 JWT secret은 환경변수로 주입한다. |
| NFR-08 | 시간대 | JVM과 Quartz 시간대는 `Asia/Seoul`로 고정한다 (`-Duser.timezone=Asia/Seoul`, CronTrigger `inTimeZone`). |
| NFR-09 | 테스트 | `./gradlew test`는 외부 DB 없이 단위 테스트를 수행한다. `./gradlew integrationTest`(`@Tag("integration")`)는 PostgreSQL에서 실행하며, E2E 테스트(기동 → 스케줄 동기화 → 관리 API → Quartz 수동 실행 → 실행내역)는 임시 스키마를 만들어 쓴다. 신규 Job은 정상, skip, 재시작 시나리오 테스트를 포함한다. |

---

## 12. 일정 (안)

| 단계 | 내용 | 산출물 | 상태 |
|---|---|---|---|
| Phase 1-1 | 프로젝트 골격, 메타 테이블 DDL, Batch/Quartz 설정, 클러스터 검증 | `coder-batch` 프로젝트, `schema/batch/*.sql` | 구현 완료 / PostgreSQL·2노드 클러스터 검증 미수행 |
| Phase 1-2 | 배치직무/실행내역 테이블, 스케줄 동기화, 실행 브리지, 공통 Listener | `TB_DSPT_JOB_M.sql`, `TB_DSPT_EXEC_L.sql` | 구현 완료 |
| Phase 1-3 | 관리 API, 샘플 Job 2종, 테스트, README | API, 테스트 코드 | 구현 완료 (E2E 통합 테스트 통과) |
| Phase 2 | 프론트엔드 배치 관리 화면, 실패 알림 | 화면설계서 (`documents/batch/`) | |
| Phase 3 | Job 선후행 의존성, 대시보드 | | |

---

## 13. 결정 필요 사항 (Open Issues)

| # | 항목 | 선택지 | 제안 |
|---|---|---|---|
| 1 | 메타 테이블 위치 | ① `coder` 스키마 공용 ② 별도 스키마 `coder_batch` | ① (설정 단순화). 운영 정책상 분리가 필요하면 ② |
| 2 | 배치 관리 API 제공 위치 | ① `coder-batch`가 직접 제공 ② `coder-backend`가 DB/Quartz를 통해 간접 제어 | ① (Quartz Scheduler 객체에 직접 접근해야 함) |
| 3 | 신규 단어 검토 | `EXPR`, `MSFR`, `PLCY` (`word-abbreviations.md` 1-13절에 등록함) | DB 표준 담당자 검토 |
| 4 | 실패 알림 채널 | 메일 / 사내 메신저 / Slack | Phase 2에서 결정 |
| 5 | `AuditInterceptor` 연계 | 현재 `BaseAuditDto`(createdAt/updatedAt)와 표준 감사 컬럼(`FIRS_REGR_ID` 등)이 불일치 | `coder-core-db`에서 표준 컬럼 기준으로 맞추는 별도 과제로 분리 |
| 6 | ~~배치 관리 화면 ID~~ | ~~화면ID 채번 규칙 (`SYPG020` 형식)~~ | → `SYLK050` 배치관리로 확정 (`documents/frontend/spec/system/SYLK050.md`) |
