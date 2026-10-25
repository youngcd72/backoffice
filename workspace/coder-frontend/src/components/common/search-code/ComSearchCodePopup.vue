<script setup lang="ts">
/*****************************************************************************************
	Program ID   : ComSearchCodePopup
	Program Type : vue
	Program Name : 공통 코드검색 팝업
	Description  : 코드검색 결과를 조회하고 단건/다건으로 선택하는 팝업
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
import { msg } from '@/utils/message'
import { ref, computed, watch } from 'vue'
import Button from '../button/Button.vue'
import Input from '../input/Input.vue'
import DataTable from '../table/DataTable.vue'
import PopupWindow from '../dialog/PopupWindow.vue'
import {
  getSearchCodeType,
  searchCodeApi,
  type SearchCodeParams,
  type SearchCodeRow,
} from '@/api/common/searchCode'

/**
 * 공통 코드서치 선택 팝업
 * - 열릴 때 keyword 로 조회하고, 행 클릭(단건) 또는 체크 후 [선택](다건)으로 결과를 돌려준다.
 */
const props = withDefaults(
  defineProps<{
    modelValue: boolean
    type: string
    keyword?: string
    params?: SearchCodeParams
    multiple?: boolean
  }>(),
  { keyword: '', params: () => ({}), multiple: false },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'select', rows: SearchCodeRow[]): void
}>()

interface PopupRow extends SearchCodeRow {
  id: string
}

const typeDef = computed(() => getSearchCodeType(props.type))

const columns = computed(() => {
  const def = typeDef.value
  if (!def) return []
  return [
    { key: 'CD', label: def.codeLabel, width: '120px' },
    { key: 'NM', label: def.nameLabel },
    ...(def.extraColumns ?? []),
  ]
})

const searchKeyword = ref('')
const rows = ref<PopupRow[]>([])
const selectedRows = ref<PopupRow[]>([])
const isLoading = ref(false)

/**
 * searchRows : 검색어로 코드 목록을 조회해 그리드에 표시
 * @return  N/A
 */
async function searchRows(): Promise<void> {
  isLoading.value = true
  try {
    const result = await searchCodeApi(props.type, searchKeyword.value, props.params)
    rows.value = result.map((row, idx) => ({ ...row, id: `${row.CD}-${idx}` }))
    selectedRows.value = []
  } finally {
    isLoading.value = false
  }
}

/**
 * toResult : 팝업 행에서 내부 id 를 제거해 결과 행으로 변환
 * @param   list - 팝업 행 목록
 * @return  결과 행 목록
 */
function toResult(list: PopupRow[]): SearchCodeRow[] {
  return list.map(({ id: _id, ...row }) => row as SearchCodeRow)
}

/**
 * close : 팝업을 닫음
 * @return  N/A
 */
function close(): void {
  emit('update:modelValue', false)
}

/**
 * onBtnCdsSrchClick : 조회 버튼 클릭 시 코드 목록을 조회
 * @return  N/A
 */
function onBtnCdsSrchClick(): void {
  searchRows()
}

/**
 * onGrdCdsListRowClick : 그리드 행 클릭 시 단건 선택 결과를 반환하고 팝업을 닫음 (다건 모드는 무시)
 * @param   row - 클릭된 행
 * @return  N/A
 */
function onGrdCdsListRowClick(row: PopupRow): void {
  if (props.multiple) return
  emit('select', toResult([row]))
  close()
}

/**
 * onBtnCdsSelectClick : 선택 버튼 클릭 시 체크된 행을 반환하고 팝업을 닫음
 * @return  N/A
 */
function onBtnCdsSelectClick(): void {
  if (selectedRows.value.length === 0) return
  emit('select', toResult(selectedRows.value))
  close()
}

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) return
    searchKeyword.value = props.keyword
    rows.value = []
    searchRows()
  },
  { immediate: true },
)
</script>

<template>
  <PopupWindow
    id="mdlComSearchCode"
    :model-value="modelValue"
    :title="`${typeDef?.title ?? ''} 검색`"
    :width="640"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <form class="cds-popup-search" @submit.prevent="onBtnCdsSrchClick">
      <span class="cds-popup-label">{{ typeDef?.codeLabel }}/{{ typeDef?.nameLabel }}</span>
      <Input id="txtCdsKeyword" v-model="searchKeyword" size="small" @enter="onBtnCdsSrchClick" />
      <Button id="btnCdsSrch" size="small" @click="onBtnCdsSrchClick">조회</Button>
    </form>
    <DataTable
      id="grdCdsList"
      v-model:selected-rows="selectedRows"
      :columns="columns"
      :data="rows"
      :loading="isLoading"
      :page-size="10"
      :selectable="multiple"
      :empty-text="msg('cm.info.no.data')"
      dense
      @row-click="onGrdCdsListRowClick"
    />
    <p v-if="!multiple" class="cds-popup-hint">{{ msg('cm.info.row.click.select') }}</p>
    <template #footer>
      <Button v-if="multiple" id="btnCdsSelect" :disabled="selectedRows.length === 0" @click="onBtnCdsSelectClick">
        선택 ({{ selectedRows.length }})
      </Button>
      <Button id="btnCdsClose" variant="outline" @click="close">닫기</Button>
    </template>
  </PopupWindow>
</template>

<style scoped>
.cds-popup-search {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.cds-popup-label {
  white-space: nowrap;
  font-size: 13px;
  font-weight: 600;
  color: var(--ds-ink-2);
}

.cds-popup-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--ds-ink-3);
}
</style>
