<script setup lang="ts">
/*****************************************************************************************
	Program ID   : AuthScreenAddPopup
	Program Type : vue
	Program Name : 화면추가 팝업
	Description  : 프로그램이 연결된 메뉴 중 권한에 추가할 화면을 검색·선택하는 팝업
	               (추가/닫기 버튼은 버튼 권한(v-permission) 관리를 위해 화면에서 footer 슬롯으로 넘긴다)
	               사용 화면: SYAU010 사용자권한관리, SYAU020 그룹권한관리
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-20  ywkim     최초생성 (SYAU010, SYAU020 화면추가 팝업 통합)
------------------------------------------------------------------------------------------
*****************************************************************************************/
import { msg } from '@/utils/message'
import { DataTable, Input, PopupWindow } from '@/components/common'
import type { ScrnCandidate } from '@/composables/useMenuCmpnAuth'

defineProps<{
  /** 추가할 수 있는 화면 후보 (이미 추가된 화면 제외, 검색어 반영) */
  candidates: ScrnCandidate[]
}>()

const isOpen = defineModel<boolean>({ required: true })
const keyword = defineModel<string>('keyword', { required: true })
const selectedRows = defineModel<ScrnCandidate[]>('selectedRows', { required: true })

const columns = [
  { key: 'menuId', label: '메뉴ID', width: '90px' },
  { key: 'menuPath', label: '메뉴' },
  { key: 'prgmId', label: '프로그램ID', width: '120px' },
  { key: 'prgmNm', label: '프로그램명', width: '140px' },
]
</script>

<template>
  <PopupWindow id="mdlScrnSrch" v-model="isOpen" title="화면 추가" :width="760">
    <div class="popup-search">
      <span class="field-label">검색</span>
      <Input id="txtScrnKeyword" v-model="keyword" placeholder="메뉴ID·메뉴·프로그램" />
    </div>
    <DataTable
      id="grdScrnList"
      v-model:selected-rows="selectedRows"
      :columns="columns"
      :data="candidates"
      :page-size="8"
      :empty-text="msg('sy.auth.group.empty.addable.none')"
      selectable
      dense
    />
    <template #footer>
      <slot name="footer" />
    </template>
  </PopupWindow>
</template>

<style scoped>
.popup-search {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.field-label {
  min-width: 56px;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 600;
  color: var(--ds-ink-2);
}
</style>
