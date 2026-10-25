---
name: git-push
description: CODER 변경사항 GitHub push - 변경 파일 확인, 민감정보·대용량 파일 검사 후 커밋하고 origin(github.com/youngcd72/backoffice)에 push한다. "git push 해줘", "커밋하고 올려줘", "깃에 올려줘", "git-push" 요청 시 사용.
---

# git-push

`c:\coder` 저장소의 변경사항을 커밋하고 `origin`에 push한다.

| 항목 | 값 |
|---|---|
| remote | `origin` → `https://github.com/youngcd72/backoffice.git` (**공개 저장소**) |
| 브랜치 | `main` (개인 저장소라 main에 바로 push) |
| 커밋 작성자 | git 전역 설정 그대로 (`youngcd72 <46082313+youngcd72@users.noreply.github.com>`) — 바꾸지 않는다 |
| 인증 | Git Credential Manager (Windows 자격 증명 관리자) |

Bash 툴(Git Bash) 기준이다. CRLF 변환 경고(`warning: in the working copy of ...`)는 무시해도 된다.

## 1. 상태 확인

```bash
cd /c/coder
git status --short
git log --oneline origin/main..HEAD   # 아직 push 안 된 커밋
```

- 변경도 없고 push 안 된 커밋도 없으면 "올릴 것이 없다"고 알리고 끝낸다.
- 변경 파일이 이번 요청과 관계없어 보이면(다른 작업 중인 파일 등) 커밋에 넣을지 사용자에게 확인한다.

## 2. 스테이징 + 검사

```bash
git add -A
git diff --cached --stat
```

공개 저장소이므로 커밋 전에 아래를 확인한다.

```bash
# 실제 키·토큰 (나오면 커밋 중단하고 사용자에게 알림)
git diff --cached -U0 | grep -nE "ghp_|github_pat_|sk-[A-Za-z0-9]{20}|AKIA[0-9A-Z]{16}|BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY"

# 들어가면 안 되는 파일 (.gitignore 누락 확인)
git diff --cached --name-only | grep -E "(^|/)(build|bin|dist|node_modules|logs|\.gradle)/|\.log$|^tools/|\.local$|settings\.local\.json"

# 큰 파일 (GitHub 100MB 거절, 50MB 경고)
git diff --cached --name-only -z | xargs -0 -r stat -c "%s %n" 2>/dev/null | awk '$1 > 5000000'
```

- 걸리는 파일이 있으면 `git restore --staged <파일>`로 빼고 `.gitignore` 추가를 제안한다.
- `application*.yml`의 로컬 기본 비밀번호(`coder1234!`, `batch1234!` 등)는 사용자가 허용한 값이라 그대로 둔다. 새로 추가되는 실제 운영 비밀값만 막는다.

## 3. 커밋

변경 내용을 보고 영어 한 줄 요약으로 메시지를 쓴다 (기존 이력과 같은 형식: `Add root README`, `Change login brand text to ...`). 커밋 메시지 끝에는 시스템이 지정한 attribution 줄(`Co-Authored-By: ...`)이 있으면 붙인다.

```bash
git commit -q -m "$(cat <<'EOF'
<요약>

Co-Authored-By: ...
EOF
)"
```

- 사용자가 커밋 메시지를 지정하면 그대로 쓴다.
- 이미 커밋만 되어 있고 push가 안 된 상태면 이 단계는 건너뛴다.

## 4. push

```bash
GIT_TERMINAL_PROMPT=0 timeout 120 git push origin main 2>&1 | grep -v "^warning"
git status -sb | head -1   # "## main...origin/main" (ahead 표시 없음) 이면 완료
```

- `Invalid username or token` / `Authentication failed`: 이 터미널은 브라우저 로그인 창을 띄울 수 없다. 사용자에게 VS Code 터미널에서 `git push origin main`을 직접 실행해 GitHub(`youngcd72`) 로그인을 하도록 안내한다. 계속 실패하면 오래된 자격 증명 삭제를 안내한다:
  `cmdkey /delete:"LegacyGeneric:target=GitHub - https://api.github.com/youngcd72"`
- `rejected (fetch first)` / `non-fast-forward`: 원격에 다른 커밋이 있다. `git pull --rebase origin main` 후 다시 push한다. 충돌이 나면 멈추고 사용자에게 알린다.
- **`--force` push는 사용자가 명시적으로 요청할 때만** 한다 (공개 저장소 이력이 바뀐다).

## 5. 보고

커밋 해시·메시지, 올라간 파일 수(또는 주요 파일), push 결과를 알린다. 저장소 주소: https://github.com/youngcd72/backoffice
