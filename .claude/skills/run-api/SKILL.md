---
name: run-api
description: CODER 대외 연계 API 서버 실행 - Docker DB(postgres, redis) 기동 후 coder-api(8082)를 띄우고 헬스체크까지 확인한다. 요청 시 클라이언트 키 발급, 토큰 발급과 샘플 API 호출까지 한다. "API 서버 띄워줘", "coder-api 실행", "연계 API 테스트", "run-api" 요청 시 사용.
---

# run-api

`coder-api`(대외 연계 REST API, OAuth2 Client Credentials + JWT RS256)를 **DB → api** 순서로 띄운다.
api는 기동 시 DB 커넥션 풀을 만들고 Redis(호출 한도·캐시 삭제 채널)를 쓰므로 postgres·redis가 먼저 떠 있어야 한다.

| 구성 | 실행 방식 | 포트 |
|---|---|---|
| postgres (`postgres:18`) | Docker 컨테이너 `postgres` | 5432 (DB/Schema/계정 `coder`, backend와 같은 계정) |
| redis (`redis:latest`) | Docker 컨테이너 `redis` | 6379 |
| coder-api | `workspace/coder-api` → `./gradlew bootRun` | 8082 |

coder-backend(8080)·frontend(3000)는 필요 없다 (`run-server` 스킬과 별개로 띄울 수 있다).
로그는 세션 scratchpad 디렉터리(아래 `$S`)에 남긴다. Bash 툴(Git Bash) 기준이다.

## 0. 이미 떠 있는지 확인

```bash
netstat -ano | grep LISTENING | grep -E ":(5432|6379|8082) "
docker ps -a --format '{{.Names}}\t{{.Status}}\t{{.Ports}}'
```

이미 LISTENING 중인 포트의 서버는 다시 띄우지 않는다 (포트 충돌로 실패한다).

## 1. Docker DB 기동

```bash
docker start postgres redis
until docker exec postgres pg_isready -q; do sleep 1; done   # postgres 준비 대기
docker exec redis redis-cli ping                               # PONG
```

- 컨테이너가 없으면(`No such container`) 임의로 새로 만들지 말고 사용자에게 알린다.
- Docker Desktop이 꺼져 있으면(`error during connect ... dockerDesktopLinuxEngine`) 사용자에게 Docker Desktop 실행을 요청한다.
- 연계 테이블이 없으면(기동 로그에 `relation "tb_lnk_key_m" does not exist` 등) 임의로 만들지 말고 `workspace/coder-api/README.md`의 "1. DB 준비" 절차를 사용자에게 안내한다.
  확인: `docker exec postgres psql -U coder -d coder -tAc "select table_name from information_schema.tables where table_schema='coder' and (table_name like 'tb_lnk%' or table_name like 'tb_infc%') order by 1"`
  (`tb_infc_call_l`(+ 월 파티션), `tb_infc_call_s`, `tb_infc_m`, `tb_lnk_key_h`, `tb_lnk_key_m`, `tb_lnk_org_infc_r`, `tb_lnk_org_m`)

## 2. api 기동 (백그라운드)

Bash 툴 `run_in_background: true`로 실행한다. **`./gradlew`(sh 스크립트)를 쓴다** — Git Bash 백그라운드에서 `./gradlew.bat`은 `No such file or directory`(exit 127)로 실패한 적이 있다.

```bash
cd /c/coder/workspace/coder-api && ./gradlew bootRun > "$S/api.log" 2>&1
```

준비 대기 (성공·실패 모두 잡는다):

```bash
until grep -qE "Started .* in|run failed|BUILD FAILED|APPLICATION FAILED" "$S/api.log"; do sleep 1; done
grep -E "Started .* in|run failed|BUILD FAILED|APPLICATION FAILED" "$S/api.log"
```

실패했으면 `grep -nE "Caused by|ERROR" "$S/api.log" | head`로 원인을 본다.
업무 API Controller에 `@ApiInterface` 선언이 빠져 있으면 기동 시 검사에서 실패한다 (README "업무 API 추가 방법").

## 3. 동작 확인 (띄우기만 하고 끝내지 않는다)

```bash
curl -s localhost:8082/actuator/health                                    # status UP, components db/redis 모두 UP
curl -s localhost:8082/api/ping                                           # "status":"UP"
curl -s -o /dev/null -w "%{http_code}\n" localhost:8082/api/v1/samples/SYSM_CLS_CD  # 401 (토큰 필요 = 정상)
```

health의 `db`·`redis`가 모두 `UP`이어야 DB까지 정상 연결된 것이다.
API 문서: http://localhost:8082/swagger-ui.html (`partner` 그룹 = 연계기관 공유용)

## 4. 토큰 발급·샘플 API 호출 (요청 시에만)

### 4-1. 클라이언트 키 발급 (키가 없을 때만)

먼저 기존 키를 확인한다.

```bash
docker exec postgres psql -U coder -d coder -c "select key_id, lnk_org_id, key_stat_cd, expi_dttm from coder.tb_lnk_key_m order by 1"
```

비밀키는 DB에 KMS 암호화로만 저장되어 **다시 볼 수 없다**. 사용할 키의 `client_secret`을 모르면 새로 발급한다.
발급은 키를 저장하고 `client_id`/`client_secret`을 콘솔에 한 번만 출력한 뒤 종료한다 (`--server.port=0`이라 실행 중인 8082와 충돌하지 않는다).

```bash
cd /c/coder/workspace/coder-api && ./gradlew bootRun --args="--coder.api.key-issue.lnk-org-id=LOCAL01 --server.port=0" > "$S/api-key-issue.log" 2>&1
grep -E "^(lnk_org_id|client_id|client_secret|expires_at) " "$S/api-key-issue.log"
```

- `LOCAL01`은 `schema/data/TB_INFC_M-data.sql`의 로컬 샘플 연계기관이다 (`IFSY0001` 권한 포함).
- `client_secret`은 사용자에게 그대로 전달하되, 커밋·문서에 남기지 않는다.
- 로컬 KMS 마스터키(`CODER_KMS_MASTER_KEY`, 기본값 `application-local.yml`)를 바꾸면 기존 키는 복호화할 수 없다.

### 4-2. 토큰 발급 → 업무 API 호출

```bash
TOKEN=$(curl -s -d "grant_type=client_credentials&client_id=$ID&client_secret=$SECRET" localhost:8082/oauth2/token \
  | sed -E 's/.*"access_token":"([^"]+)".*/\1/')
curl -s localhost:8082/api/v1/samples/SYSM_CLS_CD -H "Authorization: Bearer $TOKEN"
```

| 응답 | 원인 |
|---|---|
| 401 `A001` | 토큰 없음·만료·서명 불일치, 또는 키·연계기관이 사용 중지 |
| 403 `A002` | 요청 IP가 `ALLW_IP_ADDR`에 없음 |
| 403 `A003` | 인터페이스 권한 없음 (`TB_LNK_ORG_INFC_R` 변경 후에는 토큰 재발급 필요) |
| 429 `A004` | 호출 한도 초과 |
| `invalid_client` (토큰 발급) | id/secret 불일치. 연속 5회 실패 시 10분 잠금 |

호출 이력: `docker exec postgres psql -U coder -d coder -c "select * from coder.tb_infc_call_l order by 1 desc limit 5"` (큐에 모았다가 약 1초 간격으로 저장된다).

## 종료

- api: 띄운 백그라운드 작업을 `TaskStop`으로 중지한다.
  남아 있으면 `netstat -ano | grep LISTENING | grep ":8082 "`로 PID를 찾아 `taskkill //PID <pid> //F`.
- DB: `docker stop postgres redis` (사용자가 요청할 때만. 다른 작업에서 쓰고 있을 수 있다).

## 참고

- 구조·요청 처리 순서·설정(`coder.api.*`)은 `workspace/coder-api/README.md`, 요구사항은 `documents/api/PRD.md`.
- 단위 테스트(DB·Redis 불필요): `cd /c/coder/workspace/coder-api && ./gradlew test`
- DB 비밀번호는 환경변수 `CODER_API_DB_PASSWORD`(기본 `coder1234!`), Redis는 `CODER_REDIS_HOST`/`CODER_REDIS_PORT`.
