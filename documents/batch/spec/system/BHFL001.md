# BHFL001 사용자상태변경 적용 프로그램 사양서

| 항목 | 내용 |
|---|---|
| 배치ID | BHFL001 |
| 배치명 | 사용자상태변경 적용 |
| 업무모듈 | SY (시스템) |
| Job 유형 | Tasklet |
| Job 빈 / Step | `userStatusApplyJob` / `userStatusApplyStep` |
| 실행클래스 (`EXEC_CLASS_NM`) | `kr.or.coder.batch.job.system.userstatusapply.UserStatusApplyJobConfig` |
| 소스 | `workspace/coder-batch/src/main/java/kr/or/coder/batch/job/system/userstatusapply/` (`UserStatusApplyJobConfig`, `UserStatusApplyTasklet`, `mapper/UserStatusApplyBatchMapper`) |
| SQL | `workspace/coder-batch/src/main/resources/mapper/job/system/UserStatusApplyBatchMapper.xml` |
| 스케줄 | `0 10 0 * * ?` (매일 00:10) |
| 미스파이어정책 / 다중실행 | `01` 건너뜀 / `N` (동시 실행 불가) |
| 사용여부 (시드) | `Y` |
| 대상 테이블 | `TB_USR_M` 사용자기본 (UPDATE) |
| 테스트 | `src/test/java/.../userstatusapply/UserStatusApplyJobIntegrationTest.java` (DB 필요) |
| 작성자 | ywkim |
| 작성일 | 2026-10-25 |

### 변경 이력

| 일자 | 작성자 | 내용 |
|---|---|---|
| 2026-10-25 | ywkim | 최초 작성 (현재 구현 기준) |

---

## 1. 개요

사용자상태(`USR_STAT_CD`)가 퇴직(`R`)이나 휴직(`L`)인 사용자를 로그인할 수 없게 만든다. 사용여부(`USE_YN`)를 `N`으로 바꾸고, 퇴직 사용자는 사용종료일자(`USE_END_DT`)를 퇴사일자(`RESI_DT`)로 맞춘다. 사용자권한관리(SYAU010) 등에서 상태만 바꿔 둔 사용자를 매일 새벽에 일괄 반영하는 용도다.

---

## 2. 실행 정보

### 2.1 실행 방법

| 구분 | 방법 |
|---|---|
| 스케줄 | Quartz 클러스터가 매일 00:10에 한 노드에서 실행 (실행구분 `S`) |
| 수동 실행 | 배치관리(SYLK050) 실행 팝업 또는 `POST /api/v1/batch-jobs/BHFL001/executions` (실행구분 `M`) |
| 재시작 | 실패·중지 건을 같은 JobParameters로 재시작 (실행구분 `R`) |

### 2.2 JobParameters

| 키 | 필수 | 기본값 | 형식 | 설명 |
|---|---|---|---|---|
| `baseDt` | | 실행일 | `yyyyMMdd` | 기준일자. 퇴사일자가 없는 퇴직 사용자의 사용종료일자 계산에 쓴다 |

- 배치직무 기본 파라미터(`JOB_PARM_CONT`)는 없다. 과거 일자로 다시 반영할 때 수동 실행에서 `{"baseDt":"20261008"}`처럼 넘긴다.
- 프레임이 붙이는 공통 파라미터(`requestDttm`, `fireId`, `jobId`, `execClsCd`, `reqUsrId`, `execSrvNm`)는 batch PRD FR-03-2를 따른다.

---

## 3. Job 구성

```text
userStatusApplyJob
 └─ userStatusApplyStep (Tasklet, Step 트랜잭션 1개)
      └─ UserStatusApplyTasklet
           ├─ updateRetiredUser(baseDt, BATCH)   퇴직(R) 반영
           └─ updateLeaveUser(BATCH)             휴직(L) 반영
```

| 구성요소 | 클래스 / 빈 | 비고 |
|---|---|---|
| Job | `userStatusApplyJob` | Step 1개 |
| Step | `userStatusApplyStep` | `tasklet(..., transactionManager)`. 두 UPDATE가 한 트랜잭션으로 커밋된다 |
| Tasklet | `UserStatusApplyTasklet` (`@StepScope`) | 생성 시 `baseDt`를 검증·확정한다 |
| Mapper | `UserStatusApplyBatchMapper` | |

---

## 4. 처리 대상

| 구분 | 조건 | 변경 내용 |
|---|---|---|
| 퇴직 | `USR_STAT_CD = 'R'` 이고, 아직 반영되지 않은 사용자 (아래 6.1 WHERE) | `USE_YN = 'N'`, `USE_END_DT` = 퇴사일자 (없으면 기존 종료일자와 기준일자 중 이른 날짜) |
| 휴직 | `USR_STAT_CD = 'L'` 이고 `USE_YN <> 'N'` | `USE_YN = 'N'` (사용종료일자는 유지) |

- 두 경우 모두 `FINA_REGR_ID`에 `coder.batch.system-user-id`(`BATCH`), `FINA_REG_DT`에 현재 시각을 넣는다.
- 재직(`T`) 등 다른 상태의 사용자는 바꾸지 않는다.

---

## 5. 처리 로직

1. Tasklet 생성 시 `baseDt`를 확정한다. 값이 있으면 `yyyyMMdd`로 파싱하고(형식이 틀리면 `DateTimeParseException`), 없으면 실행일을 쓴다.
2. `updateRetiredUser(baseDt, BATCH)`로 퇴직 사용자를 갱신한다.
3. `updateLeaveUser(BATCH)`로 휴직 사용자를 갱신한다.
4. 두 갱신 건수의 합을 Step 쓰기 건수(`writeCount`)에 더하고, `applied user status (baseDt=...): retired N row(s), leave N row(s).`를 INFO 로그로 남긴다.
5. `RepeatStatus.FINISHED`로 1회 실행 후 끝난다.

- 이미 반영된 사용자는 WHERE 조건에서 빠지므로 같은 날 여러 번 실행해도 결과가 같다 (멱등).

---

## 6. SQL

### 6.1 `updateRetiredUser` - 퇴직 사용자 반영

```sql
UPDATE TB_USR_M
   SET USE_YN       = 'N',
       USE_END_DT   = COALESCE(NULLIF(TRIM(RESI_DT), ''),
                               LEAST(COALESCE(NULLIF(TRIM(USE_END_DT), ''), #{baseDt}), #{baseDt})),
       FINA_REGR_ID = #{finaRegrId},
       FINA_REG_DT  = CURRENT_TIMESTAMP
 WHERE USR_STAT_CD = 'R'
   AND (USE_YN <> 'N'                                                  -- 아직 사용 중
        OR (RESI_DT 있음 AND USE_END_DT IS DISTINCT FROM RESI_DT)      -- 종료일자가 퇴사일자와 다름
        OR (RESI_DT 없음 AND USE_END_DT 없음))                         -- 종료일자 미설정
```

### 6.2 `updateLeaveUser` - 휴직 사용자 반영

```sql
UPDATE TB_USR_M
   SET USE_YN = 'N', FINA_REGR_ID = #{finaRegrId}, FINA_REG_DT = CURRENT_TIMESTAMP
 WHERE USR_STAT_CD = 'L'
   AND USE_YN <> 'N'
```

---

## 7. 실행 결과 기록

| 항목 | 값 |
|---|---|
| `TB_DSPT_EXEC_L.PROC_CNT` (readCount 합) | 0 (Tasklet은 읽기 건수를 올리지 않음) |
| `TB_DSPT_EXEC_L.SUCC_CNT` (writeCount 합) | 퇴직 갱신 건수 + 휴직 갱신 건수 |
| `TB_DSPT_EXEC_L.FAIL_CNT` (skipCount 합) | 0 |
| 로그 | `coder-batch.log`, MDC `[jobId:jobExecutionId:stepName]` |
| 실패 알람 | 상태가 `FAILED`면 배치직무의 알람수신자(`TB_DSPT_ALRM_RCVR_R`)에게 `TB_ALRM_L` 알람(유형 `DSPT`, 제목 `sy.batch.alarm.failed.title`, 연결 화면 SYLK050) |

---

## 8. 오류 처리 / 재실행

| 상황 | 결과 | 조치 |
|---|---|---|
| `baseDt` 형식 오류 | Step 시작 전 실패 → `FAILED` | 올바른 `yyyyMMdd`로 다시 수동 실행 |
| DB 오류 | Step 트랜잭션 롤백 → `FAILED` (두 UPDATE 모두 반영 안 됨) | 원인 해결 후 재시작 또는 재실행 (멱등) |
| 이전 실행이 아직 실행 중 | 이번 실행을 건너뜀 → `SKIP` (`MLT_EXEC_YN = N`) | 조치 불필요 |
| 서버 중지로 놓친 실행 | 미스파이어정책 `01`로 건너뜀 | 필요하면 수동 실행 |

---

## 9. 비고 / 미결사항

- 퇴사일자가 미래인 퇴직 사용자도 실행 즉시 `USE_YN = N`이 된다 (WHERE에 `RESI_DT <= baseDt` 조건이 없음). 퇴사 예정자를 퇴사일까지 사용하게 하려면 조건 추가가 필요하다.
- 휴직에서 복직(`T`)한 사용자의 `USE_YN`을 `Y`로 되돌리는 처리는 없다. 복직 시 화면에서 직접 바꿔야 한다.
- `PROC_CNT`가 0으로 기록된다. Tasklet 배치의 처리 건수를 `SUCC_CNT`로만 볼지, Tasklet에서 읽기 건수도 올릴지 정해야 한다 (BHFL002도 같음).
- 사용자상태 코드(`R` 퇴직, `L` 휴직)는 SQL에 직접 적혀 있다. 상태 코드가 바뀌면 SQL도 함께 고쳐야 한다.
