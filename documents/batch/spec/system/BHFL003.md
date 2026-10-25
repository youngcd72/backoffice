# BHFL003 장기 미접속 사용자 잠금 프로그램 사양서

| 항목 | 내용 |
|---|---|
| 배치ID | BHFL003 |
| 배치명 | 장기 미접속 사용자 잠금 |
| 업무모듈 | SY (시스템) |
| Job 유형 | Chunk (Reader / Processor / Writer) |
| Job 빈 / Step | `inactiveUserLockJob` / `inactiveUserLockStep` |
| 실행클래스 (`EXEC_CLASS_NM`) | `kr.or.coder.batch.job.system.inactiveuserlock.InactiveUserLockJobConfig` |
| 소스 | `workspace/coder-batch/src/main/java/kr/or/coder/batch/job/system/inactiveuserlock/` (`InactiveUserLockJobConfig`, `InactiveUserLockProcessor`, `InactiveUser`, `mapper/InactiveUserLockBatchMapper`) |
| SQL | `workspace/coder-batch/src/main/resources/mapper/job/system/InactiveUserLockBatchMapper.xml` |
| 스케줄 | `0 30 2 * * ?` (매일 02:30) |
| 미스파이어정책 / 다중실행 | `01` 건너뜀 / `N` (동시 실행 불가) |
| 사용여부 (시드) | **`N`** (Chunk형 샘플 - 운영 적용 전 검토. 스케줄 등록 안 됨) |
| 직무파라미터 (시드) | `{"idleDays":"90"}` |
| 대상 테이블 | `TB_USR_M` 사용자기본 (SELECT / UPDATE) |
| 테스트 | 없음 |
| 작성자 | ywkim |
| 작성일 | 2026-10-25 |

### 변경 이력

| 일자 | 작성자 | 내용 |
|---|---|---|
| 2026-10-25 | ywkim | 최초 작성 (현재 구현 기준) |

---

## 1. 개요

최종접속일시(`FINA_ACC_DTTM`)가 기준 일수(기본 90일)보다 오래된 사용 중 사용자를 잠근다(`USR_LOCK_YN = 'Y'`). 잠긴 사용자는 사용자권한관리(SYAU010)에서 잠금을 풀어야 로그인할 수 있다. 대량 처리용 Chunk형 Job의 샘플이며, 시드에서 사용여부가 `N`이라 배치관리 화면에서 사용으로 바꾸기 전에는 스케줄로 돌지 않는다.

---

## 2. 실행 정보

### 2.1 실행 방법

| 구분 | 방법 |
|---|---|
| 스케줄 | 사용여부 `Y`일 때 매일 02:30 (실행구분 `S`) |
| 수동 실행 | 사용여부 `Y`일 때 배치관리(SYLK050) 또는 `POST /api/v1/batch-jobs/BHFL003/executions` (실행구분 `M`) |
| 재시작 | 실패·중지 건을 같은 JobParameters로 재시작 (실행구분 `R`) - 9절 주의 |

### 2.2 JobParameters

| 키 | 필수 | 기본값 | 형식 | 설명 |
|---|---|---|---|---|
| `idleDays` | | 직무파라미터 `90`, 없으면 코드 기본값 90 (`DEFAULT_IDLE_DAYS`) | 정수(일) | 미접속 기준 일수. 오늘 0시에서 이 일수를 뺀 시각보다 전에 마지막으로 접속한 사용자가 대상 |

---

## 3. Job 구성

```text
inactiveUserLockJob
 └─ inactiveUserLockStep (Chunk, chunk size = coder.batch.default-chunk-size 1000)
      ├─ Reader    inactiveUserReader      MyBatisCursorItemReader  selectInactiveUserList (fetchSize 1000)
      ├─ Processor InactiveUserLockProcessor  잠금일시·수정자 설정
      └─ Writer    inactiveUserLockWriter  MyBatisBatchItemWriter   updateUserLock (assertUpdates=true)
      faultTolerant: retry TransientDataAccessException ×3, skip DataIntegrityViolationException 최대 10건
```

| 구성요소 | 클래스 / 빈 | 설정 |
|---|---|---|
| Job | `inactiveUserLockJob` | Step 1개 |
| Step | `inactiveUserLockStep` | chunk 1000건마다 커밋 |
| Reader | `inactiveUserReader` (`@StepScope`) | Cursor 방식, `saveState=true`. 처리 중 대상 조건(`USR_LOCK_YN`)이 바뀌므로 Paging Reader 대신 Cursor를 쓴다 (Paging은 OFFSET이 밀려 누락) |
| Processor | `InactiveUserLockProcessor` | `usrLockDttm` = 현재 시각, `finaRegrId` = `BATCH` |
| Writer | `inactiveUserLockWriter` | JDBC batch UPDATE, 갱신 0건이면 오류 |
| Item | `InactiveUser` | `usrId`, `usrNm`, `finaAccDttm`, `usrLockDttm`, `finaRegrId` |

---

## 4. 처리 대상

| 조건 | 설명 |
|---|---|
| `USE_YN = 'Y'` | 사용 중 사용자 |
| `COALESCE(USR_LOCK_YN, 'N') = 'N'` | 아직 잠기지 않은 사용자 |
| `FINA_ACC_DTTM < baseDttm` | 기준일시 전에 마지막으로 접속 (최종접속일시가 없는 신규 계정은 제외) |

- 정렬: `USR_ID` 오름차순.
- 변경 내용: `USR_LOCK_YN = 'Y'`, `USR_LOCK_DTTM` = 처리 시각, `FINA_REGR_ID` = `BATCH`, `FINA_REG_DT` = 현재 시각.

---

## 5. 처리 로직

1. Reader 생성 시 `idleDays`로 `baseDttm`(오늘 00:00 - idleDays일)을 계산한다.
2. Reader가 대상 사용자를 Cursor로 1건씩 읽는다.
3. Processor가 잠금일시와 수정자를 채운다.
4. 1000건이 모이면 Writer가 일괄 UPDATE하고 커밋한다. 남은 건은 마지막 chunk로 커밋한다.
5. 일시적 DB 오류(`TransientDataAccessException`)는 해당 chunk를 3회까지 재시도한다. 무결성 위반(`DataIntegrityViolationException`)은 건너뛰고 skip 건수로 남기며, 10건을 넘으면 Step이 실패한다.

- 이미 잠긴 사용자는 대상에서 빠지므로 다시 실행해도 결과가 같다 (멱등).

---

## 6. SQL

### 6.1 `selectInactiveUserList` (Reader, fetchSize 1000)

```sql
SELECT USR_ID, USR_NM, FINA_ACC_DTTM
  FROM TB_USR_M
 WHERE USE_YN = 'Y'
   AND COALESCE(USR_LOCK_YN, 'N') = 'N'
   AND FINA_ACC_DTTM < #{baseDttm}
 ORDER BY USR_ID
```

- PostgreSQL JDBC는 `autoCommit=false`일 때만 fetchSize 단위로 커서를 쓴다. 대상이 매우 많으면 메모리 사용량을 확인하고, 필요하면 키 범위 분할(Partitioning)을 검토한다.

### 6.2 `updateUserLock` (Writer)

```sql
UPDATE TB_USR_M
   SET USR_LOCK_YN = 'Y', USR_LOCK_DTTM = #{usrLockDttm},
       FINA_REGR_ID = #{finaRegrId}, FINA_REG_DT = CURRENT_TIMESTAMP
 WHERE USR_ID = #{usrId}
```

---

## 7. 실행 결과 기록

| 항목 | 값 |
|---|---|
| `TB_DSPT_EXEC_L.PROC_CNT` | 읽은 대상 사용자 수 (readCount) |
| `TB_DSPT_EXEC_L.SUCC_CNT` | 잠금 처리한 사용자 수 (writeCount) |
| `TB_DSPT_EXEC_L.FAIL_CNT` | 건너뛴 건수 (skipCount) |
| 로그 | `coder-batch.log`, MDC `[jobId:jobExecutionId:stepName]`. 건너뛴 건의 개별 로그(SkipListener)는 없음 |
| 실패 알람 | `FAILED`면 알람수신자에게 `TB_ALRM_L` 알람 (BHFL001과 같음) |

---

## 8. 오류 처리 / 재실행

| 상황 | 결과 | 조치 |
|---|---|---|
| `idleDays`가 정수가 아님 | Step 시작 전 실패 → `FAILED` | 올바른 값으로 다시 수동 실행 |
| 일시적 DB 오류 | chunk 3회 재시도, 계속 실패하면 `FAILED` | 이미 커밋된 chunk는 유지. 원인 해결 후 재실행 |
| 무결성 위반 | 건너뜀(skip). 10건 초과 시 `FAILED` | 데이터 확인 |
| 갱신 0건 (`assertUpdates`) | 해당 chunk 실패 → `FAILED` | 처리 중 사용자가 삭제된 경우 등. 재실행 |
| 이전 실행이 아직 실행 중 | `SKIP` | 조치 불필요 |

---

## 9. 비고 / 미결사항

- **재시작 시 누락 가능:** Reader가 `saveState=true`라 재시작하면 이전 실행에서 읽은 건수만큼 결과를 건너뛰고 시작한다. 그런데 이미 잠근 사용자는 조회 조건에서 빠지므로, 건너뛴 만큼 아직 처리하지 않은 사용자가 누락된다. 이 Job은 재시작 대신 새로 실행하거나, Reader를 `saveState(false)`로 바꾸는 것이 맞다 (조건 자체가 멱등이라 처음부터 다시 읽어도 된다).
- 운영 적용 전 확인 사항: 사용여부 `N`(시드), 잠금 기준 일수(90일), 계정 종류별 예외(시스템·배치 계정, 협력업체 등) 여부, 잠금 시 사용자 알림 여부.
- 건너뛴(skip) 사용자를 기록하는 `SkipListener`가 없다 (batch PRD 10.2-5 권고). FAIL_CNT만 남는다.
- 통합 테스트가 없다.
