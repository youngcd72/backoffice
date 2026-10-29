<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div 
        v-if="modelValue" 
        class="modal-overlay" 
        @click="handleOverlayClick"
      >
        <div 
          class="modal-container" 
          :class="[`size-${size}`]" 
          @click.stop
          role="dialog"
          aria-modal="true"
        >
          <!-- 모달 헤더 -->
          <header class="modal-header">
            <div class="modal-title-wrap">
              <slot name="header">
                <h3 class="modal-title">{{ title }}</h3>
              </slot>
            </div>

            <!-- 닫기 버튼 -->
            <button
              v-if="closable"
              type="button"
              class="modal-close-btn"
              title="닫기"
              @click="closeModal"
            >
              <svg viewBox="0 0 24 24" width="18" height="18" stroke="currentColor" stroke-width="2.2" fill="none" stroke-linecap="round">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          </header>

          <!-- 모달 본문 바디 -->
          <div class="modal-body">
            <slot></slot>
          </div>

          <!-- 모달 푸터 -->
          <footer v-if="showFooter" class="modal-footer">
            <slot name="footer">
              <button
                type="button"
                class="footer-btn cancel-btn"
                @click="handleCancel"
              >
                {{ cancelText }}
              </button>
              <button
                type="button"
                class="footer-btn confirm-btn"
                :disabled="confirmLoading"
                @click="handleConfirm"
              >
                <span v-if="confirmLoading" class="btn-spinner"></span>
                <span>{{ confirmText }}</span>
              </button>
            </slot>
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
/*****************************************************************************************
	Program ID   : Modal
	Program Type : vue
	Program Name : 모달
	Description  : 오버레이, 확인/취소 버튼, ESC 닫기를 지원하는 공통 모달
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-09-07  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
import { onMounted, onBeforeUnmount, watch } from 'vue';

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '알림'
  },
  size: {
    type: String,
    default: 'medium', // 'small' | 'medium' | 'large' | 'fullscreen'
    validator: (v) => ['small', 'medium', 'large', 'fullscreen'].includes(v)
  },
  closable: {
    type: Boolean,
    default: true
  },
  closeOnClickOverlay: {
    type: Boolean,
    default: true
  },
  showFooter: {
    type: Boolean,
    default: true
  },
  confirmText: {
    type: String,
    default: '확인'
  },
  cancelText: {
    type: String,
    default: '취소'
  },
  confirmLoading: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel', 'close']);

/**
 * closeModal : 모달을 닫고 update:modelValue / close 이벤트를 발생
 * @return  N/A
 */
const closeModal = () => {
  emit('update:modelValue', false);
  emit('close');
};

/**
 * handleOverlayClick : 오버레이 클릭 시 closeOnClickOverlay 가 켜져 있으면 모달을 닫음
 * @return  N/A
 */
const handleOverlayClick = () => {
  if (props.closeOnClickOverlay) {
    closeModal();
  }
};

/**
 * handleConfirm : 확인 버튼 클릭 시 confirm 이벤트를 발생
 * @return  N/A
 */
const handleConfirm = () => {
  emit('confirm');
};

/**
 * handleCancel : 취소 버튼 클릭 시 cancel 이벤트를 발생하고 모달을 닫음
 * @return  N/A
 */
const handleCancel = () => {
  emit('cancel');
  closeModal();
};

/**
 * handleKeyDown : ESC 입력 시 열린 상태이고 닫기 가능하면 모달을 닫음
 * @param   e - 키보드 이벤트
 * @return  N/A
 */
const handleKeyDown = (e) => {
  if (props.modelValue && e.key === 'Escape' && props.closable) {
    closeModal();
  }
};

// 스크롤 방지 제어
watch(() => props.modelValue, (val) => {
  if (val) {
    document.body.style.overflow = 'hidden';
  } else {
    document.body.style.overflow = '';
  }
});

onMounted(() => {
  window.addEventListener('keydown', handleKeyDown);
});

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeyDown);
  document.body.style.overflow = '';
});
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: var(--ds-scrim);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.modal-container {
  background: var(--ds-surface);
  border: 1px solid var(--ds-slate-900);
  border-radius: var(--ds-radius-overlay);
  box-shadow: var(--ds-shadow-overlay);
  color: var(--ds-ink);
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  width: 100%;
  overflow: hidden;
  font-family: inherit;
}

/* 크기 */
.size-small { max-width: 400px; }
.size-medium { max-width: 540px; }
.size-large { max-width: 800px; }
.size-fullscreen {
  max-width: 95vw;
  height: 90vh;
}

/* 헤더 */
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: none;
  height: 42px;
  padding: 0 8px 0 16px;
  background: var(--ds-slate-900);
  color: var(--ds-ink-inverse);
}

.modal-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--ds-ink-inverse);
}

.modal-close-btn {
  background: none;
  border: none;
  color: var(--ds-ink-inverse);
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--ds-radius-control);
  cursor: pointer;
  transition: background-color var(--ds-ease);
}

.modal-close-btn:hover {
  background: var(--ds-slate-700);
}

.modal-close-btn:focus-visible {
  outline: none;
  box-shadow: var(--ds-focus);
}

/* 바디 */
.modal-body {
  padding: 18px 18px 16px;
  overflow-y: auto;
  font-size: 13px;
  line-height: 1.6;
  color: var(--ds-ink);
}

/* 푸터 */
.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex: none;
  padding: 12px 18px;
  border-top: 1px solid var(--ds-rule);
  background: var(--ds-slate-050);
}

.footer-btn {
  height: var(--ds-control-height-sm);
  padding: 0 14px;
  border-radius: var(--ds-radius-control);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid transparent;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: background-color var(--ds-ease), border-color var(--ds-ease), color var(--ds-ease);
  font-family: inherit;
}

.footer-btn:focus-visible {
  outline: none;
  box-shadow: var(--ds-focus);
}

.cancel-btn {
  background: var(--ds-surface);
  color: var(--ds-ink);
  border-color: var(--ds-rule-strong);
}
.cancel-btn:hover {
  background: var(--ds-slate-050);
  border-color: var(--ds-slate-700);
  color: var(--ds-slate-900);
}

.confirm-btn {
  background: var(--ds-slate-700);
  border-color: var(--ds-slate-700);
  color: var(--ds-ink-inverse);
}
.confirm-btn:hover:not(:disabled) {
  background: var(--ds-slate-900);
  border-color: var(--ds-slate-900);
}
.confirm-btn:disabled {
  background: var(--ds-surface-muted);
  border-color: var(--ds-rule);
  color: var(--ds-ink-3);
  cursor: not-allowed;
}

.btn-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid var(--ds-rule);
  border-top-color: var(--ds-slate-700);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 트랜지션: 페이드만 */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.15s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}
</style>
