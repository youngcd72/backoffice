---
name: save-component
description: CODER 화면 버튼 컴포넌트 등록 - 프로그램ID(SYAU030 등)에 해당하는 Vue 화면의 버튼(id="btn...")을 찾아 v-permission을 확인하고 TB_PRGM_CMPN_M 시드 파일과 DB에 등록한다. 요청 시 GRP_IT 권한(TB_AUTH_GRP_CMPN_R)도 부여한다. "컴포넌트 등록", "버튼 컴포넌트 등록해줘", "SYAU030 component 등록", "save-component" 요청 시 사용.
---

# save-component

프로그램ID(`PRGM_ID`)에 해당하는 화면의 버튼을 **TB_PRGM_CMPN_M**(프로그램컴포넌트기본)에 등록한다.
등록된 버튼은 `v-permission` 디렉티브가 권한을 판정한다
(권한그룹 `TB_AUTH_GRP_CMPN_R` 또는 사용자 `TB_USR_CMPN_R` 중 하나라도 `AUTH_YN='Y'`면 노출, 미등록 버튼은 항상 노출).

| 구분 | 위치 |
|---|---|
| 라우트 (이름 = PRGM_ID) | `workspace/coder-frontend/src/router/index.ts` |
| 디렉티브 | `workspace/coder-frontend/src/directives/permission.ts` |
| 컴포넌트 시드 | `schema/data/TB_PRGM_CMPN_M-data.sql` |
| 그룹 권한 시드 | `schema/data/TB_AUTH_GRP-data.sql` (GRP_IT 컴포넌트 권한 블록) |
| DB | Docker 컨테이너 `postgres`, DB/계정 `coder`, schema `coder` |

DB 조회는 Bash 툴(Git Bash) 기준으로 아래처럼 한다.

```bash
q(){ docker exec postgres psql -U coder -d coder -c "set search_path=coder; $1"; }
```

## 주의: 등록하면 바로 버튼이 숨겨진다

등록만 하고 권한이 없으면 **admin을 포함한 모든 사용자에게서 그 버튼이 사라진다.**
특히 권한을 주는 화면(SYAU010 사용자권한관리, SYAU020 그룹권한관리)의 버튼이 모두 숨겨지면
화면에서 컴포넌트 권한을 줄 방법이 없어진다. 그래서 4단계에서 권한 부여 여부를 반드시 사용자에게 묻는다.

## 1. 프로그램 확인

인자가 메뉴ID(`SY030100`처럼 8자리)이면 TB_MENU_M에서 프로그램ID로 바꾸고, 바꾼 결과를 사용자에게 알린다.

```bash
q "select menu_id, menu_nm, prgm_id from tb_menu_m where menu_id = '<인자>'"
```

```bash
q "select prgm_id, prgm_nm, use_yn from tb_prgm_m where prgm_id = '<PRGM_ID>'"   # FK: 없으면 등록 불가
q "select cmpn_id, cmpn_nm, alin_seq, use_yn from tb_prgm_cmpn_m where prgm_id = '<PRGM_ID>' order by alin_seq"
```

- TB_PRGM_M에 없으면 멈추고 사용자에게 알린다 (FK `TB_PRGM_CMPN_M_FK01`). 프로그램 등록은 이 스킬 범위가 아니다.
- 라우트에서 화면 파일을 찾는다: `router/index.ts`에서 `name: '<PRGM_ID>'`의 `component` → 상단 `import`의 `.vue` 경로.
- Docker가 꺼져 있으면(`error during connect`) 사용자에게 Docker Desktop 실행을 요청한다.

## 2. 화면 버튼 추출

화면 파일에서 공통 `Button` 컴포넌트의 **정적 id**(`id="btn..."`)를 등장 순서대로 모은다.
`id`가 태그 첫 줄에 없고 여러 줄로 나뉜 태그도 있으므로 `<Button`부터 `>`까지 묶어서 본다.

```bash
grep -nE '<Button|id="btn' <화면.vue>
```

각 버튼에 대해 정리한다.

- **CMPN_ID**: `id` 값 그대로 (`btnSave`). CLAUDE.md Ref/ID 네이밍(`btn` + 약어 PascalCase)을 따르는지 본다.
- **CMPN_NM**: 버튼 라벨(태그 안 텍스트). 50자 이내.
  - 라벨이 같은 버튼이 둘 이상이면(`행삭제` 등) 대상을 붙여 구분한다: `사용자 행삭제`, `권한 행삭제`.
  - 팝업(Modal) 안 버튼은 `화면검색 팝업 추가`처럼 팝업명을 붙인다.
  - 라벨이 `{{ }}` 표현식이면 버튼 역할을 짧게 쓴다.
- **ALIN_SEQ**: 화면 등장 순서대로 1부터.

제외 / 확인 대상:

- `:id`(동적 id), id 없는 `Button`, 네이티브 `<button>`(그리드 셀 버튼 등)은 등록하지 않고 목록만 알린다.
  - 단, 그리드 셀 버튼이 실행·삭제처럼 실제 동작을 하면 등록을 제안한다. 행마다 반복되므로 id 없이
    `v-permission="'btnJobExec'"`처럼 값으로 컴포넌트ID를 준다 (예: SYLK050 수동실행). 시드 행 위에 그 이유를 주석으로 남긴다.
- `v-permission`이 없는 `Button`은 `id="btnX"` 바로 뒤에 `v-permission`을 붙인다. 파일 헤더 `Program History`에는 이력 줄을 추가하지 않는다 (최초생성 한 줄만 둠, CLAUDE.md 6.1).
- 화면 파일은 LF와 CRLF가 섞여 있다. 스크립트로 고칠 때는 줄바꿈을 `\r?\n`으로 맞춘다.

팝업(Modal) 안 버튼:

- 팝업 안 버튼(취소·닫기·복사·팝업 저장 등)이 있으면 4단계에서 **등록할지 함께 묻는다**.
  권장은 "팝업을 여는 버튼만 등록"이다. 팝업 안 버튼은 권한을 따로 주다 보면 팝업을 닫지 못하는 상황이 생길 수 있다
  (예: SYLK050 배치관리의 실행·배치 저장 팝업 버튼은 팝업을 여는 버튼으로만 제어한다).
- 등록하지 않는 팝업 버튼에서는 `v-permission`을 빼고, 시드 블록에 `--   팝업 안 버튼은 ... 등록하지 않는다` 주석을 남긴다.
- 기존 등록 화면(SYAU010/SYAU020/SYAU030)은 팝업 버튼까지 등록되어 있다.

## 3. DB와 비교

1단계에서 조회한 DB 행과 2단계 목록을 비교해 사용자에게 보여 준다.

| 구분 | 처리 |
|---|---|
| 화면에만 있음 | 신규 등록 대상 |
| 둘 다 있음 | 그대로 둔다 (시드가 `ON CONFLICT DO NOTHING`이라 이름·순번이 바뀌지 않는다) |
| DB에만 있음 | 화면에서 삭제된 버튼. **임의로 지우지 않고** 사용자에게 알린다 (권한 행이 FK로 걸려 있을 수 있다) |

이름·순번을 바꿔야 하면 사용자에게 확인받은 뒤 `UPDATE`로 처리한다.

## 4. 권한 부여 여부 확인

AskUserQuestion으로 묻는다.

- **GRP_IT에 권한 부여 (권장)**: GRP_IT(admin/operator/viewer 소속)에 등록한 버튼 전부 `AUTH_YN='Y'`.
- **등록만**: 권한은 SYAU010/SYAU020 화면에서 준다. 그 전까지 버튼이 모두 숨겨진다고 함께 알린다.

권한관리 화면(SYAU010/SYAU020)을 "등록만" 하려는 경우에는 화면에서 권한을 줄 수 없게 될 수 있다고 다시 알린다.

팝업 안 버튼이 있으면 같은 AskUserQuestion에서 "화면 버튼만 등록 (권장)" / "전부 등록"도 함께 묻는다 (2단계 참고).

## 5. 시드 파일 수정

`schema/data/TB_PRGM_CMPN_M-data.sql`의 `VALUES` 끝(`ON CONFLICT` 바로 앞)에 프로그램 블록을 추가한다.
같은 프로그램 블록이 이미 있으면 그 블록 끝에 신규 행만 이어 붙인다. 앞 행 끝의 `,` 처리에 주의한다.

```sql
    -- SYAU030 그룹별사용자관리 (views/system/auths/AuthGroupUserManageView.vue)
    ('SYAU030', 'btnSave',          '저장',                   1, 'Y', 'admin', 'admin'),
    ('SYAU030', 'btnSrch',          '조회',                   2, 'Y', 'admin', 'admin')
ON CONFLICT (PRGM_ID, CMPN_ID) DO NOTHING;
```

GRP_IT 권한을 부여하면 `schema/data/TB_AUTH_GRP-data.sql`의 `-- GRP_IT 컴포넌트 권한` 블록
`WHERE PRGM_ID IN (...)`에 프로그램ID를 추가하고, 파일 헤더 설명의 프로그램 목록도 함께 고친다.

## 6. DB 반영

컴포넌트 시드는 파일 전체를 실행한다 (기존 행은 건드리지 않는다).

```bash
docker exec -i postgres psql -U coder -d coder -q -c "set search_path=coder" -f - < /c/coder/schema/data/TB_PRGM_CMPN_M-data.sql
```

그룹 권한은 **GRP_IT 컴포넌트 권한 블록만** 실행한다.
`TB_AUTH_GRP-data.sql` 전체를 다시 돌리면 GRP_IT에 메뉴 권한이 의도치 않게 추가될 수 있다.

```bash
sed -n '/^-- GRP_IT 컴포넌트 권한/,/DO NOTHING;/p' /c/coder/schema/data/TB_AUTH_GRP-data.sql \
  | docker exec -i postgres psql -U coder -d coder -c "set search_path=coder" -f -
```

## 7. 확인 (등록만 하고 끝내지 않는다)

```bash
q "select c.cmpn_id, c.cmpn_nm, c.alin_seq, c.use_yn, g.auth_yn grp_it
     from tb_prgm_cmpn_m c
     left join tb_auth_grp_cmpn_r g
       on g.prgm_id = c.prgm_id and g.cmpn_id = c.cmpn_id and g.usr_grp_cd = 'GRP_IT'
    where c.prgm_id = '<PRGM_ID>' order by c.alin_seq"
```

서버가 떠 있으면(`run-server` 스킬) 브라우저로 화면을 열어 버튼 노출을 확인한다.
권한은 화면별로 처음 열 때 조회해 캐시하므로 **새로고침한 뒤** 확인한다.

```js
// Playwright browser_evaluate
() => [...document.querySelectorAll('.layout-content button[id^=btn]:not([id^=btnTab])')]
  .map(b => `${b.id}:${getComputedStyle(b).display === 'none' ? 'HIDDEN' : 'shown'}`)
```

팝업 안 버튼은 팝업을 열어야 렌더링되므로, 확인하지 못했으면 그렇게 보고한다.

## 보고

- 등록한 버튼 표 (순번, CMPN_ID, 컴포넌트명)와 GRP_IT 권한 부여 여부
- 제외한 버튼(동적 id, id 없음, 네이티브 button), DB에만 남은 버튼
- `v-permission`을 새로 붙인 파일
- 권한을 주지 않았다면 "지금 모든 사용자에게서 숨겨진다"는 점
