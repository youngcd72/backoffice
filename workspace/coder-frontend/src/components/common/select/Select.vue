<template>
  <div 
    class="custom-select-group" 
    :class="[
      `size-${size}`,
      { 
        'is-open': isOpen, 
        'is-disabled': disabled, 
        'has-value': hasValue,
        'has-error': Boolean(error),
        'is-required': isRequired
      }
    ]"
    :data-validate="validateRuleText"
    :data-empty="validateRuleText ? String(isEmptyValue) : undefined"
    :data-field-name="validateRuleText ? (fieldName || label || placeholder) : undefined"
    ref="selectRef"
  >
    <!-- 상단 라벨 -->
    <label v-if="label" class="select-label">
      {{ label }}
    </label>

    <!-- 셀렉트 트리거 박스 -->
    <div 
      class="select-trigger" 
      @click="toggleDropdown"
      tabindex="0"
      @keydown.esc="closeDropdown"
      @keydown.enter.prevent="toggleDropdown"
      @keydown.space.prevent="toggleDropdown"
    >
      <span class="selected-text" :class="{ 'placeholder': !hasValue }">
        {{ displayLabel }}
      </span>

      <div class="select-actions">
        <!-- 클리어 버튼 (클릭이 트리거로 전달되어 드롭다운이 열리지 않도록 이 버튼만 전파 중단) -->
        <button
          v-if="clearable && hasValue && !disabled"
          type="button"
          class="action-btn clear-btn"
          title="선택 해제"
          @click.stop="clearSelection"
        >
          <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
          </svg>
        </button>

        <!-- 화살표 아이콘 -->
        <span class="chevron-icon" :class="{ 'rotate': isOpen }">
          <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="6 9 12 15 18 9"></polyline>
          </svg>
        </span>
      </div>
    </div>

    <!-- 드롭다운 메뉴 팝업 -->
    <Transition name="select-dropdown">
      <div v-if="isOpen" class="select-menu">
        <div v-if="normalizedOptions.length === 0" class="select-empty">
          {{ msg('cm.info.no.options') }}
        </div>
        <ul v-else class="options-list">
          <li
            v-for="opt in normalizedOptions"
            :key="opt.value"
            class="option-item"
            :class="{
              'is-selected': isSelected(opt.value),
              'is-disabled': opt.disabled
            }"
            @click="selectOption(opt)"
          >
            <span class="option-label">{{ opt.label }}</span>
            <span v-if="isSelected(opt.value)" class="check-icon">
              <svg viewBox="0 0 24 24" width="15" height="15" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="20 6 9 17 4 12"></polyline>
              </svg>
            </span>
          </li>
        </ul>
      </div>
    </Transition>

    <!-- 에러 메시지 -->
    <p v-if="error" class="select-error">{{ error }}</p>
  </div>
</template>

<script setup>
/*****************************************************************************************
	Program ID   : Select
	Program Type : vue
	Program Name : 셀렉트
	Description  : 드롭다운 형태의 공통 단일 선택 콤보박스
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-09-07  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
import { msg } from '@/utils/message';
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { normalizeValidateRules } from '@/utils/validate';

const props = defineProps({
  modelValue: {
    type: [String, Number, Boolean, Object],
    default: null
  },
  options: {
    type: Array,
    default: () => []
  },
  placeholder: {
    type: String,
    default: '선택하세요'
  },
  label: {
    type: String,
    default: ''
  },
  error: {
    type: String,
    default: ''
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
  disabled: {
    type: Boolean,
    default: false
  },
  clearable: {
    type: Boolean,
    default: false
  },
  size: {
    type: String,
    default: 'medium',
    validator: (v) => ['small', 'medium', 'large'].includes(v)
  }
});

const emit = defineEmits(['update:modelValue', 'change', 'clear', 'open', 'close']);

const selectRef = ref(null);

// 검증 규칙 (required 속성은 validate="required" 의 줄임말)
const validateRules = computed(() => normalizeValidateRules(props.validate, props.required));
const validateRuleText = computed(() => validateRules.value.join(' ') || undefined);
const isRequired = computed(() => validateRules.value.includes('required'));

// 검증용 빈 값 여부
const isEmptyValue = computed(() => props.modelValue === null || props.modelValue === undefined || props.modelValue === '');
const isOpen = ref(false);

const normalizedOptions = computed(() => {
  return props.options.map((item) => {
    if (typeof item === 'object' && item !== null) {
      return {
        label: item.label !== undefined ? item.label : String(item.value),
        value: item.value !== undefined ? item.value : item,
        disabled: Boolean(item.disabled)
      };
    }
    return {
      label: String(item),
      value: item,
      disabled: false
    };
  });
});

const selectedOption = computed(() => {
  return normalizedOptions.value.find((opt) => opt.value === props.modelValue);
});

const hasValue = computed(() => {
  return props.modelValue !== null && props.modelValue !== undefined && props.modelValue !== '';
});

const displayLabel = computed(() => {
  if (selectedOption.value) {
    return selectedOption.value.label;
  }
  return props.placeholder;
});

/**
 * isSelected : 해당 값이 현재 선택되어 있는지 판별
 * @param   val - 확인할 항목 값
 * @return  선택 여부
 */
const isSelected = (val) => {
  return props.modelValue === val;
};

/**
 * toggleDropdown : 드롭다운을 열거나 닫음 (비활성 상태면 무시)
 * @return  N/A
 */
const toggleDropdown = () => {
  if (props.disabled) return;
  if (isOpen.value) {
    closeDropdown();
  } else {
    isOpen.value = true;
    emit('open');
  }
};

/**
 * closeDropdown : 열려 있는 드롭다운을 닫고 close 이벤트를 발생
 * @return  N/A
 */
const closeDropdown = () => {
  if (isOpen.value) {
    isOpen.value = false;
    emit('close');
  }
};

/**
 * selectOption : 항목 선택 시 값을 갱신하고 드롭다운을 닫음
 * @param   opt - 선택된 항목
 * @return  N/A
 */
const selectOption = (opt) => {
  if (opt.disabled) return;
  emit('update:modelValue', opt.value);
  emit('change', opt.value);
  closeDropdown();
};

/**
 * clearSelection : 선택 값을 null 로 비우고 clear 이벤트를 발생
 * @return  N/A
 */
const clearSelection = () => {
  if (props.disabled) return;
  emit('update:modelValue', null);
  emit('change', null);
  emit('clear');
};

/**
 * handleOutsideClick : 드롭다운 바깥 클릭 시 닫음
 * @param   e - 클릭 이벤트
 * @return  N/A
 */
const handleOutsideClick = (e) => {
  if (!selectRef.value) return;
  if (!selectRef.value.contains(e.target)) {
    closeDropdown();
  }
};

onMounted(() => {
  document.addEventListener('click', handleOutsideClick);
});

onBeforeUnmount(() => {
  document.removeEventListener('click', handleOutsideClick);
});
</script>

<style scoped>
.custom-select-group {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
  font-family: inherit;
  user-select: none;
  --select-bg: var(--ds-surface);
  --select-border: var(--ds-rule-strong);
  --select-border-hover: var(--ds-ink-3);
  --select-border-focus: var(--ds-pine-700);
  --select-text: var(--ds-ink);
  --select-text-muted: var(--ds-ink-3);
  --select-menu-bg: var(--ds-surface);
  --select-radius: var(--ds-radius-control);
}

/* 크기 */
.size-small {
  --select-height: var(--ds-control-height-sm);
  --select-font-size: 13px;
  --select-padding: 0 10px;
}
.size-medium {
  --select-height: var(--ds-control-height);
  --select-font-size: 14px;
  --select-padding: 0 12px;
}
.size-large {
  --select-height: 40px;
  --select-font-size: 15px;
  --select-padding: 0 14px;
}

.select-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--ds-ink-2);
}

.select-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  height: var(--select-height);
  padding: var(--select-padding);
  background: var(--select-bg);
  border: 1px solid var(--select-border);
  border-radius: var(--select-radius);
  cursor: pointer;
  outline: none;
  transition: background-color var(--ds-ease), border-color var(--ds-ease), box-shadow var(--ds-ease);
}

.select-trigger:hover:not(.is-disabled) {
  border-color: var(--select-border-hover);
}

.select-trigger:focus-visible,
.is-open .select-trigger,
.is-open .select-trigger:hover {
  border-color: var(--select-border-focus);
  box-shadow: var(--ds-focus-field);
}

.selected-text {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--select-text);
  font-size: var(--select-font-size);
  font-weight: 400;
}

.selected-text.placeholder {
  color: var(--select-text-muted);
  font-weight: 400;
}

.select-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.action-btn.clear-btn {
  background: none;
  border: none;
  padding: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--select-text-muted);
  border-radius: 2px;
  cursor: pointer;
  transition: background-color var(--ds-ease), color var(--ds-ease);
}

.action-btn.clear-btn:hover {
  background: var(--ds-stamp-tint);
  color: var(--ds-stamp);
}

.action-btn.clear-btn:focus-visible {
  box-shadow: var(--ds-focus);
}

.chevron-icon {
  display: flex;
  align-items: center;
  color: var(--select-text-muted);
}

.chevron-icon.rotate {
  transform: rotate(180deg);
  color: var(--select-border-focus);
}

/* 드롭다운 메뉴: 최소 콤보 너비, 항목이 길면 한 줄로 보이도록 내용만큼 넓힌다 */
.select-menu {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  min-width: 100%;
  width: max-content;
  max-width: 360px;
  z-index: 1000;
  background: var(--select-menu-bg);
  border: 1px solid var(--ds-rule-strong);
  border-radius: var(--ds-radius-panel);
  box-shadow: var(--ds-shadow-overlay);
  max-height: 240px;
  overflow-y: auto;
  padding: 4px 0;
}

.options-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.option-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  white-space: nowrap;
  padding: 7px 12px;
  border-radius: 0;
  color: var(--select-text);
  font-size: var(--select-font-size);
  cursor: pointer;
  transition: background-color var(--ds-ease), color var(--ds-ease);
}

.option-item:hover:not(.is-disabled) {
  background: var(--ds-pine-050);
}

.option-item.is-selected,
.option-item.is-selected:hover:not(.is-disabled) {
  background: var(--ds-pine-100);
  color: var(--ds-pine-900);
  font-weight: 600;
}

.option-item.is-disabled {
  color: var(--ds-ink-3);
  cursor: not-allowed;
}

.check-icon {
  color: var(--ds-pine-700);
}

.select-empty {
  padding: 12px;
  text-align: center;
  color: var(--select-text-muted);
  font-size: 13px;
}

/* 트랜지션 */
.select-dropdown-enter-active,
.select-dropdown-leave-active {
  transition: opacity var(--ds-ease);
}

.select-dropdown-enter-from,
.select-dropdown-leave-to {
  opacity: 0;
}

/* 에러 상태 */
.has-error .select-trigger,
.has-error .select-trigger:hover {
  border-color: var(--ds-stamp);
}

/* 필수 항목 표시 : 선택 박스 왼쪽 안쪽 빨간 줄 (비활성 제외) */
.is-required:not(.is-disabled) .select-trigger {
  box-shadow: inset 2px 0 0 var(--ds-stamp);
}

.is-required:not(.is-disabled) .select-trigger:focus-visible,
.is-required.is-open:not(.is-disabled) .select-trigger,
.is-required.is-open:not(.is-disabled) .select-trigger:hover {
  box-shadow: inset 2px 0 0 var(--ds-stamp), var(--ds-focus-field);
}

/* 비활성화 */
.is-disabled {
  cursor: not-allowed;
}
.is-disabled .select-trigger {
  pointer-events: none;
  background: var(--ds-surface-muted);
  border-color: var(--ds-rule);
}
.is-disabled .selected-text {
  color: var(--ds-ink-3);
}

.select-error {
  margin: 0;
  font-size: 12px;
  color: var(--ds-stamp);
}
</style>
