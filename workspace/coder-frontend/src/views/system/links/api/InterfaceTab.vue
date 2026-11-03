<script setup lang="ts">
/*****************************************************************************************
	Program ID   : InterfaceTab
	Program Type : vue
	Program Name : API관리 - 인터페이스 탭
	Description  : SYLK010 ② 인터페이스 탭. 인터페이스(TB_INFC_M) 목록 조회와 등록/수정 (본문 로그 여부, 마스킹 필드)
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-11-03  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { ref, computed, nextTick } from 'vue'
import { Button, Input, Select, DataTable, useDialog, useToast } from '@/components/common'
import { msg } from '@/utils/message'
import { HTTP_METH_OPTIONS, getInterfaceListApi, saveInterfaceApi, type LinkInterface } from '@/api/sy/link'
import { USE_YN_OPTIONS, codeName, withAll, type CodeOption } from '@/utils/code'
import { formatCount } from '@/utils/format'
import { validateFields } from '@/utils/validate'

const props = defineProps<{
  /** 업무모듈 공통코드 (SY/WRK_MDUL_CD, 인터페이스ID 업무약어 검증) */
  wrkMdulOptions: CodeOption[]
}>()

const emit = defineEmits<{
  /** 인터페이스 저장 */
  (e: 'saved'): void
}>()

/** 인터페이스 그리드 행 (표시용 값 포함) */
interface LinkInterfaceRow extends LinkInterface {
  bodyLogYnNm: string
  useYnNm: string
}

/** 인터페이스 정보 폼 (빈 값은 빈 문자열, 마스킹 필드는 줄바꿈 구분) */
interface LinkInterfaceForm {
  infcId: string
  infcNm: string
  httpMethNm: string
  url: string
  bodyLogYn: string
  maskFldText: string
  useYn: string
  rmk: string
}

const infcColumns = [
  { key: 'infcId', label: '인터페이스ID', width: '104px', sortable: true },
  { key: 'infcNm', label: '인터페이스명', sortable: true },
  { key: 'httpMethNm', label: '메서드', width: '64px', align: 'center' },
  { key: 'bodyLogYnNm', label: '본문로그', width: '68px', align: 'center' },
  { key: 'useYnNm', label: '사용', width: '60px', align: 'center' },
  { key: 'allwOrgCnt', label: '허용기관', width: '68px', align: 'right' },
]

const { alert, confirm } = useDialog()
const toast = useToast()

// 조회 조건
const searchForm = ref({ keyword: '', useYn: 'Y' })

// 인터페이스 목록
const infcRows = ref<LinkInterfaceRow[]>([])
const activeInfcRow = ref<LinkInterfaceRow | null>(null)
const isInfcLoading = ref(false)

// 인터페이스 정보 폼
const isNewInfc = ref(false)
const infcForm = ref<LinkInterfaceForm>(createEmptyForm())
const infcFormSnapshot = ref('')
const hasInfc = computed(() => isNewInfc.value || !!activeInfcRow.value)

// ---------------------------------------------------------------------
// 공통 함수
// ---------------------------------------------------------------------
/**
 * toInfcRow : 인터페이스 응답을 그리드 행으로 변환
 * @param   infc - 인터페이스
 * @return  그리드 행
 */
function toInfcRow(infc: LinkInterface): LinkInterfaceRow {
  return { ...infc, bodyLogYnNm: infc.bodyLogYn === 'Y' ? '기록' : '-', useYnNm: codeName(USE_YN_OPTIONS, infc.useYn) }
}

/**
 * createEmptyForm : 인터페이스 신규 등록용 빈 폼 생성
 * @return  기본값이 채워진 인터페이스 폼
 */
function createEmptyForm(): LinkInterfaceForm {
  return { infcId: '', infcNm: '', httpMethNm: 'GET', url: '', bodyLogYn: 'N', maskFldText: '', useYn: 'N', rmk: '' }
}

/**
 * toForm : 인터페이스를 폼으로 변환 (마스킹 필드는 한 줄에 하나)
 * @param   infc - 인터페이스
 * @return  인터페이스 폼
 */
function toForm(infc: LinkInterface): LinkInterfaceForm {
  return {
    infcId: infc.infcId,
    infcNm: infc.infcNm,
    httpMethNm: infc.httpMethNm,
    url: infc.url,
    bodyLogYn: infc.bodyLogYn,
    maskFldText: (infc.maskFldCont ?? '').split(',').filter(Boolean).join('\n'),
    useYn: infc.useYn,
    rmk: infc.rmk ?? '',
  }
}

/**
 * toMaskFldCont : 마스킹 필드 입력값을 쉼표 구분 문자열로 변환
 * @param   text - 마스킹 필드 입력값 (줄바꿈·쉼표 구분)
 * @return  쉼표 구분 문자열 (없으면 빈 문자열)
 */
function toMaskFldCont(text: string): string {
  return text.split(/[\n,]/).map((field) => field.trim()).filter(Boolean).join(',')
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
 * confirmDiscard : 저장하지 않은 변경이 있으면 버릴지 확인
 * @return  계속 진행하면 true
 */
async function confirmDiscard(): Promise<boolean> {
  if (!hasInfc.value || JSON.stringify(infcForm.value) === infcFormSnapshot.value) return true
  return confirm({ message: msg('cm.confirm.discard.and.search'), icon: 'question' })
}

/**
 * checkInfcId : 인터페이스ID 형식 검증 (IF + 업무약어 2자리 + 숫자 4자리, 업무약어는 공통코드에 있어야 함)
 * @param   value - 인터페이스ID 입력값
 * @return  오류 메시지 (올바르면 undefined)
 */
function checkInfcId(value: unknown): string | undefined {
  const infcId = String(value).trim()
  if (!/^IF[A-Z]{2}\d{4}$/.test(infcId)) return msg('sy.link.validate.infc.id')
  const wrkMdulCd = infcId.slice(2, 4)
  if (props.wrkMdulOptions.length > 0 && !props.wrkMdulOptions.some((option) => option.value === wrkMdulCd)) {
    return msg('sy.link.validate.infc.wrk.mdul', wrkMdulCd)
  }
  return undefined
}

/**
 * validateInfcForm : 인터페이스 폼 필수값·형식 검증
 * @return  검증 통과 시 true
 */
async function validateInfcForm(): Promise<boolean> {
  const form = infcForm.value
  return validateFields([
    { label: '인터페이스ID', value: form.infcId, required: true, custom: checkInfcId, focus: () => focusField('txtDtlInfcId') },
    { label: '인터페이스명', value: form.infcNm, required: true, maxBytes: 100, focus: () => focusField('txtDtlInfcNm') },
    { label: '메서드', value: form.httpMethNm, required: true, focus: () => focusField('cboDtlHttpMethNm') },
    {
      label: 'URL',
      value: form.url,
      required: true,
      maxLength: 200,
      custom: (value) => (/^\/api\/v\d+\/\S*$/.test(String(value).trim()) ? undefined : msg('sy.link.validate.url')),
      focus: () => focusField('txtDtlUrl'),
    },
    { label: '마스킹 필드', value: toMaskFldCont(form.maskFldText), maxLength: 1000, focus: () => focusField('txaDtlMaskFldCont') },
    { label: '비고', value: form.rmk, maxBytes: 200, focus: () => focusField('txtDtlInfcRmk') },
  ])
}

// ---------------------------------------------------------------------
// 조회
// ---------------------------------------------------------------------
/**
 * searchInterfaceList : 인터페이스 목록 조회 (이전 선택 행 유지, 다른 탭 저장 후에도 호출)
 * @param   alertEmpty - 결과가 없을 때 안내할지 여부
 * @param   keepInfcId - 조회 후 선택할 인터페이스ID (없으면 이전 선택)
 * @return  N/A
 */
async function searchInterfaceList(alertEmpty = true, keepInfcId?: string): Promise<void> {
  const prevInfcId = keepInfcId ?? activeInfcRow.value?.infcId ?? ''
  isInfcLoading.value = true
  try {
    const res = await getInterfaceListApi({ ...searchForm.value, keyword: searchForm.value.keyword.trim() })
    infcRows.value = (res.data ?? []).map(toInfcRow)
  } finally {
    isInfcLoading.value = false
  }
  const prevRow = infcRows.value.find((row) => row.infcId === prevInfcId)
  if (prevRow) {
    selectInterface(prevRow)
  } else if (!isNewInfc.value) {
    activeInfcRow.value = null
  }
  if (alertEmpty && infcRows.value.length === 0) {
    await alert({ message: msg('cm.info.no.data'), icon: 'information' })
  }
}

/**
 * selectInterface : 인터페이스 선택: 폼에 상세 표시
 * @param   row - 선택한 인터페이스 행
 * @return  N/A
 */
function selectInterface(row: LinkInterfaceRow): void {
  activeInfcRow.value = row
  isNewInfc.value = false
  infcForm.value = toForm(row)
  infcFormSnapshot.value = JSON.stringify(infcForm.value)
}

// ---------------------------------------------------------------------
// 이벤트 핸들러
// ---------------------------------------------------------------------
/**
 * onBtnInfcSrchClick : 조회 버튼 클릭
 * @return  N/A
 */
async function onBtnInfcSrchClick(): Promise<void> {
  if (!(await confirmDiscard())) return
  await searchInterfaceList()
}

/**
 * onGrdInfcListRowClick : 인터페이스 행 클릭: 폼에 상세 표시
 * @param   row - 클릭한 인터페이스 행
 * @return  N/A
 */
async function onGrdInfcListRowClick(row: LinkInterfaceRow): Promise<void> {
  if (!isNewInfc.value && row.infcId === activeInfcRow.value?.infcId) return
  if (!(await confirmDiscard())) return
  selectInterface(row)
}

/**
 * onBtnInfcNewClick : 신규 버튼 클릭: 빈 폼으로 전환
 * @return  N/A
 */
async function onBtnInfcNewClick(): Promise<void> {
  if (!(await confirmDiscard())) return
  activeInfcRow.value = null
  isNewInfc.value = true
  infcForm.value = createEmptyForm()
  infcFormSnapshot.value = JSON.stringify(infcForm.value)
  await nextTick()
  focusField('txtDtlInfcId')
}

/**
 * onChkDtlBodyLogYnChange : 본문로그 체크박스 변경 (해제하면 마스킹 필드 비활성)
 * @param   event - 체크박스 변경 이벤트
 * @return  N/A
 */
function onChkDtlBodyLogYnChange(event: Event): void {
  infcForm.value.bodyLogYn = (event.target as HTMLInputElement).checked ? 'Y' : 'N'
}

/**
 * onChkDtlInfcUseYnChange : 사용여부 체크박스 변경
 * @param   event - 체크박스 변경 이벤트
 * @return  N/A
 */
function onChkDtlInfcUseYnChange(event: Event): void {
  infcForm.value.useYn = (event.target as HTMLInputElement).checked ? 'Y' : 'N'
}

/**
 * onBtnDtlInfcAnnotationCopyClick : 코드 선언(@ApiInterface) 복사 버튼 클릭
 * @return  N/A
 */
async function onBtnDtlInfcAnnotationCopyClick(): Promise<void> {
  await navigator.clipboard.writeText(`@ApiInterface("${infcForm.value.infcId}")`)
  toast.success(msg('cm.info.copied'))
}

/**
 * onBtnInfcSaveClick : 저장 버튼 클릭: 검증 후 인터페이스 저장
 * @return  N/A
 */
async function onBtnInfcSaveClick(): Promise<void> {
  if (!(await validateInfcForm())) return
  const ok = await confirm({ message: msg('cm.confirm.save'), icon: 'question' })
  if (!ok) return

  const form = infcForm.value
  const infcId = form.infcId.trim()
  const res = await saveInterfaceApi(
    {
      infcId,
      infcNm: form.infcNm.trim(),
      httpMethNm: form.httpMethNm,
      url: form.url.trim(),
      bodyLogYn: form.bodyLogYn,
      // 본문로그를 남기지 않으면 마스킹 필드는 의미가 없으므로 비운다
      maskFldCont: form.bodyLogYn === 'Y' ? toMaskFldCont(form.maskFldText) || null : null,
      useYn: form.useYn,
      rmk: form.rmk.trim() || null,
    },
    isNewInfc.value,
  )
  if (!res.success) {
    await alert({ message: res.message ?? msg('cm.error.save.failed'), icon: 'error' })
    if (res.code === 'sy.link.infc.duplicate') focusField('txtDtlInfcId')
    return
  }
  await alert({ message: msg('cm.info.saved'), icon: 'information' })
  isNewInfc.value = false
  await searchInterfaceList(false, infcId)
  emit('saved')
}

// ---------------------------------------------------------------------
// 탭 초기화 (화면에서 처음 활성화될 때 호출)
// ---------------------------------------------------------------------
/**
 * initTab : 조회 조건을 기본값으로 초기화하고 인터페이스 목록 최초 조회
 * @return  N/A
 */
async function initTab(): Promise<void> {
  searchForm.value = { keyword: '', useYn: 'Y' }
  activeInfcRow.value = null
  isNewInfc.value = false
  await searchInterfaceList()
}

defineExpose({ initTab, searchInterfaceList })
</script>

<template>
  <div class="tab-root">
    <!-- 조회 조건 영역 -->
    <form id="frmInfcSrch" class="search-area" @submit.prevent="onBtnInfcSrchClick">
      <div class="search-fields">
        <label class="search-field">
          <span class="field-label">인터페이스ID/명</span>
          <Input id="txtInfcKeyword" v-model="searchForm.keyword" placeholder="ID·명·URL" @enter="onBtnInfcSrchClick" />
        </label>
        <label class="search-field">
          <span class="field-label">사용여부</span>
          <Select id="cboInfcUseYn" v-model="searchForm.useYn" :options="withAll(USE_YN_OPTIONS)" />
        </label>
      </div>
      <div class="search-actions">
        <Button id="btnInfcSrch" v-permission @click="onBtnInfcSrchClick">조회</Button>
      </div>
    </form>

    <div class="infc-split">
      <!-- 인터페이스 목록 (TB_INFC_M) -->
      <div class="grid-area">
        <div class="grid-header">
          <span class="grid-title">인터페이스 - <strong id="lblInfcTotCnt" class="total-count">{{ infcRows.length }}건</strong></span>
          <div class="grid-actions">
            <Button id="btnInfcNew" v-permission variant="outline" size="small" @click="onBtnInfcNewClick">신규</Button>
          </div>
        </div>
        <DataTable
          id="grdInfcList"
          :columns="infcColumns"
          :data="infcRows"
          :loading="isInfcLoading"
          :active-row="activeInfcRow"
          :pagination="false"
          class="nowrap-grid"
          dense
          @row-click="onGrdInfcListRowClick"
        >
          <template #cell-infcId="{ row }"><span class="mono">{{ row.infcId }}</span></template>
          <template #cell-infcNm="{ row }">
            <span class="cell-ellipsis" :title="`${row.infcNm}\n${row.httpMethNm} ${row.url}`">{{ row.infcNm }}</span>
          </template>
          <template #cell-useYnNm="{ row }">
            <span v-if="row.useYn === 'Y'">{{ row.useYnNm }}</span>
            <span v-else class="badge badge-mute">{{ row.useYnNm }}</span>
          </template>
          <template #cell-allwOrgCnt="{ row }">{{ formatCount(row.allwOrgCnt) }}</template>
        </DataTable>
      </div>

      <!-- 인터페이스 정보 -->
      <div class="detail-area">
        <p v-if="!hasInfc" class="empty-panel">{{ msg('sy.link.infc.select.guide') }}</p>

        <template v-else>
          <section class="panel">
            <div class="grid-header">
              <span class="grid-title">
                인터페이스 정보
                <span v-if="isNewInfc" class="sub-title">신규 등록</span>
              </span>
              <div class="grid-actions">
                <Button id="btnInfcSave" v-permission variant="success" size="small" @click="onBtnInfcSaveClick">저장</Button>
              </div>
            </div>
            <div id="frmInfcDtl" class="dtl-form">
              <div class="dtl-row">
                <span class="dtl-label">인터페이스ID</span>
                <div class="dtl-value">
                  <Input
                    id="txtDtlInfcId"
                    v-model="infcForm.infcId"
                    required
                    :readonly="!isNewInfc"
                    :disabled="!isNewInfc"
                    maxlength="8"
                    placeholder="IFSY0001"
                    size="small"
                  />
                  <p v-if="isNewInfc" class="field-hint">IF + 업무약어 2자리 + 순번 4자리</p>
                </div>
                <span class="dtl-label">메서드</span>
                <div class="dtl-value">
                  <Select id="cboDtlHttpMethNm" v-model="infcForm.httpMethNm" required :options="HTTP_METH_OPTIONS" size="small" />
                </div>
              </div>
              <div class="dtl-row">
                <span class="dtl-label">인터페이스명</span>
                <div class="dtl-value is-wide">
                  <Input id="txtDtlInfcNm" v-model="infcForm.infcNm" required maxlength="100" size="small" />
                </div>
              </div>
              <div class="dtl-row">
                <span class="dtl-label">URL</span>
                <div class="dtl-value is-wide">
                  <Input id="txtDtlUrl" v-model="infcForm.url" required maxlength="200" placeholder="/api/v1/samples/{id}" size="small" />
                  <p class="field-hint">Controller 매핑 경로와 같게 입력 (경로 변수는 {id} 형식)</p>
                </div>
              </div>
              <div class="dtl-row">
                <span class="dtl-label">본문로그</span>
                <label class="dtl-value check-item">
                  <input id="chkDtlBodyLogYn" type="checkbox" :checked="infcForm.bodyLogYn === 'Y'" @change="onChkDtlBodyLogYnChange" />
                  기록
                </label>
                <span class="dtl-label">사용여부</span>
                <label class="dtl-value check-item">
                  <input id="chkDtlInfcUseYn" type="checkbox" :checked="infcForm.useYn === 'Y'" @change="onChkDtlInfcUseYnChange" />
                  사용
                </label>
              </div>
              <div class="dtl-row">
                <span class="dtl-label">마스킹 필드</span>
                <div class="dtl-value is-wide">
                  <textarea
                    id="txaDtlMaskFldCont"
                    v-model="infcForm.maskFldText"
                    class="dtl-textarea mono"
                    rows="3"
                    placeholder="usrNm&#10;data.mblTelNo"
                    :disabled="infcForm.bodyLogYn !== 'Y'"
                  ></textarea>
                  <p class="field-hint">
                    한 줄에 하나. 필드명만 쓰면 모든 위치, 점으로 이으면 그 경로만 마스킹. 개인정보 필드는 반드시 입력
                    (토큰·비밀키·비밀번호 필드는 항상 마스킹)
                  </p>
                </div>
              </div>
              <div class="dtl-row">
                <span class="dtl-label">비고</span>
                <div class="dtl-value is-wide">
                  <Input id="txtDtlInfcRmk" v-model="infcForm.rmk" maxlength="200" size="small" />
                </div>
              </div>
              <div v-if="!isNewInfc" class="dtl-row">
                <span class="dtl-label">코드 선언</span>
                <div class="dtl-value is-wide dtl-inline">
                  <code id="lblDtlInfcAnnotation" class="annotation mono">@ApiInterface("{{ infcForm.infcId }}")</code>
                  <button type="button" class="icon-button" title="선언 복사" @click="onBtnDtlInfcAnnotationCopyClick">복사</button>
                  <span class="field-hint">coder-api Controller 메서드에 붙일 선언</span>
                </div>
              </div>
            </div>
            <p class="field-hint infc-hint">{{ msg('sy.link.infc.code.hint') }}</p>
          </section>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped src="@/assets/page.css"></style>

<style scoped>
.tab-root {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.infc-split {
  display: grid;
  grid-template-columns: 520px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

@media (max-width: 1280px) {
  .infc-split {
    grid-template-columns: 1fr;
  }
}

.detail-area {
  min-width: 0;
}


.dtl-inline, .check-item {
  flex-direction: row;
  align-items: center;
  justify-content: flex-start;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 13px;
}

.sub-title {
  color: var(--ds-ink-2);
}

.annotation {
  padding: 2px 8px;
  background: var(--ds-surface-muted);
  border: 1px solid var(--ds-rule);
  border-radius: var(--ds-radius-control);
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

</style>
