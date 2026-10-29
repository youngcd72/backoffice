# 단어약어 사전 (3~4자)

테이블명·컬럼명 내부에서 의미 단위를 표현하는 약어입니다. 신규 단어 추가 시 이 표에 먼저 등록합니다.

## 1. 일반 단어 (3~4자) — 프로젝트 초안

### 1-1. 공통 / 시스템 / 감사·로그

| 단어 | 약어 | 비고 |
|---|---|---|
| 등록 (Register) | `REG` | |
| 등록자 (Registrant) | `REGR` | `REG`(등록 동작)와 별도 표기 |
| 최초 (First) | `FIRS` | |
| 최종 (Final) | `FINA` | |
| 비고 (Remark) | `RMK` | 거의 모든 테이블에 존재 — 공통 예외 후보 |
| 일시 (DateTime) | `DTTM` | `일자(DT)`와 구분 — 날짜만은 DT, 날짜+시간은 DTTM |
| 시간 (Time, 시각) | `TM` | |
| 확정 (Confirm/Fix) | `FIX` | |
| 확인 (Confirm/Verify) | `CFM` | `FIX`(확정)와 별도 |
| 완료 (Complete) | `CPLT` | |
| 처리 (Process) | `PROC` | |
| 승인 (Approve) | `APPR` | |
| 요청 (Request) | `RQMT` | |
| 의뢰 (Request/Referral) | `REQT` |  |
| 반영 (Reflect/Apply, 배포) | `REFL` | 거의 모든 테이블의 `REFL_PRGM_ID`에 사용 — 공통 예외 후보 |
| 정정 (Correction) | `CHG` | |
| 수정 (Update) | `UPD` | |
| 삭제 (Delete) | `DEL` | |
| 취소 (Cancel) | `CNCL` | |
| 초과 (Exceed) | `EXCE` | |
| 중복 (Duplicate) | `DUP` | |
| 미~ (Un-/Not yet) | `NON` | |
| 오류 (Error) | `ERR` | |
| 정보 (Info) | `INFO` | |
| 참조 (Reference) | `REFN` | |
| 값 (Value) | `VAL` | |
| 구분 (Classification) | `CLS` | |
| 분류 (Category, 일반) | `CTG` | `CLS`(구분)와 별도 |
| 유형 (Type) | `TYPE` | |
| 관리 (Management) | `MNGE` | |
| 인터페이스 (Interface) | `INFC` | |
| 신고 (Declaration/Report) | `RPT` | |
| 자동 (Automatic) | `AUTO` | |
| 정렬 (Sort/Align) | `ALIN` | |
| 기타 (Etc.) | `ETC` | |
| 통제 (Control) | `CTRL` | |
| 정상 (Normal) | `NORM` | |
| 기간 (Period) | `PRID` | |
| 기안 (Draft/Proposal) | `DFTR` | |
| 시행 (Enforce/Effective) | `ENFR` | |
| 결과 (Result) | `RESU` | |
| 진행 (Progress) | `PROG` | |
| 발행 (Publish/Issue) | `PUBL` | |
| 원본 (Original) | `ORGN` | |
| 발생 (Occur) | `OBRK` | |
| 배치 (Batch job) | `DSPT` | |
| 실행 (Execute) | `EXEC` | |
| 시스템 (System) | `SYSM` | |
| 성공 (Success) | `SUCC` | |
| 실패 (Fail) | `FAIL` | |
| 메시지 (Message) | `MSG` | |
| 차이 (Difference) | `DIFF` | |
| 경과 (Elapsed) | `ELAPSED` | |
| 상위 (Upper) | `UPPR` | |
| 관리자 (Manager/Admin) | `MNGR` | |
| 종류 (Kind) | `KIND` | |
| 연결 (Connect) | `CONN` | |
| 파라미터 (Parameter) | `PARM` | |
| 다중 (Multiple) | `MLT` | |
| 영역 (Domain/Area) | `DOMA` | |
| 원격 (Remote) | `RMT` | |
| 모니터링 (Monitoring) | `MONI` | |
| 알람 (Alarm) | `ALRM` | |
| 조치 (Measure/Action) | `MEAS` | |
| 위험 (Danger/Risk) | `DANG` | |
| 신규 (New) | `NEW` | |
| 검색 (Search) | `SRCH` | |
| 저장 (Save) | `SAV` | |
| 엑셀 (Excel) | `XLS` | |
| 업로드 (Upload) | `UPLD` | |
| 다운로드 (Download) | `DNLD` | |
| 첨부 (Attach) | `APND` | |
| 경로 (Path/Course) | `CRS` | 파일 경로·매출 경로 등 도메인 무관하게 동일 확인 |
| 서버 (Server) | `SRV` | |
| 확장자 (Extension) | `EXPS` | |
| 버튼 (Button) | `BTN` | |
| 연계 (Link) | `LNK` | |
| 로그인 (Login) | `LGIN` | |
| 접속 (Access) | `ACC` | |
| 통합 (Integrated) | `INTG` | |
| 잠김 (Locked) | `LOCK` | |
| 대외계 (External system) | `OSU` | |
| 입력 (Input) | `INPT` | |
| 유입 (Inflow) | `IFLW` | |

### 1-2. 사람·조직 (사원 / 회원 / 거래처 / 고객)

| 단어 | 약어 | 비고 |
|---|---|---|
| 사원 (Employee) | `EMP` | |
| 협력사원 (Partner/outsourced employee) | `COEMP` | |
| 소속 (Belong/Affiliation) | `BLG` | |
| 고용 (Employment) | `RECR` | |
| 주민등록 (Resident registration) | `REGI` | |
| 직급 (Position/Rank) | `PSIT` | |
| 직무 (Job) | `JOB` | |
| 교육 (Education) | `EDU` | |
| 권한 (Authority) | `AUTH` | |
| 입사 (Join company) | `ECOM` | |
| 퇴사 (Resign) | `RESI` | |
| 타사 (Other company) | `OTCO` | |
| 근무 (Work, 재직) | `WORK` | |
| 개월 (Months, count) | `MONS` | |
| 취득 (Acquire) | `ACQU` | |
| 능력 (Ability) | `ABLT` | |
| 평가 (Evaluate) | `EVAL` | |
| 상금 (Prize) | `PRIZ` | |
| 수상 (Award-winning) | `WINP` | |
| 출신 (Background/Origin) | `CMFR` | |
| 대학 (University) | `UNIV` | |
| 학과 (Department of study) | `SCHS` | |
| 보유 (Possess) | `POSS` | |
| 중문 (Chinese) | `CHN` | |
| 인사 (Personnel/HR) | `PSNL` | |
| 법인 (Corporation) | `CORP` | |
| 담당 (In-charge) | `CHAR` | |
| 지배인 (Store manager) | `MANA` | |
| 이름 (First name) | `FSTNM` | |
| 한글 (Hangeul) | `HNGL` | |
| 영문 (English) | `ENG` | |
| 성별 (Sex) | `SEX` | |
| 전화 (Telephone) | `TEL` | |
| 모바일 (Mobile) | `MOBL` | |
| 이메일 (Email) | `EML` | |
| 주소 (Address) | `ADDR` | |
| 우편 (Zip/Postal) | `ZIP` | |
| 임직원 (Staff) | `STF` | |
| 인증 (Authentication) | `AUCA` | |
| 결혼 (Marriage) | `MARI` | |
| 기념 (Anniversary) | `ANVS` | |
| 회원 (Membership) | `MBSH` | |
| 통합멤버십 (Umbrella Membership) | `UMB` | 복합어 전체를 하나의 약어로 사용(레거시) |
| 등급 (Level/Grade) | `LVL` | |
| 승급 (Level-up) | `LVLU` | |
| 최저 (Minimum) | `MIN` | |
| 탈퇴 (Withdraw) | `WIDR` | |
| 사유 (Reason) | `RESN` | |
| 고유 (Unique) | `UNQ` | |
| 식별 (Identify) | `IDT` | |
| 여권 (Passport) | `PSPT` | |
| 만료 (Expire) | `EXPI` | |
| 국가 (Nation) | `NATI` | |
| 고객 (Consumer/Customer) | `CNSM` | |
| 거래처 (Vendor/Customer) | `CUST` | |
| 대행사 (Agent/Agency) | `AGNT` | |
| 에이전시 (Travel agency) | `AGEN` | |
| 가이드 (Tour guide) | `GID` | |
| 여행 (Travel) | `TRAV` | |
| 그룹 (Group, 단체) | `GRP` | |

### 1-3. 상품 / 브랜드 / 분류

| 단어 | 약어 | 비고 |
|---|---|---|
| 상품 (Goods) | `GOOS` | |
| 품목 (Item) | `ITEM` | |
| 브랜드 (Brand) | `BRAN` | |
| 대분류 (Top category) | `HIGP` | |
| 중분류 (Middle category) | `MLF` | 확정 |
| 소분류 (Small category) | `LGRP` | 확정 |
| 약명 (Short/abbreviated name) | `SNM` | |
| 주요 (Major) | `MAJ` | |
| 색상 (Color) | `CLR` | |
| 단위 (Unit) | `UNIT` | |
| 규격 (Standard/Spec) | `STDZ` | |
| 소재 (Material) | `MATL` | |
| 액체 (Liquid) | `LQD` | |
| 화기 (Flammable) | `FIRE` | |
| 배터리 (Battery) | `BATT` | |
| 바코드 (Barcode) | `BRCD` | |
| 판매가 (Selling price) | `SPRC` | |
| 시즌 (Season) | `SEAS` | |
| 원산지 (Place of origin) | `PLOR` | |
| 필수 (Essential) | `ESSN` | |

### 1-4. 매장 / 입지 / 재고 / 물류

| 단어 | 약어 | 비고 |
|---|---|---|
| 점 (Store) | `STOR` | |
| 코너 (Corner) | `CORN` | |
| 부서 (Department) | `DEPT` | |
| 위치 (Location) | `LOC` | |
| 장소 (Place) | `PLAC` | |
| 층 (Floor) | `FLOR` | |
| 열 (Line/Row) | `LINE` | |
| 창고 (Warehouse) | `WH` | |
| 매장 (Shop floor) | `SHOP` | |
| 입점 (Move-in / check-in) | `STO` | |
| 퇴점 (Check-out) | `STGO` |  |
| 인도장 (Delivery counter, BCA) | `BCA` | |
| 세관 (Customs authority) | `CSTM` | |
| 관세사 (Customs broker) | `CUSTHBR` | |
| 면세 (Duty-free) | `DTFR` | |
| 공항 (Airport) | `AIRP` | |
| 구역 (Zone/Region) | `RGN` | |
| 특허(면적) (Licensed/patent) | `PTNT` | |
| 공통 (Common) | `COMM` | |
| 면적 (Area) | `EXTT` | |
| 규모 (Scale) | `SCAL` | |
| 기업 (Enterprise) | `ENTE` | |
| 재고 (Stock) | `STOC` | |
| 실사 (Physical inventory count) | `INVT` | |
| 에이징 (Aging) | `AGIG` | |
| 이동 (Move) | `MOV` | |
| 가감 (Adjustment) | `ADJ` | |
| 만적 (Fully loaded) | `FULO` | |
| 가용 (Available/Usable) | `UE` | |
| 적치 (Load/Stack) | `LOAD` | |
| 전시 (Display) | `DISP` |  |

### 1-5. 금액 / 정산 / 결제 / 카드

| 단어 | 약어 | 비고 |
|---|---|---|
| 정산 (Settlement/Calc) | `CALC` | |
| 결제 (Payment) | `SETT` | `CALC`(정산)와 별도 |
| 조건 (Condition) | `COND` | |
| 원화 (KRW) | `WON` | |
| 달러 (Dollar/USD) | `DOLR` | |
| 외화 (Foreign Currency) | `FC` | |
| 통화 (Currency) | `CUR` | |
| 환율 (Exchange rate) | `EXCHR` | |
| 매매 (Trading, buy-sell) | `TRD` | |
| 순~ (Net) | `NET` | |
| 합계 (Total) | `TOT` | |
| 잔액 (Balance) | `RMD` | |
| 할인 (Discount) | `DC` | |
| 율 (Rate) | `RATE` | |
| 수수료 (Fee) | `FEE` | |
| 부대비(용) (Incidental cost) | `ADDF` | |
| 배부/배분 (Distribute/Allocate) | `DSTR` |  |
| 이자 (Interest) | `INTE` | |
| 과세 (Tax) | `TAX` | |
| 부가세 (VAT) | `VAT` | |
| 전자세금(계산서) (E-Tax invoice) | `ETAX` | |
| 환급 (Refund/Tax-refund) | `REFU` | |
| 입금 (Deposit) | `DEPO` | |
| 출금 (Withdrawal) | `WI` |  |
| 현금 (Cash) | `CASH` | |
| 청구 (Charge/Claim) | `CHAG` | |
| 이익 (Profit) | `PROF` | |
| 소득 (Income) | `INCM` | |
| 보류 (Suspend/Hold) | `SUSP` | |
| 체크(카드) (Check) | `CHK` | |
| 결재 (Approval doc) | `APV` |  |
| 계좌 (Account) | `ACNT` | |
| 은행 (Bank) | `BNK` | |
| 송금 (Remittance) | `REMT` | |
| 어음 (Bill/Draft) | `BIOE` | |
| 보험 (Insurance) | `INSU` | |
| 보험료 (Insurance premium) | `INFD` | |
| 관련 (Related) | `RTD` | |
| 거래 (Transaction) | `TRS` | |
| 주기 (Cycle) | `CYCL` | |
| 마진 (Margin) | `MARG` | |
| 예산 (Budget) | `BUDG` | |
| 산술 (Arithmetic, 이론치) | `ARIT` | |
| 조정 (Adjustment) | `ADJU` | |
| 실~ (Actual/Real) | `REAL` | |
| 계획 (Plan) | `PLAN` | |
| 목표 (Goal/Target) | `GOAL` | |
| 실적 (Result/Performance) | `RSLT` | |
| 진척(율) (Progress) | `PRGR` | |
| 집계 (Tally/Aggregate) | `TTL` | |
| 인센티브 (Incentive) | `INCT` | |
| 에누리 (Markdown allowance) | `EDC` | |
| 월 (Month) | `MON` | |
| 평균 (Average) | `AVG` | |
| 전년 (Last year) | `BYY` | |
| 전월 (Last month) | `LAM` | |
| 대비 (Compared to/vs) | `VS` | |
| 성장 (Growth) | `GROW` | |
| 안전 (Safety) | `SAFE` | |
| 지수 (Index) | `IDX` | |
| 적정 (Appropriate/Proper) | `PPRT` | |
| 필요 (Need/Required) | `NEED` | |

### 1-6. 구매 / 입출고 / 통관 / 배송

| 단어 | 약어 | 비고 |
|---|---|---|
| 매입 (Purchase) | `PUR` | |
| 구매 (Buy) | `BUY` |  |
| 발주 (Order/PO) | `ORD` | |
| 년월 (Year-Month) | `YMON` | |
| 입고 (Receiving) | `RECP` | |
| 출고 (Release/Ship-out) | `DLFW` | |
| 반입 (Bring-in) | `CARI` | `TKOT`(반출)과 대응 |
| 반출 (Take-out/Send-out) | `TKOT` |  |
| 반송 (Return to vendor) | `SDBK` | |
| 반품 (Customer return) | `RTN` |  |
| 재매입 (Repurchase/Buy-back) | `RPUC` | |
| 판매취소 (Sale cancellation) | `SACL` | |
| 판매 (Sell) | `SELL` | |
| 매출 (Sales) | `SALE` | |
| 발급 (Issue) | `ISSU` | |
| 교환 (Exchange) | `EXCH` | |
| 취급 (Handling) | `HADL` | |
| 운송 (Transport) | `TRAN` | |
| 보관 (Storage) | `KEEP` | |
| 항공 (Air) | `AIR` | |
| 차량 (Vehicle) | `CAR` | |
| 제조 (Manufacture) | `MANU` | |
| 증빙 (Evidence/Proof) | `EVID` | |
| 수입 (Import) | `IMP` | |
| 수출 (Export) | `EPT` | `IMP`(수입)의 대응 짝 |
| 국산 (Domestic goods) | `DMST` | |
| 외산 (Foreign goods) | `FRN` | |
| 선적 (Shipment) | `SIPM` | |
| 환적 (Transship) | `TRA` | |
| 도착 (Arrival) | `ARRV` | |
| 도착지 (Destination) | `DEST` | |
| 출발 (Depart, 운항) | `DEPA` | |
| 방법 (Method) | `METH` | `WAY`(수단)와 유사 — 통합 여부 재검토 |
| 수단 (Means) | `WAY` | 위 `METH`와 함께 재검토 |
| 오퍼 (Offer) | `OFFR` | |
| 공지 (Notice) | `NOTI` | |
| 공급 (Supply) | `SUPP` | |
| 분할 (Partial) | `PRTN` | |
| 가격 (Price) | `PRIC` | |
| 포워더 (Forwarder) | `FRDR` | |
| 카톤 (Carton) | `CT` | |
| 중량 (Weight) | `WT` | |
| 인보이스 (Invoice) | `INVO` | |
| 전표 (Slip/Voucher) | `SLIP` | |
| 인터넷 (Internet) | `INET` | |
| 접수 (Receipt/Reception) | `RCEP` | |
| 인수 (Take-over) | `TAOV` | |
| 원패킹 (One-packing) | `ONEP` | |
| 미인도 (Undelivered) | `UDLV` | |
| 유치 (Hold/Detain) | `CSTD` | |
| 면장 (Customs permit doc) | `PMIT` | |
| 발송 (Send/Dispatch) | `SND` | |
| 교환권 (Coupon/Voucher) | `COUP` | `CUP`(쿠폰)과 별도 |
| 쿠폰 (Coupon) | `CUP` | `COUP`(교환권)와 별도 |
| 수신자 (Receiver) | `RCVR` | |
| 서명 (Signature) | `SGNT` | |
| 이미지 (Image) | `IMG` | |
| 보세 (Bonded) | `BOND` | |
| 항목 (Item, 세부) | `ALTI` | |
| 물류 (Logistics) | `LOGI` | |
| 채널 (Channel) | `CHL` | |
| 제외 (Exclude) | `EXCL` | |
| 환산 (Conversion) | `CONV` | |
| 리워즈 (Rewards) | `REWA` | |
| 대표 (Representative) | `REPR` | |
| 금융 (Finance) | `FNCE` | |
| 기관 (Institution) | `ORG` |  |
| 조직 (Organization) | `ORNZ` |  |
| 서비스 (Service) | `SRVC` | |
| 수기 (Manual/Handwritten) | `WRT` | |
| 체류 (Stay) | `STAY` | |
| 분 (Minute) | `MI` | |
| 후~ (After) | `AFT` | |
| 이전 (Previous/Before) | `PREV` | 시점·순서 모두 사용 (탭/페이지 이동 포함) |
| 카지노 (Casino) | `CASN` | |
| 순번 (Sequence) | `SEQ` | |
| 일련 (Serial) | `SER` | |
| 단가 (Unit price) | `UCST` | |
| 시작 (Start) | `ST` | |
| 종료 (End) | `END` | |
| 제목 (Title) | `TITL` | |
| 지급 (Payment/Provide) | `PMNT` | |
| 사용 (Use) | `USE` | |
| 회수 (Collect) | `COLL` | |
| 기말 (Term-end) | `TEND` | |
| 기초 (Beginning) | `TST` | |
| 업무 (Work/Task) | `WRK` | |
| 내용 (Content) | `CONT` | |
| 적용 (Apply) | `APPL` | |
| 기준 (Standard) | `STD` | |
| 수신 (Receive) | `RCV` | |

### 1-7. 마케팅 / 프로모션 / 포인트 / 특판 / 여행

| 단어 | 약어 | 비고 |
|---|---|---|
| 프로모션 (Promotion) | `PMTN` | |
| 판촉품 (Promotional item) | `SPTM` | |
| 기획 (Plan/Planning) | `PLN` | `PMTN`(프로모션)과 별도 |
| 용도 (Usage) | `USAG` | |
| 한도 (Limit) | `LMT` | |
| 경품 (Giveaway) | `GVAW` | |
| 추첨 (Lottery/Drawing) | `LOTT` | |
| 대상 (Target) | `TARG` | |
| 요일 (Day of Week) | `DOW` | |
| 증정 (Present/Gift) | `PREN` | |
| 제휴(사) (Partner) | `PTNR` | |
| 포인트 (Point) | `POIN` | |
| 적립 (Accumulate) | `RSVG` | |
| 스케줄 (Schedule) | `SCHE` | |
| 사진 (Photo) | `PHOT` | |
| 긴급 (Urgent) | `UGNT` | |
| 특판 (Special sale) | `SPSA` | |
| 입국 (Entry) | `ENTR` | |
| 출국 (Departure) | `DPAT` | |
| 희망 (Hope/Desired) | `HOPE` | |
| 해외 (Overseas) | `OVSE` | |
| 전송 (Send/Transmit) | `SEND` | |
| 예약 (Reservation) | `RSVT` | |
| 여객 (Passenger) | `PSNG` | |
| 크루즈 (Cruise) | `CRUZ` | |
| 경유 (Via/Transit) | `VIA` | |
| 탑승 (Boarding) | `BORD` | |
| 인도 (Hand-over/Delivery) | `DELV` | |
| 밴사 (VAN company) | `VAN` | |
| 행사 (Event) | `EVT` | |
| 사전 (Beforehand) | `BFHD` | |
| 휴일 (Holiday) | `HLDY` | |

### 1-8. 화면 / UI (Ref·ID 라벨명, 프론트엔드)

프론트엔드 `ref`/`id` 라벨명(`txtPrgmId`, `btnSrch` 등)에 쓰는 약어입니다. 이미 등록된 단어(`SRCH` 검색, `SAV` 저장, `NEW` 신규, `USE` 사용, `TOT` 합계 등)는 위 표의 약어를 그대로 씁니다. 화면 버튼 `조회`는 `SRCH`를 사용합니다.

| 단어 | 약어 | 비고 |
|---|---|---|
| 프로그램 (Program) | `PRGM` | |
| 명 (Name) | `NM` | 분류어 |
| 여부 (Yes/No) | `YN` | 분류어 |
| 아이디 (Identification) | `ID` | 분류어 |
| 초기화 (Initialize) | `INIT` | |
| 건수 (Count) | `CNT` | |
| 목록 (List) | `LIST` | |
| 탭 (Tab) | `TAB` | |
| 홈 (Home) | `HOME` | |
| 다음 (Next) | `NEXT` | |
| 닫기 (Close) | `CLOSE` | |
| 전체 (All) | `ALL` | |
| 행 (Row) | `ROW` | 그리드 행추가/행삭제 버튼 (`btnRowAdd`, `btnRowDel`) |
| 추가 (Add) | `ADD` | |
| 트리 (Tree) | `TREE` | 트리 전체 열기/닫기 버튼 (`btnTreeOpen`, `btnTreeClose`) |
| 열기 (Open) | `OPEN` | |
| 복사 (Copy) | `COPY` | 그룹권한관리 권한복사 버튼 (`btnAuthCopy`) |
| 최상위 (Root) | `ROOT` | 상위메뉴 팝업의 최상위 지정 버튼 (`btnUpprMenuRoot`) |

### 1-9. 시스템 / 사용자·권한 (시스템관리 ERD 출처)

`table/` 폴더의 시스템관리 테이블(`TB_USR_M` 등) 작성 시 ERD(시스템관리)에서 사용된 약어 중 사전에 없던 단어입니다. `프로그램`은 기존 `PRGM`(1-8)을 쓰며, ERD의 `PGM`은 사용하지 않습니다.

| 단어 | 약어 | 비고 |
|---|---|---|
| 사용자 (User) | `USR` | |
| 비밀번호 (Password) | `PWD` | |
| 메뉴 (Menu) | `MENU` | |
| 마스터 (Master) | `MST` | 엔티티 유형 `M`(마스터·기본)에 대응. 테이블명 접미사는 `MST`가 아닌 `_M` 사용 (`TB_USR_M`) |
| 상태 (Status) | `STAT` | 코드 유형명 `상태`(5.9) — `STATUS` 6자 초과로 축약 |
| 출력 (Print) | `PRT` | |
| URL | `URL` | |

`TB_PRGM_FAV_L`(구 `TB_FAV_M`) 작성 시 추가된 단어입니다.

| 단어 | 약어 | 비고 |
|---|---|---|
| 즐겨찾기 (Favorite) | `FAV` | |

### 1-10. 공통코드 (기준정보 ERD 출처)

`TB_COMM_CD_M` / `TB_COMM_CD_D` 작성 시 ERD(기준정보관리)에서 사용된 약어 중 사전에 없던 단어입니다.

| 단어 | 약어 | 비고 |
|---|---|---|
| 상세 (Detail) | `DTL` | 엔티티 유형 `D`(상세)에 대응. 테이블명 접미사는 `DTL`이 아닌 `_D` 사용 (`TB_COMM_CD_D`) |
| 모듈 (Module) | `MDUL` | `WRK_MDUL_CD`(업무모듈코드) |
| 자릿수 (Digit number) | `DNUM` | `COMM_CD_DNUM`(공통코드자릿수) |
| 관계 (Relation) | `REL` | 엔티티 유형 `R`(관계)에 대응. 테이블명 접미사는 `REL`이 아닌 `_R` 사용 (`TB_AUTH_GRP_MENU_R`, `TB_USR_MENU_R`) |

### 1-11. 컴포넌트 권한 (신규 설계)

`TB_PRGM_CMPN_M` / `TB_AUTH_GRP_CMPN_R` / `TB_USR_CMPN_R` 작성 시 추가된 단어입니다.

| 단어 | 약어 | 비고 |
|---|---|---|
| 컴포넌트 (Component) | `CMPN` | 화면 요소(버튼 등). `CMPN_ID`는 프론트 ref/id 명과 동일 |

### 1-12. 첨부파일 (시스템관리 ERD 출처)

`TB_APND_FILE_M`(ERD `SY0050M` 시스템_첨부파일기본) 작성 시 ERD 물리명에서 사용된 약어 중 사전에 없던 단어입니다.

| 단어 | 약어 | 비고 |
|---|---|---|
| 파일 (File) | `FILE` | |
| 사이즈 (Size) | `SIZE` | 바이트 단위 크기 (`FILE_SIZE`) |

### 1-13. 배치 (coder-batch)

`TB_DSPT_JOB_M`(배치직무기본) / `TB_DSPT_EXEC_L`(배치실행내역) 작성 시 추가된 단어입니다 ([documents/batch/PRD.md](../../documents/batch/PRD.md) 7.2). DB 표준 담당자 검토 대상입니다.

| 단어 | 약어 | 비고 |
|---|---|---|
| 표현식 (Expression) | `EXPR` | `SCHE_EXPR`(스케줄표현식, Quartz cron) |
| 미스파이어 (Misfire) | `MSFR` | Quartz 실행 시각을 놓친 Trigger 처리. `MSFR_PLCY_CD`(미스파이어정책코드) |
| 정책 (Policy) | `PLCY` | |
| 클래스 (Class) | `CLASS` | `EXEC_CLASS_NM`(실행클래스명, Job 빈을 정의한 Java 클래스). `CLS`는 구분(Classification)으로 사용 중 |

### 1-14. 대외 연계

대외 연계(연계기관·키·인터페이스·이력·집계, 시스템인증서) 용어 작성 시 추가된 단어입니다. coder-api 테이블(`TB_LNK_*`, `TB_INFC_*`, documents/api/PRD.md 7장)에서 사용합니다. DB 표준 담당자 검토 대상입니다.

| 단어 | 약어 | 비고 |
|---|---|---|
| 키 (Key) | `KEY` | `KEY_ID`, `ENC_KEY_VAL`, `KEY_STAT_CD` |
| 암호화 (Encryption) | `ENC` | `ENC_KEY_VAL`(KMS 암호화된 비밀키) |
| KMS | `KMS` | Key Management Service. `KMS_KEY_ID`, `KMS_AUDIT_ID` |
| 감사 (Audit) | `AUDIT` | 6자 이하 일반 단어로 축약하지 않음 |
| 인증서 (Certificate) | `CERT` | `CERT_SN`, `CERT_STAT_CD` |
| 인스턴스 (Instance) | `INST` | `INST_NM` |
| 이력 (History) | `HIST` | `KEY_HIST_SN`. 엔티티 유형 `H`(이력)와 별도 |
| 참조 (Reference) | `REF` | `SEND_AUCA_REF_KEY`(비밀 저장소 참조키) |
| 호출 (Call) | `CALL` | `MI_CALL_LMT_CNT`, `CALL_CNT` |
| 일 (Day) | `DAY` | 기간 단위. 날짜 분류어 `일자(DT)`와 별도 (`DAY_CALL_LMT_CNT`) |
| 동시 (Simultaneous) | `SIMU` | `SIMU_RQMT_LMT_CNT`(동시요청한도건수) |
| 허용 (Allow) | `ALLW` | `ALLW_IP_ADDR` |
| IP | `IP` | `ALLW_IP_ADDR`, `CLNT_IP_ADDR` |
| 채널 (Channel) | `CHNL` | `CHNL_CLS_CD` (C B2C, B B2B, I 내부) |
| 주체 (Principal) | `PRNC` | `PRNC_ID`(회원ID / 연계기관ID / 시스템ID) |
| HTTP | `HTTP` | `HTTP_METH_NM`, `HTTP_STAT_CD` |
| 메서드 (Method) | `METH` | 1-6의 `방법(METH)`과 같은 약어 재사용 - 검토 필요 |
| 본문 (Body) | `BODY` | `BODY_LOG_YN` |
| 로그 (Log) | `LOG` | `BODY_LOG_YN` |
| 마스킹 (Masking) | `MASK` | `MASK_FLD_CONT` |
| 필드 (Field) | `FLD` | `MASK_FLD_CONT` |
| 추적 (Trace) | `TRC` | `TRC_ID`(X-Trace-Id) |
| 응답 (Response) | `RSPN` | `RSPN_DTTM`, `RSPN_CONT` |
| 재시도 (Retry) | `RTRY` | `RTRY_CNT` |
| 재전송 (Resend) | `RESEND` | `RESEND_TARG_YN` |
| 클라이언트 (Client) | `CLNT` | `CLNT_IP_ADDR` |
| 밀리초 (Millisecond) | `MS` | `ELAPSED_MS`(경과밀리초 = 소요시간). 2026-10-27 coder-api 추가 - 검토 필요 |
| 최대 (Maximum) | `MAX` | `MAX_ELAPSED_MS`(최대경과밀리초). 2026-10-27 coder-api 추가 - 검토 필요 |
