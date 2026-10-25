<template>
  <div 
    class="custom-listbox-wrap" 
    :class="{ 
      'is-disabled': disabled,
      'is-multiple': multiple 
    }"
  >
    <!-- 상단 라벨 및 선택 카운트 헤더 -->
    <div v-if="label || (multiple && showCount)" class="listbox-header">
      <label v-if="label" class="listbox-label">{{ label }}</label>
      <div v-if="multiple && showCount" class="count-badge">
        <span class="selected-num">{{ selectedCount }}</span> / {{ normalizedOptions.length }}
      </div>
    </div>

    <!-- 검색창 필터 (searchable 옵션) -->
    <div v-if="searchable" class="listbox-search">
      <span class="search-icon">
        <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2.2" fill="none" stroke-linecap="round">
          <circle cx="11" cy="11" r="8"></circle>
          <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
        </svg>
      </span>
      <input
        type="text"
        v-model="searchQuery"
        class="search-input"
        placeholder="목록 검색..."
        :disabled="disabled"
      />
      <button 
        v-if="searchQuery" 
        type="button" 
        class="search-clear-btn" 
        @click="searchQuery = ''"
      >
        ×
      </button>
    </div>

    <!-- 전체 선택 / 전체 해제 빠른 액션 바 (다중 선택 모드일 때) -->
    <div v-if="multiple && showActionToolbar" class="listbox-toolbar">
      <button 
        type="button" 
        class="toolbar-btn" 
        :disabled="disabled || isAllSelected"
        @click="selectAll"
      >
        전체 선택
      </button>
      <span class="divider">|</span>
      <button 
        type="button" 
        class="toolbar-btn" 
        :disabled="disabled || selectedCount === 0"
        @click="clearAll"
      >
        선택 해제
      </button>
    </div>

    <!-- 메인 리스트 박스 컨테이너 -->
    <div 
      class="listbox-body" 
      :style="{ height: height, maxHeight: maxHeight }"
      role="listbox"
      :aria-multiselectable="multiple"
      tabindex="0"
      @keydown="handleKeyDown"
    >
      <!-- 빈 목록 상태 -->
      <div v-if="filteredOptions.length === 0" class="listbox-empty">
        <p>{{ searchQuery ? msg('cm.info.no.search.result') : (emptyText ?? msg('cm.info.no.items')) }}</p>
      </div>

      <!-- 항목 리스트 -->
      <ul v-else class="listbox-list">
        <li
          v-for="(opt, idx) in filteredOptions"
          :key="opt.value"
          role="option"
          :aria-selected="isSelected(opt.value)"
          :aria-disabled="opt.disabled"
          class="listbox-item"
          :class="{
            'is-selected': isSelected(opt.value),
            'is-disabled': opt.disabled,
            'is-focused': focusedIndex === idx
          }"
          @click="handleSelect(opt)"
        >
          <!-- 커스텀 슬롯 지원 -->
          <slot 
            name="item" 
            :option="opt" 
            :selected="isSelected(opt.value)" 
            :index="idx"
          >
            <!-- 다중 선택 시 체크박스 -->
            <span v-if="multiple" class="item-checkbox" :class="{ 'checked': isSelected(opt.value) }">
              <svg v-if="isSelected(opt.value)" viewBox="0 0 24 24" width="12" height="12" stroke="currentColor" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="20 6 9 17 4 12"></polyline>
              </svg>
            </span>

            <!-- 아이콘 -->
            <span v-if="opt.icon" class="item-icon">{{ opt.icon }}</span>

            <!-- 텍스트 & 설명 라벨 -->
            <div class="item-content">
              <span class="item-label">{{ opt.label }}</span>
              <span v-if="opt.description" class="item-desc">{{ opt.description }}</span>
            </div>

            <!-- 단일 선택 시 체크 표시 -->
            <span v-if="!multiple && isSelected(opt.value)" class="check-mark">
              <svg viewBox="0 0 24 24" width="15" height="15" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="20 6 9 17 4 12"></polyline>
              </svg>
            </span>
          </slot>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
/*****************************************************************************************
	Program ID   : Listbox
	Program Type : vue
	Program Name : 리스트박스
	Description  : 단일/다중 선택, 검색, 키보드 이동을 지원하는 공통 리스트박스
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-09-07  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
import { msg } from '@/utils/message';
import { ref, computed } from 'vue';

const props = defineProps({
  modelValue: {
    type: [String, Number, Boolean, Array, Object],
    default: null
  },
  options: {
    type: Array,
    default: () => []
  },
  multiple: {
    type: Boolean,
    default: false
  },
  searchable: {
    type: Boolean,
    default: false
  },
  label: {
    type: String,
    default: ''
  },
  height: {
    type: String,
    default: '220px'
  },
  maxHeight: {
    type: String,
    default: '400px'
  },
  disabled: {
    type: Boolean,
    default: false
  },
  showCount: {
    type: Boolean,
    default: true
  },
  showActionToolbar: {
    type: Boolean,
    default: true
  },
  emptyText: {
    type: String,
    default: undefined
  }
});

const emit = defineEmits(['update:modelValue', 'change', 'item-click']);

const searchQuery = ref('');
const focusedIndex = ref(-1);

// 옵션 배열 표준화
const normalizedOptions = computed(() => {
  return props.options.map((item) => {
    if (typeof item === 'object' && item !== null) {
      return {
        label: item.label !== undefined ? item.label : String(item.value),
        value: item.value !== undefined ? item.value : item,
        description: item.description || '',
        icon: item.icon || '',
        disabled: Boolean(item.disabled)
      };
    }
    return {
      label: String(item),
      value: item,
      description: '',
      icon: '',
      disabled: false
    };
  });
});

// 검색 필터 적용
const filteredOptions = computed(() => {
  if (!searchQuery.value.trim()) return normalizedOptions.value;
  const q = searchQuery.value.trim().toLowerCase();
  return normalizedOptions.value.filter((opt) => {
    return (
      opt.label.toLowerCase().includes(q) ||
      (opt.description && opt.description.toLowerCase().includes(q))
    );
  });
});

// 선택 여부 판별
/**
 * isSelected : 해당 값이 현재 선택되어 있는지 판별
 * @param   val - 확인할 항목 값
 * @return  선택 여부
 */
const isSelected = (val) => {
  if (props.multiple) {
    if (Array.isArray(props.modelValue)) {
      return props.modelValue.includes(val);
    }
    return false;
  }
  return props.modelValue === val;
};

// 선택된 항목 개수
const selectedCount = computed(() => {
  if (props.multiple && Array.isArray(props.modelValue)) {
    return props.modelValue.length;
  }
  return props.modelValue !== null && props.modelValue !== undefined ? 1 : 0;
});

const isAllSelected = computed(() => {
  if (!props.multiple || normalizedOptions.value.length === 0) return false;
  const validOpts = normalizedOptions.value.filter((o) => !o.disabled);
  if (validOpts.length === 0) return false;
  return validOpts.every((o) => isSelected(o.value));
});

// 항목 클릭 처리
/**
 * handleSelect : 항목 클릭 시 단일/다중 선택 값을 갱신하고 이벤트를 발생
 * @param   opt - 클릭된 항목
 * @return  N/A
 */
const handleSelect = (opt) => {
  if (props.disabled || opt.disabled) return;

  emit('item-click', opt);

  if (!props.multiple) {
    emit('update:modelValue', opt.value);
    emit('change', opt.value);
    return;
  }

  // 다중 선택 모드
  let next = Array.isArray(props.modelValue) ? [...props.modelValue] : [];
  const idx = next.indexOf(opt.value);
  if (idx > -1) {
    next.splice(idx, 1);
  } else {
    next.push(opt.value);
  }

  emit('update:modelValue', next);
  emit('change', next);
};

// 전체 선택
/**
 * selectAll : 다중 선택 모드에서 선택 가능한 모든 항목을 선택
 * @return  N/A
 */
const selectAll = () => {
  if (props.disabled || !props.multiple) return;
  const validVals = normalizedOptions.value
    .filter((o) => !o.disabled)
    .map((o) => o.value);
  emit('update:modelValue', validVals);
  emit('change', validVals);
};

// 선택 해제
/**
 * clearAll : 다중 선택 모드에서 선택을 모두 해제
 * @return  N/A
 */
const clearAll = () => {
  if (props.disabled || !props.multiple) return;
  emit('update:modelValue', []);
  emit('change', []);
};

// 키보드 내비게이션
/**
 * handleKeyDown : 방향키/Enter/Space 키로 항목 포커스 이동 및 선택
 * @param   e - 키보드 이벤트
 * @return  N/A
 */
const handleKeyDown = (e) => {
  if (props.disabled || filteredOptions.value.length === 0) return;

  if (e.key === 'ArrowDown') {
    e.preventDefault();
    focusedIndex.value = (focusedIndex.value + 1) % filteredOptions.value.length;
  } else if (e.key === 'ArrowUp') {
    e.preventDefault();
    focusedIndex.value = (focusedIndex.value - 1 + filteredOptions.value.length) % filteredOptions.value.length;
  } else if (e.key === 'Enter' || e.key === ' ') {
    e.preventDefault();
    if (focusedIndex.value >= 0 && focusedIndex.value < filteredOptions.value.length) {
      handleSelect(filteredOptions.value[focusedIndex.value]);
    }
  }
};
</script>

<style scoped>
.custom-listbox-wrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  font-family: inherit;
  user-select: none;
  --lb-bg: var(--ds-surface);
  --lb-item-hover: var(--ds-pine-050);
  --lb-item-active: var(--ds-pine-100);
  --lb-border: var(--ds-rule-strong);
  --lb-border-focus: var(--ds-pine-700);
  --lb-text: var(--ds-ink);
  --lb-text-muted: var(--ds-ink-3);
  --lb-radius: var(--ds-radius-panel);
}

/* 헤더 */
.listbox-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}

.listbox-label {
  font-weight: 600;
  color: var(--ds-ink-2);
}

.count-badge {
  font-size: 12px;
  color: var(--lb-text-muted);
  background: var(--ds-surface-muted);
  padding: 0 7px;
  line-height: 20px;
  border-radius: 2px;
  border: 1px solid var(--ds-rule);
  font-variant-numeric: tabular-nums;
}

.selected-num {
  color: var(--ds-pine-700);
  font-weight: 600;
}

/* 검색창 */
.listbox-search {
  position: relative;
  display: flex;
  align-items: center;
  background: var(--ds-surface);
  border: 1px solid var(--lb-border);
  border-radius: var(--ds-radius-control);
  padding: 0 10px;
  height: var(--ds-control-height-sm);
  transition: border-color var(--ds-ease), box-shadow var(--ds-ease);
}

.listbox-search:hover {
  border-color: var(--ds-ink-3);
}

.listbox-search:focus-within {
  border-color: var(--lb-border-focus);
  box-shadow: var(--ds-focus-field);
}

.search-icon {
  display: flex;
  align-items: center;
  color: var(--lb-text-muted);
  margin-right: 6px;
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: var(--lb-text);
  font-size: 13px;
  font-family: inherit;
}

.search-input::placeholder {
  color: var(--lb-text-muted);
  opacity: 1;
}

.search-clear-btn {
  background: none;
  border: none;
  color: var(--lb-text-muted);
  font-size: 16px;
  cursor: pointer;
  padding: 0 4px;
  border-radius: 2px;
}

.search-clear-btn:hover {
  color: var(--ds-stamp);
}

.search-clear-btn:focus-visible {
  box-shadow: var(--ds-focus);
}

/* 툴바 */
.listbox-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.toolbar-btn {
  background: none;
  border: none;
  color: var(--ds-pine-700);
  font-size: 12px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  padding: 0;
  border-radius: 2px;
  transition: color var(--ds-ease);
}

.toolbar-btn:hover:not(:disabled) {
  color: var(--ds-pine-900);
  text-decoration: underline;
}

.toolbar-btn:focus-visible {
  box-shadow: var(--ds-focus);
}

.toolbar-btn:disabled {
  color: var(--ds-ink-3);
  cursor: not-allowed;
}

.divider {
  color: var(--ds-rule-strong);
}

/* 바디 컨테이너 */
.listbox-body {
  background: var(--lb-bg);
  border: 1px solid var(--lb-border);
  border-radius: var(--lb-radius);
  overflow-y: auto;
  padding: 4px 0;
  outline: none;
  transition: border-color var(--ds-ease), box-shadow var(--ds-ease);
}

.listbox-body:focus-visible {
  border-color: var(--lb-border-focus);
  box-shadow: var(--ds-focus-field);
}

/* 리스트 및 항목 */
.listbox-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.listbox-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 12px;
  border-radius: 0;
  color: var(--lb-text);
  font-size: 13px;
  cursor: pointer;
  transition: background-color var(--ds-ease), color var(--ds-ease);
}

.listbox-item:hover:not(.is-disabled) {
  background: var(--lb-item-hover);
}

.listbox-item.is-selected,
.listbox-item.is-selected:hover:not(.is-disabled) {
  background: var(--lb-item-active);
  color: var(--ds-pine-900);
  font-weight: 600;
}

.listbox-item.is-focused {
  box-shadow: inset 3px 0 0 var(--ds-sign);
}

.listbox-item.is-disabled {
  color: var(--ds-ink-3);
  cursor: not-allowed;
}

/* 체크박스 (다중 선택 시) */
.item-checkbox {
  width: 16px;
  height: 16px;
  border-radius: 2px;
  border: 1px solid var(--ds-rule-strong);
  background: var(--ds-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background-color var(--ds-ease), border-color var(--ds-ease);
}

.item-checkbox.checked {
  background: var(--ds-pine-700);
  border-color: var(--ds-pine-700);
  color: var(--ds-ink-inverse);
}

.item-icon {
  font-size: 15px;
  flex-shrink: 0;
}

.item-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.item-label {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-desc {
  font-size: 11px;
  font-weight: 400;
  color: var(--lb-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.check-mark {
  color: var(--ds-pine-700);
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

/* 빈 상태 */
.listbox-empty {
  padding: 30px 10px;
  text-align: center;
  color: var(--lb-text-muted);
  font-size: 13px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

/* 비활성화 */
.is-disabled {
  cursor: not-allowed;
}
.is-disabled .listbox-body {
  pointer-events: none;
  background: var(--ds-surface-muted);
  border-color: var(--ds-rule);
}
.is-disabled .listbox-item {
  color: var(--ds-ink-3);
}
</style>
