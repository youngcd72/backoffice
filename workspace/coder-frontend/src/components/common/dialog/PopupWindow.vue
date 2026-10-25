<script setup lang="ts">
/*****************************************************************************************
	Program ID   : PopupWindow
	Program Type : vue
	Program Name : 팝업 윈도우
	Description  : 제목과 닫기 버튼, ESC 닫기를 가진 공통 팝업 대화상자 프레임
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-09-29  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
import { useEscClose } from '@/composables/useEscClose';

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    /** 대화상자 요소 id (화면별 mdl 접두사 id, 예: mdlUpprMenu) */
    id?: string;
    title?: string;
    width?: number | string;
    closeOnEsc?: boolean;
  }>(),
  { id: 'mdlPopup', title: '', width: 600, closeOnEsc: true },
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'close'): void;
}>();

/**
 * onBtnCloseClick : 닫기 버튼 클릭 시 팝업을 닫고 close 이벤트를 발생
 * @return  N/A
 */
const onBtnCloseClick = () => {
  emit('update:modelValue', false);
  emit('close');
};

// 가장 위에 열린 팝업일 때만 ESC 로 닫는다 (closeOnEsc 가 꺼져 있으면 ESC 를 무시)
useEscClose(
  () => props.modelValue,
  () => {
    if (props.closeOnEsc) onBtnCloseClick();
  },
);

/**
 * toCssSize : 숫자는 px 단위로, 문자열은 그대로 CSS 크기 값으로 변환
 * @param   v - 크기 값 (숫자 또는 CSS 문자열)
 * @return  CSS 크기 문자열
 */
const toCssSize = (v: number | string) => (typeof v === 'number' ? `${v}px` : v);
</script>

<template>
  <Teleport to="body">
    <Transition name="popup-fade">
      <div v-if="modelValue" class="popup-overlay">
        <div :id="id" class="popup" role="dialog" aria-modal="true" :style="{ width: toCssSize(width) }">
          <header class="popup-header">
            <slot name="header">
              <span class="popup-title">{{ title }}</span>
            </slot>
            <button type="button" class="popup-close" title="닫기" @click="onBtnCloseClick">
              <svg viewBox="0 0 24 24" width="18" height="18" stroke="currentColor" stroke-width="1.6" fill="none">
                <line x1="4" y1="4" x2="20" y2="20" />
                <line x1="20" y1="4" x2="4" y2="20" />
              </svg>
            </button>
          </header>
          <div class="popup-body"><slot /></div>
          <footer v-if="$slots.footer" class="popup-footer"><slot name="footer" /></footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.popup-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ds-scrim);
}
.popup {
  display: flex;
  flex-direction: column;
  max-width: calc(100vw - 32px);
  max-height: calc(100vh - 32px);
  background: var(--ds-surface);
  border: 1px solid var(--ds-pine-900);
  border-radius: var(--ds-radius-overlay);
  box-shadow: var(--ds-shadow-overlay);
  overflow: hidden;
  font-size: 13px;
  color: var(--ds-ink);
}
.popup-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: none;
  height: 42px;
  padding: 0 8px 0 16px;
  background: var(--ds-pine-900);
  color: var(--ds-ink-inverse);
}
.popup-title {
  font-size: 14px;
  font-weight: 600;
}
.popup-close {
  display: flex;
  padding: 4px;
  border: 0;
  border-radius: var(--ds-radius-control);
  background: none;
  color: var(--ds-ink-inverse);
  cursor: pointer;
  transition: background-color var(--ds-ease);
}
.popup-close:hover {
  background: var(--ds-pine-700);
}
.popup-close:focus-visible {
  outline: none;
  box-shadow: var(--ds-focus);
}
.popup-body {
  padding: 18px 18px 16px;
  font-size: 13px;
  overflow: auto;
}
.popup-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  flex: none;
  padding: 12px 18px;
  background: var(--ds-pine-050);
  border-top: 1px solid var(--ds-rule);
}
.popup-fade-enter-active,
.popup-fade-leave-active {
  transition: opacity 0.15s ease;
}
.popup-fade-enter-from,
.popup-fade-leave-to {
  opacity: 0;
}
</style>
