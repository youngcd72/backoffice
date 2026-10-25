<template>
  <div 
    class="custom-input-group" 
    :class="[
      `size-${size}`,
      { 
        'is-disabled': disabled,
        'is-readonly': readonly,
        'has-error': Boolean(error),
        'is-focused': isFocused,
        'has-value': Boolean(modelValue),
        'is-required': isRequired
      }
    ]"
    :data-validate="validateRuleText"
    :data-empty="validateRuleText ? String(isEmptyValue) : undefined"
    :data-field-name="validateRuleText ? (fieldName || label || placeholder) : undefined"
  >
    <!-- 상단 라벨 -->
    <label v-if="label" class="input-label">
      {{ label }}
    </label>

    <!-- 인풋 컨테이너 -->
    <div class="input-control" @click="focusInput">
      <!-- 접두사 아이콘 슬롯 -->
      <span v-if="$slots.prefix || prefixIcon" class="input-affix prefix">
        <slot name="prefix">{{ prefixIcon }}</slot>
      </span>

      <!-- 실제 HTML input -->
      <input
        ref="inputRef"
        :type="computedType"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :readonly="readonly"
        class="native-input"
        @input="handleInput"
        @focus="handleFocus"
        @blur="handleBlur"
        @keydown.enter="handleEnter"
      />

      <!-- 클리어 버튼 -->
      <button
        v-if="clearable && modelValue && !disabled && !readonly"
        type="button"
        class="input-action-btn clear-btn"
        title="내용 지우기"
        @click.stop="handleClear"
      >
        <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round">
          <line x1="18" y1="6" x2="6" y2="18"></line>
          <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
      </button>

      <!-- 비밀번호 표시/숨김 토글 -->
      <button
        v-if="type === 'password' && !disabled"
        type="button"
        class="input-action-btn eye-btn"
        :title="showPassword ? '비밀번호 숨기기' : '비밀번호 보기'"
        @click.stop="showPassword = !showPassword"
      >
        <!-- 눈 열림 -->
        <svg v-if="showPassword" viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round">
          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
          <circle cx="12" cy="12" r="3"></circle>
        </svg>
        <!-- 눈 감김 -->
        <svg v-else viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round">
          <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
          <line x1="1" y1="1" x2="23" y2="23"></line>
        </svg>
      </button>

      <!-- 접미사 아이콘 슬롯 -->
      <span v-if="$slots.suffix || suffixIcon" class="input-affix suffix">
        <slot name="suffix">{{ suffixIcon }}</slot>
      </span>
    </div>

    <!-- 하단 힌트 또는 에러 메시지 -->
    <p v-if="error" class="input-message error-message">{{ error }}</p>
    <p v-else-if="hint" class="input-message hint-message">{{ hint }}</p>
  </div>
</template>

<script setup>
/*****************************************************************************************
	Program ID   : Input
	Program Type : vue
	Program Name : 입력 필드
	Description  : 공통 텍스트 입력 (지우기, 비밀번호 표시 전환, 검증 규칙 지원)
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-09-07  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
import { ref, computed } from 'vue';
import { normalizeValidateRules } from '@/utils/validate';

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: ''
  },
  type: {
    type: String,
    default: 'text'
  },
  label: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: ''
  },
  hint: {
    type: String,
    default: ''
  },
  error: {
    type: String,
    default: ''
  },
  disabled: {
    type: Boolean,
    default: false
  },
  readonly: {
    type: Boolean,
    default: false
  },
  required: {
    type: Boolean,
    default: false
  },
  // 검증 규칙: "required|email|juminno" 또는 배열 (utils/validate.ts validateForm 대상)
  validate: {
    type: [String, Array],
    default: ''
  },
  // 검증 메시지에 쓸 항목명 (없으면 label → placeholder)
  fieldName: {
    type: String,
    default: ''
  },
  clearable: {
    type: Boolean,
    default: false
  },
  size: {
    type: String,
    default: 'medium',
    validator: (v) => ['small', 'medium', 'large'].includes(v)
  },
  prefixIcon: {
    type: String,
    default: ''
  },
  suffixIcon: {
    type: String,
    default: ''
  }
});

const emit = defineEmits(['update:modelValue', 'change', 'clear', 'focus', 'blur', 'enter']);

const inputRef = ref(null);
const isFocused = ref(false);
const showPassword = ref(false);

const computedType = computed(() => {
  if (props.type === 'password') {
    return showPassword.value ? 'text' : 'password';
  }
  return props.type;
});

/**
 * handleInput : 입력 값 변경 시 update:modelValue / change 이벤트를 발생
 * @param   e - 입력 이벤트
 * @return  N/A
 */
const handleInput = (e) => {
  emit('update:modelValue', e.target.value);
  emit('change', e.target.value);
};

/**
 * handleClear : 지우기 버튼 클릭 시 값을 비우고 clear 이벤트를 발생한 뒤 포커스를 이동
 * @return  N/A
 */
const handleClear = () => {
  emit('update:modelValue', '');
  emit('change', '');
  emit('clear');
  focusInput();
};

/**
 * handleFocus : 포커스 진입 시 상태를 갱신하고 focus 이벤트를 발생
 * @param   e - 포커스 이벤트
 * @return  N/A
 */
const handleFocus = (e) => {
  isFocused.value = true;
  emit('focus', e);
};

/**
 * handleBlur : 포커스 이탈 시 상태를 갱신하고 blur 이벤트를 발생
 * @param   e - 포커스 이벤트
 * @return  N/A
 */
const handleBlur = (e) => {
  isFocused.value = false;
  emit('blur', e);
};

/**
 * handleEnter : Enter 키 입력 시 enter 이벤트를 발생
 * @param   e - 키보드 이벤트
 * @return  N/A
 */
const handleEnter = (e) => {
  emit('enter', e);
};

// 검증 규칙 (required 속성은 validate="required" 의 줄임말)
const validateRules = computed(() => normalizeValidateRules(props.validate, props.required));
const validateRuleText = computed(() => validateRules.value.join(' ') || undefined);
const isRequired = computed(() => validateRules.value.includes('required'));

// 검증용 빈 값 여부
const isEmptyValue = computed(() => props.modelValue === null || props.modelValue === undefined || String(props.modelValue).trim() === '');

/**
 * focusInput : 입력 요소에 포커스를 줌
 * @return  N/A
 */
const focusInput = () => {
  inputRef.value?.focus();
};

defineExpose({
  focus: focusInput,
  inputRef
});
</script>

<style scoped>
.custom-input-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
  font-family: inherit;
  --input-bg: var(--ds-surface);
  --input-bg-focus: var(--ds-surface);
  --input-border: var(--ds-rule-strong);
  --input-border-hover: var(--ds-ink-3);
  --input-border-focus: var(--ds-pine-700);
  --input-text: var(--ds-ink);
  --input-text-muted: var(--ds-ink-3);
  --input-radius: var(--ds-radius-control);
}

/* 크기 */
.size-small {
  --input-height: var(--ds-control-height-sm);
  --input-font-size: 13px;
  --input-padding: 0 10px;
}
.size-medium {
  --input-height: var(--ds-control-height);
  --input-font-size: 14px;
  --input-padding: 0 12px;
}
.size-large {
  --input-height: 40px;
  --input-font-size: 15px;
  --input-padding: 0 14px;
}

.input-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--ds-ink-2);
  display: flex;
  align-items: center;
  gap: 4px;
}

.input-control {
  display: flex;
  align-items: center;
  gap: 8px;
  height: var(--input-height);
  padding: var(--input-padding);
  background: var(--input-bg);
  border: 1px solid var(--input-border);
  border-radius: var(--input-radius);
  transition: background-color var(--ds-ease), border-color var(--ds-ease), box-shadow var(--ds-ease);
}

.input-control:hover:not(.is-disabled) {
  border-color: var(--input-border-hover);
}

.is-focused .input-control,
.is-focused .input-control:hover {
  background: var(--input-bg-focus);
  border-color: var(--input-border-focus);
  box-shadow: var(--ds-focus-field);
}

.native-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: var(--input-text);
  font-size: var(--input-font-size);
  font-family: inherit;
  width: 100%;
  min-width: 0;
}

.native-input::placeholder {
  color: var(--input-text-muted);
  opacity: 1;
}

.input-affix {
  display: flex;
  align-items: center;
  color: var(--input-text-muted);
  font-size: 14px;
  flex-shrink: 0;
}

.input-action-btn {
  background: none;
  border: none;
  color: var(--input-text-muted);
  padding: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 2px;
  transition: background-color var(--ds-ease), color var(--ds-ease);
  flex-shrink: 0;
}

.input-action-btn:hover {
  color: var(--input-text);
  background: var(--ds-pine-050);
}

.input-action-btn:focus-visible {
  box-shadow: var(--ds-focus);
}

.clear-btn:hover {
  color: var(--ds-stamp);
  background: var(--ds-stamp-tint);
}

/* 에러 상태 */
.has-error .input-control,
.has-error.is-focused .input-control {
  border-color: var(--ds-stamp);
}

.has-error.is-focused .input-control {
  box-shadow: 0 0 0 3px var(--ds-stamp-tint);
}

/* 필수 항목 표시 : 입력 박스 왼쪽 안쪽 빨간 줄 (비활성/읽기전용 제외) */
.is-required:not(.is-disabled):not(.is-readonly) .input-control {
  box-shadow: inset 2px 0 0 var(--ds-stamp);
}

.is-required.is-focused:not(.is-disabled):not(.is-readonly) .input-control {
  box-shadow: inset 2px 0 0 var(--ds-stamp), var(--ds-focus-field);
}

.is-required.has-error.is-focused:not(.is-disabled):not(.is-readonly) .input-control {
  box-shadow: inset 2px 0 0 var(--ds-stamp), 0 0 0 3px var(--ds-stamp-tint);
}

.input-message {
  margin: 0;
  font-size: 12px;
  line-height: 1.3;
}

.error-message {
  color: var(--ds-stamp);
}

.hint-message {
  color: var(--input-text-muted);
}

/* 비활성화 */
.is-disabled {
  cursor: not-allowed;
}

.is-disabled .input-control {
  pointer-events: none;
  background: var(--ds-surface-muted);
  border-color: var(--ds-rule);
}

.is-disabled .native-input {
  color: var(--ds-ink-3);
}
</style>
