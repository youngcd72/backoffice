<template>
  <div class="login-wrapper">
    <aside class="login-brand" aria-label="CODER">
      <div class="brand-card">
        <BrandLogo layout="stacked" :size="96" tagline="Freshly brewed code" animated class="brand-card-logo" />
      </div>
    </aside>

    <main class="login-main">
    <div class="login-container">
      <div class="login-header">
        <h1 class="title">로그인</h1>
      </div>

      <form class="login-form" @submit.prevent="handleLogin">
        <div class="form-group">
          <Input
            v-model="form.email"
            label="아이디 또는 이메일"
            placeholder="아이디 또는 이메일 입력"
            :error="errors.email"
            clearable
            required
            @blur="validateEmail"
          >
            <template #prefix>
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
              </svg>
            </template>
          </Input>
        </div>

        <div class="form-group">
          <Input
            v-model="form.password"
            type="password"
            label="비밀번호"
            placeholder="비밀번호 입력"
            :error="errors.password"
            required
            @blur="validatePassword"
          >
            <template #prefix>
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
              </svg>
            </template>
          </Input>
        </div>

        <div class="form-options">
          <label class="remember-me">
            <input type="checkbox" v-model="form.rememberMe" class="custom-checkbox" />
            <span>로그인 상태 유지</span>
          </label>
          <a href="#" class="forgot-link" @click.prevent="handleForgotPassword">비밀번호 찾기</a>
        </div>

        <div v-if="serverError" class="server-error-alert">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
          <span>{{ serverError }}</span>
        </div>

        <Button
          type="submit"
          variant="primary"
          size="large"
          class="submit-btn"
          :loading="isLoading"
          :disabled="isLoading"
        >
          {{ isLoading ? '로그인 처리 중...' : '로그인' }}
        </Button>
      </form>

      <div class="login-footer">
        <span class="footer-text">계정 문의 및 권한 신청:</span>
        <a href="mailto:admin@coder.dev" class="signup-link">관리자 문의</a>
      </div>
    </div>
    </main>
  </div>
</template>

<script setup lang="ts">
/*****************************************************************************************
	Program ID   : LoginView
	Program Type : vue
	Program Name : 로그인
	Description  : 이메일/아이디 로그인, 비밀번호 찾기 안내
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { msg } from '@/utils/message';
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { BrandLogo, Button, Input } from '@/components';
import { useAuthStore } from '@/stores/auth';
import { useDialog } from '@/composables/useDialog';

const emit = defineEmits<{
  (e: 'login-success', token: string): void;
}>();

const router = useRouter();
const authStore = useAuthStore();
const dialog = useDialog();

// 개발 서버에서만 테스트 계정을 미리 채운다 (운영 빌드에는 포함하지 않음)
const form = reactive({
  email: import.meta.env.DEV ? 'admin' : '',
  password: import.meta.env.DEV ? 'coder1234' : '',
  rememberMe: false
});

const errors = reactive({
  email: '',
  password: ''
});

const serverError = ref('');
const isLoading = ref(false);

/**
 * validateEmail : 아이디/이메일 입력값 검증
 * @return  유효하면 true, 아니면 false
 */
const validateEmail = () => {
  if (!form.email.trim()) {
    errors.email = msg('sy.login.id.required');
    return false;
  }
  errors.email = '';
  return true;
};

/**
 * validatePassword : 비밀번호 입력값(필수, 4자 이상) 검증
 * @return  유효하면 true, 아니면 false
 */
const validatePassword = () => {
  if (!form.password) {
    errors.password = msg('sy.login.password.required');
    return false;
  }
  if (form.password.length < 4) {
    errors.password = msg('sy.login.password.min.length');
    return false;
  }
  errors.password = '';
  return true;
};

/**
 * handleLogin : 입력값 검증 후 로그인 요청, 성공 시 login-success 이벤트 발행 및 대시보드 이동
 * @return  N/A
 */
const handleLogin = async () => {
  serverError.value = '';
  const isEmailValid = validateEmail();
  const isPasswordValid = validatePassword();

  if (!isEmailValid || !isPasswordValid) {
    return;
  }

  isLoading.value = true;
  try {
    const username = form.email.includes('@') ? form.email.split('@')[0] : form.email;
    await authStore.loginWithCredentials(username.trim(), form.password);

    emit('login-success', authStore.token || '');
    router.push('/dashboard');
  } catch (err: any) {
    console.error('[CODER Auth] 로그인 에러:', err);
    if (err.response?.data?.message) {
      serverError.value = err.response.data.message;
    } else if (err.response?.status === 401) {
      serverError.value = msg('sy.login.credentials.mismatch');
    } else if (err.message) {
      serverError.value = err.message;
    } else {
      serverError.value = msg('sy.login.server.unreachable');
    }
  } finally {
    isLoading.value = false;
  }
};

/**
 * handleForgotPassword : 비밀번호 초기화 요청 안내 팝업 표시
 * @return  N/A
 */
const handleForgotPassword = () => {
  dialog.alert({
    title: 'INFORMATION',
    icon: 'information',
    message: msg('sy.login.password.reset.guide'),
  });
};
</script>

<style scoped>
/* 분할 화면: 왼쪽 슬레이트 위 명함 + 오른쪽 코튼지 위 폼 */
.login-wrapper {
  display: flex;
  min-height: 100vh;
  width: 100%;
  background: var(--ds-ground);
  color: var(--ds-ink);
  font-family: var(--ds-font);
}

/* ---------- 왼쪽: 슬레이트 돌판 위에 놓인 명함 ---------- */
.login-brand {
  flex: 0 0 46%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 28px;
  padding: 56px;
  background: var(--ds-slate-900);
  color: var(--ds-ink-inverse);
  box-sizing: border-box;
}

/* 명함 비율(90×50mm)의 코튼지 카드. 아래 두 줄의 단단한 띠가 쌓인 명함의 두께다 */
.brand-card {
  width: min(440px, 100%);
  aspect-ratio: 9 / 5;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 44px;
  background: var(--ds-surface);
  border-radius: 2px;
  box-shadow: 0 3px 0 var(--ds-slate-100), 0 6px 0 var(--ds-rule-strong);
  color: var(--ds-ink);
  box-sizing: border-box;
}

/* 레터프레스: 먹 선 아래 1px 밝은 턱으로 종이에 눌린 느낌 */
.brand-card-logo {
  font-size: 44px;
  filter: drop-shadow(0 1px 0 var(--ds-surface-muted));
}

/* ---------- 오른쪽: 폼 패널 ---------- */
.login-main {
  flex: 1 1 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  min-width: 0;
  box-sizing: border-box;
}

.login-container {
  width: 100%;
  max-width: 380px;
  padding: 32px;
  background: var(--ds-surface);
  border: 1px solid var(--ds-rule);
  border-radius: var(--ds-radius-panel);
  box-sizing: border-box;
}

.login-header {
  margin-bottom: 24px;
}

.title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--ds-ink);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
}

/* 공용 Input 을 로그인 화면 규격(40px, 라벨 13px/600)으로 맞춘다 */
.login-form :deep(.custom-input-group) {
  --input-bg: var(--ds-surface);
  --input-bg-focus: var(--ds-surface);
  --input-border: var(--ds-rule-strong);
  --input-border-hover: var(--ds-rule-strong);
  --input-border-focus: var(--ds-slate-700);
  --input-text: var(--ds-ink);
  --input-text-muted: var(--ds-ink-3);
  --input-radius: var(--ds-radius-control);
  --input-height: 40px;
  gap: 6px;
}

.login-form :deep(.input-label) {
  font-size: 13px;
  font-weight: 600;
  color: var(--ds-ink-2);
}

.login-form :deep(.input-control) {
  height: 40px;
  background: var(--ds-surface);
  border: 1px solid var(--ds-rule-strong);
  border-radius: var(--ds-radius-control);
  box-shadow: none;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
  color: var(--ds-ink-3);
  transition: border-color var(--ds-ease), box-shadow var(--ds-ease);
}

.login-form :deep(.is-focused .input-control) {
  background: var(--ds-surface);
  border-color: var(--ds-slate-700);
  box-shadow: var(--ds-focus-field);
}

.login-form :deep(.has-error .input-control) {
  border-color: var(--ds-stamp);
}

.login-form :deep(.native-input) {
  color: var(--ds-ink);
}

.login-form :deep(.input-action-btn:focus-visible) {
  outline: none;
  box-shadow: var(--ds-focus);
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 13px;
}

.remember-me {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--ds-ink-2);
  cursor: pointer;
  user-select: none;
}

.custom-checkbox {
  appearance: none;
  flex: none;
  width: 16px;
  height: 16px;
  margin: 0;
  border: 1px solid var(--ds-rule-strong);
  border-radius: 2px;
  outline: none;
  background: var(--ds-surface);
  cursor: pointer;
  position: relative;
  transition: background-color var(--ds-ease), border-color var(--ds-ease);
}

.custom-checkbox:focus-visible {
  box-shadow: var(--ds-focus);
}

.custom-checkbox:checked {
  background-color: var(--ds-slate-700);
  border-color: var(--ds-slate-700);
}

.custom-checkbox:checked::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 5px;
  width: 4px;
  height: 8px;
  border: solid var(--ds-ink-inverse);
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.forgot-link,
.signup-link {
  color: var(--ds-slate-700);
  text-decoration: none;
  border-radius: 2px;
  outline: none;
}

.forgot-link:hover,
.signup-link:hover {
  text-decoration: underline;
}

.forgot-link:focus-visible,
.signup-link:focus-visible {
  box-shadow: var(--ds-focus);
}

/* 오류 메시지: 도장색 글자 + 옅은 바탕 + 좌측 3px 바 */
.server-error-alert {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: var(--ds-stamp-tint);
  border-left: 3px solid var(--ds-stamp);
  border-radius: 2px;
  color: var(--ds-stamp);
  font-size: 13px;
}

.server-error-alert svg {
  flex: none;
}

.login-form :deep(.submit-btn) {
  width: 100%;
  height: 40px;
  margin-top: 4px;
  font-weight: 600;
  background: var(--ds-slate-700);
  border: 1px solid var(--ds-slate-700);
  border-radius: var(--ds-radius-control);
  color: var(--ds-ink-inverse);
  box-shadow: none;
  transform: none;
  transition: background-color var(--ds-ease), border-color var(--ds-ease);
}

.login-form :deep(.submit-btn:hover:not(:disabled)) {
  background: var(--ds-slate-900);
  border-color: var(--ds-slate-900);
  transform: none;
  box-shadow: none;
}

.login-form :deep(.submit-btn:focus-visible) {
  box-shadow: var(--ds-focus);
}

.login-footer {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--ds-rule);
  font-size: 13px;
  color: var(--ds-ink-2);
}

.signup-link {
  font-weight: 600;
  margin-left: 6px;
}

/* ---------- 900px 미만: 상단 120px 밴드 + 폼 ---------- */
@media (max-width: 899px) {
  .login-wrapper {
    flex-direction: column;
  }

  .login-brand {
    flex: none;
    padding: 24px 16px 30px;
  }

  .brand-card {
    width: auto;
    aspect-ratio: auto;
    padding: 16px 22px;
  }

  .brand-card-logo {
    font-size: 26px;
  }

  .brand-card-logo :deep(.brand-mark) {
    width: 44px;
    height: 41px;
    stroke-width: 2.6;
  }

  /* 좁은 화면에서는 심볼 옆에 워드마크를 둔다 */
  .brand-card-logo.is-stacked {
    flex-direction: row;
    align-items: center;
    gap: 14px;
  }

  .login-main {
    align-items: flex-start;
    padding: 24px 16px 32px;
  }
}

@media (max-width: 420px) {
  .login-container {
    padding: 24px 16px;
  }
}
</style>
