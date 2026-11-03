<script setup lang="ts">
/*****************************************************************************************
	Program ID   : LinkOrgTab
	Program Type : vue
	Program Name : API관리 - 연계기관 탭
	Description  : SYLK010 ① 연계기관 탭. 연계기관 목록·기본정보(TB_LNK_ORG_M), 클라이언트 키 발급·수정(TB_LNK_KEY_M,
	               TB_LNK_KEY_H), 기관별 허용 인터페이스(TB_LNK_ORG_INFC_R)를 관리한다.
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-11-03  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { ref, computed, nextTick } from 'vue'
import { Button, Input, Select, DataTable, DatePicker, PopupWindow, useDialog, useToast } from '@/components/common'
import { msg } from '@/utils/message'
import {
  KEY_EXPI_OPTIONS,
  getLinkKeyHistListApi,
  getLinkKeyListApi,
  getLinkOrgApi,
  getLinkOrgInterfaceListApi,
  getLinkOrgListApi,
  saveLinkKeyApi,
  saveLinkKeyDtlApi,
  saveLinkOrgApi,
  saveLinkOrgInterfaceListApi,
  type LinkKey,
  type LinkKeyHist,
  type LinkKeyIssueResult,
  type LinkOrg,
  type LinkOrgInterface,
} from '@/api/sy/link'
import { USE_YN_OPTIONS, codeName, withAll, type CodeOption } from '@/utils/code'
import { daysUntil, dashToYmd, todayDash, todayYmd, ymdToDash } from '@/utils/date'
import { formatCount } from '@/utils/format'
import { validateFields } from '@/utils/validate'
import { useComponentAuthStore } from '@/stores/componentAuth'

const props = defineProps<{
  /** 채널구분 공통코드 (SY/CHNL_CLS_CD) */
  chnlClsOptions: CodeOption[]
  /** 키상태 공통코드 (SY/KEY_STAT_CD) */
  keyStatOptions: CodeOption[]
}>()

const emit = defineEmits<{
  /** 호출이력 보기 (연계기관ID) */
  (e: 'call-view', lnkOrgId: string): void
  /** 연계기관 저장 */
  (e: 'saved'): void
  /** 허용 인터페이스 저장 */
  (e: 'infc-saved'): void
}>()

/** 연계기관 그리드 행 (표시용 값 포함) */
interface LinkOrgRow extends LinkOrg {
  chnlClsNm: string
  useYnNm: string
  /** 사용종료일자가 지났는지 */
  isPridExpired: boolean
}

/** 클라이언트 키 그리드 행 (표시용 값 포함) */
interface LinkKeyRow extends LinkKey {
  keyStatNm: string
  issuDttmText: string
  expiDttmText: string
  /** 사용 상태인데 만료일시가 지났는지 */
  isExpired: boolean
  /** 만료일시까지 30일 이내인지 */
  isExpiring: boolean
}

/** 키 이력 그리드 행 */
interface LinkKeyHistRow extends LinkKeyHist {
  keyStatNm: string
  firsRegDtText: string
}

/** 연계기관 기본정보 폼 (빈 값은 빈 문자열, 일자는 YYYY-MM-DD) */
interface LinkOrgForm {
  lnkOrgId: string
  lnkOrgNm: string
  chnlClsCd: string
  useStDt: string
  useEndDt: string
  useYn: string
  rmk: string
}

/** 키 관리 팝업 폼 (한도는 입력 문자열, 허용IP는 줄바꿈 구분) */
interface LinkKeyForm {
  keyStatCd: string
  expiDt: string
  allwIpText: string
  miCallLmtCnt: string
  dayCallLmtCnt: string
  simuRqmtLmtCnt: string
  rmk: string
}

const PAGE_ID = 'SYLK010'
const KEY_STAT_REVOKED = '03'

const orgColumns = [
  { key: 'lnkOrgId', label: '기관ID', width: '100px', sortable: true },
  { key: 'lnkOrgNm', label: '기관명', sortable: true },
  { key: 'chnlClsNm', label: '채널', width: '84px', align: 'center' },
  { key: 'useYnNm', label: '사용', width: '72px', align: 'center' },
  { key: 'useKeyCnt', label: '키', width: '44px', align: 'right' },
]

const keyColumns = [
  { key: 'keyId', label: 'Client ID', width: '250px' },
  { key: 'keyStatNm', label: '상태', width: '70px', align: 'center' },
  { key: 'issuDttmText', label: '발급일시', width: '140px', align: 'center' },
  { key: 'expiDttmText', label: '만료일시', width: '140px', align: 'center' },
  { key: 'allwIpAddr', label: '허용IP', width: '180px' },
  { key: 'miCallLmtCnt', label: '분 한도', width: '64px', align: 'right' },
  { key: 'dayCallLmtCnt', label: '일 한도', width: '76px', align: 'right' },
  { key: 'simuRqmtLmtCnt', label: '동시 한도', width: '68px', align: 'right' },
]

const orgInfcColumns = [
  { key: 'infcId', label: '인터페이스ID', width: '104px' },
  { key: 'infcNm', label: '인터페이스명' },
  { key: 'httpMethNm', label: '메서드', width: '64px', align: 'center' },
  { key: 'url', label: 'URL', width: '240px' },
  { key: 'infcUseYn', label: '사용', width: '64px', align: 'center' },
]

const keyHistColumns = [
  { key: 'firsRegDtText', label: '일시', width: '140px', align: 'center' },
  { key: 'keyStatNm', label: '상태', width: '70px', align: 'center' },
  { key: 'chgCont', label: '변경내용' },
  { key: 'firsRegrId', label: '변경자', width: '90px', align: 'center' },
]

const { alert, confirm } = useDialog()
const toast = useToast()
const componentAuthStore = useComponentAuthStore()

// 조회 조건
const searchForm = ref({ keyword: '', chnlClsCd: '', useYn: 'Y' })

// 연계기관 목록
const orgRows = ref<LinkOrgRow[]>([])
const activeOrgRow = ref<LinkOrgRow | null>(null)
const isOrgLoading = ref(false)

// 기본정보 (신규면 isNewOrg, 선택한 기관이 없으면 둘 다 비어 있음)
const isNewOrg = ref(false)
const orgForm = ref<LinkOrgForm>(createEmptyOrgForm())
const orgFormSnapshot = ref('')
const hasOrg = computed(() => isNewOrg.value || !!activeOrgRow.value)
const selectedOrgId = computed(() => (isNewOrg.value ? '' : activeOrgRow.value?.lnkOrgId ?? ''))

// 클라이언트 키
const keyRows = ref<LinkKeyRow[]>([])
const isKeyLoading = ref(false)

// 허용 인터페이스
// 그리드에서 선택(체크)한 행 = 허용 인터페이스
const orgInfcRows = ref<LinkOrgInterface[]>([])
const allowedInfcRows = ref<LinkOrgInterface[]>([])
const orgInfcSnapshot = ref('')
const isOrgInfcChanged = computed(() => toAllowedSignature(allowedInfcRows.value) !== orgInfcSnapshot.value)

// 키 관리 팝업
const isKeyDtlOpen = ref(false)
const keyDtlTarget = ref<LinkKeyRow | null>(null)
const keyDtlForm = ref<LinkKeyForm>(createEmptyKeyForm())
const keyHistRows = ref<LinkKeyHistRow[]>([])
const isKeyDtlReadonly = computed(
  () => keyDtlTarget.value?.keyStatCd === KEY_STAT_REVOKED || componentAuthStore.isComponentAllowed(PAGE_ID, 'btnKeyIssu') === false,
)

// 키 발급 팝업 / 발급 결과 팝업
const isKeyIssuOpen = ref(false)
const keyIssuExpiCls = ref('365')
const isKeyIssuRsltOpen = ref(false)
const keyIssuResult = ref<LinkKeyIssueResult | null>(null)
const isSecretVisible = ref(false)

// ---------------------------------------------------------------------
// 공통 함수
// ---------------------------------------------------------------------
/**
 * formatDttm : ISO 일시를 YYYY-MM-DD HH:mm:ss 형식으로 변환
 * @param   dttm - ISO 일시 문자열
 * @return  YYYY-MM-DD HH:mm:ss 문자열 (값이 없으면 빈 문자열)
 */
function formatDttm(dttm: string | null | undefined): string {
  return dttm ? dttm.replace('T', ' ').slice(0, 19) : ''
}

/**
 * formatLimit : 호출 한도 표시 (없으면 "-")
 * @param   value - 한도 건수
 * @return  천 단위 구분 문자열
 */
function formatLimit(value: number | null | undefined): string {
  return value === null || value === undefined ? '-' : formatCount(value)
}

/**
 * keyStatBadgeClass : 키 상태 배지 CSS 클래스 반환
 * @param   row - 키 행
 * @return  배지 CSS 클래스명
 */
function keyStatBadgeClass(row: LinkKeyRow): string {
  if (row.isExpired) return 'badge-err'
  if (row.keyStatCd === '01') return 'badge-ok'
  if (row.keyStatCd === '02') return 'badge-warn'
  return 'badge-mute'
}

/**
 * toOrgRow : 연계기관 응답을 그리드 행으로 변환
 * @param   org - 연계기관
 * @return  그리드 행
 */
function toOrgRow(org: LinkOrg): LinkOrgRow {
  return {
    ...org,
    chnlClsNm: codeName(props.chnlClsOptions, org.chnlClsCd),
    useYnNm: codeName(USE_YN_OPTIONS, org.useYn),
    isPridExpired: !!org.useEndDt && org.useEndDt < todayYmd(),
  }
}

/**
 * toKeyRow : 클라이언트 키 응답을 그리드 행으로 변환
 * @param   key - 클라이언트 키
 * @return  그리드 행
 */
function toKeyRow(key: LinkKey): LinkKeyRow {
  const remainDays = key.expiDttm ? daysUntil(formatDttm(key.expiDttm)) : null
  const isExpired = key.keyStatCd === '01' && remainDays !== null && remainDays < 0
  return {
    ...key,
    keyStatNm: isExpired ? '만료' : codeName(props.keyStatOptions, key.keyStatCd),
    issuDttmText: formatDttm(key.issuDttm),
    expiDttmText: key.expiDttm ? formatDttm(key.expiDttm) : '만료 없음',
    isExpired,
    isExpiring: key.keyStatCd === '01' && remainDays !== null && remainDays >= 0 && remainDays <= 30,
  }
}

/**
 * focusField : 지정한 ID의 입력 필드에 포커스 이동
 * @param   id - 대상 엘리먼트 ID
 * @return  N/A
 */
function focusField(id: string): void {
  const el = document.getElementById(id)
  const target = (el?.matches('input, select, textarea') ? el : el?.querySelector('input, select, textarea')) as HTMLElement | null
  ;(target ?? el)?.focus()
}

/**
 * copyText : 클립보드에 복사하고 안내 토스트 표시
 * @param   text - 복사할 문자열
 * @return  N/A
 */
async function copyText(text: string): Promise<void> {
  await navigator.clipboard.writeText(text)
  toast.success(msg('cm.info.copied'))
}

/**
 * createEmptyOrgForm : 연계기관 신규 등록용 빈 기본정보 폼 생성
 * @return  기본값이 채워진 기본정보 폼
 */
function createEmptyOrgForm(): LinkOrgForm {
  return { lnkOrgId: '', lnkOrgNm: '', chnlClsCd: 'B', useStDt: todayDash(), useEndDt: '', useYn: 'N', rmk: '' }
}

/**
 * toOrgForm : 연계기관을 기본정보 폼으로 변환
 * @param   org - 연계기관
 * @return  기본정보 폼
 */
function toOrgForm(org: LinkOrg): LinkOrgForm {
  return {
    lnkOrgId: org.lnkOrgId,
    lnkOrgNm: org.lnkOrgNm,
    chnlClsCd: org.chnlClsCd,
    useStDt: ymdToDash(org.useStDt),
    useEndDt: ymdToDash(org.useEndDt),
    useYn: org.useYn,
    rmk: org.rmk ?? '',
  }
}

/**
 * createEmptyKeyForm : 빈 키 관리 폼 생성
 * @return  키 관리 폼
 */
function createEmptyKeyForm(): LinkKeyForm {
  return { keyStatCd: '01', expiDt: '', allwIpText: '', miCallLmtCnt: '', dayCallLmtCnt: '', simuRqmtLmtCnt: '', rmk: '' }
}

/**
 * toKeyForm : 클라이언트 키를 키 관리 폼으로 변환 (허용IP는 한 줄에 하나)
 * @param   key - 클라이언트 키
 * @return  키 관리 폼
 */
function toKeyForm(key: LinkKey): LinkKeyForm {
  return {
    keyStatCd: key.keyStatCd,
    expiDt: key.expiDttm ? key.expiDttm.slice(0, 10) : '',
    allwIpText: (key.allwIpAddr ?? '').split(',').filter(Boolean).join('\n'),
    miCallLmtCnt: key.miCallLmtCnt === null ? '' : String(key.miCallLmtCnt),
    dayCallLmtCnt: key.dayCallLmtCnt === null ? '' : String(key.dayCallLmtCnt),
    simuRqmtLmtCnt: key.simuRqmtLmtCnt === null ? '' : String(key.simuRqmtLmtCnt),
    rmk: key.rmk ?? '',
  }
}

/**
 * splitIpList : 허용IP 입력값을 줄바꿈·쉼표 기준으로 나눠 공백 제거
 * @param   text - 허용IP 입력값
 * @return  IP/CIDR 목록
 */
function splitIpList(text: string): string[] {
  return text.split(/[\n,]/).map((ip) => ip.trim()).filter(Boolean)
}

/**
 * isValidIpOrCidr : IPv4/IPv6 주소 또는 CIDR 형식인지 판별
 * @param   value - IP 또는 CIDR
 * @return  형식이 맞으면 true
 */
function isValidIpOrCidr(value: string): boolean {
  const [ip, prefix, ...rest] = value.split('/')
  if (rest.length > 0) return false
  const octet = '(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)'
  if (new RegExp(`^${octet}(\\.${octet}){3}$`).test(ip)) {
    return prefix === undefined || (/^\d{1,2}$/.test(prefix) && Number(prefix) <= 32)
  }
  if (/^[0-9A-Fa-f:]+$/.test(ip) && ip.includes(':') && ip.split('::').length <= 2) {
    return prefix === undefined || (/^\d{1,3}$/.test(prefix) && Number(prefix) <= 128)
  }
  return false
}

/**
 * toAllowedSignature : 허용(선택)한 인터페이스를 비교용 문자열로 변환
 * @param   rows - 허용한 인터페이스 행 목록
 * @return  인터페이스ID를 정렬해 이은 문자열
 */
function toAllowedSignature(rows: LinkOrgInterface[]): string {
  return rows.map((row) => row.infcId).sort().join(',')
}

/**
 * hasUnsavedChanges : 기본정보 또는 허용 인터페이스에 저장하지 않은 변경이 있는지 판별
 * @return  변경이 있으면 true
 */
function hasUnsavedChanges(): boolean {
  if (!hasOrg.value) return false
  return JSON.stringify(orgForm.value) !== orgFormSnapshot.value || (!isNewOrg.value && isOrgInfcChanged.value)
}

/**
 * confirmDiscard : 저장하지 않은 변경이 있으면 버릴지 확인
 * @return  계속 진행하면 true
 */
async function confirmDiscard(): Promise<boolean> {
  if (!hasUnsavedChanges()) return true
  return confirm({ message: msg('cm.confirm.discard.and.search'), icon: 'question' })
}

// ---------------------------------------------------------------------
// 조회
// ---------------------------------------------------------------------
/**
 * searchOrgList : 연계기관 목록 조회 (이전 선택 기관 유지)
 * @param   keepOrgId - 조회 후 선택할 연계기관ID (없으면 이전 선택 기관)
 * @return  N/A
 */
async function searchOrgList(keepOrgId?: string): Promise<void> {
  const prevOrgId = keepOrgId ?? activeOrgRow.value?.lnkOrgId ?? ''
  isOrgLoading.value = true
  try {
    const res = await getLinkOrgListApi({ ...searchForm.value, keyword: searchForm.value.keyword.trim() })
    orgRows.value = (res.data ?? []).map(toOrgRow)
  } finally {
    isOrgLoading.value = false
  }
  const prevRow = orgRows.value.find((row) => row.lnkOrgId === prevOrgId)
  if (prevRow) {
    await selectOrg(prevRow)
  } else {
    clearSelection()
  }
}

/**
 * clearSelection : 선택한 기관과 오른쪽 상세 영역 비우기
 * @return  N/A
 */
function clearSelection(): void {
  activeOrgRow.value = null
  isNewOrg.value = false
  orgForm.value = createEmptyOrgForm()
  orgFormSnapshot.value = ''
  keyRows.value = []
  orgInfcRows.value = []
  allowedInfcRows.value = []
  orgInfcSnapshot.value = ''
}

/**
 * selectOrg : 연계기관 선택: 기본정보·클라이언트 키·허용 인터페이스 조회
 * @param   row - 선택한 연계기관 행
 * @return  N/A
 */
async function selectOrg(row: LinkOrgRow): Promise<void> {
  activeOrgRow.value = row
  isNewOrg.value = false
  const res = await getLinkOrgApi(row.lnkOrgId)
  if (!res.success || !res.data) {
    await alert({ message: res.message ?? msg('sy.link.org.not.found', row.lnkOrgId), icon: 'warning' })
    return
  }
  orgForm.value = toOrgForm(res.data)
  orgFormSnapshot.value = JSON.stringify(orgForm.value)
  await Promise.all([searchKeyList(), reloadOrgInterfaces()])
}

/**
 * searchKeyList : 선택한 기관의 클라이언트 키 목록 조회
 * @return  N/A
 */
async function searchKeyList(): Promise<void> {
  if (!selectedOrgId.value) return
  isKeyLoading.value = true
  try {
    const res = await getLinkKeyListApi(selectedOrgId.value)
    keyRows.value = (res.data ?? []).map(toKeyRow)
  } finally {
    isKeyLoading.value = false
  }
}

/**
 * reloadOrgInterfaces : 선택한 기관의 허용 인터페이스 조회 (인터페이스 저장 후 화면에서도 호출)
 * @return  N/A
 */
async function reloadOrgInterfaces(): Promise<void> {
  if (!selectedOrgId.value) return
  const res = await getLinkOrgInterfaceListApi(selectedOrgId.value)
  orgInfcRows.value = res.data ?? []
  allowedInfcRows.value = orgInfcRows.value.filter((row) => row.allwYn === 'Y')
  orgInfcSnapshot.value = toAllowedSignature(allowedInfcRows.value)
}

// ---------------------------------------------------------------------
// 검증
// ---------------------------------------------------------------------
/**
 * validateOrgForm : 기본정보 필수값·형식 검증
 * @return  검증 통과 시 true
 */
async function validateOrgForm(): Promise<boolean> {
  const form = orgForm.value
  return validateFields([
    {
      label: '기관ID',
      value: form.lnkOrgId,
      required: true,
      custom: (value) => (/^[A-Z0-9]{2,20}$/.test(String(value).trim()) ? undefined : msg('sy.link.validate.org.id')),
      focus: () => focusField('txtDtlLnkOrgId'),
    },
    { label: '기관명', value: form.lnkOrgNm, required: true, maxBytes: 100, focus: () => focusField('txtDtlLnkOrgNm') },
    { label: '채널', value: form.chnlClsCd, required: true, focus: () => focusField('cboDtlChnlClsCd') },
    {
      label: '사용기간',
      value: form.useEndDt,
      custom: (value) => (form.useStDt && value && String(value) < form.useStDt ? msg('sy.link.validate.use.period') : undefined),
      focus: () => focusField('dtpDtlUseEndDt'),
    },
    { label: '비고', value: form.rmk, maxBytes: 200, focus: () => focusField('txtDtlOrgRmk') },
  ])
}

/**
 * checkLimit : 호출 한도 형식 검증 (1 이상 정수)
 * @param   label - 항목명
 * @return  검증 함수
 */
function checkLimit(label: string): (value: unknown) => string | undefined {
  return (value) => (/^[1-9]\d{0,8}$/.test(String(value).trim()) ? undefined : msg('sy.link.validate.limit', label))
}

/**
 * validateKeyForm : 키 관리 폼 형식 검증 (허용IP·한도·만료일)
 * @return  검증 통과 시 true
 */
async function validateKeyForm(): Promise<boolean> {
  const form = keyDtlForm.value
  const invalidIp = splitIpList(form.allwIpText).find((ip) => !isValidIpOrCidr(ip))
  return validateFields([
    {
      label: '만료일',
      value: form.expiDt,
      custom: (value) => (String(value) < todayDash() ? msg('sy.link.validate.expire.date') : undefined),
      focus: () => focusField('dtpKeyDtlExpiDt'),
    },
    {
      label: '허용IP',
      value: form.allwIpText,
      custom: () => (invalidIp ? msg('sy.link.validate.ip', invalidIp) : undefined),
      focus: () => focusField('txaKeyDtlAllwIpAddr'),
    },
    {
      label: '허용IP',
      value: splitIpList(form.allwIpText).join(','),
      maxLength: 1000,
      focus: () => focusField('txaKeyDtlAllwIpAddr'),
    },
    { label: '분당 호출 한도', value: form.miCallLmtCnt, custom: checkLimit('분당 호출 한도'), focus: () => focusField('txtKeyDtlMiCallLmtCnt') },
    { label: '일 호출 한도', value: form.dayCallLmtCnt, custom: checkLimit('일 호출 한도'), focus: () => focusField('txtKeyDtlDayCallLmtCnt') },
    { label: '동시 요청 한도', value: form.simuRqmtLmtCnt, custom: checkLimit('동시 요청 한도'), focus: () => focusField('txtKeyDtlSimuRqmtLmtCnt') },
    { label: '비고', value: form.rmk, maxBytes: 200, focus: () => focusField('txtKeyDtlRmk') },
  ])
}

// ---------------------------------------------------------------------
// 이벤트 핸들러
// ---------------------------------------------------------------------
/**
 * onBtnOrgSrchClick : 조회 버튼 클릭
 * @return  N/A
 */
async function onBtnOrgSrchClick(): Promise<void> {
  if (!(await confirmDiscard())) return
  await searchOrgList()
}

/**
 * onGrdLnkOrgListRowClick : 연계기관 행 클릭: 선택한 기관의 상세 조회
 * @param   row - 클릭한 연계기관 행
 * @return  N/A
 */
async function onGrdLnkOrgListRowClick(row: LinkOrgRow): Promise<void> {
  if (row.lnkOrgId === selectedOrgId.value) return
  if (!(await confirmDiscard())) return
  await selectOrg(row)
}

/**
 * onBtnOrgNewClick : 신규 버튼 클릭: 빈 기본정보 폼으로 전환
 * @return  N/A
 */
async function onBtnOrgNewClick(): Promise<void> {
  if (!(await confirmDiscard())) return
  clearSelection()
  isNewOrg.value = true
  orgFormSnapshot.value = JSON.stringify(orgForm.value)
  await nextTick()
  focusField('txtDtlLnkOrgId')
}

/**
 * onBtnOrgSaveClick : 기본정보 저장 버튼 클릭: 검증 후 연계기관 저장
 * @return  N/A
 */
async function onBtnOrgSaveClick(): Promise<void> {
  if (!(await validateOrgForm())) return
  const ok = await confirm({ message: msg('cm.confirm.save'), icon: 'question' })
  if (!ok) return

  const form = orgForm.value
  const res = await saveLinkOrgApi(
    {
      lnkOrgId: form.lnkOrgId.trim(),
      lnkOrgNm: form.lnkOrgNm.trim(),
      chnlClsCd: form.chnlClsCd,
      useStDt: dashToYmd(form.useStDt) || null,
      useEndDt: dashToYmd(form.useEndDt) || null,
      useYn: form.useYn,
      rmk: form.rmk.trim() || null,
    },
    isNewOrg.value,
  )
  if (!res.success) {
    await alert({ message: res.message ?? msg('cm.error.save.failed'), icon: 'error' })
    if (res.code === 'sy.link.org.duplicate') focusField('txtDtlLnkOrgId')
    return
  }
  await alert({ message: msg('cm.info.saved'), icon: 'information' })
  const savedOrgId = form.lnkOrgId.trim()
  isNewOrg.value = false
  orgFormSnapshot.value = JSON.stringify(orgForm.value)
  await searchOrgList(savedOrgId)
  emit('saved')
}

/**
 * onBtnOrgCallViewClick : 호출이력 보기 버튼 클릭: 호출이력 탭으로 이동해 선택 기관으로 조회
 * @return  N/A
 */
function onBtnOrgCallViewClick(): void {
  if (selectedOrgId.value) emit('call-view', selectedOrgId.value)
}

/**
 * onChkDtlOrgUseYnChange : 기본정보 사용여부 체크박스 변경
 * @param   event - 체크박스 변경 이벤트
 * @return  N/A
 */
function onChkDtlOrgUseYnChange(event: Event): void {
  orgForm.value.useYn = (event.target as HTMLInputElement).checked ? 'Y' : 'N'
}

/**
 * onBtnKeyIssuClick : 키발급 버튼 클릭: 키 발급 팝업 열기
 * @return  N/A
 */
function onBtnKeyIssuClick(): void {
  keyIssuExpiCls.value = '365'
  isKeyIssuOpen.value = true
}

/**
 * onBtnKeyIssuExecClick : 키 발급 팝업의 발급 버튼 클릭: 클라이언트 키 발급 후 결과 팝업 표시
 * @return  N/A
 */
async function onBtnKeyIssuExecClick(): Promise<void> {
  const ok = await confirm({ message: msg('sy.link.confirm.key.issue', orgForm.value.lnkOrgNm), icon: 'question' })
  if (!ok) return

  const res = await saveLinkKeyApi(selectedOrgId.value, Number(keyIssuExpiCls.value))
  if (!res.success || !res.data) {
    await alert({ message: res.message ?? msg('cm.error.save.failed'), icon: 'error' })
    return
  }
  isKeyIssuOpen.value = false
  keyIssuResult.value = res.data
  isSecretVisible.value = false
  isKeyIssuRsltOpen.value = true
}

/**
 * onBtnKeyIssuRsltKeyIdCopyClick : 발급 결과 client_id 복사 버튼 클릭
 * @return  N/A
 */
async function onBtnKeyIssuRsltKeyIdCopyClick(): Promise<void> {
  if (keyIssuResult.value) await copyText(keyIssuResult.value.keyId)
}

/**
 * onBtnKeyIssuRsltSecretCopyClick : 발급 결과 client_secret 복사 버튼 클릭
 * @return  N/A
 */
async function onBtnKeyIssuRsltSecretCopyClick(): Promise<void> {
  if (keyIssuResult.value) await copyText(keyIssuResult.value.clientSecret)
}

/**
 * onBtnKeyIssuRsltOkClick : 발급 결과 팝업 확인(또는 닫기): 보관 확인 후 비밀키를 지우고 키 목록 재조회
 * @return  N/A
 */
async function onBtnKeyIssuRsltOkClick(): Promise<void> {
  const ok = await confirm({ message: msg('sy.link.confirm.key.secret.copied'), icon: 'warning' })
  if (!ok) return
  isKeyIssuRsltOpen.value = false
  keyIssuResult.value = null
  isSecretVisible.value = false
  await searchKeyList()
  await searchOrgList(selectedOrgId.value)
}

/**
 * onMdlKeyIssuRsltUpdate : 발급 결과 팝업의 닫기(X) 버튼: 바로 닫지 않고 확인 버튼과 같이 처리
 * @param   isOpen - 팝업 열림 여부
 * @return  N/A
 */
async function onMdlKeyIssuRsltUpdate(isOpen: boolean): Promise<void> {
  if (!isOpen) await onBtnKeyIssuRsltOkClick()
}

/**
 * onGrdKeyListRowClick : 키 행 클릭: 키 관리 팝업 열기 (키 이력 포함)
 * @param   row - 클릭한 키 행
 * @return  N/A
 */
async function onGrdKeyListRowClick(row: LinkKeyRow): Promise<void> {
  keyDtlTarget.value = row
  keyDtlForm.value = toKeyForm(row)
  const res = await getLinkKeyHistListApi(row.lnkOrgId, row.keyId)
  keyHistRows.value = (res.data ?? []).map((hist) => ({
    ...hist,
    keyStatNm: codeName(props.keyStatOptions, hist.keyStatCd),
    firsRegDtText: formatDttm(hist.firsRegDt),
  }))
  isKeyDtlOpen.value = true
}

/**
 * onGrdKeyListKeyIdCopyClick : 키 그리드의 Client ID 복사 버튼 클릭
 * @param   row - 대상 키 행
 * @return  N/A
 */
async function onGrdKeyListKeyIdCopyClick(row: LinkKeyRow): Promise<void> {
  await copyText(row.keyId)
}

/**
 * onBtnKeyDtlKeyIdCopyClick : 키 관리 팝업의 Client ID 복사 버튼 클릭
 * @return  N/A
 */
async function onBtnKeyDtlKeyIdCopyClick(): Promise<void> {
  if (keyDtlTarget.value) await copyText(keyDtlTarget.value.keyId)
}

/**
 * onBtnKeyDtlSaveClick : 키 관리 팝업의 저장 버튼 클릭: 검증 후 키 수정 (폐기는 한 번 더 확인)
 * @return  N/A
 */
async function onBtnKeyDtlSaveClick(): Promise<void> {
  const target = keyDtlTarget.value
  if (!target || !(await validateKeyForm())) return
  const form = keyDtlForm.value
  const isRevoke = form.keyStatCd === KEY_STAT_REVOKED
  const ok = await confirm({
    message: msg(isRevoke ? 'sy.link.confirm.key.revoke' : 'cm.confirm.save'),
    icon: isRevoke ? 'warning' : 'question',
  })
  if (!ok) return

  const toLimit = (value: string): number | null => (value.trim() ? Number(value.trim()) : null)
  const allwIpAddr = splitIpList(form.allwIpText).join(',')
  const res = await saveLinkKeyDtlApi(target.lnkOrgId, target.keyId, {
    keyStatCd: form.keyStatCd,
    expiDt: dashToYmd(form.expiDt) || null,
    allwIpAddr: allwIpAddr || null,
    miCallLmtCnt: toLimit(form.miCallLmtCnt),
    dayCallLmtCnt: toLimit(form.dayCallLmtCnt),
    simuRqmtLmtCnt: toLimit(form.simuRqmtLmtCnt),
    rmk: form.rmk.trim() || null,
  })
  if (!res.success) {
    await alert({ message: res.message ?? msg('cm.error.save.failed'), icon: 'error' })
    return
  }
  await alert({ message: msg('cm.info.saved'), icon: 'information' })
  isKeyDtlOpen.value = false
  await searchOrgList(selectedOrgId.value)
}

/**
 * onBtnOrgInfcSaveClick : 허용 인터페이스 저장 버튼 클릭: 체크한 인터페이스로 전체 교체
 * @return  N/A
 */
async function onBtnOrgInfcSaveClick(): Promise<void> {
  if (!isOrgInfcChanged.value) {
    await alert({ message: msg('cm.info.no.changes'), icon: 'information' })
    return
  }
  const ok = await confirm({ message: msg('cm.confirm.save'), icon: 'question' })
  if (!ok) return

  const infcIdList = allowedInfcRows.value.map((row) => row.infcId)
  const res = await saveLinkOrgInterfaceListApi(selectedOrgId.value, infcIdList)
  if (!res.success) {
    await alert({ message: res.message ?? msg('cm.error.save.failed'), icon: 'error' })
    return
  }
  orgInfcSnapshot.value = toAllowedSignature(allowedInfcRows.value)
  await alert({ message: msg('sy.link.info.org.infc.saved'), icon: 'information' })
  emit('infc-saved')
}

// ---------------------------------------------------------------------
// 탭 초기화 (화면에서 처음 활성화될 때 호출)
// ---------------------------------------------------------------------
/**
 * initTab : 조회 조건을 기본값으로 초기화하고 연계기관 목록 최초 조회
 * @return  N/A
 */
async function initTab(): Promise<void> {
  searchForm.value = { keyword: '', chnlClsCd: '', useYn: 'Y' }
  clearSelection()
  await searchOrgList()
}

defineExpose({ initTab, reloadOrgInterfaces })
</script>

<template>
  <div class="tab-root">
    <!-- 조회 조건 영역 -->
    <form id="frmOrgSrch" class="search-area" @submit.prevent="onBtnOrgSrchClick">
      <div class="search-fields">
        <label class="search-field">
          <span class="field-label">기관ID/명</span>
          <Input id="txtOrgKeyword" v-model="searchForm.keyword" @enter="onBtnOrgSrchClick" />
        </label>
        <label class="search-field">
          <span class="field-label">채널</span>
          <Select id="cboOrgChnlClsCd" v-model="searchForm.chnlClsCd" :options="withAll(chnlClsOptions)" />
        </label>
        <label class="search-field">
          <span class="field-label">사용여부</span>
          <Select id="cboOrgUseYn" v-model="searchForm.useYn" :options="withAll(USE_YN_OPTIONS)" />
        </label>
      </div>
      <div class="search-actions">
        <Button id="btnOrgSrch" v-permission @click="onBtnOrgSrchClick">조회</Button>
      </div>
    </form>

    <div class="org-split">
      <!-- 연계기관 목록 (TB_LNK_ORG_M) -->
      <div class="grid-area">
        <div class="grid-header">
          <span class="grid-title">연계기관 - <strong id="lblOrgTotCnt" class="total-count">{{ orgRows.length }}건</strong></span>
          <div class="grid-actions">
            <Button id="btnOrgNew" v-permission variant="outline" size="small" @click="onBtnOrgNewClick">신규</Button>
          </div>
        </div>
        <DataTable
          id="grdLnkOrgList"
          :columns="orgColumns"
          :data="orgRows"
          :loading="isOrgLoading"
          :active-row="activeOrgRow"
          :pagination="false"
          class="nowrap-grid"
          dense
          @row-click="onGrdLnkOrgListRowClick"
        >
          <template #cell-lnkOrgNm="{ row }">
            <span class="cell-ellipsis" :title="row.lnkOrgNm">{{ row.lnkOrgNm }}</span>
          </template>
          <template #cell-useYnNm="{ row }">
            <span v-if="row.useYn === 'Y' && row.isPridExpired" class="badge badge-mute">기간만료</span>
            <template v-else>{{ row.useYnNm }}</template>
          </template>
          <template #cell-useKeyCnt="{ row }">{{ formatCount(row.useKeyCnt) }}</template>
        </DataTable>
      </div>

      <!-- 선택 기관 상세 -->
      <div class="detail-area">
        <p v-if="!hasOrg" class="empty-panel">{{ msg('sy.link.org.select.guide') }}</p>

        <template v-else>
          <!-- 기본정보 (TB_LNK_ORG_M) -->
          <section class="panel">
            <div class="grid-header">
              <span class="grid-title">
                기본정보
                <span v-if="isNewOrg" class="sub-title">신규 등록</span>
              </span>
              <div class="grid-actions">
                <Button v-if="!isNewOrg" id="btnOrgCallView" variant="outline" size="small" @click="onBtnOrgCallViewClick">
                  호출이력 보기
                </Button>
                <Button id="btnOrgSave" v-permission variant="success" size="small" @click="onBtnOrgSaveClick">저장</Button>
              </div>
            </div>
            <div id="frmOrgDtl" class="dtl-form">
              <div class="dtl-row">
                <span class="dtl-label">기관ID</span>
                <div class="dtl-value">
                  <Input
                    id="txtDtlLnkOrgId"
                    v-model="orgForm.lnkOrgId"
                    required
                    :readonly="!isNewOrg"
                    :disabled="!isNewOrg"
                    maxlength="20"
                    placeholder="PARTNER01"
                    size="small"
                  />
                  <p v-if="isNewOrg" class="field-hint">영문 대문자·숫자 2~20자리</p>
                </div>
                <span class="dtl-label">채널</span>
                <div class="dtl-value">
                  <Select id="cboDtlChnlClsCd" v-model="orgForm.chnlClsCd" required :options="chnlClsOptions" size="small" />
                </div>
              </div>
              <div class="dtl-row">
                <span class="dtl-label">기관명</span>
                <div class="dtl-value is-wide">
                  <Input id="txtDtlLnkOrgNm" v-model="orgForm.lnkOrgNm" required maxlength="100" size="small" />
                </div>
              </div>
              <div class="dtl-row">
                <span class="dtl-label">사용시작일</span>
                <div class="dtl-value">
                  <DatePicker id="dtpDtlUseStDt" v-model="orgForm.useStDt" :presets="false" size="small" />
                </div>
                <span class="dtl-label">사용종료일</span>
                <div class="dtl-value">
                  <DatePicker id="dtpDtlUseEndDt" v-model="orgForm.useEndDt" :presets="false" size="small" />
                  <p class="field-hint">비우면 기간 제한 없음</p>
                </div>
              </div>
              <div class="dtl-row">
                <span class="dtl-label">사용여부</span>
                <label class="dtl-value check-item">
                  <input id="chkDtlOrgUseYn" type="checkbox" :checked="orgForm.useYn === 'Y'" @change="onChkDtlOrgUseYnChange" />
                  사용
                </label>
                <span class="dtl-label">비고</span>
                <div class="dtl-value">
                  <Input id="txtDtlOrgRmk" v-model="orgForm.rmk" maxlength="200" size="small" />
                </div>
              </div>
            </div>
          </section>

          <p v-if="isNewOrg" class="empty-panel">{{ msg('sy.link.org.save.first') }}</p>

          <template v-else>
            <!-- 클라이언트 키 (TB_LNK_KEY_M) -->
            <section class="grid-area">
              <div class="grid-header">
                <span class="grid-title">
                  클라이언트 키 - <strong id="lblKeyTotCnt" class="total-count">{{ keyRows.length }}건</strong>
                  <span class="sub-title">행을 클릭하면 허용IP·호출 한도·상태를 관리합니다</span>
                </span>
                <div class="grid-actions">
                  <Button id="btnKeyIssu" v-permission variant="outline" size="small" @click="onBtnKeyIssuClick">키발급</Button>
                </div>
              </div>
              <DataTable
                id="grdKeyList"
                :columns="keyColumns"
                :data="keyRows"
                :loading="isKeyLoading"
                :pagination="false"
                :empty-text="msg('sy.link.key.empty')"
                class="nowrap-grid"
                dense
                @row-click="onGrdKeyListRowClick"
              >
                <template #cell-keyId="{ row }">
                  <span class="key-id mono">{{ row.keyId }}</span>
                  <button type="button" class="icon-button" title="Client ID 복사" @click.stop="onGrdKeyListKeyIdCopyClick(row)">복사</button>
                </template>
                <template #cell-keyStatNm="{ row }">
                  <span class="badge" :class="keyStatBadgeClass(row)">{{ row.keyStatNm }}</span>
                </template>
                <template #cell-expiDttmText="{ row }">
                  <span :class="{ 'text-warn': row.isExpiring }">{{ row.expiDttmText }}</span>
                </template>
                <template #cell-allwIpAddr="{ row }">
                  <span v-if="row.allwIpAddr" class="cell-ellipsis" :title="row.allwIpAddr">{{ row.allwIpAddr }}</span>
                  <span v-else class="sub-title">제한 없음</span>
                </template>
                <template #cell-miCallLmtCnt="{ row }">{{ formatLimit(row.miCallLmtCnt) }}</template>
                <template #cell-dayCallLmtCnt="{ row }">{{ formatLimit(row.dayCallLmtCnt) }}</template>
                <template #cell-simuRqmtLmtCnt="{ row }">{{ formatLimit(row.simuRqmtLmtCnt) }}</template>
              </DataTable>
            </section>

            <!-- 허용 인터페이스 (TB_LNK_ORG_INFC_R) -->
            <section class="grid-area">
              <div class="grid-header">
                <span class="grid-title">
                  허용 인터페이스 -
                  <strong id="lblOrgInfcCnt" class="total-count">허용 {{ allowedInfcRows.length }} / 전체 {{ orgInfcRows.length }}건</strong>
                  <span v-if="isOrgInfcChanged" class="sub-title text-warn">변경됨</span>
                </span>
                <div class="grid-actions">
                  <Button id="btnOrgInfcSave" v-permission variant="success" size="small" @click="onBtnOrgInfcSaveClick">저장</Button>
                </div>
              </div>
              <DataTable
                id="grdOrgInfcList"
                v-model:selected-rows="allowedInfcRows"
                :columns="orgInfcColumns"
                :data="orgInfcRows"
                :pagination="false"
                :empty-text="msg('cm.info.no.items')"
                class="nowrap-grid"
                selectable
                dense
              >
                <template #cell-infcId="{ row }"><span class="mono">{{ row.infcId }}</span></template>
                <template #cell-url="{ row }">
                  <span class="cell-ellipsis mono" :title="row.url">{{ row.url }}</span>
                </template>
                <template #cell-infcUseYn="{ row }">
                  <span v-if="row.infcUseYn === 'Y'">사용</span>
                  <span v-else class="badge badge-mute" title="인터페이스가 미사용이라 허용해도 호출되지 않습니다">미사용</span>
                </template>
              </DataTable>
              <p class="field-hint org-infc-hint">{{ msg('sy.link.org.infc.hint') }}</p>
            </section>
          </template>
        </template>
      </div>
    </div>

    <!-- 키 관리 팝업 -->
    <PopupWindow id="mdlKeyDtl" v-model="isKeyDtlOpen" :title="`클라이언트 키 관리 - ${keyDtlTarget?.keyId ?? ''}`" :width="820">
      <div class="dtl-toolbar">
        <span class="grid-title">
          키 정보
          <span v-if="keyDtlTarget" class="badge" :class="keyStatBadgeClass(keyDtlTarget)">{{ keyDtlTarget.keyStatNm }}</span>
        </span>
        <div class="page-actions">
          <Button v-if="!isKeyDtlReadonly" id="btnKeyDtlSave" variant="success" size="small" @click="onBtnKeyDtlSaveClick">저장</Button>
        </div>
      </div>
      <div id="frmKeyDtl" class="dtl-form">
        <div class="dtl-row">
          <span class="dtl-label">Client ID</span>
          <div class="dtl-value is-wide dtl-inline">
            <span id="lblKeyDtlKeyId" class="mono">{{ keyDtlTarget?.keyId }}</span>
            <button type="button" class="icon-button" title="Client ID 복사" @click="onBtnKeyDtlKeyIdCopyClick">복사</button>
          </div>
        </div>
        <div class="dtl-row">
          <span class="dtl-label">발급일시</span>
          <div id="lblKeyDtlIssuDttm" class="dtl-value is-text">{{ keyDtlTarget?.issuDttmText }}</div>
          <span class="dtl-label">만료일</span>
          <div class="dtl-value">
            <DatePicker id="dtpKeyDtlExpiDt" v-model="keyDtlForm.expiDt" :presets="false" size="small" :disabled="isKeyDtlReadonly" />
            <p class="field-hint">비우면 만료 없음 (만료일 23:59:59까지 사용)</p>
          </div>
        </div>
        <div class="dtl-row">
          <span class="dtl-label">상태</span>
          <div id="rdoKeyDtlKeyStatCd" class="dtl-value is-wide radio-group">
            <label v-for="option in keyStatOptions" :key="option.value" class="radio-item">
              <input v-model="keyDtlForm.keyStatCd" type="radio" name="keyStatCd" :value="option.value" :disabled="isKeyDtlReadonly" />
              {{ option.label }}
            </label>
            <span v-if="keyDtlTarget?.keyStatCd === '03'" class="field-hint">폐기된 키는 수정할 수 없습니다</span>
          </div>
        </div>
        <div class="dtl-row">
          <span class="dtl-label">허용IP</span>
          <div class="dtl-value is-wide">
            <textarea
              id="txaKeyDtlAllwIpAddr"
              v-model="keyDtlForm.allwIpText"
              class="dtl-textarea mono"
              rows="3"
              placeholder="203.0.113.10&#10;203.0.113.0/28"
              :readonly="isKeyDtlReadonly"
            ></textarea>
            <p class="field-hint">한 줄에 하나 (IPv4/IPv6 주소 또는 CIDR). 비우면 제한 없음</p>
          </div>
        </div>
        <div class="dtl-row limit-row">
          <span class="dtl-label">호출 한도</span>
          <div class="dtl-value is-wide limit-group">
            <label class="limit-item">
              <span>분당</span>
              <Input id="txtKeyDtlMiCallLmtCnt" v-model="keyDtlForm.miCallLmtCnt" inputmode="numeric" maxlength="9" :readonly="isKeyDtlReadonly" size="small" />
            </label>
            <label class="limit-item">
              <span>일</span>
              <Input id="txtKeyDtlDayCallLmtCnt" v-model="keyDtlForm.dayCallLmtCnt" inputmode="numeric" maxlength="9" :readonly="isKeyDtlReadonly" size="small" />
            </label>
            <label class="limit-item">
              <span>동시</span>
              <Input id="txtKeyDtlSimuRqmtLmtCnt" v-model="keyDtlForm.simuRqmtLmtCnt" inputmode="numeric" maxlength="9" :readonly="isKeyDtlReadonly" size="small" />
            </label>
            <p class="field-hint">비우면 제한 없음</p>
          </div>
        </div>
        <div class="dtl-row">
          <span class="dtl-label">비고</span>
          <div class="dtl-value is-wide">
            <Input id="txtKeyDtlRmk" v-model="keyDtlForm.rmk" maxlength="200" :readonly="isKeyDtlReadonly" size="small" />
          </div>
        </div>
      </div>

      <div class="dtl-toolbar dtl-section">
        <span class="grid-title">키 이력 <span class="sub-title">총 {{ keyHistRows.length }}건</span></span>
      </div>
      <DataTable id="grdKeyHistList" :columns="keyHistColumns" :data="keyHistRows" :page-size="5" class="nowrap-grid" dense>
        <template #cell-chgCont="{ row }">
          <span class="cell-ellipsis hist-cont" :title="row.chgCont ?? ''">{{ row.chgCont }}</span>
        </template>
      </DataTable>
    </PopupWindow>

    <!-- 키 발급 팝업 -->
    <PopupWindow id="mdlKeyIssu" v-model="isKeyIssuOpen" :title="`클라이언트 키 발급 - ${orgForm.lnkOrgNm}`" :width="520">
      <div class="dtl-toolbar">
        <span class="grid-title">발급 정보 <span class="sub-title">[{{ selectedOrgId }}]</span></span>
        <div class="page-actions">
          <Button id="btnKeyIssuExec" variant="success" size="small" @click="onBtnKeyIssuExecClick">발급</Button>
        </div>
      </div>
      <div class="dtl-form">
        <div class="dtl-row">
          <span class="dtl-label">유효기간</span>
          <div id="rdoKeyIssuExpiCls" class="dtl-value is-wide radio-group">
            <label v-for="option in KEY_EXPI_OPTIONS" :key="option.value" class="radio-item">
              <input v-model="keyIssuExpiCls" type="radio" name="keyIssuExpiCls" :value="option.value" />
              {{ option.label }}
            </label>
          </div>
        </div>
      </div>
      <p id="lblKeyIssuGuide" class="field-hint issue-guide">{{ msg('sy.link.key.issue.guide') }}</p>
    </PopupWindow>

    <!-- 발급 결과 팝업: 확인·닫기 모두 보관 확인 후 닫는다 (ESC 로 닫히지 않음) -->
    <PopupWindow
      id="mdlKeyIssuRslt"
      :model-value="isKeyIssuRsltOpen"
      title="클라이언트 키 발급 완료"
      :width="600"
      :close-on-esc="false"
      @update:model-value="onMdlKeyIssuRsltUpdate"
    >
      <div v-if="keyIssuResult" class="issue-result">
        <div class="issue-row">
          <span class="issue-label">client_id</span>
          <code id="lblKeyIssuRsltKeyId" class="issue-value">{{ keyIssuResult.keyId }}</code>
          <button id="btnKeyIssuRsltKeyIdCopy" type="button" class="icon-button" @click="onBtnKeyIssuRsltKeyIdCopyClick">복사</button>
        </div>
        <div class="issue-row">
          <span class="issue-label">client_secret</span>
          <code id="lblKeyIssuRsltSecret" class="issue-value">
            {{ isSecretVisible ? keyIssuResult.clientSecret : '•'.repeat(43) }}
          </code>
          <button type="button" class="icon-button" @click="isSecretVisible = !isSecretVisible">{{ isSecretVisible ? '숨기기' : '보기' }}</button>
          <button id="btnKeyIssuRsltSecretCopy" type="button" class="icon-button" @click="onBtnKeyIssuRsltSecretCopyClick">복사</button>
        </div>
        <div class="issue-row">
          <span class="issue-label">만료일시</span>
          <span id="lblKeyIssuRsltExpiDttm" class="issue-value">{{ keyIssuResult.expiDttm ? formatDttm(keyIssuResult.expiDttm) : '만료 없음' }}</span>
        </div>
        <p id="lblKeyIssuRsltWarn" class="issue-warn">{{ msg('sy.link.key.secret.once') }}</p>
      </div>
      <template #footer>
        <Button id="btnKeyIssuRsltOk" variant="success" size="small" @click="onBtnKeyIssuRsltOkClick">확인</Button>
      </template>
    </PopupWindow>
  </div>
</template>

<style scoped src="@/assets/page.css"></style>

<style scoped>
.tab-root {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 왼쪽 목록 고정폭, 오른쪽 상세 (좁은 화면은 위아래) */
.org-split {
  display: grid;
  grid-template-columns: 440px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

@media (max-width: 1280px) {
  .org-split {
    grid-template-columns: 1fr;
  }
}

.detail-area {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.dtl-section {
  margin-top: 18px;
}

.dtl-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}


.dtl-inline, .radio-group, .check-item, .limit-group {
  flex-direction: row;
  align-items: center;
  justify-content: flex-start;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
}

.radio-item, .limit-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.limit-item > span {
  white-space: nowrap;
}

.limit-item :deep(input) {
  width: 110px;
}

.sub-title {
  color: var(--ds-ink-2);
}

.key-id {
  margin-right: 6px;
}

.hist-cont {
  max-width: 360px;
}

.icon-button {
  padding: 1px 8px;
  font: inherit;
  font-size: 12px;
  color: var(--ds-ink-2);
  background: var(--ds-surface);
  border: 1px solid var(--ds-rule-strong);
  border-radius: var(--ds-radius-control);
  cursor: pointer;
}

.icon-button:hover {
  color: var(--ds-slate-900);
  border-color: var(--ds-slate-700);
}

.icon-button:focus-visible {
  outline: none;
  box-shadow: var(--ds-focus);
}

.issue-guide {
  margin-top: 8px;
}

/* 발급 결과 */
.issue-result {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.issue-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.issue-label {
  flex: none;
  width: 100px;
  font-size: 13px;
  font-weight: 600;
  color: var(--ds-ink-2);
}

.issue-value {
  flex: 1;
  min-width: 0;
  padding: 6px 8px;
  font-family: var(--ds-font-code);
  font-size: 12.5px;
  word-break: break-all;
  background: var(--ds-surface-muted);
  border: 1px solid var(--ds-rule);
  border-radius: var(--ds-radius-control);
}

.issue-warn {
  margin: 4px 0 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--ds-stamp);
}
</style>
