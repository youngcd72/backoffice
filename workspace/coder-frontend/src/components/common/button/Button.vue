<template>
  <button
    :type="type"
    class="custom-btn"
    :class="[
      `btn-${variant}`,
      `size-${size}`,
      {
        'is-loading': loading,
        'is-disabled': disabled || loading,
        'is-block': block,
        'has-icon-only': $slots['icon-only']
      }
    ]"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <!-- 로딩 스피너 -->
    <span v-if="loading" class="btn-spinner" aria-hidden="true"></span>

    <!-- 아이콘 전용 슬롯 -->
    <span v-if="$slots['icon-only']" class="btn-icon icon-only">
      <slot name="icon-only"></slot>
    </span>

    <!-- 접두사 아이콘 슬롯 -->
    <span v-else-if="$slots.icon || $slots.prefix" class="btn-icon prefix-icon">
      <slot name="icon"><slot name="prefix"></slot></slot>
    </span>

    <!-- 텍스트 라벨 -->
    <span v-if="$slots.default && !$slots['icon-only']" class="btn-content">
      <slot></slot>
    </span>

    <!-- 접미사 아이콘 슬롯 -->
    <span v-if="$slots.suffix && !$slots['icon-only']" class="btn-icon suffix-icon">
      <slot name="suffix"></slot>
    </span>
  </button>
</template>

<script setup>
/*****************************************************************************************
	Program ID   : Button
	Program Type : vue
	Program Name : 버튼
	Description  : 공통 버튼 (비활성/로딩 상태에서는 클릭 차단)
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-09-07  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
const props = defineProps({
  variant: {
    type: String,
    default: 'primary',
    validator: (v) => ['primary', 'secondary', 'success', 'danger', 'warning', 'outline', 'ghost'].includes(v)
  },
  size: {
    type: String,
    default: 'medium',
    validator: (v) => ['small', 'medium', 'large'].includes(v)
  },
  type: {
    type: String,
    default: 'button'
  },
  disabled: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  },
  block: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['click']);

/**
 * handleClick : 버튼 클릭 처리. 비활성/로딩 중이면 무시하고, 아니면 click 이벤트를 발생
 * @param   e - 클릭 이벤트
 * @return  N/A
 */
const handleClick = (e) => {
  if (props.disabled || props.loading) {
    e.preventDefault();
    return;
  }
  emit('click', e);
};
</script>

<style scoped>
.custom-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-family: inherit;
  font-weight: 600;
  border-radius: var(--ds-radius-control);
  border: 1px solid transparent;
  cursor: pointer;
  white-space: nowrap;
  user-select: none;
  transition: background-color var(--ds-ease), border-color var(--ds-ease), color var(--ds-ease);
  outline: none;
  text-decoration: none;
  line-height: 1.2;
}
.custom-btn:focus-visible {
  box-shadow: var(--ds-focus);
}

/* 크기 */
.size-small {
  height: var(--ds-control-height-sm);
  padding: 0 12px;
  font-size: 13px;
}
.size-medium {
  height: var(--ds-control-height);
  padding: 0 16px;
  font-size: 14px;
}
.size-large {
  height: 40px;
  padding: 0 20px;
  font-size: 15px;
}

/* 아이콘 전용 버튼 크기 정방형 */
.size-small.has-icon-only { width: var(--ds-control-height-sm); padding: 0; }
.size-medium.has-icon-only { width: var(--ds-control-height); padding: 0; }
.size-large.has-icon-only { width: 40px; padding: 0; }

.is-block {
  display: flex;
  width: 100%;
}

/* 색상 테마별 변형 */
.btn-primary,
.btn-success {
  background: var(--ds-pine-700);
  border-color: var(--ds-pine-700);
  color: var(--ds-ink-inverse);
}
.btn-primary:hover:not(.is-disabled),
.btn-success:hover:not(.is-disabled) {
  background: var(--ds-pine-900);
  border-color: var(--ds-pine-900);
}

.btn-secondary,
.btn-outline {
  background: var(--ds-surface);
  border-color: var(--ds-rule-strong);
  color: var(--ds-ink);
}
.btn-secondary:hover:not(.is-disabled),
.btn-outline:hover:not(.is-disabled) {
  background: var(--ds-pine-050);
  border-color: var(--ds-pine-700);
  color: var(--ds-pine-900);
}

.btn-danger {
  background: var(--ds-stamp);
  border-color: var(--ds-stamp);
  color: var(--ds-ink-inverse);
}
.btn-danger:hover:not(.is-disabled) {
  background: var(--ds-stamp-hover);
  border-color: var(--ds-stamp-hover);
}

.btn-warning {
  background: var(--ds-surface);
  border-color: var(--ds-ochre);
  color: var(--ds-ochre);
}
.btn-warning:hover:not(.is-disabled) {
  background: var(--ds-sign-tint);
}

.btn-ghost {
  background: transparent;
  border-color: transparent;
  color: var(--ds-ink-2);
}
.btn-ghost:hover:not(.is-disabled) {
  background: var(--ds-pine-050);
  color: var(--ds-ink);
}

/* 비활성화 및 로딩 */
.custom-btn.is-disabled {
  opacity: 1;
  cursor: not-allowed;
  background: var(--ds-surface-muted);
  border-color: var(--ds-rule);
  color: var(--ds-ink-3);
}

.btn-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid var(--ds-rule);
  border-top-color: currentColor;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
</style>
