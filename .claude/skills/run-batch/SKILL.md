---
name: run-batch
description: CODER 배치 서버 실행 - Docker DB(postgres) 기동 후 coder-batch(8081)를 띄우고 헬스체크까지 확인한다. 요청 시 배치직무 수동 실행과 실행 이력 확인까지 한다. "배치 서버 띄워줘", "배치 실행", "BHFL002 돌려줘", "run-batch" 요청 시 사용.
---

# run-batch

`coder-batch`(Spring Batch + Quartz)를 **DB → (backend) → batch** 순서로 띄운다.
batch는 기동 시 DB 커넥션 풀과 Quartz 스케줄러를 만들기 때문에 postgres가 꺼져 있으면 바로 종료된다.

| 구성 | 실행 방식 | 포트 |
|---|---|---|
| postgres (`postgres:18`) | Docker 컨테이너 `postgres` | 5432 (DB `coder`, 계정 `batch` / 스키마 `batch,coder`) |
| coder-backend | `workspace/coder-backend` → `./gradlew bootRun` | 8080 (관리 API 호출용 JWT 발급. 서버만 띄울 때는 불필요) |
| coder-batch | `workspace/coder-batch` → `./gradlew bootRun` | 8081 |

로그는 세션 scratchpad 디렉터리(아래 `$S`)에 남긴다. Bash 툴(Git Bash) 기준이다.

## 0. 이미 떠 있는지 확인

```bash
netstat -ano | grep LISTENING | grep -E ":(5432|8080|8081) "
docker ps -a --format '{{.Names}}\t{{.Status}}\t{{.Ports}}'
```

이미 LISTENING 중인 포트의 서버는 다시 띄우지 않는다 (포트 충돌로 실패한다).

## 1. Docker DB 기동

```bash
docker start postgres
until docker exec postgres pg_isready -q; do sleep 1; done
```

- 컨테이너가 없으면(`No such container`) 새로 만들지 말고 사용자에게 알린다.
- Docker Desktop이 꺼져 있으면(`error during connect`) 사용자에게 실행을 요청한다.
- `batch` 스키마·계정이 없으면(기동 로그에 `password authentication failed for user "batch"`, `relation "qrtz_..." does not exist`) 임의로 만들지 말고 `workspace/coder-batch/README.md`의 "1. DB 준비" 절차를 사용자에게 안내한다.
  확인: `docker exec postgres psql -U coder -d coder -tAc "select count(*) from information_schema.tables where table_schema='batch'"` (Spring Batch·Quartz 테이블 17개)

## 2. batch 기동 (백그라운드)

Bash 툴 `run_in_background: true`로 실행한다. **`./gradlew`(sh 스크립트)를 쓴다** — Git Bash 백그라운드에서 `./gradlew.bat`은 `No such file or directory`(exit 127)로 실패한 적이 있다.

```bash
cd /c/coder/workspace/coder-batch && ./gradlew bootRun > "$S/batch.log" 2>&1
```

준비 대기 (성공·실패 모두 잡는다):

```bash
until grep -qE "Started .* in|run failed|BUILD FAILED|APPLICATION FAILED" "$S/batch.log"; do sleep 1; done
grep -E "Started .* in|run failed|BUILD FAILED|APPLICATION FAILED" "$S/batch.log"
```

실패했으면 `grep -nE "Caused by|ERROR" "$S/batch.log" | head`로 원인을 본다.

## 3. 동작 확인 (띄우기만 하고 끝내지 않는다)

```bash
curl -s localhost:8081/actuator/health                                    # status UP, db UP
curl -s -o /dev/null -w "%{http_code}\n" localhost:8081/api/v1/batch-jobs  # 401 (인증 필요 = 정상 기동)
```

> ⚠️ 서버가 떠 있는 동안 `USE_YN='Y'`인 배치직무는 cron 스케줄대로 **자동 실행된다**
> (예: BHFL001 사용자상태변경 적용은 매일 00:10에 `TB_USR_M`을 변경). 사용자에게 이 점을 알린다.

## 4. 배치직무 수동 실행 (요청 시에만)

사용자가 특정 배치 실행을 요청했을 때만 한다. 배치는 업무 데이터를 바꾸므로, 대상 직무와 파라미터가 요청에 명확하지 않으면 실행 전에 확인한다.

### 4-1. 관리자 JWT 발급 (coder-backend 필요)

관리 API는 JWT `role=ADMIN`이 필요하다. backend(8080)가 떠 있지 않으면 `run-server` 스킬 순서로 먼저 띄운다.

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/login -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"coder1234"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')
```

### 4-2. 배치직무 목록 확인

```bash
curl -s localhost:8081/api/v1/batch-jobs -H "Authorization: Bearer $TOKEN"
# 또는 DB 직접
docker exec postgres psql -U coder -d coder -c "select job_id, job_nm, sche_expr, use_yn, job_parm_cont from coder.tb_dspt_job_m order by job_id"
```

샘플 직무 (2026-10-17 기준, 최신 목록은 위 조회로 확인):

| jobId | 이름 | 기본 파라미터 | 데이터 영향 |
|---|---|---|---|
| BHFL001 | 사용자상태변경 적용 | `baseDt`(미지정 시 실행일) | 퇴직·휴직 사용자 `TB_USR_M` 사용여부/종료일 변경 |
| BHFL002 | 배치 실행이력 정리 | `retentionDays=90` | 보관기간 지난 `TB_DSPT_EXEC_L` 삭제 |
| BHFL003 | 장기 미접속 사용자 잠금 | `idleDays=90` | 장기 미접속 사용자 잠금 |

### 4-3. 실행 (비동기)

```bash
curl -s -X POST localhost:8081/api/v1/batch-jobs/BHFL002/executions \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"params":{"retentionDays":"90"}}'
# {"success":true,...,"message":"배치 실행을 요청했습니다. ..."}
```

- 직무의 기본 파라미터(`job_parm_cont`)에 요청 `params`를 덮어써서 실행한다. 생략하면(`{}`) 기본 파라미터 그대로 실행한다.
- 파라미터 값에 한글이 있으면 `-d` 대신 UTF-8 파일로 보낸다 (`--data-binary @file.json`). Git Bash curl은 인자의 한글을 CP949로 보내 JSON 파싱 오류(500)가 난다.
- 다중 실행 불가(`mlt_exec_yn='N'`) 직무가 이미 실행 중이면 409(`sy.batch.job.already.running`)로 거절된다.

### 4-4. 결과 확인

실행 요청은 즉시 반환되므로 `STARTED`가 끝날 때까지 이력을 조회한다.

```bash
until ! curl -s "localhost:8081/api/v1/batch-executions?jobId=BHFL002&size=1" -H "Authorization: Bearer $TOKEN" \
  | grep -q '"execStatCd":"STARTED"'; do sleep 1; done
curl -s "localhost:8081/api/v1/batch-executions?jobId=BHFL002&size=1" -H "Authorization: Bearer $TOKEN"
```

- `execStatCd`: `COMPLETED` / `FAILED` / `STOPPED` / `SKIP` 등, `procCnt`·`succCnt`·`failCnt`, 실패 시 `errMsgCont`
- Step별 현황: `GET /api/v1/batch-executions/{dsptExecSn}`
- 실패 원인 로그: `grep -n "BHFL002" "$S/batch.log"` (로그 MDC가 `[jobId:jobExecutionId:stepName]`). 파일 로그는 `workspace/coder-batch/logs/coder-batch.log`
- FAILED/STOPPED 재시작: `POST /api/v1/batch-executions/{dsptExecSn}/restarts`

사용자에게는 직무ID, 실행 결과 상태, 처리 건수, 실패 시 오류 내용을 보고한다.

## 종료

- batch: 띄운 백그라운드 작업을 `TaskStop`으로 중지한다.
  남아 있으면 `netstat -ano | grep LISTENING | grep ":8081 "`로 PID를 찾아 `taskkill //PID <pid> //F`.
- DB·backend는 다른 작업에서 쓰고 있을 수 있으므로 사용자가 요청할 때만 끈다.

## 참고

- 화면에서 실행·이력 조회: 배치관리(SYLK050). frontend(3000)는 `/api/v1/batch-jobs`, `/api/v1/batch-executions`를 8081로 프록시한다.
- 관리 API 전체 목록과 새 Job 추가 방법은 `workspace/coder-batch/README.md`, 요구사항은 `documents/batch/PRD.md`.
- DB 비밀번호는 `CODER_BATCH_DB_PASSWORD`(기본 `batch1234!`), JWT 비밀키는 `CODER_JWT_SECRET`(backend와 같아야 함).
