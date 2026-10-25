# Frontend (Vue) 네이밍 컨벤션

이 프로젝트의 Vue 코드를 작성/수정할 때는 아래 네이밍 규칙을 따른다.
전체 문서(표, 예시 포함): https://claude.ai/code/artifact/2ddc3851-199c-4d11-ba22-7e640ae277fc

## 1. 컴포넌트 네이밍

- 파일명/등록명은 **PascalCase**, 두 단어 이상, 역할을 드러내는 이름을 사용한다.
  - 재사용 가능한 프레젠테이셔널 컴포넌트: `Base` 접두사 (`BaseButton`, `BaseInput`, `BaseSelect`)
  - 도메인/화면 종속 컴포넌트: 상위 맥락 + 역할 (`TodoItem`, `UserProfileCard`)
- `btn`, `cbo` 같은 헝가리안 접두사나 특정 라벨명을 컴포넌트 이름에 쓰지 않는다 (재사용성을 해침). 이런 접두사는 아래 3번 "Ref / ID 네이밍"에서만 사용한다.

## 2. Ref / ID 네이밍

형식: `{접두사}{라벨명(PascalCase, 약어 사용)}` — `ref`와 `id` 속성 모두 camelCase 그대로 사용한다 (kebab-case 변환 금지). 라벨명은 약어를 쓴다 (`txtPrgmId`, `btnSrch`, `grdPrgmList`). 약어는 DB 단어약어 사전과 동일한 것을 우선 사용한다.

| 접두사 | 엘리먼트 | 예시 |
|---|---|---|
| `btn` | button | `btnSave`, `btnCancel`, `btnSearch` |
| `cbo` | select / combobox | `cboUserType`, `cboStatus` |
| `txt` | text input | `txtName`, `txtEmail` |
| `txa` | textarea | `txaMemo` |
| `chk` | checkbox | `chkAgree`, `chkUseYn` |
| `rdo` | radio | `rdoGender` |
| `dtp` | datepicker | `dtpStartDate` |
| `grd` | table / grid | `grdUserList` |
| `lbl` | label / static text | `lblTotalCount` |
| `img` | image | `imgProfile` |
| `mdl` | modal / dialog | `mdlConfirm` |
| `frm` | form (전체) | `frmUserEdit` |
| `tab` | tab | `tabDetail` |
| `tre` | tree | `treMenu` |
| `cds` | 코드검색 (코드 입력 + 돋보기 + 명칭, `ComSearchCode`·`BaseSearchCode`) | `cdsUsr` |
| `fup` | 첨부파일 업로드 (`ComFileUpload`) | `fupAttach` |

세부 규칙:
- 이벤트명까지 접미사로 붙이지 않는다 (`btnSaveClick` 금지, 이벤트는 핸들러 이름에서 별도 규정).
- 반복 요소는 인덱스를 추가한다 (`txtPhone1`, `txtPhone2`).
- 이 접두사 규칙은 `ref`/`id`에만 적용하고, `v-model`이 바인딩하는 상태 필드(`form.userType`)에는 적용하지 않는다.
- 그룹핑이 필요하면 맥락 접두사를 추가한다 (`btnSearchApply`, `btnSearchReset`).
- 표에 없는 새 접두사가 필요하면 임의로 만들지 말고 이 문서에 먼저 등록한다.

## 3. 이벤트 핸들러 네이밍

형식: `on{Ref이름}{이벤트명}` — 전체 camelCase, 언더스코어(`_`) 결합 금지 (`btnSearch_click` 같은 ASP.NET 관례 금지).

- 예: `onBtnSaveClick`, `onGrdUserListRowClick`, `onCboUserTypeChange`
- `Handler` 접미사는 붙이지 않는다 (`onBtnSaveClickHandler` 금지). `on` 접두사 자체가 이미 "핸들러"라는 정보를 담고 있어 중복이다.

## 4. 일반 함수 네이밍

- 함수명은 `fn` 같은 무의미한 접두사 없이 **동사로 시작하는 camelCase**를 사용한다.
  - 계산/처리: `calculateTotal`, `formatDate`, `validateForm`
  - 조회: `getUserList`, `fetchOrderDetail`
  - 판별(boolean 반환): `isValid`, `hasPermission`
- 템플릿에 바인딩되는 이벤트 핸들러만 위 3번 규칙(`on~`)을 따른다.

## 5. 페이지 라이프사이클 함수

페이지 진입 시 최초로 실행되는 초기화 함수는 2단계로 나누고, Vue의 `onMounted` 안에서 순서대로 호출한다.

| 함수명 | 순서 | 역할 |
|---|---|---|
| `fetchPageData` | 1 | 페이지에서 사용할 공통코드, 다국어 라벨 등 참조 데이터 조회 |
| `initPageUi` | 2 | 검색영역 초기화, 그리드 초기화 등 화면 요소 초기화 |

```js
onMounted(async () => {
  await fetchPageData();   // 공통코드, 다국어 라벨 조회
  initPageUi();             // 검색영역, 그리드 초기화
});
```

> 주의: ASP.NET WebForms의 `Page_Init → Page_Load`와는 순서가 반대(데이터 조회가 먼저, UI 초기화가 나중)이다.

## 6. 주석 규칙

### 6.1 파일 헤더 주석

모든 `.vue` 파일은 `<script>` 블록 맨 위에, `.ts` 파일은 파일 맨 위에 아래 헤더를 둔다. `Program Type`은 `vue` 또는 `ts`이다.

```js
/*****************************************************************************************
	Program ID   : SYLK050
	Program Type : vue
	Program Name : 배치관리
	Description  : 배치(DSPT) 목록 조회, 등록/수정, 수동 실행 및 실행이력 조회
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
```

- `Program ID`: 화면 ID(`SYLK050` 등)가 있으면 화면 ID, 없으면 파일명(확장자 제외).
- `Program History`에는 최초생성 한 줄만 둔다 (날짜 `YYYY-MM-DD`, 이름). 수정 시 이력 줄을 추가하지 않는다 (다른 팀이 프레임워크를 가져다 쓸 때 혼란이 없도록).

### 6.2 함수 / 이벤트 핸들러 주석

```js
/**
 * onBtnSaveClick : 저장 버튼 클릭 시 변경된 행을 저장
 * @param   row     - 선택된 행 데이터
 * @param   isForce - 변경 여부 확인 없이 저장할지 여부
 * @return  N/A
 * @example onBtnSaveClick(row, false)
 */
```

- 첫 줄은 `함수명 : 설명` 형식으로 쓴다.
- 파라미터마다 `@param 이름 - 설명`, 반환값이 없으면 `@return  N/A`로 쓴다.
- `@example`은 호출 예시가 도움이 될 때만 쓴다.

## 7. 필수입력 표시

- 필수 항목은 입력 컴포넌트(`Input`, `Select`, `DatePicker`, `ComSearchCode`, `BaseSearchCode`)에 `required`(또는 `validate="required"`)를 준다. 컴포넌트가 입력 박스 왼쪽 안쪽에 2px 빨간 줄(`inset 2px 0 0 var(--ds-stamp)`)을 그린다 (비활성/읽기전용이면 표시하지 않음).
- 라벨에 `*`(`required-star`, `.is-required::after`)를 붙이지 않는다.
- 공통 컴포넌트가 아닌 네이티브 컨트롤(textarea, 라디오 그룹 등)은 `is-required-field` 클래스, 그리드 셀 입력은 `cell-input` + `is-required` 클래스로 같은 빨간 줄을 표시한다.
- `required`는 DOM 검증(`validateForm`) 대상도 되므로, 화면에서 `validateForm(영역)`을 쓰는 경우 중복 검증 메시지가 없는지 확인한다.

# Backend (Java / Spring) 네이밍 컨벤션

## 1. 패키지 / 클래스

- 패키지: 소문자, 역도메인 형식 `com.company.project.{layer}.{domain}`
- Controller: `{도메인}Controller`
- Service(인터페이스): `{도메인}Service`, 구현체: `{도메인}ServiceImpl`
- Mapper(MyBatis): `{도메인}Mapper`
- Entity: 도메인명 그대로 (`User`, `Order`)
- DTO: 요청 `{도메인}Request`, 응답 `{도메인}Response`
- Enum: `{도메인}Type` / `{도메인}Status` / `{도메인}Code`

## 2. 메서드 네이밍 (계층별 CRUD 동사)

| 계층 | 조회 | 생성 | 수정 | 삭제 |
|---|---|---|---|---|
| Controller / Service | `get` | `save` | `save` | `delete` |
| Mapper (MyBatis XML) | `select` | `insert` | `update` | `delete` |

- Controller/Service는 생성·수정을 구분하지 않고 `save`로 통일 (내부에서 PK 존재 여부로 insert/update 분기)
- Mapper는 실제 SQL 태그(`<select>`, `<insert>`, `<update>`, `<delete>`)에 맞춰 4가지로 분리
- boolean 반환 메서드는 `is`/`has`/`exists` 접두사 (`isValidUser`, `existsByEmail`)

```java
// Controller / Service
User getUser(Long userId);
void saveUser(UserRequest request);   // 내부에서 insert/update 분기
void deleteUser(Long userId);

// Mapper (interface + XML)
User selectUser(Long userId);
void insertUser(User user);
void updateUser(User user);
void deleteUser(Long userId);
```

## 3. REST 엔드포인트

- URL은 kebab-case, 리소스는 복수형 명사 (`GET /api/users`, `GET /api/common-codes/{groupCode}`)
- 동사를 URL에 넣지 않고 HTTP 메서드로 표현

## 4. 변수 / 상수

- 변수: camelCase, boolean은 `is`/`has` 접두사
- 상수(`static final`): `UPPER_SNAKE_CASE`

## 5. DB 테이블 / 컬럼 네이밍

출처: 개발표준정의서_DB부문 V0.1 (HDD_ET09). 표준 단어/용어/도메인 사전에 등록된 것만 사용하며, 신규 단어·약어는 사전에 먼저 등록한다 (임의 약어 생성 금지). 사전은 [`schema/dictionary/`](schema/dictionary/README.md) 폴더에서 관리한다.

### 5.1 공통 규칙
- 영문은 **대문자**, 숫자·언더스코어(`_`)만 사용한다. `_` 외 특수문자 금지.
- 단어 사이는 `_`로 구분하고, 한 용어는 단어 1~5개 조합으로 만든다.
- 물리명(컬럼명)은 `_` 포함 **30byte 이내**. 한글은 UTF-8(AL32UTF8) 기준 3byte로 계산한다.
- 한글 발음을 영문으로 옮긴 표기(`GOGAEG`)는 쓰지 않는다.
- 부서명을 컬럼명에 넣지 않는다 (부서 변경 시 컬럼명 변경 방지).

### 5.2 용어 구성: `주제어 + [수식어...] + 분류어`
- 컬럼명은 표준용어를 단어별 영문약어로 전환해 `_`로 연결한다. 예) 품질+활동+사례+번호 → `QUALI_ACT_CASE_NO`
- 분류어(도메인)는 용어 맨 끝에 온다. 코드 컬럼은 `수식어 + 코드`로 끝난다 (`구분`, `유형`, `분류`도 허용).
- 숫자로 시작하지 않도록 약어에서 숫자를 뒤로 보낸다 (`3M` → `M3`).
- 일반 용어는 축약하지 않는다 (`주민번호` X → `주민등록번호` O).

### 5.3 영문약어 생성 규칙
- 6자 이하 (사전 등록된 예외 허용). 한 단어가 5자 초과면 축약 (`TARGET` → `TRGT`).
- 둘 이상 단어로 구성된 경우: 각 단어 첫 글자는 고정하고 나머지 모음을 제외한다. 관용 약어는 5자 이하로 예외 허용.
- 약어가 중복되면 첫 모음부터 한 글자씩 추가해 구분한다 (`DMN` → `DON`).

### 5.4 분류어 약어
| 분류어 | 약어 | 분류어 | 약어 |
|---|---|---|---|
| 일자 | `DT` | 코드 | `CD` |
| 시각 | `TM` | 여부 / 유무 | `YN` |
| 일시 | `DTTM` | 일련번호 | `SN` |
| 금액 | `AMT` | 내용 | `CONT` |
| 번호 | `NO` | 주소 | `ADDR` |
| 명 | `NM` | 율 | `RATE` |
| ID | `ID` | 건수 | `CNT` |


### 5.5 도메인 (타입/길이)
- 도메인명은 `분류어 + 타입 + 길이` 형식 (`금액N20.2`, `일자C8`, `명V30`, `내용V4000`). `C`=CHAR, `V`=VARCHAR, `N`=NUMBER.
- 일자 `C8`(YYYYMMDD), 시각 `C6`(HHMMSS), 여부/유무는 `Y`/`N` 1자리.
- 용어는 반드시 사전에 정의된 도메인 중에서 선택한다.

### 5.6 테이블명
- 온라인 테이블: `TB_` + 표준용어 약어를 `_`로 연결. 예) 브랜드정보 → `TB_BRAN_INFO`
- 32byte를 넘으면 `TB_` 헤더를 제외한다.
- 엔티티 유형 (논리명 끝에 붙이는 유형): `M` 마스터, `D` 상세, `L` 내역, `H` 이력, `C` 코드, `I` 정보, `R` 관계, `S` 집계, `Q` 채번, `B` 백업, `T` 임시, `E` 이행/연계, `P` 스냅샷, `V` 뷰.
- 물리 테이블명 끝에는 `_` + 엔티티 유형 1자를 붙인다. `MST`/`DTL`/`REL` 같은 단어 약어를 접미사로 쓰지 않는다. 예) `TB_USR_M`(마스터), `TB_COMM_CD_D`(상세), `TB_USR_MENU_R`(관계)

### 5.7 시스템(공통) 컬럼
모든 엔티티 공통 속성은 `회사코드`, `점코드`, `최초등록자ID`, `최초등록일시`, `최종수정자ID`, `최종수정일시`이다.

| 구분 | 컬럼명 | 타입 | 기본값 | 적용 |
|---|---|---|---|---|
| 사용여부 | `USE_YN` | CHAR(1) | `'N'` | 선택 |
| 입력자ID | `FIRS_REGR_ID` | VARCHAR2(10) | | 필수 |
| 입력일시 | `FIRS_REG_DT` | DATE | SYSDATE | 필수 |
| 변경자ID | `FINA_REGR_ID` | VARCHAR2(10) | | 필수 |
| 변경일시 | `FINA_REG_DT` | DATE | SYSDATE | 필수 |

최초 입력 시 입력자 정보 값을 변경자 정보에도 동일하게 넣는다.

### 5.8 기타 오브젝트 접두사
| 오브젝트 | 형식 | 예 |
|---|---|---|
| Tablespace | `TS_{업무}_{DAT/IDX/TEMP/BLOB/CLOB}` | `TS_DFS_DAT` |
| Data File | `DF_{업무}_{DATA/INDX/TEMP}_{01~99}.dbf` | `DF_SY_DATA_01.dbf` |
| PK | `{테이블명}_PK` | `TB_BRAN_INFO_PK` |
| Index | `{테이블명}_IX{01~99}` | `TB_BRAN_INFO_IX01` |
| Constraint (UK/FK/CK) | `{테이블명}_UK{01~99}` / `_FK{01~99}` / `_CK{01~99}` (NOT NULL 제외) | `TB_BRAN_INFO_FK01` |
| Sequence | `SQ_{테이블}_{컬럼}` | `SQ_SYPG010_M_MBR_ID` |
| View / MView | `VW_`/`VM_` + `{업무}_{순번}` | `VW_SYCD_01` |
| Trigger | `TR_{테이블}_{A/B}{I/U/D}{R/S}` | `TR_SYLG010_M_AIR` |
| Synonym / DB Link | `SN_{객체명}` / `LN_{DB}_{USER}` | |
| Function / Procedure / Package | `FN_`/`SP_`/`PKG_` + `{화면ID}_{기능명}` | `SP_SYCM010_XXXX` |

### 5.9 코드 표준
- 공통코드 테이블 사용을 권장한다. 단독 코드 테이블은 코드값이 999개를 넘거나 계층 구조일 때만, 애플리케이션 하드코딩 코드는 지양한다.
- 코드 유형명: 구분, 분류, 상태, 종류, 방법, 유형, 사유, 관계, 결과 (`여부`/`유무`는 `~코드`를 붙이지 않음). 범용이면 `구분코드`를 권장한다.

## 6. 다국어 메시지(i18n) 키 네이밍

`messages_ko.properties` / `messages_en.properties` 등 메시지 프로퍼티 키는 아래 형식을 따른다.

```
{업무약어(소문자)}.{메시지 식별자 (점으로 구분되는 세부 계층)}
```

- 업무약어(2자리)는 DB 네이밍과 동일한 사전([`schema/dictionary/business-abbreviations.md`](schema/dictionary/business-abbreviations.md))을 그대로 사용한다 (소문자로 표기). 신규 업무 영역은 임의로 만들지 말고 사전에 먼저 등록한다.
- 업무약어 뒤에는 점(`.`)으로 구분된 세부 식별자를 원하는 만큼 이어 붙여 의미 계층을 표현한다. 세부 식별자는 소문자 단어를 쓰고, 단어 사이 구분도 하이픈(`-`) 대신 점(`.`)을 사용한다 (DB 컬럼용 단어약어 사전과는 별개 — 메시지 키는 축약하지 않고 풀어 쓴다).
- 예시:
  - `cm.error.invalid.input` (공통 - 입력값 오류)
  - `cm.error.entity.not.found` (공통 - 리소스 없음)
  - `sy.auth.login.failed` (시스템 - 로그인 실패)
  - `sy.status.maintenance` (시스템 - 점검 중 안내)

### 6.1 메시지 관리 위치

- 화면(coder-frontend)에 표시하는 메시지를 포함한 **모든 메시지는 coder-backend의 `src/main/resources/messages/messages*.properties`에서 관리**한다. `messages.properties`(기본, 영어) / `messages_ko.properties` / `messages_en.properties` 세 파일에 같은 키를 함께 추가한다.
- frontend는 다국어 라이브러리(vue-i18n 등)나 메시지 파일을 두지 않는다. 앱 시작 시 `GET /api/v1/messages`(Accept-Language 기준, 인증 불필요)로 전체 메시지를 내려받고, 코드에서는 `msg()`로 조회한다.
  ```ts
  import { msg } from '@/utils/message'
  toast.success(msg('cm.info.saved'))
  await alert(msg('sy.batch.already.running', dsptId))   // {0}, {1} 인자 치환
  ```
- 대상은 alert/confirm/toast 문구, 검증 오류, 빈 목록 안내 같은 **문장형 메시지**다. 버튼·컬럼명·제목·placeholder 같은 라벨은 대상이 아니다.
- `msg()`는 메시지를 표시하는 시점(함수·computed·템플릿 안)에 호출한다. 모듈 최상위 상수나 `defineProps` 기본값에서 호출하면 메시지가 로드되기 전이라 키가 그대로 표시된다.
- 메시지 문구에 작은따옴표(`'`)를 쓰지 않는다 (Java MessageFormat 이스케이프 문자). 줄바꿈은 `\n`으로 쓴다.

## 7. 주석 규칙

### 7.1 클래스 주석

모든 클래스·인터페이스·enum·record 선언(어노테이션 포함) 바로 위에 아래 형식의 Javadoc을 둔다.

```java
/**
 * 인증(로그인) 및 사용자 프로필 조회 Controller
 * @author ywkim
 * @since 2026.10.09
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.09  ywkim        최초 생성
 * </pre>
 */
```

- 첫 줄에 클래스 역할을 한 줄로 쓴다.
- `<pre>` 안에는 최초 생성 한 줄만 둔다 (날짜 `YYYY.MM.DD`, 작성자). 수정 시 이력 줄을 추가하지 않는다.

### 7.2 메서드 주석

```java
/**
 * 사용자 아이디/비밀번호를 검증하고 JWT 토큰을 발급
 * @param request 로그인 요청 (아이디, 비밀번호)
 * @return 토큰과 사용자 정보
 * @throws BusinessException 사용자가 없거나 비밀번호가 일치하지 않는 경우
 */
```

- 첫 줄에 메서드가 하는 일을 쓴다.
- 파라미터마다 `@param`, 반환값이 있으면 `@return`, 선언되거나 의도적으로 던지는 예외가 있으면 `@throws`를 쓴다 (설명까지 채운다).

### 7.3 MyBatis 매퍼 XML 주석

파일 헤더는 `<!DOCTYPE>`과 `<mapper>` 사이에 둔다. 쿼리(`select`/`insert`/`update`/`delete`)마다 위에 XML 주석(설명 + 작성자)을, 본문 첫 줄에 SQL 주석 `/* {매퍼명}.{쿼리ID} {설명} */`을 둔다. SQL 주석은 DB까지 전달되므로 DB 모니터링·슬로우 쿼리 로그에서 어느 쿼리인지 바로 찾을 수 있다.

```xml
<!--
    SQL File Name : ProgramMapper.xml
    Description   : SYPG010 프로그램관리 MyBatis Mapper (TB_PRGM_M)

    ==개정이력========================================
    수정일     수정자   Version    Query Id
    ========== ======= =========   ===================
    2026.10.17  ywkim    0.1       최초등록
-->
<mapper namespace="kr.or.coder.biz.system.menu.mapper.ProgramMapper">

    <!--
           조회 조건에 맞는 프로그램 목록을 정렬순번 순으로 조회
           작성자 : ywkim
      -->
    <select id="selectProgramList" resultType="kr.or.coder.biz.system.menu.dto.ProgramResponse">
        /* ProgramMapper.selectProgramList 조회 조건에 맞는 프로그램 목록을 정렬순번 순으로 조회 */
        SELECT ...
```

- `namespace`는 매퍼 인터페이스의 전체 클래스명을 그대로 둔다 (MyBatis가 인터페이스와 연결하는 값). SQL 주석의 `{매퍼명}`은 인터페이스의 클래스명만 쓴다.
- 설명은 매퍼 인터페이스 메서드 Javadoc 첫 줄과 같게 쓴다.
- `<sql>` 조각에는 XML 주석만 두고 SQL 주석은 넣지 않는다 (다른 쿼리 중간에 포함되므로).
- 개정이력에는 최초등록 한 줄만 둔다. 수정 시 이력 줄을 추가하지 않는다.
- SQL 주석 안에 `*/`, `#{}`, `${}`를 쓰지 않는다.

## 8. 로그

coder-backend 로그 설정은 `src/main/resources/logback-spring.xml`이다. 로그 폴더는 `LOG_PATH`이고, 없으면 실행 폴더의 `logs/`이다.

| 파일 | 내용 | 형식 | 보관 |
|---|---|---|---|
| `coder-backend.log` | INFO 이상 전체 | 텍스트 `일시 레벨 [스레드] [traceId\|usrId\|sqlId\|errCd] 로거 - 메시지` | 30일 |
| `coder-backend-error.yyyy-MM-dd.N.log` | ERROR만 | JSON Lines (한 줄 = 한 건), 일자별·비압축 (100MB 초과 시 N 증가). SYLO030 시스템로그관리가 파일을 직접 읽는다 | 90일 |

에러 로그(JSON) 필드:

| 필드 | 화면 항목 (SYLO030) | 채우는 곳 |
|---|---|---|
| `logDttm` | 로그일시 (`yyyy-MM-dd HH:mm:ss.SSS`) | logback |
| `sysmClsCd` | 시스템유형 (SY003) | `application.yml` `coder.log.sysm-cls-cd` |
| `usrId` | 사용자ID (사용자명·유형·점은 사용자 테이블 조인) | `LogMdcFilter` (인증 사용자) |
| `errMsg` | 에러내용 | 로그 메시지 |
| `sqlId` | SQL_ID (`{매퍼명}.{쿼리ID}`) | `SqlIdInterceptor` (쿼리 오류 시) |
| `errCd` | 상세 - 에러코드 | `GlobalExceptionHandler` |
| `traceId`, `uri`, `method`, `logger`, `thread`, `stackTrace`, `appNm` | 상세 | `LogMdcFilter` / logback |

- MDC 키는 `kr.or.coder.common.log.LogMdcKey` 상수를 쓴다. 요청 처리 중 MDC는 `LogMdcFilter`가 요청 종료 시 비운다.
- 응답 헤더 `X-Trace-Id`로 화면 오류와 로그를 연결할 수 있다.
- 업무 코드에서 예외를 직접 잡아 로그를 남길 때는 `log.error("설명", e)`처럼 예외 객체를 함께 넘긴다 (스택트레이스가 `stackTrace` 필드에 들어간다). 처리 흐름상 예상된 상황은 ERROR가 아닌 WARN/INFO로 남긴다 (에러 로그 화면에 나오지 않도록).
