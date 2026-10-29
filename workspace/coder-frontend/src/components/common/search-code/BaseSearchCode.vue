<script setup lang="ts">
/*****************************************************************************************
	Program ID   : BaseSearchCode
	Program Type : vue
	Program Name : 기본 코드검색
	Description  : 코드 입력 + 돋보기 버튼 + 명칭 표시를 제공하는 프레젠테이셔널 코드검색 입력
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-05  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
import { ref, computed } from 'vue'
import { normalizeValidateRules } from '@/utils/validate'
import searchIcon from '@/assets/images/common/btn_WF_Grdexpand.png'

/** 검색 요청 경로: 코드칸 Enter / 돋보기 버튼 클릭 */
export type SearchCodeTrigger = 'enter' | 'button'

/**
 * 코드 입력 + 돋보기 버튼 + 명칭 표시 (예: 사용자ID 🔍 사용자명)
 * - v-model:code / v-model:name 으로 값을 주고받는다.
 * - 코드칸 Enter·돋보기 클릭 시 search(code, trigger) 이벤트를 발생시킨다 (조회·팝업은 사용하는 쪽에서 처리).
 * - 업무구분별 조회 로직이 필요하면 ComSearchCode 를 사용한다.
 */
const props = withDefaults(
  defineProps<{
    code?: string
    name?: string
    codePlaceholder?: string
    namePlaceholder?: string
    codeWidth?: string
    nameWidth?: string
    codeMaxlength?: number
    disabled?: boolean
    /** 코드 직접 입력 불가 (돋보기로만 선택) */
    codeReadonly?: boolean
    /** 필수 입력 (validate="required" 의 줄임말) */
    required?: boolean
    /** 검증 규칙: "required|email" 또는 배열 (utils/validate.ts validateForm 대상) */
    validate?: string | string[]
    /** 필수 검증 메시지에 쓸 항목명 (없으면 codePlaceholder) */
    fieldName?: string
    /** 선택 완료 여부를 직접 지정 (없으면 code 가 비었는지로 판단) */
    empty?: boolean
    /** 영문 입력을 대문자로 변환 */
    uppercase?: boolean
    /** 코드를 직접 고치면 이전 코드의 명칭을 비운다 */
    clearNameOnInput?: boolean
    searchTitle?: string
  }>(),
  {
    code: '',
    name: '',
    codePlaceholder: '',
    namePlaceholder: '',
    codeWidth: '90px',
    nameWidth: '120px',
    codeMaxlength: undefined,
    disabled: false,
    codeReadonly: false,
    required: false,
    validate: '',
    fieldName: '',
    empty: undefined,
    uppercase: false,
    clearNameOnInput: true,
    searchTitle: '검색',
  },
)

const emit = defineEmits<{
  (e: 'update:code', value: string): void
  (e: 'update:name', value: string): void
  (e: 'search', code: string, trigger: SearchCodeTrigger): void
  /** 포커스가 컴포넌트 밖으로 나감 */
  (e: 'leave'): void
}>()

// 검증 규칙 (required 속성은 validate="required" 의 줄임말)
const validateRules = computed(() => normalizeValidateRules(props.validate, props.required))
const validateRuleText = computed(() => validateRules.value.join(' ') || undefined)
const isRequired = computed(() => validateRules.value.includes('required'))

const rootRef = ref<HTMLElement | null>(null)
const codeInputRef = ref<HTMLInputElement | null>(null)

/**
 * onCodeInput : 코드 입력 시 대문자 변환을 적용하고 update:code 를 발생, 필요하면 명칭을 비움
 * @param   event - 입력 이벤트
 * @return  N/A
 */
function onCodeInput(event: Event): void {
  const input = event.target as HTMLInputElement
  let value = input.value
  if (props.uppercase && value !== value.toUpperCase()) {
    const caret = input.selectionStart
    value = value.toUpperCase()
    input.value = value
    if (caret !== null) input.setSelectionRange(caret, caret)
  }
  emit('update:code', value)
  if (props.clearNameOnInput && props.name) emit('update:name', '')
}

/**
 * requestSearch : 검색 요청 (비활성 상태면 무시) 시 search 이벤트를 발생
 * @param   trigger - 검색 요청 경로 (enter, button)
 * @return  N/A
 */
function requestSearch(trigger: SearchCodeTrigger): void {
  if (props.disabled) return
  emit('search', props.code.trim(), trigger)
}

/**
 * onFocusOut : 포커스가 컴포넌트 밖으로 나가면 leave 이벤트를 발생
 * @param   event - 포커스 이벤트
 * @return  N/A
 */
function onFocusOut(event: FocusEvent): void {
  const next = event.relatedTarget as Node | null
  if (!next || !rootRef.value?.contains(next)) emit('leave')
}

defineExpose({
  focus: () => codeInputRef.value?.focus(),
  /** 컴포넌트 루트 요소 (다음 입력칸 포커스 이동 등에 사용) */
  getRootEl: () => rootRef.value,
})
</script>

<template>
  <div
    ref="rootRef"
    class="search-code"
    :class="{ 'is-disabled': disabled, 'is-required': isRequired }"
    :data-validate="validateRuleText"
    :data-empty="validateRuleText ? String(empty ?? !code.trim()) : undefined"
    :data-field-name="validateRuleText ? (fieldName || codePlaceholder) : undefined"
    @focusout="onFocusOut"
  >
    <input
      ref="codeInputRef"
      class="search-code-code"
      type="text"
      :value="code"
      :placeholder="codePlaceholder"
      :maxlength="codeMaxlength"
      :disabled="disabled"
      :readonly="codeReadonly"
      :aria-required="isRequired || undefined"
      :style="{ width: codeWidth }"
      @input="onCodeInput"
      @keydown.enter.prevent="requestSearch('enter')"
    />
    <button
      type="button"
      class="search-code-btn"
      :title="searchTitle"
      :aria-label="searchTitle"
      :disabled="disabled"
      tabindex="-1"
      @click="requestSearch('button')"
    >
      <img :src="searchIcon" alt="" width="13" height="13" />
    </button>
    <input
      class="search-code-name"
      type="text"
      :value="name"
      :placeholder="namePlaceholder"
      :style="{ width: nameWidth }"
      readonly
      tabindex="-1"
    />
  </div>
</template>

<style scoped>
.search-code {
  --cs-height: var(--ds-control-height);
  --cs-border: var(--ds-rule-strong);
  --cs-border-hover: var(--ds-ink-3);
  --cs-border-focus: var(--ds-slate-700);
  --cs-bg: var(--ds-surface);
  --cs-bg-readonly: var(--ds-surface-muted);
  --cs-btn-bg: var(--ds-surface);
  --cs-btn-bg-hover: var(--ds-slate-050);
  --cs-text: var(--ds-ink);
  --cs-text-muted: var(--ds-ink-3);

  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: var(--cs-height);
  font-size: 13px;
  vertical-align: middle;
}

.search-code-code,
.search-code-name {
  box-sizing: border-box;
  height: 100%;
  padding: 0 8px;
  font: inherit;
  color: var(--cs-text);
  background: var(--cs-bg);
  border: 1px solid var(--cs-border);
  border-radius: var(--ds-radius-control);
  outline: none;
  transition: border-color var(--ds-ease), box-shadow var(--ds-ease);
}

/* 코드 입력칸 + 돋보기 버튼은 한 덩어리로 붙인다 */
.search-code-code {
  position: relative;
  font-family: var(--ds-font-code);
  font-size: 12.5px;
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
}

.search-code-code:hover:not(:disabled):not([readonly]) {
  border-color: var(--cs-border-hover);
}

.search-code-code:focus {
  z-index: 1;
  border-color: var(--cs-border-focus);
  box-shadow: var(--ds-focus-field);
}

.search-code-code::placeholder,
.search-code-name::placeholder {
  color: var(--cs-text-muted);
  font-family: var(--ds-font);
}

.search-code-code[readonly] {
  background: var(--cs-bg-readonly);
}

/* 필수 입력 표시: 좌측 스탬프 표시선 */
.is-required .search-code-code:not([readonly]):not(:disabled) {
  box-shadow: inset 2px 0 0 var(--ds-stamp);
}

.is-required .search-code-code:not([readonly]):not(:disabled):focus {
  box-shadow: inset 2px 0 0 var(--ds-stamp), var(--ds-focus-field);
}

/* 돋보기 버튼: 코드 입력칸에 붙여 배치 */
.search-code-btn {
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: var(--cs-height);
  height: 100%;
  margin-left: -5px;
  padding: 0;
  background: var(--cs-btn-bg);
  border: 1px solid var(--cs-border);
  border-radius: 0 var(--ds-radius-control) var(--ds-radius-control) 0;
  cursor: pointer;
  transition: background-color var(--ds-ease), border-color var(--ds-ease);
}

.search-code-btn:hover:not(:disabled) {
  background: var(--cs-btn-bg-hover);
  border-color: var(--ds-slate-700);
}

.search-code-btn:focus-visible {
  outline: none;
  box-shadow: var(--ds-focus);
}

.search-code-name {
  background: var(--cs-bg-readonly);
  color: var(--ds-ink-2);
  cursor: default;
}

.is-disabled .search-code-code,
.is-disabled .search-code-btn {
  background: var(--cs-bg-readonly);
  border-color: var(--ds-rule);
  color: var(--cs-text-muted);
  cursor: not-allowed;
}

.is-disabled .search-code-btn img {
  opacity: 0.4;
}
</style>
