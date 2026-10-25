# BHFL002 배치 실행이력 정리 프로그램 사양서

| 항목 | 내용 |
|---|---|
| 배치ID | BHFL002 |
| 배치명 | 배치 실행이력 정리 |
| 업무모듈 | SY (시스템) |
| Job 유형 | Tasklet |
| Job 빈 / Step | `execLogPurgeJob` / `execLogPurgeStep` |
| 실행클래스 (`EXEC_CLASS_NM`) | `kr.or.coder.batch.job.system.execlogpurge.ExecLogPurgeJobConfig` |
| 소스 | `workspace/coder-batch/src/main/java/kr/or/coder/batch/job/system/execlogpurge/` (`ExecLogPurgeJobConfig`, `ExecLogPurgeTasklet`, `mapper/ExecLogPurgeBatchMapper`) |
| SQL | `workspace/coder-batch/src/main/resources/mapper/job/system/ExecLogPurgeBatchMapper.xml` |
| 스케줄 | `0 0 3 * * ?` (매일 03:00) |
| 미스파이어정책 / 다중실행 | `01` 건너뜀 / `N` (동시 실행 불가) |
| 사용여부 (시드) | `Y` |
| 직무파라미터 (시드) | `{"retentionDays":"90"}` |
| 대상 테이블 | `BATCH_*` Spring Batch 메타 6종, `TB_DSPT_EXEC_L` 배치실행내역 (DELETE) |
| 테스트 | `src/test/java/.../execlogpurge/ExecLogPurgeJobIntegrationTest.java` (DB 필요) |
| 작성자 | ywkim |
| 작성일 | 2026-10-25 |

### 변경 이력

| 일자 | 작성자 | 내용 |
|---|---|---|
| 2026-10-25 | ywkim | 최초 작성 (현재 구현 기준) |

---

## 1. 개요

배치 실행이 쌓이며 계속 커지는 실행 이력을 보관 기간(기본 90일)만 남기고 지운다. 대상은 Spring Batch 메타 테이블(`BATCH_*`)과 배치관리 화면(SYLK050)이 보는 배치실행내역(`TB_DSPT_EXEC_L`)이다. batch PRD FR-06(데이터 보관 주기)의 구현이며 Tasklet형 Job의 샘플이기도 하다.

---

## 2. 실행 정보

### 2.1 실행 방법

| 구분 | 방법 |
|---|---|
| 스케줄 | 매일 03:00 (실행구분 `S`) |
| 수동 실행 | 배치관리(SYLK050) 실행 팝업 또는 `POST /api/v1/batch-jobs/BHFL002/executions` (실행구분 `M`) |
| 재시작 | 실패·중지 건을 같은 JobParameters로 재시작 (실행구분 `R`) |

### 2.2 JobParameters

| 키 | 필수 | 기본값 | 형식 | 설명 |
|---|---|---|---|---|
| `retentionDays` | | 직무파라미터 `90`, 없으면 `coder.batch.retention-days`(90) | 정수(일) | 보관 기간. 오늘 0시에서 이 일수를 뺀 시각보다 이전 이력을 지운다 |

- 수동 실행 파라미터가 직무파라미터보다 우선한다 (예: `{"retentionDays":"30"}`).

---

## 3. Job 구성

```text
execLogPurgeJob
 └─ execLogPurgeStep (Tasklet, Step 트랜잭션 1개)
      └─ ExecLogPurgeTasklet
           삭제 순서 (FK 순서):
           1 BATCH_STEP_EXECUTION_CONTEXT
           2 BATCH_STEP_EXECUTION
           3 BATCH_JOB_EXECUTION_CONTEXT
           4 BATCH_JOB_EXECUTION_PARAMS
           5 BATCH_JOB_EXECUTION
           6 BATCH_JOB_INSTANCE (실행 건이 없는 인스턴스)
           7 TB_DSPT_EXEC_L
```

| 구성요소 | 클래스 / 빈 | 비고 |
|---|---|---|
| Job | `execLogPurgeJob` | Step 1개 |
| Step | `execLogPurgeStep` | 7개 DELETE가 한 트랜잭션으로 커밋된다 |
| Tasklet | `ExecLogPurgeTasklet` (`@StepScope`) | 생성 시 `retentionDays` 확정 |
| Mapper | `ExecLogPurgeBatchMapper` | |

---

## 4. 처리 대상

- 기준일시 `baseDttm` = 오늘 00:00 - `retentionDays`일.
- **Spring Batch 메타:** `END_TIME`이 있고(종료된 실행) `CREATE_TIME < baseDttm`인 `BATCH_JOB_EXECUTION`과 그 하위 Step·컨텍스트·파라미터. 실행 중인 건(`END_TIME` 없음)은 지우지 않는다.
- **JobInstance:** 실행 건이 하나도 남지 않은 `BATCH_JOB_INSTANCE` (기준일시와 무관).
- **배치실행내역:** `EXEC_ST_DTTM < baseDttm`이고 `EXEC_END_DTTM`이 있는 `TB_DSPT_EXEC_L`.

---

## 5. 처리 로직

1. Tasklet 생성 시 `retentionDays`를 정한다 (JobParameter → 없으면 설정값).
2. `baseDttm`을 계산하고 `purge batch execution logs before {baseDttm} (retentionDays=N)`을 INFO 로그로 남긴다.
3. 3절 순서대로 7개 DELETE를 실행하고 삭제 건수를 모두 더한다.
4. 합계를 Step 쓰기 건수(`writeCount`)에 더하고 `purged N row(s).`를 로그로 남긴다.
5. `RepeatStatus.FINISHED`로 1회 실행 후 끝난다.

- 같은 기준으로 다시 실행하면 지울 대상이 없어 0건으로 끝난다 (멱등).

---

## 6. SQL

| 쿼리 ID | 대상 | 조건 |
|---|---|---|
| (sql) `purgeTargetJobExecutionIds` | `BATCH_JOB_EXECUTION` | `END_TIME IS NOT NULL AND CREATE_TIME < #{baseDttm}` 인 `JOB_EXECUTION_ID` |
| `deleteBatchStepExecutionContext` | `BATCH_STEP_EXECUTION_CONTEXT` | 대상 JobExecution의 Step 실행 |
| `deleteBatchStepExecution` | `BATCH_STEP_EXECUTION` | 대상 JobExecution |
| `deleteBatchJobExecutionContext` | `BATCH_JOB_EXECUTION_CONTEXT` | 대상 JobExecution |
| `deleteBatchJobExecutionParams` | `BATCH_JOB_EXECUTION_PARAMS` | 대상 JobExecution |
| `deleteBatchJobExecution` | `BATCH_JOB_EXECUTION` | 대상 JobExecution |
| `deleteBatchJobInstance` | `BATCH_JOB_INSTANCE` | `NOT EXISTS (BATCH_JOB_EXECUTION)` |
| `deleteDsptExecLog` | `TB_DSPT_EXEC_L` | `EXEC_ST_DTTM < #{baseDttm} AND EXEC_END_DTTM IS NOT NULL` |

---

## 7. 실행 결과 기록

| 항목 | 값 |
|---|---|
| `TB_DSPT_EXEC_L.PROC_CNT` | 0 (Tasklet은 읽기 건수를 올리지 않음) |
| `TB_DSPT_EXEC_L.SUCC_CNT` | 7개 테이블 삭제 건수 합 (테이블별 건수는 로그에도 남지 않음) |
| `TB_DSPT_EXEC_L.FAIL_CNT` | 0 |
| 로그 | `coder-batch.log`, MDC `[jobId:jobExecutionId:stepName]` |
| 실패 알람 | `FAILED`면 알람수신자에게 `TB_ALRM_L` 알람 (BHFL001과 같음) |

---

## 8. 오류 처리 / 재실행

| 상황 | 결과 | 조치 |
|---|---|---|
| `retentionDays`가 정수가 아님 | Step 시작 전 실패 → `FAILED` | 올바른 값으로 다시 수동 실행 |
| DB 오류 | Step 트랜잭션 롤백 → `FAILED` (모두 반영 안 됨) | 원인 해결 후 재실행 (멱등) |
| 이전 실행이 아직 실행 중 | `SKIP` | 조치 불필요 |
| 서버 중지로 놓친 실행 | 미스파이어정책 `01`로 건너뜀 | 다음 날 실행에서 함께 정리됨 |

---

## 9. 비고 / 미결사항

- 7개 DELETE를 한 트랜잭션에서 처리한다. 첫 운영 적용이나 보관 기간 단축으로 지울 건수가 많으면 잠금·undo가 커질 수 있다. 이때는 `retentionDays`를 단계적으로 줄여 나눠 실행한다.
- `SUCC_CNT`는 테이블 구분 없이 합산한 행 수라 "삭제된 실행 건수"와 다르다. 테이블별 건수를 로그에 남길지 검토한다.
- `PROC_CNT`가 0으로 기록된다 (BHFL001 비고와 같은 결정 필요).
- 실패 이력도 보관 기간이 지나면 지워진다. 실패 건을 더 오래 남겨야 하면 조건 분리가 필요하다.
