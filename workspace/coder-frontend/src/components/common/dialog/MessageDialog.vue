<script setup lang="ts">
/*****************************************************************************************
	Program ID   : MessageDialog
	Program Type : vue
	Program Name : 메시지 다이얼로그
	Description  : alert/confirm 형태의 메시지 대화상자 (아이콘, 제목, 확인/취소 버튼, ESC 닫기)
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
import { computed, nextTick, ref, watch } from 'vue';
import type { DialogIcon, DialogKind } from '@/composables/useDialog';
import { useEscClose } from '@/composables/useEscClose';

// 사인 표지 글리프: 아이콘 종류별 글자와 제목
const ICON_SIGNS: Record<DialogIcon, { glyph: string; title: string }> = {
  default: { glyph: 'i', title: '알림' },
  information: { glyph: 'i', title: '알림' },
  question: { glyph: '?', title: '확인' },
  warning: { glyph: '!', title: '경고' },
  error: { glyph: '×', title: '오류' },
};

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    kind?: DialogKind;
    title?: string;
    message: string;
    detail?: string;
    icon?: DialogIcon;
    confirmText?: string;
    cancelText?: string;
  }>(),
  { kind: 'alert', icon: undefined, title: undefined, detail: undefined, confirmText: '확인', cancelText: '취소' },
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'confirm'): void;
  (e: 'cancel'): void;
}>();

const btnConfirm = ref<HTMLButtonElement | null>(null);

const isConfirm = computed(() => props.kind === 'confirm');
// Alert 는 오류, Confirm 은 물음표가 기본. 제목도 아이콘 종류를 따른다 (안내 메시지가 '오류'로 뜨지 않게)
const iconName = computed<DialogIcon>(() => props.icon ?? (isConfirm.value ? 'question' : 'error'));
const titleText = computed(() => props.title ?? ICON_SIGNS[iconName.value].title);
const lines = computed(() => props.message.split('\n'));

/**
 * onBtnConfirmClick : 확인 버튼 클릭 시 대화상자를 닫고 confirm 이벤트를 발생
 * @return  N/A
 */
const onBtnConfirmClick = () => {
  emit('update:modelValue', false);
  emit('confirm');
};

/**
 * onBtnCancelClick : 취소 버튼 클릭 시 대화상자를 닫고 cancel 이벤트를 발생
 * @return  N/A
 */
const onBtnCancelClick = () => {
  emit('update:modelValue', false);
  emit('cancel');
};

// alert 는 닫기(X)/ESC 도 확인과 동일하게 닫힘, confirm 은 취소로 처리
/**
 * onBtnCloseClick : 닫기(X)/ESC 시 confirm 은 취소, alert 는 확인과 동일하게 처리
 * @return  N/A
 */
const onBtnCloseClick = () => (isConfirm.value ? onBtnCancelClick() : onBtnConfirmClick());

// 가장 위에 열린 대화상자일 때만 ESC 로 닫는다
useEscClose(() => props.modelValue, onBtnCloseClick);

watch(
  () => props.modelValue,
  async (visible) => {
    if (!visible) return;
    await nextTick();
    btnConfirm.value?.focus();
  },
  { immediate: true },
);
</script>

<template>
  <Teleport to="body">
    <Transition name="dialog-fade">
      <div v-if="modelValue" class="dialog-overlay">
        <div
          id="mdlMessage"
          class="dialog"
          :class="[isConfirm ? 'is-confirm' : 'is-alert', `icon-${iconName}`]"
          role="alertdialog"
          aria-modal="true"
        >
          <header class="dialog-header">
            <span class="dialog-title">{{ titleText }}</span>
            <button type="button" class="dialog-close" title="닫기" @click="onBtnCloseClick">
              <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="1.6" fill="none">
                <line x1="4" y1="4" x2="20" y2="20" />
                <line x1="20" y1="4" x2="4" y2="20" />
              </svg>
            </button>
          </header>

          <div class="dialog-body">
            <span class="dialog-icon" :class="`is-${iconName}`" aria-hidden="true">{{ ICON_SIGNS[iconName].glyph }}</span>
            <div class="dialog-message">
              <p v-if="detail">{{ detail }}</p>
              <p v-for="(line, i) in lines" :key="i">{{ line }}</p>
            </div>
          </div>

          <footer class="dialog-footer">
            <button ref="btnConfirm" type="button" class="dialog-btn" @click="onBtnConfirmClick">
              {{ confirmText }}
            </button>
            <button v-if="isConfirm" type="button" class="dialog-btn" @click="onBtnCancelClick">
              {{ cancelText }}
            </button>
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ds-scrim);
}

.dialog {
  width: 380px;
  max-width: calc(100vw - 32px);
  background: var(--ds-surface);
  border: 1px solid var(--ds-slate-900);
  border-radius: var(--ds-radius-overlay);
  box-shadow: var(--ds-shadow-overlay);
  overflow: hidden;
  font-size: 13px;
  color: var(--ds-ink);
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 42px;
  padding: 0 8px 0 16px;
  background: var(--ds-slate-900);
  color: var(--ds-ink-inverse);
}
/* 오류·경고 대화상자만 헤더 하단 선으로 구분 (안내 메시지에는 표시하지 않는다) */
.dialog.icon-error .dialog-header {
  box-shadow: inset 0 -3px 0 var(--ds-stamp);
}
.dialog.icon-warning .dialog-header {
  box-shadow: inset 0 -3px 0 var(--ds-foil);
}
.dialog-title {
  font-size: 14px;
  font-weight: 600;
}
.dialog-close {
  display: flex;
  padding: 4px;
  border: 0;
  border-radius: var(--ds-radius-control);
  background: none;
  color: var(--ds-ink-inverse);
  cursor: pointer;
  transition: background-color var(--ds-ease);
}
.dialog-close:hover {
  background: var(--ds-slate-700);
}
.dialog-close:focus-visible {
  outline: none;
  box-shadow: var(--ds-focus);
}

.dialog-body {
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 84px;
  padding: 20px 20px 16px;
}
.dialog-icon {
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  font-size: 22px;
  font-weight: 700;
  line-height: 1;
  color: var(--ds-ink-inverse);
  background: var(--ds-slate-700);
  border-radius: 2px;
}
.dialog-icon.is-warning {
  color: var(--ds-slate-900);
  background: var(--ds-foil);
}
.dialog-icon.is-error {
  background: var(--ds-stamp);
}
.dialog-message {
  line-height: 1.7;
  word-break: keep-all;
  overflow-wrap: anywhere;
}
.dialog-message p {
  margin: 0;
}

.dialog-footer {
  display: flex;
  justify-content: center;
  gap: 8px;
  padding: 12px 18px;
  background: var(--ds-slate-050);
  border-top: 1px solid var(--ds-rule);
}
.dialog-btn {
  min-width: 64px;
  height: var(--ds-control-height-sm);
  padding: 0 14px;
  border: 1px solid var(--ds-rule-strong);
  border-radius: var(--ds-radius-control);
  background: var(--ds-surface);
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  color: var(--ds-ink);
  cursor: pointer;
  transition: background-color var(--ds-ease), border-color var(--ds-ease), color var(--ds-ease);
}
.dialog-btn:hover {
  background: var(--ds-slate-050);
  border-color: var(--ds-slate-700);
  color: var(--ds-slate-900);
}
/* 첫 번째 버튼 = 확인 */
.dialog-btn:first-child {
  background: var(--ds-slate-700);
  border-color: var(--ds-slate-700);
  color: var(--ds-ink-inverse);
}
.dialog-btn:first-child:hover {
  background: var(--ds-slate-900);
  border-color: var(--ds-slate-900);
  color: var(--ds-ink-inverse);
}
.dialog-btn:focus-visible {
  outline: none;
  box-shadow: var(--ds-focus);
}

.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.15s ease;
}
.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
}
</style>
