<script setup lang="ts">
/*****************************************************************************************
	Program ID   : DialogHost
	Program Type : vue
	Program Name : 다이얼로그 호스트
	Description  : 전역 다이얼로그(useDialog) 상태를 MessageDialog 로 렌더링하는 호스트
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-09-29  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
import MessageDialog from './MessageDialog.vue';
import { useDialog } from '@/composables/useDialog';

const { state, closeDialog } = useDialog();

/**
 * onMdlMessageConfirm : 메시지 다이얼로그 확인 시 결과 true 로 닫음
 * @return  N/A
 */
const onMdlMessageConfirm = () => closeDialog(true);
/**
 * onMdlMessageCancel : 메시지 다이얼로그 취소 시 결과 false 로 닫음
 * @return  N/A
 */
const onMdlMessageCancel = () => closeDialog(false);
</script>

<template>
  <MessageDialog
    :model-value="state.visible"
    :kind="state.kind"
    :title="state.title"
    :message="state.message"
    :detail="state.detail"
    :icon="state.icon"
    :confirm-text="state.confirmText"
    :cancel-text="state.cancelText"
    @confirm="onMdlMessageConfirm"
    @cancel="onMdlMessageCancel"
  />
</template>
