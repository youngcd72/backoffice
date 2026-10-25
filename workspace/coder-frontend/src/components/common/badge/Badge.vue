<template>
  <span 
    class="custom-badge" 
    :class="[
      `badge-${variant}`,
      `size-${size}`,
      { 
        'is-dot': dot, 
        'is-pill': pill, 
        'is-outline': outline 
      }
    ]"
  >
    <span v-if="dot" class="badge-dot-indicator"></span>
    <slot></slot>
  </span>
</template>

<script setup>
/*****************************************************************************************
	Program ID   : Badge
	Program Type : vue
	Program Name : 배지
	Description  : 짧은 상태/라벨을 표시하는 공통 배지
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-09-07  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
defineProps({
  variant: {
    type: String,
    default: 'primary',
    validator: (v) => ['primary', 'success', 'warning', 'danger', 'info', 'neutral'].includes(v)
  },
  size: {
    type: String,
    default: 'medium',
    validator: (v) => ['small', 'medium'].includes(v)
  },
  dot: {
    type: Boolean,
    default: false
  },
  pill: {
    type: Boolean,
    default: true
  },
  outline: {
    type: Boolean,
    default: false
  }
});
</script>

<style scoped>
/* 도장형 배지: 1px currentColor 테두리 + 옅은 바탕 */
.custom-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-family: inherit;
  font-size: 12px;
  font-weight: 600;
  line-height: 20px;
  padding: 0 7px;
  white-space: nowrap;
  user-select: none;
  border-radius: 2px;
  border: 1px solid currentColor;
}

/* pill: v2 에서는 쓰지 않는다 (props 호환을 위해 클래스만 유지, 모서리 2px 동일) */

/* 크기 */
.size-small {
  font-size: 11px;
  line-height: 16px;
  padding: 0 5px;
}
.size-medium {
  font-size: 12px;
  line-height: 20px;
  padding: 0 7px;
}

/* 도트 인디케이터 */
.badge-dot-indicator {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

/* 변형별 색상: 성공/정상 pine, 경고 ochre, 오류 stamp, 중립 ink-3 */
.badge-primary {
  background: var(--ds-pine-100);
  color: var(--ds-pine-700);
}

.badge-success {
  background: var(--ds-pine-100);
  color: var(--ds-pine-700);
}

.badge-warning {
  background: var(--ds-sign-tint);
  color: var(--ds-ochre);
}

.badge-danger {
  background: var(--ds-stamp-tint);
  color: var(--ds-stamp);
}

.badge-info {
  background: var(--ds-pine-050);
  color: var(--ds-pine-500);
}

.badge-neutral {
  background: var(--ds-surface-muted);
  color: var(--ds-ink-3);
}

/* 아웃라인 모드 */
.is-outline {
  background: var(--ds-surface);
}
</style>
