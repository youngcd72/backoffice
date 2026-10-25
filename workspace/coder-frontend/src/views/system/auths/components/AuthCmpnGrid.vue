<script setup lang="ts">
/*****************************************************************************************
	Program ID   : AuthCmpnGrid
	Program Type : vue
	Program Name : 컴포넌트 권한 그리드
	Description  : 선택한 화면(프로그램)의 컴포넌트 목록과 권한여부 체크박스(헤더 전체 선택 포함)
	               사용 화면: SYAU010 사용자권한관리, SYAU020 그룹권한관리
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-20  ywkim     최초생성 (SYAU010, SYAU020 컴포넌트목록 그리드 통합)
------------------------------------------------------------------------------------------
*****************************************************************************************/
import { msg } from '@/utils/message'
import { DataTable } from '@/components/common'
import type { CmpnAuthRow } from '@/composables/useMenuCmpnAuth'

defineProps<{
  /** 그리드 id (grd 접두사) */
  gridId: string
  rows: CmpnAuthRow[]
  loading: boolean
  /** 선택한 화면의 프로그램명 (선택 없으면 null) */
  prgmNm: string | null
  /** 선택한 화면이 삭제 표시됐는지 */
  isPrgmDeleted: boolean
  /** 소유자·화면이 삭제 표시되어 편집할 수 없는지 */
  locked: boolean
  /** 모든 컴포넌트 권한이 Y인지 (헤더 체크박스) */
  allChecked: boolean
}>()

const emit = defineEmits<{
  (e: 'change', row: CmpnAuthRow, authYn: string): void
  (e: 'change-all', authYn: string): void
}>()

const columns = [
  { key: 'no', label: 'NO', width: '40px', align: 'center' },
  { key: 'cmpnId', label: '컴포넌트ID', width: '110px' },
  { key: 'cmpnNm', label: '컴포넌트명' },
  { key: 'authYn', label: '권한여부', width: '72px', align: 'center' },
]

/**
 * toYn : 체크박스 이벤트를 Y/N 으로 변환
 * @param   event - 체크박스 change 이벤트
 * @return  체크되면 Y, 아니면 N
 */
function toYn(event: Event): string {
  return (event.target as HTMLInputElement).checked ? 'Y' : 'N'
}
</script>

<template>
  <div class="grid-area">
    <div class="area-header">
      <span class="area-title">
        컴포넌트목록
        <span v-if="prgmNm !== null" id="lblCmpnPrgmNm" class="sub-title">[{{ prgmNm }}]</span>
        <span v-if="isPrgmDeleted" class="locked-note">(삭제 예정)</span>
        - <strong id="lblCmpnTotCnt" class="total-count">{{ rows.length }}건</strong>
      </span>
    </div>
    <DataTable
      :id="gridId"
      :columns="columns"
      :data="rows"
      :loading="loading"
      :page-size="15"
      :empty-text="prgmNm !== null ? msg('sy.auth.group.empty.component.none') : msg('sy.auth.group.empty.component.select.screen')"
      :class="{ 'is-locked': locked }"
      dense
      flag-column
    >
      <template #header-authYn>
        <span class="auth-header">
          <input
            type="checkbox"
            class="cell-check"
            :checked="allChecked"
            aria-label="권한여부 전체 선택"
            @click.stop
            @change="emit('change-all', toYn($event))"
          />
          <span>권한여부</span>
        </span>
      </template>
      <template #cell-authYn="{ row }">
        <input
          type="checkbox"
          class="cell-check"
          :checked="row.authYn === 'Y'"
          @change="emit('change', row, toYn($event))"
        />
      </template>
    </DataTable>
  </div>
</template>

<style scoped>
.grid-area {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.area-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 32px;
  flex-wrap: wrap;
}

.area-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--ds-ink);
}

.sub-title {
  font-weight: 400;
  color: var(--ds-ink-2);
}

.locked-note {
  font-weight: 600;
  color: var(--ds-stamp);
}

/* 건수는 오류가 아니므로 브랜드 색 */
.total-count {
  color: var(--ds-pine-700);
  font-variant-numeric: tabular-nums;
}

/* 삭제(D) 표시된 소유자·화면의 컴포넌트 권한은 편집 불가 */
.is-locked {
  pointer-events: none;
  opacity: 0.5;
}

.cell-check {
  width: 16px;
  height: 16px;
  accent-color: var(--ds-pine-700);
  cursor: pointer;
}

.auth-header {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
</style>
