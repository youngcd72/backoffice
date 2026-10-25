<template>
  <div class="custom-data-table-wrap">
    <!-- 테이블 본체 -->
    <div class="table-container">
      <table class="custom-table" :class="{ 'is-dense': dense }">
        <thead>
          <tr v-for="(headerRow, hIdx) in headerRows" :key="hIdx">
            <!-- 행 상태(_flag) 컬럼: 헤더 없음 -->
            <th v-if="flagColumn && hIdx === 0" class="th-flag" :rowspan="headerRows.length" aria-label="행 상태"></th>

            <!-- 전체 선택 체크박스 -->
            <th v-if="selectable && hIdx === 0" class="th-checkbox" :rowspan="headerRows.length">
              <input
                type="checkbox"
                :checked="isAllSelected"
                :indeterminate.prop="isIndeterminate"
                @change="toggleSelectAll"
                class="table-checkbox"
              />
            </th>

            <!-- 그룹 헤더 (columns[].group) -->
            <template v-for="cell in headerRow" :key="cell.key">
              <th
                v-if="cell.isGroup"
                class="th-group"
                :colspan="cell.colspan"
              >
                {{ cell.label }}
              </th>

              <!-- 컬럼 헤더 -->
              <th
                v-else
                :rowspan="cell.rowspan"
                :style="{ width: cell.col.width, textAlign: cell.col.align || 'left' }"
                :class="{ 'is-sortable': cell.col.sortable }"
                @click="handleSort(cell.col)"
              >
                <div class="th-content" :style="{ justifyContent: getJustifyContent(cell.col.align) }">
                  <slot :name="`header-${cell.col.key}`" :col="cell.col">
                    <span>{{ cell.col.label }}</span>
                  </slot>
                  <span v-if="cell.col.sortable" class="sort-icon">
                    <svg 
                      v-if="sortKey === cell.col.key && sortOrder === 'asc'" 
                      viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round"
                    >
                      <polyline points="18 15 12 9 6 15"></polyline>
                    </svg>
                    <svg 
                      v-else-if="sortKey === cell.col.key && sortOrder === 'desc'" 
                      viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round"
                    >
                      <polyline points="6 9 12 15 18 9"></polyline>
                    </svg>
                    <svg 
                      v-else 
                      viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none" opacity="0.3"
                    >
                      <path d="M7 15l5 5 5-5M7 9l5-5 5 5"></path>
                    </svg>
                  </span>
                </div>
              </th>
            </template>
          </tr>
        </thead>

        <tbody>
          <!-- 로딩 상태 -->
          <tr v-if="loading" class="state-row">
            <td :colspan="totalColumns" class="state-cell">
              <div class="loading-wrap">
                <span class="table-spinner"></span>
                <span>{{ msg('cm.info.loading') }}</span>
              </div>
            </td>
          </tr>

          <!-- 빈 데이터 상태 -->
          <tr v-else-if="paginatedData.length === 0" class="state-row">
            <td :colspan="totalColumns" class="state-cell">
              <div class="empty-wrap">
                <p>{{ emptyText ?? msg('cm.info.no.data') }}</p>
              </div>
            </td>
          </tr>

          <!-- 데이터 행 -->
          <tr
            v-else
            v-for="(row, rIdx) in paginatedData"
            :key="row.id || rIdx"
            class="table-row"
            :class="[
              { 'is-selected': isRowSelected(row), 'is-active': activeRow !== null && row === activeRow },
              flagColumn && row[flagKey] ? `is-flag-${row[flagKey]}` : '',
            ]"
            @click="handleRowClick(row)"
            @dblclick="handleRowDblClick(row)"
          >
            <!-- 행 상태(_flag) 색상 표시 -->
            <td
              v-if="flagColumn"
              class="td-flag"
              :title="FLAG_LABEL[row[flagKey]] || ''"
            ></td>

            <!-- 행 선택 체크박스 -->
            <td v-if="selectable" class="td-checkbox" @click.stop>
              <input
                type="checkbox"
                :checked="isRowSelected(row)"
                @change="toggleSelectRow(row)"
                class="table-checkbox"
              />
            </td>

            <!-- 셀 데이터 -->
            <td
              v-for="col in columns"
              :key="col.key"
              :style="{ textAlign: col.align || 'left' }"
            >
              <slot 
                :name="`cell-${col.key}`" 
                :row="row" 
                :value="row[col.key]" 
                :index="rIdx"
              >
                {{ row[col.key] !== undefined && row[col.key] !== null ? row[col.key] : '-' }}
              </slot>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 페이지네이션 바 -->
    <div v-if="pagination && totalPages > 1" class="table-pagination">
      <div class="pagination-info">
        총 <strong>{{ sortedData.length }}</strong> 건 중 
        {{ (currentPage - 1) * pageSize + 1 }} ~ {{ Math.min(currentPage * pageSize, sortedData.length) }}
      </div>

      <div class="pagination-controls">
        <button
          type="button"
          class="page-nav-btn"
          :disabled="currentPage === 1"
          @click="goToPage(currentPage - 1)"
        >
          &lt;
        </button>

        <button
          v-for="p in totalPages"
          :key="p"
          type="button"
          class="page-number-btn"
          :class="{ 'is-active': p === currentPage }"
          @click="goToPage(p)"
        >
          {{ p }}
        </button>

        <button
          type="button"
          class="page-nav-btn"
          :disabled="currentPage === totalPages"
          @click="goToPage(currentPage + 1)"
        >
          &gt;
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
/*****************************************************************************************
	Program ID   : DataTable
	Program Type : vue
	Program Name : 데이터 테이블
	Description  : 정렬, 페이징, 행 선택(체크박스)을 지원하는 공통 그리드
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
import { msg } from '@/utils/message';
import { ref, computed } from 'vue';

const props = defineProps({
  columns: {
    type: Array,
    required: true
  },
  data: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  emptyText: {
    type: String,
    default: undefined
  },
  selectable: {
    type: Boolean,
    default: false
  },
  selectedRows: {
    type: Array,
    default: () => []
  },
  pagination: {
    type: Boolean,
    default: true
  },
  pageSize: {
    type: Number,
    default: 5
  },
  // 행 상태 컬럼 표시: row[flagKey] 가 I(등록)·U(수정)·D(삭제)면 색상으로 구분
  flagColumn: {
    type: Boolean,
    default: false
  },
  flagKey: {
    type: String,
    default: '_flag'
  },
  // 셀 여백을 줄여 많은 컬럼을 표시
  dense: {
    type: Boolean,
    default: false
  },
  // 현재 작업 중인 행 강조 (row-click 으로 선택한 행 등)
  activeRow: {
    type: Object,
    default: null
  }
});

const emit = defineEmits(['update:selectedRows', 'row-click', 'row-dblclick', 'sort-change']);

const FLAG_LABEL = { I: '등록', U: '수정', D: '삭제' };

const currentPage = ref(1);
const sortKey = ref('');
const sortOrder = ref(''); // 'asc' | 'desc' | ''

// 헤더 행 구성: columns[].group 이 있으면 2단 헤더(그룹 + 컬럼), 없으면 1단
const hasGroups = computed(() => props.columns.some((col) => col.group));

const headerRows = computed(() => {
  if (!hasGroups.value) {
    return [props.columns.map((col) => ({ key: col.key, isGroup: false, col, rowspan: 1 }))];
  }
  const top = [];
  const bottom = [];
  props.columns.forEach((col) => {
    if (!col.group) {
      top.push({ key: col.key, isGroup: false, col, rowspan: 2 });
      return;
    }
    const last = top[top.length - 1];
    if (last && last.isGroup && last.label === col.group) {
      last.colspan += 1;
    } else {
      top.push({ key: `group-${col.key}`, isGroup: true, label: col.group, colspan: 1 });
    }
    bottom.push({ key: col.key, isGroup: false, col, rowspan: 1 });
  });
  return [top, bottom];
});

const totalColumns = computed(() => {
  return props.columns.length + (props.selectable ? 1 : 0) + (props.flagColumn ? 1 : 0);
});

/**
 * getJustifyContent : 컬럼 정렬 값을 flex 의 justify-content 값으로 변환
 * @param   align - 정렬 (left, center, right)
 * @return  justify-content 값
 */
const getJustifyContent = (align) => {
  if (align === 'center') return 'center';
  if (align === 'right') return 'flex-end';
  return 'flex-start';
};

// 정렬
/**
 * handleSort : 헤더 클릭 시 정렬 상태를 오름차순 -> 내림차순 -> 해제 순으로 전환하고 sort-change 이벤트를 발생
 * @param   col - 클릭된 컬럼 정의
 * @return  N/A
 */
const handleSort = (col) => {
  if (!col.sortable) return;
  if (sortKey.value !== col.key) {
    sortKey.value = col.key;
    sortOrder.value = 'asc';
  } else if (sortOrder.value === 'asc') {
    sortOrder.value = 'desc';
  } else {
    sortKey.value = '';
    sortOrder.value = '';
  }
  emit('sort-change', { key: sortKey.value, order: sortOrder.value });
};

const sortedData = computed(() => {
  if (!sortKey.value || !sortOrder.value) {
    return props.data;
  }
  const key = sortKey.value;
  const isAsc = sortOrder.value === 'asc';
  return [...props.data].sort((a, b) => {
    const va = a[key];
    const vb = b[key];
    if (va === vb) return 0;
    if (va === null || va === undefined) return 1;
    if (vb === null || vb === undefined) return -1;
    if (typeof va === 'number' && typeof vb === 'number') {
      return isAsc ? va - vb : vb - va;
    }
    return isAsc 
      ? String(va).localeCompare(String(vb)) 
      : String(vb).localeCompare(String(va));
  });
});

// 페이지네이션
const totalPages = computed(() => {
  if (!props.pagination) return 1;
  return Math.ceil(sortedData.value.length / props.pageSize) || 1;
});

const paginatedData = computed(() => {
  if (!props.pagination) return sortedData.value;
  const start = (currentPage.value - 1) * props.pageSize;
  return sortedData.value.slice(start, start + props.pageSize);
});

/**
 * goToPage : 유효한 범위의 페이지로 이동
 * @param   p - 이동할 페이지 번호 (1부터)
 * @return  N/A
 */
const goToPage = (p) => {
  if (p >= 1 && p <= totalPages.value) {
    currentPage.value = p;
  }
};

// 체크박스 선택 제어
/**
 * isRowSelected : 행이 선택된 상태인지 판별
 * @param   row - 확인할 행 데이터
 * @return  선택 여부
 */
const isRowSelected = (row) => {
  return props.selectedRows.includes(row);
};

/**
 * toggleSelectRow : 행 선택 상태를 토글하고 update:selectedRows 이벤트를 발생
 * @param   row - 토글할 행 데이터
 * @return  N/A
 */
const toggleSelectRow = (row) => {
  const current = [...props.selectedRows];
  const idx = current.indexOf(row);
  if (idx > -1) {
    current.splice(idx, 1);
  } else {
    current.push(row);
  }
  emit('update:selectedRows', current);
};

const isAllSelected = computed(() => {
  if (paginatedData.value.length === 0) return false;
  return paginatedData.value.every((row) => props.selectedRows.includes(row));
});

const isIndeterminate = computed(() => {
  const selectedCount = paginatedData.value.filter((row) => props.selectedRows.includes(row)).length;
  return selectedCount > 0 && selectedCount < paginatedData.value.length;
});

/**
 * toggleSelectAll : 현재 페이지의 행을 전체 선택하거나 전체 해제
 * @return  N/A
 */
const toggleSelectAll = () => {
  let next = [...props.selectedRows];
  if (isAllSelected.value) {
    // 현재 페이지 항목 해제
    next = next.filter((r) => !paginatedData.value.includes(r));
  } else {
    // 현재 페이지 항목 추가
    paginatedData.value.forEach((r) => {
      if (!next.includes(r)) next.push(r);
    });
  }
  emit('update:selectedRows', next);
};

/**
 * handleRowClick : 행 클릭 시 row-click 이벤트를 발생
 * @param   row - 클릭된 행 데이터
 * @return  N/A
 */
const handleRowClick = (row) => {
  emit('row-click', row);
};

/**
 * handleRowDblClick : 행 더블클릭 시 row-dblclick 이벤트를 발생
 * @param   row - 더블클릭된 행 데이터
 * @return  N/A
 */
const handleRowDblClick = (row) => {
  emit('row-dblclick', row);
};
</script>

<style scoped>
.custom-data-table-wrap {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-family: inherit;
  color: var(--ds-ink);
}

.table-container {
  overflow-x: auto;
  border-radius: var(--ds-radius-panel);
  border: 1px solid var(--ds-rule-strong);
  background: var(--ds-surface);
}

.custom-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}

/* 테이블 헤더 */
.custom-table thead tr {
  background: var(--ds-pine-100);
  border-bottom: 1px solid var(--ds-rule-strong);
}

.custom-table th {
  padding: 10px 12px;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ds-pine-900);
  white-space: nowrap;
}

.th-group {
  text-align: center;
  border-bottom: 1px solid var(--ds-rule-strong);
}

.th-content {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  width: 100%;
}

.custom-table th.is-sortable {
  cursor: pointer;
  user-select: none;
  transition: color var(--ds-ease);
}

.custom-table th.is-sortable:hover {
  color: var(--ds-pine-500);
}

.sort-icon {
  display: flex;
  align-items: center;
  color: var(--ds-pine-500);
}

/* 테이블 바디 셀 */
.custom-table td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--ds-rule);
  color: var(--ds-ink);
}

.table-row {
  transition: background-color var(--ds-ease);
  cursor: pointer;
}

.table-row:hover {
  background: var(--ds-pine-050);
}

.table-row.is-selected {
  background: var(--ds-pine-100);
}

.custom-table.is-dense th,
.custom-table.is-dense td {
  padding: 7px 8px;
}

/* 활성 행: 사인 옐로 바 + 옅은 옐로 바탕 */
.table-row.is-active {
  background: var(--ds-sign-tint);
}

.table-row.is-active > td:first-child {
  box-shadow: inset 4px 0 0 var(--ds-sign);
}

/* 행 상태(_flag) 컬럼 */
.custom-table th.th-flag,
.custom-table td.td-flag,
.custom-table.is-dense th.th-flag,
.custom-table.is-dense td.td-flag {
  width: 3px;
  min-width: 3px;
  max-width: 3px;
  padding: 0;
}

.table-row.is-flag-I .td-flag {
  background: var(--ds-pine-500);
}

.table-row.is-flag-U .td-flag {
  background: var(--ds-sign);
}

.table-row.is-flag-D .td-flag {
  background: var(--ds-stamp);
}

/* 삭제 표시 행: 저장 전까지 편집 불가 (행 클릭·선택은 가능) */
.table-row.is-flag-D td:not(.td-checkbox):not(.td-flag) {
  opacity: 0.5;
}

.table-row.is-flag-D td:not(.td-checkbox):not(.td-flag) * {
  pointer-events: none;
}

/* 체크박스 열 */
.th-checkbox, .td-checkbox {
  width: 44px;
  text-align: center !important;
}

.table-checkbox {
  accent-color: var(--ds-pine-700);
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.table-checkbox:focus-visible {
  outline: none;
  box-shadow: var(--ds-focus);
}

/* 상태 행 (로딩, 빈 데이터) */
.state-cell {
  padding: 40px 16px !important;
  text-align: center;
}

.loading-wrap, .empty-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: var(--ds-ink-3);
}

.table-spinner {
  width: 24px;
  height: 24px;
  border: 2px solid var(--ds-rule);
  border-top-color: var(--ds-pine-700);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 페이지네이션 */
.table-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 6px;
  font-size: 12.5px;
  color: var(--ds-ink-3);
  font-variant-numeric: tabular-nums;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-nav-btn, .page-number-btn {
  background: var(--ds-surface);
  border: 1px solid var(--ds-rule);
  border-radius: var(--ds-radius-control);
  color: var(--ds-ink-2);
  min-width: 28px;
  height: 28px;
  padding: 0 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12.5px;
  font-family: inherit;
  font-variant-numeric: tabular-nums;
  transition: background-color var(--ds-ease), border-color var(--ds-ease), color var(--ds-ease);
}

.page-nav-btn:hover:not(:disabled), .page-number-btn:hover:not(.is-active) {
  background: var(--ds-pine-050);
  border-color: var(--ds-rule-strong);
}

.page-nav-btn:focus-visible, .page-number-btn:focus-visible {
  outline: none;
  box-shadow: var(--ds-focus);
}

.page-number-btn.is-active {
  background: var(--ds-pine-700);
  color: var(--ds-ink-inverse);
  font-weight: 600;
  border-color: var(--ds-pine-700);
}

.page-nav-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>
