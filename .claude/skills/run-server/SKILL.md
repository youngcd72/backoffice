---
name: run-server
description: CODER 로컬 개발 서버 실행 - Docker DB(postgres, redis) 기동 후 coder-backend(8080)와 coder-frontend(3000)를 띄우고 헬스체크까지 확인한다. "서버 띄워줘", "서버 실행", "run" 요청 시 사용.
---

# run-server

로컬 개발 환경을 **DB → backend → frontend** 순서로 띄운다. 순서가 중요하다:
backend는 기동 시 DB 커넥션 풀을 만들기 때문에 postgres가 꺼져 있으면
`Connection to localhost:5432 refused`로 바로 종료된다.

| 구성 | 실행 방식 | 포트 |
|---|---|---|
| postgres (`postgres:18`) | Docker 컨테이너 `postgres` | 5432 (DB/Schema/계정 `coder`) |
| redis (`redis:latest`) | Docker 컨테이너 `redis` | 6379 |
| coder-backend | `workspace/coder-backend` → `gradlew.bat bootRun` | 8080 |
| coder-frontend | `workspace/coder-frontend` → `pnpm dev` (Vite, `/api` → 8080 프록시) | 3000 |

로그는 세션 scratchpad 디렉터리(아래 `$S`)에 남긴다. Bash 툴(Git Bash) 기준이다.

## 0. 이미 떠 있는지 확인

```bash
netstat -ano | grep LISTENING | grep -E ":(5432|6379|8080|3000) "
docker ps -a --format '{{.Names}}\t{{.Status}}\t{{.Ports}}'
```

이미 LISTENING 중인 포트의 서버는 다시 띄우지 않는다 (포트 충돌로 실패한다).

## 1. Docker DB 기동

```bash
docker start postgres redis
until docker exec postgres pg_isready -q; do sleep 1; done   # postgres 준비 대기
docker exec redis redis-cli ping                               # PONG
```

- 컨테이너가 아예 없으면(`No such container`) 임의로 새로 만들지 말고 사용자에게 알린다
  (DB 데이터·비밀번호가 기존 컨테이너에 들어 있다. 생성 절차는 각 모듈 README의 "DB 준비" 참고).
- Docker Desktop이 꺼져 있으면(`error during connect`) 사용자에게 Docker Desktop 실행을 요청한다.

## 2. backend 기동 (백그라운드)

Bash 툴 `run_in_background: true`로 실행한다.

```bash
cd /c/coder/workspace/coder-backend && ./gradlew.bat bootRun > "$S/backend.log" 2>&1
```

준비 대기 (성공·실패 모두 잡는다):

```bash
until grep -qE "Started .* in|run failed|BUILD FAILED" "$S/backend.log"; do sleep 1; done
grep -E "Started .* in|run failed|BUILD FAILED" "$S/backend.log"
```

실패했으면 `grep -nE "Caused by|ERROR" "$S/backend.log" | head`로 원인을 본다.

## 3. frontend 기동 (백그라운드)

```bash
cd /c/coder/workspace/coder-frontend && pnpm dev > "$S/frontend.log" 2>&1
```

`Local:   http://localhost:3000/` 줄이 찍히면 준비 완료. backend와 동시에 띄워도 된다.

## 4. 동작 확인 (띄우기만 하고 끝내지 않는다)

```bash
curl -s localhost:8080/actuator/health          # status UP, components db/redis 모두 UP
curl -s -o /dev/null -w "%{http_code}\n" localhost:3000                  # 200
curl -s -o /dev/null -w "%{http_code}\n" localhost:3000/api/v1/messages  # 200 (프록시 → backend)
```

health의 `db`·`redis`가 모두 `UP`이어야 DB까지 정상 연결된 것이다.

## 종료

- backend/frontend: 띄운 백그라운드 작업을 `TaskStop`으로 중지한다.
  남아 있으면 `netstat -ano | grep ":8080 "`로 PID를 찾아 `taskkill //PID <pid> //F`.
- DB: `docker stop postgres redis` (사용자가 요청할 때만. 다른 작업에서 쓰고 있을 수 있다).

## 참고

- `coder-batch`(8081)는 `run-batch` 스킬을 쓴다.
- DB 비밀번호는 환경변수 `CODER_DB_PASSWORD`로 바꿀 수 있다 (기본값은 application.yml).
