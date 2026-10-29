<script setup lang="ts">
/*****************************************************************************************
	Program ID   : ToastHost
	Program Type : vue
	Program Name : 토스트 호스트
	Description  : 전역 토스트(useToast) 목록을 화면 우측 상단에 쌓아서 렌더링하는 호스트
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
import { useToast, type ToastType } from '@/composables/useToast';

const { toasts, removeToast } = useToast();

const ICONS: Record<ToastType, string> = {
  success: '✓',
  info: 'i',
  warning: '!',
  error: '✕',
};

/**
 * onBtnCloseClick : 닫기 버튼 클릭 시 해당 토스트를 즉시 제거
 * @param   id - 닫을 토스트 ID
 * @return  N/A
 */
const onBtnCloseClick = (id: number) => removeToast(id);
</script>

<template>
  <Teleport to="body">
    <div class="toast-stack" aria-live="polite" aria-atomic="false">
      <TransitionGroup name="toast">
        <div
          v-for="toast in toasts"
          :key="toast.id"
          class="toast"
          :class="`toast--${toast.type}`"
          :role="toast.type === 'error' ? 'alert' : 'status'"
        >
          <span class="toast-icon" aria-hidden="true">{{ ICONS[toast.type] }}</span>
          <div class="toast-body">
            <p v-if="toast.title" class="toast-title">{{ toast.title }}</p>
            <p class="toast-message">{{ toast.message }}</p>
          </div>
          <button
            type="button"
            class="toast-close"
            aria-label="닫기"
            @click="onBtnCloseClick(toast.id)"
          >×</button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-stack {
  position: fixed;
  top: 16px;
  right: 16px;
  z-index: 10001;
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: min(360px, calc(100vw - 32px));
  pointer-events: none;
}

.toast {
  --toast-accent: var(--ds-slate-700);
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 12px 12px 14px;
  background: var(--ds-surface);
  color: var(--ds-ink);
  border: 1px solid var(--ds-rule);
  border-left: 4px solid var(--toast-accent);
  border-radius: var(--ds-radius-overlay);
  box-shadow: var(--ds-shadow-overlay);
  font-family: var(--ds-font);
  pointer-events: auto;
}

.toast--success { --toast-accent: var(--ds-slate-500); }
.toast--info    { --toast-accent: var(--ds-slate-700); }
.toast--warning { --toast-accent: var(--ds-ochre); }
.toast--error   { --toast-accent: var(--ds-stamp); }

.toast-icon {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  margin-top: 1px;
  border-radius: 50%;
  background: var(--toast-accent);
  color: var(--ds-ink-inverse);
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.toast-body {
  flex: 1 1 auto;
  min-width: 0;
}

.toast-title {
  margin: 0 0 2px;
  font-size: 13px;
  font-weight: 700;
}

.toast-message {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--ds-ink-2);
  white-space: pre-line;
  overflow-wrap: anywhere;
}

.toast-close {
  flex: 0 0 auto;
  width: 22px;
  height: 22px;
  padding: 0;
  border: none;
  border-radius: var(--ds-radius-control);
  background: transparent;
  color: var(--ds-ink-3);
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  transition: background var(--ds-ease), color var(--ds-ease);
}

.toast-close:hover {
  background: var(--ds-surface-muted);
  color: var(--ds-ink);
}

.toast-close:focus-visible {
  outline: 2px solid var(--ds-focus);
  outline-offset: 1px;
}

/* 등장 / 퇴장 애니메이션 */
.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(24px);
}

.toast-move {
  transition: transform 0.2s ease;
}

@media (prefers-reduced-motion: reduce) {
  .toast-enter-active,
  .toast-leave-active,
  .toast-move {
    transition: none;
  }
}
</style>
