# SYLK010 API관리 프로그램 사양서

| 항목 | 내용 |
|---|---|
| 프로그램 ID | SYLK010 |
| 프로그램명 | API관리 |
| 메뉴 경로 | 시스템관리 > 연계관리 > API관리 (메뉴ID `SY040100`) |
| 라우트 | `/sy/sylk010` (name `SYLK010`) |
| 화면 파일 | `workspace/coder-frontend/src/views/system/links/ApiManageView.vue` (탭 껍데기)<br>`workspace/coder-frontend/src/views/system/links/api/LinkOrgTab.vue` (① 연계기관)<br>`workspace/coder-frontend/src/views/system/links/api/InterfaceTab.vue` (② 인터페이스)<br>`workspace/coder-frontend/src/views/system/links/api/InterfaceCallTab.vue` (③ 호출이력) |
| API 파일 | `workspace/coder-frontend/src/api/sy/link.ts` |
| API 상태 | 실제 API 연동 (coder-backend :8080 `kr.or.coder.biz.system.link` — `LinkOrgController`, `InterfaceController`, `InterfaceCallController`) |
| 대상 데이터 | `TB_LNK_ORG_M`(연계기관), `TB_LNK_KEY_M`(연계키), `TB_LNK_KEY_H`(연계키이력), `TB_INFC_M`(인터페이스), `TB_LNK_ORG_INFC_R`(연계기관인터페이스관계), `TB_INFC_CALL_L`(인터페이스호출내역) |
| 관련 문서 | `documents/api/PRD.md` (FR-01, FR-04, FR-06, 8.2), `workspace/coder-api/README.md` |
| 작성자 | ywkim |
| 작성일 | 2026-11-03 |

### 변경 이력

| 일자 | 작성자 | 내용 |
|---|---|---|
| 2026-11-03 | ywkim | 최초 작성 (설계안) |
| 2026-11-03 | ywkim | 목업 화면 구현 반영 (메뉴ID, 사용기간 입력, 메시지 키) |
| 2026-11-03 | ywkim | backend 관리 API 구현, 실제 API 연동 (KMS 사용 설정, 키 수정·호출 상세 처리 규칙) |

---

## 1. 개요

coder-api(대외 연계 API)를 쓰는 연계기관과 클라이언트 키, 인터페이스, 기관별 허용 인터페이스를 관리하고 호출 이력을 조회한다. 관리 대상이 서로 연결되어 있어(기관 → 키·허용 인터페이스 → 호출) 화면을 나누지 않고 **한 화면에 3개 탭**으로 구성한다.

| 탭 | 내용 |
|---|---|
| ① 연계기관 | 연계기관 목록 + 선택 기관의 기본정보, 클라이언트 키(발급·상태 변경·허용 IP·호출 한도), 허용 인터페이스 |
| ② 인터페이스 | 인터페이스 목록 + 등록/수정 (본문 로그 여부, 마스킹 필드) |
| ③ 호출이력 | 기간·기관·인터페이스·결과·추적ID로 호출 이력 조회, 요청·응답 본문(마스킹) 상세 |

- 토큰 발급에는 `client_id`와 `client_secret`이 모두 필요하다. `client_id`(=`KEY_ID`)는 평문으로 저장되어 언제든 볼 수 있고, `client_secret`은 KMS로 암호화해 저장하므로 **발급 직후 한 번만** 화면에 표시한다.
- 화면에서 변경한 내용은 저장 후 Redis 채널 `coder:api:cache-evict`로 알려 실행 중인 coder-api가 캐시(기본 5분)를 바로 비운다. 단, 이미 발급된 액세스 토큰의 `scope`는 바뀌지 않으므로 허용 인터페이스 변경은 **토큰을 다시 발급받아야** 반영된다.
- 삭제 기능은 두지 않는다. 기관·인터페이스는 사용여부로, 키는 폐기 상태로 관리한다 (호출 이력·키 이력이 참조).

---

## 2. 화면 구성

```text
┌ [SYLK010] API관리 ──────────────────────────────────────────────────────────────┐
│ [ 연계기관 | 인터페이스 | 호출이력 ]   (tabMain)                                   │
├─ ① 연계기관 탭 ─────────────────────────────────────────────────────────────────┤
│ 기관ID/명 [    ]  채널 [전체▼]  사용여부 [사용▼]                        [조회]   │
├ 연계기관 - N건 ─────────[신규]┬ 기본정보 ──────────────────[호출이력 보기][저장] ┤
│ 기관ID│기관명│채널│사용│키수 │ 기관ID [LOCAL01] 기관명 [로컬 테스트 기관]       │
│ (grdLnkOrgList)              │ 채널 [내부▼] 사용기간 [20261101~99991231] ☑사용  │
│                              │ 비고 [                              ]           │
│                              ├ 클라이언트 키 - N건 ─────────────────────[키발급] ┤
│                              │ Client ID│상태│발급일시│만료일시│허용IP│분│일│동시│
│                              │ (grdKeyList) 행 클릭 → 키 관리 팝업             │
│                              ├ 허용 인터페이스 - N/M건 ────────────────────[저장] ┤
│                              │ ☑│인터페이스ID│인터페이스명│메서드│URL│사용      │
│                              │ (grdOrgInfcList)                                │
└──────────────────────────────┴─────────────────────────────────────────────────┘
  [키발급]   → 키 발급 팝업 (mdlKeyIssu, 520px) → 발급 결과 팝업 (mdlKeyIssuRslt, 600px)
  키 행 클릭 → 키 관리 팝업 (mdlKeyDtl, 820px, 키 이력 포함)

├─ ② 인터페이스 탭 ───────────────────────────────────────────────────────────────┤
│ 인터페이스ID/명 [    ]  사용여부 [사용▼]                                [조회]   │
├ 인터페이스 - N건 ───────[신규]┬ 인터페이스 정보 ───────────────────────────[저장] ┤
│ 인터페이스ID│명│메서드│URL│  │ 인터페이스ID [IFSY0001] 인터페이스명 [        ]  │
│ 본문로그│사용│허용기관수     │ 메서드 [GET▼] URL [/api/v1/samples/{id}       ]  │
│ (grdInfcList)                │ ☐본문로그 ☑사용  마스킹 필드 [txa           ]    │
└──────────────────────────────┴─────────────────────────────────────────────────┘

├─ ③ 호출이력 탭 ─────────────────────────────────────────────────────────────────┤
│ 기간 [20261103~20261103]  연계기관 [전체▼]  인터페이스 [전체▼]                  │
│ 결과 [전체▼]  추적ID [                ]                                 [조회]   │
├ 호출이력 - N건 (최근 500건 표시) ──────────────────────────────────────────────┤
│ NO│요청일시│연계기관│인터페이스│메서드│URL│HTTP상태│소요(ms)│오류코드│IP│추적ID   │
│ (grdCallList, 20행 페이징) 행 클릭 → 호출 상세 팝업 (mdlCallDtl, 900px)          │
└─────────────────────────────────────────────────────────────────────────────────┘
```

- 공통 `Tabs` 컴포넌트(`components/common/tabs/Tabs.vue`, variant `line`)를 쓴다. 탭 Ref/ID는 `tabMain`, 탭 키는 `org` / `infc` / `call`.
- 탭 본문은 `v-show`로 전환해 탭을 오가도 검색조건·선택 행·입력 중인 폼을 유지한다. 각 탭은 **처음 활성화될 때 한 번** 조회한다 (6.0).
- 목록·상세를 좌우로 나누는 탭(①②)은 왼쪽 목록 440px 고정, 오른쪽 상세가 나머지 폭을 쓴다. 1280px 미만에서는 위아래로 쌓는다.
- 그리드 셀은 줄바꿈하지 않는다. 긴 텍스트(URL, 허용IP, 비고, 추적ID)는 말줄임(최대 220px)하고 마우스오버(title)로 전체를 보여 준다.

---

## 3. 검색조건

### 3.1 ① 연계기관 탭 (`frmOrgSrch`)

| Ref/ID | 항목 | 컴포넌트 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|---|
| `txtOrgKeyword` | 기관ID/명 | Input | | | 기관ID·기관명 부분 일치 (앞뒤 공백 제거). Enter 시 조회 |
| `cboOrgChnlClsCd` | 채널 | Select | | 전체 | 공통코드 `SY/CHNL_CLS_CD` (B B2B / C B2C / I 내부 시스템) |
| `cboOrgUseYn` | 사용여부 | Select | | 사용(`Y`) | `USE_YN_OPTIONS` + 전체 |
| `btnOrgSrch` | 조회 | Button | | | `onBtnOrgSrchClick` |

### 3.2 ② 인터페이스 탭 (`frmInfcSrch`)

| Ref/ID | 항목 | 컴포넌트 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|---|
| `txtInfcKeyword` | 인터페이스ID/명 | Input | | | 인터페이스ID·명·URL 부분 일치. Enter 시 조회 |
| `cboInfcUseYn` | 사용여부 | Select | | 사용(`Y`) | `USE_YN_OPTIONS` + 전체 |
| `btnInfcSrch` | 조회 | Button | | | `onBtnInfcSrchClick` |

### 3.3 ③ 호출이력 탭 (`frmCallSrch`)

| Ref/ID | 항목 | 컴포넌트 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|---|
| `dtpCallPrid` | 기간 | DatePicker (range) | Y | 오늘 ~ 오늘 | 요청일시(`RQMT_DTTM`) 기준. 최대 31일 (`CALL_MAX_DAYS`). 프리셋: 오늘 / 최근 7일 / 최근 31일 |
| `cboCallLnkOrgId` | 연계기관 | Select | | 전체 | 연계기관 목록 (사용여부 무관, `기관명 (기관ID)`) |
| `cboCallInfcId` | 인터페이스 | Select | | 전체 | 인터페이스 목록 (사용여부 무관, `인터페이스명 (인터페이스ID)`) |
| `cboCallRsltCd` | 결과 | Select | | 전체 | `CALL_RSLT_OPTIONS`: `S` 성공(HTTP 2xx) / `F` 실패(그 외) |
| `txtCallTrcId` | 추적ID | Input | | | 완전 일치 (연계기관이 문의할 때 알려 주는 `X-Trace-Id`). 다른 조건과 함께 적용하므로 기간 안에 있어야 조회된다. Enter 시 조회 |
| `btnCallSrch` | 조회 | Button | | | `onBtnCallSrchClick` |

- 연계기관·인터페이스 옵션은 호출이력 탭이 처음 열릴 때 조회하고, ①② 탭에서 저장하면 다시 조회한다.
- 인증 실패(401)처럼 키를 알 수 없는 호출은 연계기관이 비어 있다. 연계기관 조건이 "전체"일 때만 조회된다.

---

## 4. 그리드 / 상세

### 4.1 ① 연계기관 목록 그리드 (`grdLnkOrgList`)

| 컬럼 | Key | 폭 | 정렬 | 비고 |
|---|---|---|---|---|
| 기관ID | `lnkOrgId` | 100px | | 정렬 가능 |
| 기관명 | `lnkOrgNm` | 160px | | 정렬 가능, 말줄임 |
| 채널 | `chnlClsNm` | 70px | 가운데 | `chnlClsCd` → 공통코드 명칭 |
| 사용 | `useYnNm` | 50px | 가운데 | 사용/미사용. 사용기간이 지났으면 "기간만료" 회색 배지 |
| 키 | `useKeyCnt` | 50px | 오른쪽 | 사용(`01`) 상태이면서 만료되지 않은 키 수 |

- 페이징 없음 (기관 수가 적음), 조밀 표시. 건수는 `lblOrgTotCnt`.
- 행 클릭 시 오른쪽 상세(기본정보·키·허용 인터페이스)를 조회한다. 재조회 시 선택했던 기관ID의 행을 다시 활성화한다.
- 선택한 기관이 없으면 오른쪽 영역에 `sy.link.org.select.guide`를 표시한다.

### 4.2 ① 기본정보 폼 (`frmOrgDtl`)

| Ref/ID | 항목 | 컴포넌트 | 필수 | 기본값(신규) | 설명 |
|---|---|---|---|---|---|
| `txtDtlLnkOrgId` | 기관ID | Input | Y | | maxlength 20. 영문 대문자·숫자만 (`^[A-Z0-9]{2,20}$`). 수정 시 읽기전용 |
| `txtDtlLnkOrgNm` | 기관명 | Input | Y | | maxlength 100 (검증 100byte) |
| `cboDtlChnlClsCd` | 채널 | Select | Y | `B` | 공통코드 `SY/CHNL_CLS_CD` ("전체" 없음) |
| `dtpDtlUseStDt` / `dtpDtlUseEndDt` | 사용시작일 / 사용종료일 | DatePicker (단일) 2개 | | 오늘 / 빈 값 | `USE_ST_DT`, `USE_END_DT` (`YYYYMMDD`). 종료일만 비울 수 있어야 하므로 range 대신 단일 2개. 비우면 기간 제한 없음. 종료일 < 시작일이면 `sy.link.validate.use.period` |
| `chkDtlOrgUseYn` | 사용여부 | Checkbox ("사용") | | `N` | 체크 시 `Y` |
| `txtDtlOrgRmk` | 비고 | Input | | | maxlength 200 (검증 200byte) |

- 신규 상태([신규] 클릭)에서는 키·허용 인터페이스 영역을 숨기고 `sy.link.org.save.first`를 표시한다. 저장 후 그 기관을 선택한 상태로 목록을 다시 조회한다.
- 툴바: `btnOrgCallView`(호출이력 보기, 수정 시에만), `btnOrgSave`(저장).

### 4.3 ① 클라이언트 키 그리드 (`grdKeyList`)

| 컬럼 | Key | 폭 | 정렬 | 비고 |
|---|---|---|---|---|
| Client ID | `keyId` | 230px | | 고정폭 글꼴. 셀 오른쪽에 복사 아이콘 버튼 |
| 상태 | `keyStatNm` | 70px | 가운데 | 배지: 사용(초록) / 사용중지(주황) / 폐기(회색). 사용이지만 만료일시가 지났으면 "만료"(빨강) |
| 발급일시 | `issuDttmText` | 140px | 가운데 | `YYYY-MM-DD HH:mm:ss` |
| 만료일시 | `expiDttmText` | 140px | 가운데 | 없으면 "만료 없음". 30일 이내면 경고색 |
| 허용IP | `allwIpAddr` | 180px | | 말줄임 + title. 비어 있으면 "제한 없음" |
| 분 한도 | `miCallLmtCnt` | 64px | 오른쪽 | 천 단위 구분. 비어 있으면 "-" |
| 일 한도 | `dayCallLmtCnt` | 72px | 오른쪽 | 〃 |
| 동시 한도 | `simuRqmtLmtCnt` | 64px | 오른쪽 | 〃 |

- 정렬: 상태(사용 → 사용중지 → 폐기) → 발급일시 내림차순. 폐기 키도 표시한다. 건수는 `lblKeyTotCnt`.
- 행 클릭 시 키 관리 팝업(4.5)을 연다.
- 툴바: `btnKeyIssu`(키발급). 기관 사용여부가 `N`이어도 발급할 수 있다 (사전 준비용).

### 4.4 ① 허용 인터페이스 그리드 (`grdOrgInfcList`)

| 컬럼 | Key | 폭 | 정렬 | 비고 |
|---|---|---|---|---|
| (선택) | | | 가운데 | 공통 `DataTable` 행 선택(`selectable`) 체크박스. 선택한 행 = 허용 인터페이스 (`v-model:selected-rows`), 헤더는 전체 선택 |
| 인터페이스ID | `infcId` | 104px | | |
| 인터페이스명 | `infcNm` | | | |
| 메서드 | `httpMethNm` | 64px | 가운데 | |
| URL | `url` | 240px | | 말줄임 + title |
| 사용 | `infcUseYn` | 64px | 가운데 | 인터페이스 자체의 사용여부. 미사용이면 "미사용" 회색 배지 (허용해도 호출되지 않음) |

- 다른 그리드와 같은 공통 `DataTable`을 쓴다 (페이징 없음, 조밀 표시).
- 전체 인터페이스(사용여부 무관)를 보여 주고, 이 기관에 허용(`TB_LNK_ORG_INFC_R.USE_YN='Y'`)된 것을 선택 상태로 둔다.
- 제목 `허용 인터페이스 - 허용 N / 전체 M건` (`lblOrgInfcCnt`). 체크를 바꾸면 제목 옆에 "변경됨" 표시.
- 툴바: `btnOrgInfcSave`(저장). 저장 후 `sy.link.info.org.infc.saved`(토큰 재발급 안내)를 띄운다.

### 4.5 키 관리 팝업 (`mdlKeyDtl`, 820px)

제목: `클라이언트 키 관리 - {keyId}`

키 정보 폼 (`frmKeyDtl`)

| Ref/ID | 항목 | 컴포넌트 | 필수 | 설명 |
|---|---|---|---|---|
| `lblKeyDtlKeyId` | Client ID | 텍스트 + 복사 버튼 | | `keyId` |
| `lblKeyDtlIssuDttm` | 발급일시 | 텍스트 | | |
| `dtpKeyDtlExpiDt` | 만료일 | DatePicker (단일) | | 만료일 23:59:59로 저장. 비우면 만료 없음. 오늘 이전 날짜 불가 |
| `rdoKeyDtlKeyStatCd` | 상태 | 라디오 그룹 | Y | 공통코드 `SY/KEY_STAT_CD` (01 사용 / 02 사용중지 / 03 폐기). **폐기된 키는 상태를 바꿀 수 없다** (모든 항목 읽기전용, 저장 버튼 숨김) |
| `txaKeyDtlAllwIpAddr` | 허용IP | textarea (3줄) | | 한 줄에 하나 또는 쉼표 구분. IPv4/IPv6 주소 또는 CIDR. 저장 시 쉼표 구분 문자열(최대 1000자)로 합친다. 비우면 제한 없음 |
| `txtKeyDtlMiCallLmtCnt` | 분당 호출 한도 | Input (숫자) | | 1 이상 정수. 비우면 제한 없음 |
| `txtKeyDtlDayCallLmtCnt` | 일 호출 한도 | Input (숫자) | | 〃 |
| `txtKeyDtlSimuRqmtLmtCnt` | 동시 요청 한도 | Input (숫자) | | 〃 |
| `txtKeyDtlRmk` | 비고 | Input | | maxlength 200 (검증 200byte) |

키 이력 그리드 (`grdKeyHistList`, `TB_LNK_KEY_H`, 최근순)

| 컬럼 | Key | 폭 | 비고 |
|---|---|---|---|
| 일시 | `firsRegDtText` | 140px | `YYYY-MM-DD HH:mm:ss` |
| 상태 | `keyStatNm` | 70px | 변경 후 상태 |
| 변경내용 | `chgCont` | 360px | 말줄임 + title (예: "키 발급", "상태 변경: 사용 → 사용중지", "허용IP 변경") |
| 변경자 | `firsRegrId` | 90px | |

- 툴바: `btnKeyDtlSave`(저장).
- 상태를 폐기(`03`)로 바꾸면 저장 전에 `sy.link.confirm.key.revoke`로 한 번 더 확인한다.

### 4.6 키 발급 팝업 (`mdlKeyIssu`, 520px)

제목: `클라이언트 키 발급 - {기관명}`

| Ref/ID | 항목 | 컴포넌트 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|---|
| `rdoKeyIssuExpiCls` | 유효기간 | 라디오 그룹 | Y | `365` | `KEY_EXPI_OPTIONS`: `90` 90일 / `180` 180일 / `365` 1년 / `730` 2년 / `0` 만료 없음 |
| `lblKeyIssuGuide` | 안내 | 텍스트 | | | `sy.link.key.issue.guide` |

- 허용 IP·호출 한도는 발급 후 키 관리 팝업에서 설정한다.
- 툴바: `btnKeyIssuExec`(발급). `sy.link.confirm.key.issue` 확인 후 발급한다.

### 4.7 발급 결과 팝업 (`mdlKeyIssuRslt`, 600px)

제목: `클라이언트 키 발급 완료`. **닫기 버튼과 확인 버튼만 있고, 바깥 클릭·ESC로 닫히지 않는다.**

| Ref/ID | 항목 | 설명 |
|---|---|---|
| `lblKeyIssuRsltKeyId` | client_id | 고정폭 글꼴 + 복사 버튼 (`btnKeyIssuRsltKeyIdCopy`) |
| `lblKeyIssuRsltSecret` | client_secret | 고정폭 글꼴 + 복사 버튼 (`btnKeyIssuRsltSecretCopy`). 기본은 가림(`••••`), 눈 아이콘으로 보기 전환 |
| `lblKeyIssuRsltExpiDttm` | 만료일시 | 없으면 "만료 없음" |
| `lblKeyIssuRsltWarn` | 경고 | `sy.link.key.secret.once` (빨간 글씨) |

- [확인]을 누르면 `sy.link.confirm.key.secret.copied`("client_secret을 안전한 곳에 보관했습니까?")로 확인한 뒤 닫고 키 목록을 다시 조회한다.
- 비밀키 원문은 이 팝업의 컴포넌트 상태에만 두고, 팝업을 닫으면 즉시 지운다 (스토어·로컬 스토리지·콘솔 로그에 남기지 않는다).

### 4.8 ② 인터페이스 목록 그리드 (`grdInfcList`)

| 컬럼 | Key | 폭 | 정렬 | 비고 |
|---|---|---|---|---|
| 인터페이스ID | `infcId` | 100px | | 정렬 가능 |
| 인터페이스명 | `infcNm` | 160px | | 정렬 가능, 말줄임 |
| 메서드 | `httpMethNm` | 64px | 가운데 | |
| 본문로그 | `bodyLogYnNm` | 64px | 가운데 | Y/N → 기록/- |
| 사용 | `useYnNm` | 50px | 가운데 | 사용/미사용 |
| 허용기관 | `allwOrgCnt` | 64px | 오른쪽 | 이 인터페이스를 허용한 연계기관 수 |

- 페이징 없음, 조밀 표시. 건수는 `lblInfcTotCnt`. 행 클릭 시 오른쪽 폼에 상세를 표시한다.

### 4.9 ② 인터페이스 정보 폼 (`frmInfcDtl`)

| Ref/ID | 항목 | 컴포넌트 | 필수 | 기본값(신규) | 설명 |
|---|---|---|---|---|---|
| `txtDtlInfcId` | 인터페이스ID | Input | Y | | maxlength 8, placeholder `IFSY0001`. 형식 `^IF[A-Z]{2}\d{4}$`, 업무약어 2자리는 공통코드 `SY/WRK_MDUL_CD`에 있어야 함. 수정 시 읽기전용. 신규일 때 안내 "IF + 업무약어 2자리 + 순번 4자리" |
| `txtDtlInfcNm` | 인터페이스명 | Input | Y | | maxlength 100 (검증 100byte) |
| `cboDtlHttpMethNm` | 메서드 | Select | Y | `GET` | `HTTP_METH_OPTIONS`: GET / POST / PUT / PATCH / DELETE |
| `txtDtlUrl` | URL | Input | Y | | maxlength 200. `/api/v`로 시작. 경로 변수는 `{id}` 형식. 안내 "Controller 매핑 경로와 같게 입력" |
| `chkDtlBodyLogYn` | 본문로그 | Checkbox ("기록") | | `N` | 요청·응답 본문을 호출 이력에 남길지 |
| `txaDtlMaskFldCont` | 마스킹 필드 | textarea (3줄) | | | maxlength 1000. 본문로그가 체크된 경우에만 활성. 한 줄에 하나 또는 쉼표 구분으로 입력하고 저장 시 쉼표 구분으로 합친다. 필드명만 쓰면 모든 위치(`usrNm`), 점으로 이으면 그 경로만(`data.usrNm`) 마스킹. 안내 "개인정보 필드는 반드시 입력. 토큰·비밀키·비밀번호 필드는 항상 마스킹됨" |
| `chkDtlInfcUseYn` | 사용여부 | Checkbox ("사용") | | `N` | |
| `txtDtlInfcRmk` | 비고 | Input | | | maxlength 200 (검증 200byte) |
| `lblDtlInfcAnnotation` | 코드 선언 | 텍스트 | | | 수정 시 `@ApiInterface("{infcId}")` 를 복사 버튼과 함께 표시 (Controller에 붙일 선언) |

- 툴바: `btnInfcNew`(신규, 목록 위), `btnInfcSave`(저장).
- 화면 등록과 코드 선언은 짝을 이뤄야 한다. 화면에만 등록하면 호출할 API가 없고, 코드에 `@ApiInterface("IF...")`를 선언했는데 여기 등록(또는 기관 허용)이 없으면 호출이 403 `A003`으로 막힌다. coder-api 기동 시 검사는 업무 API 메서드에 선언이 **있는지만** 보고 DB 등록 여부는 보지 않는다.

### 4.10 ③ 호출이력 그리드 (`grdCallList`)

| 컬럼 | Key | 폭 | 정렬 | 비고 |
|---|---|---|---|---|
| NO | `no` | 48px | 가운데 | 조회 순번 |
| 요청일시 | `rqmtDttmText` | 170px | 가운데 | `YYYY-MM-DD HH:mm:ss.SSS` |
| 연계기관 | `lnkOrgNm` | 120px | | 기관명. 없으면 "-" (인증 실패) |
| 인터페이스 | `infcNm` | 140px | | `인터페이스명`, title에 ID |
| 메서드 | `httpMethNm` | 60px | 가운데 | |
| URL | `url` | 200px | | 말줄임 + title |
| HTTP상태 | `httpStatCd` | 70px | 가운데 | 배지: 2xx 초록 / 4xx 주황 / 5xx 빨강 |
| 소요(ms) | `elapsedMs` | 72px | 오른쪽 | 천 단위 구분. 1,000ms 이상 경고색 |
| 오류코드 | `errCd` | 90px | | `A001` 등 |
| IP | `clntIpAddr` | 120px | | |
| Client ID | `keyId` | 120px | | 말줄임 + title |
| 추적ID | `trcId` | 160px | | 말줄임 + title |

- 요청일시 내림차순, 최대 `CALL_MAX_ROWS`(500)건을 받아 20행 페이징한다 (SYLK050 이력 팝업과 같은 방식). 잘리면 건수 뒤에 `cm.info.recent.rows.shown`을 경고색으로 표시한다.
- 행 클릭 시 호출 상세 팝업(4.11)을 연다.

### 4.11 호출 상세 팝업 (`mdlCallDtl`, 900px)

제목: `호출 상세 - {추적ID}`

- 상단 요약: 요청일시, 응답일시, 소요(ms), HTTP상태, 오류코드, 연계기관(ID), Client ID, 인터페이스(ID), 메서드·URL, IP. 추적ID 옆에 복사 버튼.
- 요청 본문(`txaCallDtlRqmtCont`) / 응답 본문(`txaCallDtlRspnCont`): 읽기전용 textarea. JSON이면 들여쓰기해 보여 준다. 저장된 본문이 없으면 `sy.link.call.body.not.logged`.
- 본문은 coder-api가 저장할 때 이미 마스킹·길이 제한(4,000자)을 거친 값이다. 화면에서 추가 마스킹은 하지 않는다.

---

## 5. 버튼 / 이벤트

| Ref/ID | 버튼/이벤트 | 핸들러 | 권한 | 처리 |
|---|---|---|---|---|
| `tabMain` | 탭 변경 | `onTabMainChange` | | 처음 여는 탭이면 초기 조회 (6.0) |
| `btnOrgSrch` | 조회 (①) | `onBtnOrgSrchClick` | v-permission | 연계기관 목록 조회 (6.1) |
| `txtOrgKeyword` | Enter | `onBtnOrgSrchClick` | | 조회 |
| `grdLnkOrgList` | 행 클릭 | `onGrdLnkOrgListRowClick` | | 기관 상세 조회 (6.1) |
| `btnOrgNew` | 신규 (①) | `onBtnOrgNewClick` | v-permission | 기본정보 폼 비우기 |
| `btnOrgSave` | 저장 (기본정보) | `onBtnOrgSaveClick` | v-permission | 검증 후 기관 저장 (6.2) |
| `btnOrgCallView` | 호출이력 보기 | `onBtnOrgCallViewClick` | | ③ 탭으로 이동, 연계기관 조건 = 선택 기관, 기간 = 오늘로 조회 |
| `btnKeyIssu` | 키발급 | `onBtnKeyIssuClick` | v-permission | 키 발급 팝업 열기 (4.6) |
| `btnKeyIssuExec` | 발급 (발급 팝업) | `onBtnKeyIssuExecClick` | | 키 발급 (6.3) |
| `btnKeyIssuRsltKeyIdCopy` / `btnKeyIssuRsltSecretCopy` | 복사 | `onBtnKeyIssuRsltKeyIdCopyClick` / `onBtnKeyIssuRsltSecretCopyClick` | | 클립보드 복사 후 `cm.info.copied` 토스트 |
| `btnKeyIssuRsltOk` | 확인 (결과 팝업) | `onBtnKeyIssuRsltOkClick` | | 보관 확인 후 닫기, 키 목록 재조회 |
| `grdKeyList` | 행 클릭 | `onGrdKeyListRowClick` | | 키 관리 팝업 열기 (4.5) |
| `btnKeyDtlSave` | 저장 (키 관리 팝업) | `onBtnKeyDtlSaveClick` | | 검증 후 키 수정 (6.4) |
| `btnOrgInfcSave` | 저장 (허용 인터페이스) | `onBtnOrgInfcSaveClick` | v-permission | 허용 인터페이스 저장 (6.5) |
| `btnInfcSrch` | 조회 (②) | `onBtnInfcSrchClick` | v-permission | 인터페이스 목록 조회 |
| `txtInfcKeyword` | Enter | `onBtnInfcSrchClick` | | 조회 |
| `grdInfcList` | 행 클릭 | `onGrdInfcListRowClick` | | 인터페이스 상세 표시 |
| `btnInfcNew` | 신규 (②) | `onBtnInfcNewClick` | v-permission | 폼 비우기 |
| `btnInfcSave` | 저장 (②) | `onBtnInfcSaveClick` | v-permission | 검증 후 인터페이스 저장 (6.6) |
| `chkDtlBodyLogYn` | 변경 | `onChkDtlBodyLogYnChange` | | 본문로그 `Y`/`N`, 마스킹 필드 활성/비활성 |
| `btnCallSrch` | 조회 (③) | `onBtnCallSrchClick` | v-permission | 호출이력 조회 (6.7) |
| `txtCallTrcId` | Enter | `onBtnCallSrchClick` | | 조회 |
| `grdCallList` | 행 클릭 | `onGrdCallListRowClick` | | 호출 상세 팝업 열기 (4.11) |

- 저장하지 않은 변경(기본정보·허용 인터페이스·인터페이스 폼)이 있는 상태에서 다른 행을 선택하거나 재조회하면 `cm.confirm.discard.and.search`로 확인한다. 탭 전환은 상태를 유지하므로 확인하지 않는다.
- 컴포넌트 권한(`TB_PRGM_CMPN_M`) 등록 대상: `btnOrgSrch`, `btnOrgNew`, `btnOrgSave`, `btnKeyIssu`, `btnOrgInfcSave`, `btnInfcSrch`, `btnInfcNew`, `btnInfcSave`, `btnCallSrch` 9건 (`save-component` 스킬). 팝업 안 버튼은 권한 제어하지 않는다. 키 관리 팝업의 저장은 `btnKeyIssu` 권한이 없으면 읽기전용으로 연다.

---

## 6. 처리 로직

### 6.0 초기화 (`onMounted`) / 탭 지연 조회
1. `fetchPageData()` : 공통코드 `SY/CHNL_CLS_CD`, `SY/KEY_STAT_CD`, `SY/WRK_MDUL_CD`를 조회한다.
2. `initPageUi()` : 세 탭의 검색조건을 기본값으로 초기화하고, 활성 탭을 `org`로 두고 ① 연계기관 목록을 조회한다.
3. `onTabMainChange` : `loadedTabs`(Set)에 없는 탭이면 그 탭의 최초 조회를 실행하고 기록한다. ② 인터페이스 목록 조회, ③ 기관·인터페이스 옵션 조회 후 오늘 날짜로 호출이력 조회.

### 6.1 연계기관 조회 / 선택
1. `searchLnkOrgList` : 검색어 공백 제거 후 `getLinkOrgListApi`. 이전 선택 기관ID가 결과에 있으면 다시 선택하고 상세를 조회한다.
2. `selectLnkOrg(lnkOrgId)` : `getLinkOrgApi`(기본정보), `getLinkKeyListApi`(키), `getLinkOrgInterfaceListApi`(허용 인터페이스)를 병렬 호출해 오른쪽 영역을 채운다.

### 6.2 연계기관 저장 (`onBtnOrgSaveClick`)
1. 필수(기관ID·기관명·채널), 기관ID 형식, 바이트 길이, 사용기간(시작 ≤ 종료)을 검증한다.
2. `cm.confirm.save` 확인 → `saveLinkOrgApi`. 신규인데 이미 있는 기관ID면 서버가 `sy.link.org.duplicate`.
3. 성공 시 `cm.info.saved`, 목록 재조회 후 그 기관 선택. 호출이력 탭이 로드되어 있으면 기관 옵션도 다시 조회한다.

### 6.3 키 발급 (`onBtnKeyIssuExecClick`)
1. `sy.link.confirm.key.issue` 확인 → `saveLinkKeyApi(lnkOrgId, { expireDays })`.
2. 응답(`keyId`, `clientSecret`, `expiDttm`)으로 발급 결과 팝업(4.7)을 연다. 발급 팝업은 닫는다.
3. 서버는 `KmsClient.generateRandom`으로 키ID(`ck` + 24자리 hex)와 비밀키(32바이트, Base64 URL)를 만들고 `KmsClient.encrypt`(KEK `api-client-secret`)로 암호화해 `TB_LNK_KEY_M`에 저장, `TB_LNK_KEY_H`에 "키 발급" 이력(KMS 감사ID 포함)을 남긴다. 비밀키 원문은 응답에만 넣고 로그·DB에 평문으로 남기지 않는다.

### 6.4 키 수정 (`onBtnKeyDtlSaveClick`)
1. 허용IP 각 항목이 IP 또는 CIDR 형식인지, 한도가 1 이상 정수인지, 만료일이 오늘 이후인지 검증한다.
2. 폐기로 바꾸는 경우 `sy.link.confirm.key.revoke` 확인, 그 외 `cm.confirm.save` 확인 → `saveLinkKeyDtlApi`.
3. 서버는 바뀐 항목만 모아 `TB_LNK_KEY_H.CHG_CONT`에 남긴다 (예: `상태 변경: 사용 → 사용중지; 허용IP 변경`). 폐기된 키는 수정할 수 없다 (`sy.link.key.revoked`).
4. 성공 시 팝업을 닫고 키 목록을 다시 조회한다.

### 6.5 허용 인터페이스 저장 (`onBtnOrgInfcSaveClick`)
1. 변경이 없으면 `cm.info.no.changes`.
2. `cm.confirm.save` 확인 → `saveLinkOrgInterfaceListApi(lnkOrgId, { infcIdList })` (체크된 인터페이스ID 전체). 서버는 목록에 있으면 `USE_YN='Y'`(없으면 INSERT), 목록에 없는 기존 행은 `USE_YN='N'`으로 바꾼다 (행을 지우지 않는다).
3. 성공 시 `sy.link.info.org.infc.saved`.

### 6.6 인터페이스 저장 (`onBtnInfcSaveClick`)
1. 필수, 인터페이스ID 형식·업무약어, URL 형식(`/api/v`로 시작, 공백 없음), 바이트 길이를 검증한다. 본문로그 `N`이면 마스킹 필드를 비워서 보낸다.
2. `cm.confirm.save` 확인 → `saveInterfaceApi`. 신규인데 이미 있으면 `sy.link.infc.duplicate`.
3. 성공 시 목록 재조회 후 그 인터페이스 선택. ① 탭의 허용 인터페이스 목록과 ③ 탭 옵션이 로드되어 있으면 다시 조회한다.

### 6.7 호출이력 조회 (`searchCallList`)
1. 기간 필수(`cm.validate.period.required`), 최대 31일(`cm.validate.period.max`).
2. `getInterfaceCallListApi` (기간 `YYYYMMDD`, `size=500`). 서버는 `RQMT_DTTM` 범위로 월 파티션만 읽는다.
3. 결과가 없으면 그리드 빈 목록 문구 `cm.info.no.data`.

### 6.8 캐시 삭제 알림 (backend)
- 기관·키·인터페이스·허용 인터페이스 저장이 **커밋된 뒤**(`TransactionSynchronization.afterCommit`) Redis 채널 `coder:api:cache-evict`에 메시지(로그 확인용으로 변경 대상, 예: `ORG:LOCAL01`)를 발행한다. coder-api(`LinkClientCache.evictAll`)는 메시지 내용과 관계없이 키·인터페이스·허용 인터페이스 캐시를 모두 비운다.
- 발행이 실패해도 저장은 성공으로 응답하고 WARN 로그를 남긴다 (coder-api 캐시는 최대 5분 뒤 갱신).

### 6.9 코드 / 표시 규칙
- 화면 상수 (`api/sy/link.ts`)

| 상수 | 값 |
|---|---|
| `KEY_EXPI_OPTIONS` | `90` 90일, `180` 180일, `365` 1년, `730` 2년, `0` 만료 없음 |
| `HTTP_METH_OPTIONS` | GET, POST, PUT, PATCH, DELETE |
| `CALL_RSLT_OPTIONS` | `S` 성공, `F` 실패 |
| `CALL_MAX_DAYS` | 31 |
| `CALL_MAX_ROWS` | 500 |

- 공통코드: `SY/CHNL_CLS_CD`(채널), `SY/KEY_STAT_CD`(키상태), `SY/WRK_MDUL_CD`(인터페이스ID 업무약어 검증).

---

## 7. 사용 API

모두 coder-backend(:8080), 사용자 JWT 인증. 패키지 `kr.or.coder.biz.system.link` (`LinkOrgController`, `InterfaceController`, `InterfaceCallController`).

| 함수 | Method / URL | 요청 | 응답 | 비고 |
|---|---|---|---|---|
| `getLinkOrgListApi` | `GET /api/v1/link-orgs` | `keyword`, `chnlClsCd`, `useYn` | `LinkOrg[]` (`useKeyCnt` 포함) | |
| `getLinkOrgApi` | `GET /api/v1/link-orgs/{lnkOrgId}` | | `LinkOrg` | 없으면 `sy.link.org.not.found` |
| `saveLinkOrgApi` | `POST /api/v1/link-orgs` | `LinkOrgRequest` (`isNew` 포함) | `null` | 신규 중복 `sy.link.org.duplicate`. 캐시 삭제 알림 |
| `getLinkKeyListApi` | `GET /api/v1/link-orgs/{lnkOrgId}/keys` | | `LinkKey[]` | `ENC_KEY_VAL`·`KMS_KEY_ID`는 응답에 넣지 않음 |
| `saveLinkKeyApi` | `POST /api/v1/link-orgs/{lnkOrgId}/keys` | `{ expireDays }` | `{ keyId, clientSecret, expiDttm }` | 키 발급. **비밀키 원문은 이 응답에만** |
| `saveLinkKeyDtlApi` | `PATCH /api/v1/link-orgs/{lnkOrgId}/keys/{keyId}` | `keyStatCd`, `expiDt`, `allwIpAddr`, 한도 3종, `rmk` | `null` | 폐기 키 수정 시 `sy.link.key.revoked`. 캐시 삭제 알림 |
| `getLinkKeyHistListApi` | `GET /api/v1/link-orgs/{lnkOrgId}/keys/{keyId}/histories` | | `LinkKeyHist[]` | 최근순 |
| `getLinkOrgInterfaceListApi` | `GET /api/v1/link-orgs/{lnkOrgId}/interfaces` | | `LinkOrgInterface[]` (전체 인터페이스 + `allwYn`) | |
| `saveLinkOrgInterfaceListApi` | `POST /api/v1/link-orgs/{lnkOrgId}/interfaces` | `{ infcIdList }` | `null` | 전체 교체(사용여부). 캐시 삭제 알림 |
| `getInterfaceListApi` | `GET /api/v1/interfaces` | `keyword`, `useYn` | `Interface[]` (`allwOrgCnt` 포함) | |
| `saveInterfaceApi` | `POST /api/v1/interfaces` | `InterfaceRequest` (`isNew` 포함) | `null` | 신규 중복 `sy.link.infc.duplicate`. 캐시 삭제 알림 |
| `getInterfaceCallListApi` | `GET /api/v1/interface-calls` | `stDt`, `endDt`, `lnkOrgId`, `infcId`, `rsltCd`, `trcId`, `size=500` | `InterfaceCallPage` (`list`, `totalCount`, `truncated`) | 서버도 31일·500건 제한 (`@Max`) |
| `getInterfaceCallApi` | `GET /api/v1/interface-calls/{callSn}?rqmtDttm=` | | `InterfaceCall` (본문 포함) | 파티션 키 `rqmtDttm` 함께 전달. 목록 응답에는 본문을 넣지 않음 |
| `getCommCdOptions` | 공통코드 조회 (`api/common/commonCode.ts`) | 업무약어, 코드분류 | `CodeOption[]` | |

backend 구성

- `coder-backend/build.gradle`에 `coder-core-kms` 의존성을 추가하고, `application*.yml`에 coder-api와 **같은** KMS 설정(`coder.kms.provider`, `coder.kms.local.master-key` = `CODER_KMS_MASTER_KEY`)과 KEK ID(`api-client-secret`)를 둔다. 마스터키가 다르면 coder-api가 비밀키를 복호화하지 못해 화면에서 발급한 키의 토큰 발급이 모두 `invalid_client`로 실패한다.
  - 로컬 KMS(`LocalKmsClient`)는 KEK를 따로 등록하지 않는다. 실제 암호화 키는 마스터키 하나이고, KEK ID는 AAD와 암호문 접두사(`local:api-client-secret:v1:...`)로만 쓰인다. 복호화는 암호문에 들어 있는 KEK ID를 쓰므로 KEK ID를 바꿔도 기존 키는 그대로 복호화된다 (2026-11-03 `coder-api-client-secret` → `api-client-secret` 변경, 그 전에 발급한 키는 이전 ID로 남음).
  - 운영 KMS를 도입하면 KEK `api-client-secret`을 KMS에 실제로 생성하고, coder-backend(암호화)와 coder-api(복호화)에 사용 권한을 줘야 한다 (PRD 13장 #3).
  - 공통 `KmsConfig`는 `coder.kms.enabled=false`면 빈을 만들지 않는다 (기본 true, coder-api 영향 없음). coder-backend 운영(`application-prod.yml`)은 운영 KMS가 정해질 때까지 `false`이고, 이때 키 발급은 `sy.link.kms.not.configured`로 거절한다. 로컬 KMS는 prod 프로필 기동을 거부하므로 이 설정이 없으면 운영 backend가 뜨지 않는다.
  - 설정: `coder.link.client-secret-kek-id`(기본 `api-client-secret`), `coder.link.cache-evict-channel`(기본 `coder:api:cache-evict`), `coder.link.call-max-period-days`(31), `coder.link.call-max-rows`(500).
- 이력 등록자ID(`FIRS_REGR_ID`/`FINA_REGR_ID`)는 로그인 사용자ID. 모든 API는 `@PrgmAuth("SYLK010")`.
- 매퍼: `LinkOrgMapper`, `LinkKeyMapper`, `InterfaceMapper`, `InterfaceCallMapper` (XML `mapper/system/link/`). CRUD 동사는 CLAUDE.md Backend 2장.
- 키 수정(PATCH)은 요청에 없는 항목을 비운다 (화면은 항상 전체 값을 보낸다). 만료일은 바꿀 때만 오늘 이후인지 검사한다 (이미 만료된 키의 상태만 바꾸는 경우 허용). 허용IP는 서버에서도 IPv4/IPv6·CIDR 형식을 검사한다.
- 호출이력 상세는 화면이 요청일시를 밀리초까지만 주므로 그 1밀리초 범위(`RQMT_DTTM >= 값 AND < 값 + 1ms`)로 찾는다 (DB는 마이크로초까지 저장).
- coder-api의 기동 인자 키 발급(`ClientKeyIssueRunner`)은 그대로 둔다 (화면 없이 발급할 때 사용).

---

## 8. 메시지

coder-backend `messages*.properties` 3개 파일에 함께 추가한다 (신규 키는 `sy.link.*`).

| 키 | 문구 (ko) | 사용 시점 |
|---|---|---|
| `sy.link.org.select.guide` | 왼쪽 목록에서 연계기관을 선택하거나 신규 버튼으로 등록하세요. | ① 상세 미선택 |
| `sy.link.org.save.first` | 기본정보를 먼저 저장하면 키 발급과 허용 인터페이스를 설정할 수 있습니다. | ① 신규 상태 |
| `sy.link.validate.org.id` | 기관ID는 영문 대문자와 숫자로 2~20자리를 입력하세요. | 기관ID 형식 오류 |
| `sy.link.org.duplicate` | 기관ID [{0}]이(가) 이미 등록되어 있습니다. | 서버 (신규 중복) |
| `sy.link.org.not.found` | 연계기관 [{0}]을(를) 찾을 수 없습니다. | 서버 |
| `sy.link.key.issue.guide` | 발급하면 client_id와 client_secret이 만들어집니다. client_secret은 발급 직후 한 번만 볼 수 있습니다. | 키 발급 팝업 |
| `sy.link.confirm.key.issue` | [{0}]에 새 클라이언트 키를 발급하시겠습니까? | 발급 확인 |
| `sy.link.key.secret.once` | client_secret은 이 창을 닫으면 다시 볼 수 없습니다. 복사해 연계기관에 안전하게 전달하세요. | 발급 결과 경고 |
| `sy.link.confirm.key.secret.copied` | client_secret을 안전한 곳에 보관했습니까?\n창을 닫으면 다시 볼 수 없습니다. | 결과 팝업 닫기 |
| `sy.link.confirm.key.revoke` | 키를 폐기하면 이 키로 토큰을 발급할 수 없고 되돌릴 수 없습니다.\n폐기하시겠습니까? | 폐기 확인 |
| `sy.link.key.revoked` | 폐기된 키는 수정할 수 없습니다. | 서버 |
| `sy.link.key.not.found` | 클라이언트 키 [{0}]을(를) 찾을 수 없습니다. | 서버 |
| `sy.link.kms.not.configured` | KMS가 설정되지 않아 클라이언트 키를 발급할 수 없습니다. 시스템 관리자에게 문의하세요. | 서버 (키 발급, `coder.kms.enabled=false`) |
| `sy.link.validate.ip` | 허용IP [{0}]이(가) 올바른 IP 또는 CIDR 형식이 아닙니다. | 허용IP 형식 오류 |
| `sy.link.validate.limit` | {0}은(는) 1 이상의 숫자로 입력하세요. | 한도 형식 오류 |
| `sy.link.validate.expire.date` | 만료일은 오늘 이후 날짜로 입력하세요. | 만료일 오류 |
| `sy.link.info.org.infc.saved` | 저장되었습니다.\n이미 발급된 토큰에는 반영되지 않으므로 연계기관이 토큰을 다시 발급받아야 합니다. | 허용 인터페이스 저장 |
| `sy.link.validate.infc.id` | 인터페이스ID는 IF + 업무약어 2자리 + 숫자 4자리로 입력하세요.\n(예: IFSY0001) | 인터페이스ID 형식 오류 |
| `sy.link.validate.infc.wrk.mdul` | 업무약어 [{0}]이(가) 등록되어 있지 않습니다. | 업무약어 미등록 |
| `sy.link.validate.url` | URL은 /api/v로 시작하고 공백 없이 입력하세요. | URL 형식 오류 |
| `sy.link.infc.duplicate` | 인터페이스ID [{0}]이(가) 이미 등록되어 있습니다. | 서버 (신규 중복) |
| `sy.link.call.body.not.logged` | 저장된 본문이 없습니다. (본문이 없는 요청이거나 본문로그 미사용 인터페이스) | 호출 상세 |
| `sy.link.call.grid.guide` | 행을 클릭하면 요청·응답 본문을 봅니다. | ③ 그리드 안내 |
| `sy.link.key.empty` | 발급된 클라이언트 키가 없습니다. 키발급 버튼으로 발급하세요. | 키 그리드 빈 목록 |
| `sy.link.org.infc.hint` | 체크한 인터페이스만 호출할 수 있습니다. 이미 발급된 토큰에는 반영되지 않으므로 저장 후 연계기관이 토큰을 다시 발급받아야 합니다. | 허용 인터페이스 하단 안내 |
| `sy.link.validate.use.period` | 사용종료일은 사용시작일 이후로 입력하세요. | 사용기간 오류 |
| `sy.link.infc.select.guide` | 왼쪽 목록에서 인터페이스를 선택하거나 신규 버튼으로 등록하세요. | ② 상세 미선택 |
| `sy.link.infc.code.hint` | 화면 등록과 coder-api 코드 선언(@ApiInterface)이 짝을 이뤄야 호출할 수 있습니다. 미사용으로 바꾸면 모든 기관의 호출이 막힙니다. | ② 폼 하단 안내 |
| `cm.info.no.data` | 조회된 내역이 없습니다. | 목록 결과 없음 |
| `cm.info.no.changes` | 저장할 변경 내용이 없습니다. | 허용 인터페이스 변경 없음 |
| `cm.info.recent.rows.shown` | (최근 {0}건 표시) | 호출이력 500건 초과 |
| `cm.validate.period.required` | 조회 기간을 입력하세요. | 호출이력 기간 미입력 |
| `cm.validate.period.max` | 조회 기간은 최대 {0}일입니다. | 호출이력 31일 초과 |
| `cm.validate.required` | {0} 필수 입력 항목입니다. | 필수 누락 |
| `cm.validate.bytes.exceeded` | {0} {1}byte를 넘을 수 없습니다. (한글 1자 = 3byte) | 바이트 초과 |
| `cm.confirm.save` | 저장하시겠습니까? | 저장 확인 |
| `cm.confirm.discard.and.search` | 저장하지 않은 변경 내용이 있습니다.\n조회하시겠습니까? | 미저장 변경 후 행 이동·재조회 |
| `cm.info.saved` | 저장되었습니다. | 저장 성공 |
| `cm.error.save.failed` | 저장에 실패했습니다. | 저장 실패 (서버 메시지 없을 때) |
| `cm.info.copied` | 복사되었습니다. | 복사 버튼 (**신규 공통 키**, 현재 messages에 없음) |

---

## 9. 등록 데이터

| 대상 | 내용 |
|---|---|
| `TB_PRGM_M` | `SYLK010` API관리, `/sy/sylk010`, 업무 `SY` |
| `TB_MENU_M` | `SY040000` 연계관리 > `SY040100` API관리(LINK, `SYLK010`, 순서 1), `SY040500` 배치관리(`SYLK050`, 순서 5). 로컬 DB 기준에 맞춤 |
| `TB_PRGM_CMPN_M` | 5장 버튼 9건 (`save-component` 스킬, `GRP_IT` 권한 부여) |
| 라우터 | `/sy/sylk010` → `ApiManageView.vue` |

---

## 10. 비고 / 미결사항

- **범위 밖 (후속)**: ④ 사용량 탭(일별 호출 집계). 집계 배치(`TB_INFC_CALL_S`, PRD FR-07)가 아직 없어 배치 개발 후 탭을 추가한다. 호출이력 엑셀 다운로드, 키 만료 임박 알림도 후속.
- 호출이력 조회는 `RQMT_DTTM` 월 파티션을 쓰므로 31일 제한을 둔다. 이력 보관 기간(PRD 13장 #7)이 정해지면 기간 선택 최솟값도 그에 맞춘다.
- 운영 KMS(PRD 13장 #3)가 정해지면 coder-backend와 coder-api의 KMS 설정을 함께 바꿔야 한다. 두 서버의 KEK가 어긋나지 않도록 설정 키를 공통(`coder.kms.*`)으로 둔다.
- coder-backend가 KMS로 비밀키를 암호화할 수 있게 되므로, backend 서버 접근 권한도 coder-api와 같은 수준으로 관리해야 한다.
- 인터페이스를 미사용으로 바꾸면 coder-api 허용 인터페이스 조회(`I.USE_YN = 'Y'`)에서 빠져 모든 기관의 호출이 403 `A003`이 된다. 기동에는 영향이 없다.
