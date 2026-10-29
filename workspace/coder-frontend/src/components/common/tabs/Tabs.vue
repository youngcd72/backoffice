<template>
  <div 
    class="custom-tabs-container"
    :class="[
      `variant-${variant}`,
      `size-${size}`,
      { 'is-block': block }
    ]"
  >
    <!-- 탭 헤더 목록 -->
    <div 
      class="tabs-header-bar" 
      role="tablist"
      @keydown="handleKeyNavigation"
    >
      <button
        v-for="tab in normalizedItems"
        :key="tab.key"
        type="button"
        role="tab"
        :id="`tab-${tab.key}`"
        :aria-selected="isSelected(tab.key)"
        :aria-controls="`panel-${tab.key}`"
        :tabindex="isSelected(tab.key) ? 0 : -1"
        :disabled="tab.disabled"
        class="tab-nav-item"
        :class="{
          'is-active': isSelected(tab.key),
          'is-disabled': tab.disabled
        }"
        @click="selectTab(tab)"
      >
        <!-- 탭 아이콘 (슬롯 또는 prop) -->
        <span v-if="tab.icon || $slots[`icon-${tab.key}`]" class="tab-icon">
          <slot :name="`icon-${tab.key}`">
            {{ tab.icon }}
          </slot>
        </span>

        <!-- 탭 라벨 (슬롯 또는 텍스트) -->
        <span class="tab-label">
          <slot :name="`label-${tab.key}`" :tab="tab">
            {{ tab.label }}
          </slot>
        </span>

        <!-- 탭 뱃지/카운트 -->
        <span 
          v-if="tab.badge !== undefined && tab.badge !== null" 
          class="tab-badge"
          :class="[`badge-${tab.badgeVariant || 'primary'}`]"
        >
          {{ tab.badge }}
        </span>
      </button>
    </div>

    <!-- 탭 본문 내용 (패널) -->
    <div class="tabs-body">
      <!-- 1. 항목별 개별 슬롯 매칭 (예: <template #profile>) -->
      <template v-for="tab in normalizedItems" :key="tab.key">
        <div
          v-if="isSelected(tab.key)"
          :id="`panel-${tab.key}`"
          role="tabpanel"
          :aria-labelledby="`tab-${tab.key}`"
          class="tab-panel"
        >
          <slot :name="tab.key">
            <!-- 기본 슬롯이 제공된 경우 fallback -->
            <slot :active-tab="modelValue" :tab="tab"></slot>
          </slot>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
/*****************************************************************************************
	Program ID   : Tabs
	Program Type : vue
	Program Name : 탭
	Description  : 키보드 이동을 지원하는 공통 탭 컴포넌트
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
  modelValue: {
    type: [String, Number],
    required: true
  },
  items: {
    type: Array,
    default: () => []
  },
  variant: {
    type: String,
    default: 'line', // 'line' | 'pill' | 'card'
    validator: (v) => ['line', 'pill', 'card'].includes(v)
  },
  size: {
    type: String,
    default: 'medium', // 'small' | 'medium' | 'large'
    validator: (v) => ['small', 'medium', 'large'].includes(v)
  },
  block: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['update:modelValue', 'change']);

// 항목 정규화
const normalizedItems = computed(() => {
  return props.items.map((item) => {
    if (typeof item === 'object' && item !== null) {
      return {
        key: item.key !== undefined ? item.key : item.value,
        label: item.label || String(item.key || item.value),
        icon: item.icon || '',
        badge: item.badge,
        badgeVariant: item.badgeVariant || 'primary',
        disabled: Boolean(item.disabled)
      };
    }
    return {
      key: item,
      label: String(item),
      icon: '',
      badge: null,
      badgeVariant: 'primary',
      disabled: false
    };
  });
});

/**
 * isSelected : 해당 탭이 현재 선택되어 있는지 판별
 * @param   key - 확인할 탭 키
 * @return  선택 여부
 */
const isSelected = (key) => {
  return props.modelValue === key;
};

/**
 * selectTab : 탭 선택 시 값을 갱신하고 change 이벤트를 발생 (비활성 탭은 무시)
 * @param   tab - 선택된 탭
 * @return  N/A
 */
const selectTab = (tab) => {
  if (tab.disabled) return;
  if (props.modelValue !== tab.key) {
    emit('update:modelValue', tab.key);
    emit('change', tab.key);
  }
};

// 키보드 좌/우 방향키 탭 이동
/**
 * handleKeyNavigation : 좌/우 방향키, Home, End 키로 사용 가능한 탭 사이를 이동
 * @param   e - 키보드 이벤트
 * @return  N/A
 */
const handleKeyNavigation = (e) => {
  if (!['ArrowRight', 'ArrowLeft', 'Home', 'End'].includes(e.key)) return;
  
  const enabledTabs = normalizedItems.value.filter((t) => !t.disabled);
  if (enabledTabs.length === 0) return;

  const currentIdx = enabledTabs.findIndex((t) => t.key === props.modelValue);
  let nextIdx = 0;

  if (e.key === 'ArrowRight') {
    nextIdx = (currentIdx + 1) % enabledTabs.length;
  } else if (e.key === 'ArrowLeft') {
    nextIdx = (currentIdx - 1 + enabledTabs.length) % enabledTabs.length;
  } else if (e.key === 'Home') {
    nextIdx = 0;
  } else if (e.key === 'End') {
    nextIdx = enabledTabs.length - 1;
  }

  const nextTab = enabledTabs[nextIdx];
  if (nextTab) {
    e.preventDefault();
    selectTab(nextTab);
    const nextBtn = document.getElementById(`tab-${nextTab.key}`);
    nextBtn?.focus();
  }
};
</script>

<style scoped>
.custom-tabs-container {
  display: flex;
  flex-direction: column;
  width: 100%;
  font-family: inherit;
  --tab-primary: var(--ds-slate-700);
  --tab-active-text: var(--ds-slate-900);
  --tab-text: var(--ds-ink-2);
  --tab-hover-text: var(--ds-ink);
  --tab-border: var(--ds-rule-strong);
}

/* 크기 변형 */
.size-small {
  --tab-height: var(--ds-control-height-sm);
  --tab-font-size: 13px;
  --tab-padding: 0 12px;
  --tab-gap: 2px;
}
.size-medium {
  --tab-height: 38px;
  --tab-font-size: 14px;
  --tab-padding: 0 16px;
  --tab-gap: 2px;
}
.size-large {
  --tab-height: 44px;
  --tab-font-size: 15px;
  --tab-padding: 0 20px;
  --tab-gap: 4px;
}

/* 탭 헤더 바 공통 */
.tabs-header-bar {
  display: flex;
  align-items: center;
  gap: var(--tab-gap);
  user-select: none;
  overflow-x: auto;
  scrollbar-width: none;
}
.tabs-header-bar::-webkit-scrollbar {
  display: none;
}

.is-block .tabs-header-bar {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: 1fr;
}

.tab-nav-item {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: var(--tab-height);
  padding: var(--tab-padding);
  font-size: var(--tab-font-size);
  font-weight: 500;
  color: var(--tab-text);
  background: none;
  border: none;
  border-radius: 0;
  cursor: pointer;
  white-space: nowrap;
  transition: background-color var(--ds-ease), color var(--ds-ease);
  outline: none;
  font-family: inherit;
}

.tab-nav-item:hover:not(.is-disabled):not(.is-active) {
  color: var(--tab-hover-text);
  background: var(--ds-slate-050);
}

.tab-nav-item:focus-visible {
  box-shadow: var(--ds-focus);
  z-index: 1;
}

.tab-nav-item.is-disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.tab-icon {
  display: flex;
  align-items: center;
  font-size: 15px;
}

.tab-label {
  line-height: 1;
}

/* 뱃지 (도장형) */
.tab-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 0 5px;
  line-height: 16px;
  border: 1px solid currentColor;
  border-radius: 2px;
  font-variant-numeric: tabular-nums;
}

.badge-primary {
  background: var(--ds-slate-100);
  color: var(--ds-slate-700);
}

.badge-danger {
  background: var(--ds-stamp-tint);
  color: var(--ds-stamp);
}

.badge-success {
  background: var(--ds-slate-050);
  color: var(--ds-slate-500);
}

/* ============================================================
   1. Variant: Line (하단 포일 바 - 기본형)
============================================================ */
.variant-line .tabs-header-bar {
  border-bottom: 1px solid var(--tab-border);
}

.variant-line .tab-nav-item.is-active {
  background: var(--ds-surface);
  color: var(--tab-active-text);
  font-weight: 600;
  box-shadow: inset 0 -3px 0 var(--ds-foil);
}

.variant-line .tab-nav-item.is-active:focus-visible {
  box-shadow: inset 0 -3px 0 var(--ds-foil), var(--ds-focus);
}

/* ============================================================
   2. Variant: Pill (세그먼트 형태)
============================================================ */
.variant-pill .tabs-header-bar {
  background: var(--ds-surface-muted);
  padding: 3px;
  border-radius: var(--ds-radius-panel);
  border: 1px solid var(--ds-rule);
  width: fit-content;
}

.is-block.variant-pill .tabs-header-bar {
  width: 100%;
}

.variant-pill .tab-nav-item {
  border-radius: var(--ds-radius-control);
}

.variant-pill .tab-nav-item.is-active {
  background: var(--ds-slate-700);
  color: var(--ds-ink-inverse);
  font-weight: 600;
}

/* ============================================================
   3. Variant: Card (폴더 탭 형태 - 상단 포일 바)
============================================================ */
.variant-card .tabs-header-bar {
  border-bottom: 1px solid var(--tab-border);
  align-items: flex-end;
}

.variant-card .tab-nav-item {
  border: 1px solid transparent;
  border-bottom: none;
  border-radius: var(--ds-radius-control) var(--ds-radius-control) 0 0;
  background: transparent;
}

.variant-card .tab-nav-item.is-active {
  background: var(--ds-surface);
  border-color: var(--tab-border);
  color: var(--tab-active-text);
  font-weight: 600;
  box-shadow: inset 0 3px 0 var(--ds-foil);
}

.variant-card .tab-nav-item.is-active:focus-visible {
  box-shadow: inset 0 3px 0 var(--ds-foil), var(--ds-focus);
}

/* 탭 바디 영역 */
.tabs-body {
  padding-top: 16px;
  flex: 1;
}
</style>
