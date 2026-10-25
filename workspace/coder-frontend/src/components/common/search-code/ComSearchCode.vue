<script setup lang="ts">
/*****************************************************************************************
	Program ID   : ComSearchCode
	Program Type : vue
	Program Name : 공통 코드검색
	Description  : 업무구분별 조회/자동조회/선택 팝업을 연동한 코드검색 컴포넌트
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-05  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
import { msg } from '@/utils/message'
import { ref, computed, nextTick, onMounted } from 'vue'
import BaseSearchCode, { type SearchCodeTrigger } from './BaseSearchCode.vue'
import ComSearchCodePopup from './ComSearchCodePopup.vue'
import { useDialog } from '@/composables/useDialog'
import {
  getSearchCodeType,
  searchCodeApi,
  type SearchCodeParams,
  type SearchCodeRow,
} from '@/api/common/searchCode'

/**
 * 공통 코드서치
 *
 * 사용 예)
 *   <ComSearchCode id="cdsUsr" type="USR" v-model:code="form.usrId" v-model:name="form.usrNm" @change="onCdsUsrChange" />
 *
 * 동작
 * - 코드 입력: 명칭·선택값을 비우고 change(null). 업무구분별 글자 수 이상이면 자동조회해서
 *   1건이거나 코드가 정확히 일치하면 바로 선택한다.
 * - Enter: 코드로 조회 → 1건이면 선택, 여러 건이면 입력값으로 팝업, 0건이면 전체 팝업. 값이 없으면 다음 입력칸으로 이동.
 * - 돋보기: 바로 팝업.
 * - 포커스가 빠질 때 코드나 명칭이 비어 있으면 둘 다 지운다 (선택된 값만 남김).
 * - 선택되면 change(row, rows) 를 발생시키고 다음 입력칸으로 포커스를 옮긴다.
 */
const props = withDefaults(
  defineProps<{
    /** 코드서치 업무구분 (USR, PRGM, MENU, AUTH_GRP, COMP, CUST ...) */
    type: string
    code?: string
    name?: string
    /** 업무구분별 추가 조회조건 (원본 reqSearchParam, 예: { STOR_CD: '01' }) */
    params?: SearchCodeParams
    /** 코드칸 폭. 없으면 업무구분 기본 폭 */
    codeWidth?: string
    nameWidth?: string
    /** 팝업에서 여러 건 선택 (원본 chkYn) */
    multiple?: boolean
    disabled?: boolean
    required?: boolean
    /** 검증 규칙: "required" 등 (utils/validate.ts validateForm 대상) */
    validate?: string | string[]
    /** 검증 메시지에 쓸 항목명 (없으면 업무구분명) */
    fieldName?: string
  }>(),
  {
    code: '',
    name: '',
    params: () => ({}),
    codeWidth: undefined,
    nameWidth: '120px',
    multiple: false,
    disabled: false,
    required: false,
    validate: '',
    fieldName: '',
  },
)

const emit = defineEmits<{
  (e: 'update:code', value: string): void
  (e: 'update:name', value: string): void
  /** 선택 변경 (원본 callback). 선택 해제 시 row 는 null */
  (e: 'change', row: SearchCodeRow | null, rows: SearchCodeRow[]): void
}>()

// 화면에서 준 id 등은 입력 영역(BaseSearchCode)에 전달 (루트가 입력 영역 + 팝업 두 개)
defineOptions({ inheritAttrs: false })

const { alert } = useDialog()

const baseRef = ref<InstanceType<typeof BaseSearchCode> | null>(null)
const selectedRows = ref<SearchCodeRow[]>([])
const isPopupOpen = ref(false)
const popupKeyword = ref('')

const typeDef = computed(() => getSearchCodeType(props.type))

const resolvedCodeWidth = computed(() => props.codeWidth ?? `${typeDef.value?.codeWidth ?? 55}px`)

/**
 * checkSearchable : 업무구분, 필수 조회조건, 조회 연동 여부를 확인
 * 원본: fn_validation
 * @param   silent - true 면 경고 메시지를 띄우지 않음
 * @return  조회 가능 여부
 */
async function checkSearchable(silent = false): Promise<boolean> {
  const def = typeDef.value
  const fail = async (message: string) => {
    if (!silent) await alert({ message, icon: 'warning' })
    return false
  }
  if (!def) return fail(msg('cm.search.code.type.invalid', props.type))
  const missing = def.requiredParams?.find((req) => !props.params[req.key])
  if (missing) return fail(msg(missing.message, missing.key))
  if (!def.fetch) return fail(msg('cm.search.code.not.connected', def.title))
  return true
}

/**
 * setCodeName : 코드와 명칭 값을 함께 갱신
 * @param   code - 코드
 * @param   name - 명칭
 * @return  N/A
 */
function setCodeName(code: string, name: string): void {
  emit('update:code', code)
  emit('update:name', name)
}

/**
 * clearSelection : 선택을 해제하고, 선택값이 있었던 경우에만 change(null) 을 알림
 * @return  N/A
 */
function clearSelection(): void {
  if (selectedRows.value.length === 0) return
  selectedRows.value = []
  emit('change', null, [])
}

/**
 * applyResult : 선택 결과를 코드/명칭에 반영하고 change 이벤트 발생 후 다음 입력칸으로 이동
 * 원본: fn_setResultVal
 * @param   rows - 선택된 결과 행 목록
 * @return  N/A
 */
function applyResult(rows: SearchCodeRow[]): void {
  if (rows.length === 0) return
  const [first] = rows
  selectedRows.value = rows
  setCodeName(first.CD, rows.length > 1 ? msg('cm.search.code.selected.more', first.NM, rows.length - 1) : first.NM)
  emit('change', first, rows)
  focusNextElement()
}

/**
 * openPopup : 검색어를 지정해 선택 팝업을 염
 * @param   keyword - 팝업 초기 검색어
 * @return  N/A
 */
function openPopup(keyword: string): void {
  popupKeyword.value = keyword
  isPopupOpen.value = true
}

/**
 * focusNextElement : 화면에서 다음 입력 가능한 요소로 포커스를 이동
 * @return  N/A
 */
function focusNextElement(): void {
  const root = baseRef.value?.getRootEl()
  if (!root) return
  const focusables = Array.from(
    document.querySelectorAll<HTMLElement>(
      'input:not([disabled]):not([tabindex="-1"]), select:not([disabled]), textarea:not([disabled]), button:not([disabled]):not([tabindex="-1"]), [tabindex]:not([tabindex="-1"])',
    ),
  ).filter((el) => el.offsetParent !== null && !root.contains(el))
  const next = focusables.find((el) => root.compareDocumentPosition(el) & Node.DOCUMENT_POSITION_FOLLOWING)
  next?.focus()
}

/**
 * onCodeUpdate : 코드 직접 입력 시 선택을 해제하고, 업무구분별 글자 수 이상이면 자동조회
 * @param   code - 입력된 코드
 * @return  N/A
 */
async function onCodeUpdate(code: string): Promise<void> {
  emit('update:code', code)
  if (props.name) emit('update:name', '')
  clearSelection()

  // 자동조회: 업무구분별 글자 수 이상 입력 시 (원본 edtCd_oninput)
  const wordSize = typeDef.value?.wordSize
  if (!code || !wordSize || code.length < wordSize || !(await checkSearchable(true))) return
  const rows = await searchCodeApi(props.type, code, props.params)
  if (props.code !== code || selectedRows.value.length > 0) return // 그 사이 입력이 바뀜
  if (rows.length === 1 || rows[0]?.CD === code) applyResult([rows[0]])
}

/**
 * onSearch : Enter/돋보기 검색 요청 시 코드로 조회하거나 팝업을 염
 * @param   code    - 입력된 코드
 * @param   trigger - 검색 요청 경로 (enter, button)
 * @return  N/A
 */
async function onSearch(code: string, trigger: SearchCodeTrigger): Promise<void> {
  if (trigger === 'enter' && !code) {
    focusNextElement()
    return
  }
  if (!(await checkSearchable())) return
  if (trigger === 'button') {
    openPopup('')
    return
  }
  // Enter: 코드로 조회 (원본 fn_search_code → fn_callback "select")
  const rows = await searchCodeApi(props.type, code, props.params)
  if (rows.length === 1) applyResult(rows)
  else openPopup(rows.length > 1 ? code : '')
}

/**
 * onPopupSelect : 팝업에서 선택한 결과를 반영
 * @param   rows - 선택된 결과 행 목록
 * @return  N/A
 */
function onPopupSelect(rows: SearchCodeRow[]): void {
  applyResult(rows)
}

/**
 * onLeave : 포커스가 빠질 때 미완성 입력(코드 또는 명칭만 있는 상태)을 정리
 * 원본: form_onkillfocus
 * @return  N/A
 */
async function onLeave(): Promise<void> {
  // 선택 직후 다음 칸으로 이동한 경우 v-model 반영 전이므로 갱신을 기다린다
  await nextTick()
  if (isPopupOpen.value || selectedRows.value.length > 0) return
  if (!props.code || !props.name) {
    if (props.code || props.name) setCodeName('', '')
    clearSelection()
  }
}

/**
 * clear : 코드/명칭/선택값을 모두 초기화
 * 원본: fn_clear
 * @return  N/A
 */
function clear(): void {
  setCodeName('', '')
  clearSelection()
}

onMounted(() => {
  if (!typeDef.value) console.warn(`[ComSearchCode] 알 수 없는 업무구분: ${props.type}`)
})

defineExpose({
  clear,
  focus: () => baseRef.value?.focus(),
  /** 선택된 결과 행 (원본 fn_getVal) */
  getValue: () => selectedRows.value,
})
</script>

<template>
  <BaseSearchCode
    v-bind="$attrs"
    ref="baseRef"
    :code="code"
    :name="name"
    :code-width="resolvedCodeWidth"
    :name-width="nameWidth"
    :disabled="disabled"
    :required="required"
    :validate="validate"
    :field-name="fieldName || typeDef?.title || ''"
    :empty="!code || !name"
    :uppercase="typeDef?.uppercase ?? true"
    :clear-name-on-input="false"
    :search-title="`${typeDef?.title ?? ''} 검색`"
    @update:code="onCodeUpdate"
    @search="onSearch"
    @leave="onLeave"
  />
  <ComSearchCodePopup
    v-model="isPopupOpen"
    :type="type"
    :keyword="popupKeyword"
    :params="params"
    :multiple="multiple"
    @select="onPopupSelect"
  />
</template>
