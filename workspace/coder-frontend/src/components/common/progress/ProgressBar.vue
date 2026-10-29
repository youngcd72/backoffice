<template>
  <div 
    class="custom-progress-wrap"
    :class="[
      `size-${size}`,
      {
        'has-outside-label': showLabel && labelPosition === 'outside'
      }
    ]"
  >
    <!-- 상단 외부 라벨 및 백분율 (labelPosition === 'outside'일 때) -->
    <div v-if="showLabel && labelPosition === 'outside'" class="progress-outer-header">
      <span class="progress-title">
        <slot name="label-text">{{ label }}</slot>
      </span>
      <span class="progress-percent">
        <slot name="label" :percentage="clampedPercentage" :value="value" :max="max">
          {{ clampedPercentage }}%
        </slot>
      </span>
    </div>

    <!-- 프로그레스 바 트랙 (배경 레일) -->
    <div 
      class="progress-track"
      :class="{ 'is-rounded': rounded }"
      :style="{ height: customHeight }"
      role="progressbar"
      :aria-valuenow="indeterminate ? undefined : clampedPercentage"
      :aria-valuemin="0"
      :aria-valuemax="max"
    >
      <!-- 프로그레스 바 채우기 (Fill) -->
      <div 
        class="progress-bar-fill"
        :class="[
          `variant-${variant}`,
          {
            'is-striped': striped || animated,
            'is-animated': animated,
            'is-indeterminate': indeterminate
          }
        ]"
        :style="fillStyle"
      >
        <!-- 내부 라벨 (labelPosition === 'inside'이고 높이가 충분할 때) -->
        <span 
          v-if="showLabel && labelPosition === 'inside' && !indeterminate && clampedPercentage >= 10" 
          class="progress-inner-label"
        >
          <slot name="label" :percentage="clampedPercentage" :value="value" :max="max">
            {{ clampedPercentage }}%
          </slot>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
/*****************************************************************************************
	Program ID   : ProgressBar
	Program Type : vue
	Program Name : 진행률 바
	Description  : 진행률을 막대로 표시하는 공통 프로그레스 바
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-09-07  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
import { computed } from 'vue';

const props = defineProps({
  value: {
    type: Number,
    default: 0
  },
  max: {
    type: Number,
    default: 100
  },
  label: {
    type: String,
    default: ''
  },
  variant: {
    type: String,
    default: 'primary', // 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'gradient'
    validator: (v) => ['primary', 'success', 'warning', 'danger', 'info', 'gradient'].includes(v)
  },
  size: {
    type: String,
    default: 'medium', // 'small' | 'medium' | 'large'
    validator: (v) => ['small', 'medium', 'large'].includes(v)
  },
  height: {
    type: String,
    default: ''
  },
  showLabel: {
    type: Boolean,
    default: false
  },
  labelPosition: {
    type: String,
    default: 'outside', // 'outside' | 'inside'
    validator: (v) => ['outside', 'inside'].includes(v)
  },
  striped: {
    type: Boolean,
    default: false
  },
  animated: {
    type: Boolean,
    default: false
  },
  indeterminate: {
    type: Boolean,
    default: false
  },
  rounded: {
    type: Boolean,
    default: true
  },
  color: {
    type: String,
    default: ''
  }
});

const clampedPercentage = computed(() => {
  if (props.max <= 0) return 0;
  const pct = Math.round((props.value / props.max) * 100);
  return Math.min(Math.max(pct, 0), 100);
});

const customHeight = computed(() => {
  if (props.height) return props.height;
  return undefined;
});

const fillStyle = computed(() => {
  const style = {};
  if (!props.indeterminate) {
    style.width = `${clampedPercentage.value}%`;
  }
  if (props.color) {
    style.background = props.color;
  }
  return style;
});
</script>

<style scoped>
.custom-progress-wrap {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-family: inherit;
  user-select: none;
}

/* 크기별 기본 높이 */
.size-small { --prog-height: 6px; }
.size-medium { --prog-height: 10px; }
.size-large { --prog-height: 18px; }

/* 외부 라벨 헤더 */
.progress-outer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.2;
}

.progress-title {
  color: var(--ds-ink-2);
}

.progress-percent {
  color: var(--ds-ink);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

/* 프로그레스 바 트랙 (배경) */
.progress-track {
  width: 100%;
  height: var(--prog-height);
  background: var(--ds-surface-muted);
  border: 1px solid var(--ds-rule);
  overflow: hidden;
  position: relative;
  border-radius: 0;
}

.progress-track.is-rounded {
  border-radius: 2px;
}

/* 프로그레스 바 채우기 (Fill) */
.progress-bar-fill {
  height: 100%;
  background: var(--ds-slate-700);
  transition: width 0.4s ease;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

/* 내부 라벨 */
.progress-inner-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--ds-ink-inverse);
  line-height: 1;
  padding: 0 6px;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

/* 변형별 색상: 단색만 */
.variant-primary,
.variant-success,
.variant-gradient {
  background: var(--ds-slate-700);
}

.variant-info {
  background: var(--ds-slate-500);
}

.variant-warning {
  background: var(--ds-ochre);
}

.variant-danger {
  background: var(--ds-stamp);
}

/* striped / animated: v2 에서는 빗살무늬를 쓰지 않는다 (props 호환을 위해 클래스만 유지) */

/* 무한 로딩 상태 (Indeterminate): 로딩 표시이므로 이동 애니메이션 유지 */
.is-indeterminate {
  width: 40% !important;
  animation: indeterminateFlow 1.2s infinite ease-in-out !important;
}

@keyframes indeterminateFlow {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(250%);
  }
}
</style>
