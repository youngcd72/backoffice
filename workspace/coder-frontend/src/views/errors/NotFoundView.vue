<script setup lang="ts">
/*****************************************************************************************
	Program ID   : NotFoundView
	Program Type : vue
	Program Name : 페이지 없음
	Description  : 존재하지 않는 경로 접근 시 안내 화면
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-09-20  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
import { msg } from '@/utils/message'

import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="not-found-view">
    <div class="not-found-card card">
      <span class="error-code">404</span>
      <h1 class="error-title">{{ msg('cm.page.not.found') }}</h1>
      <p class="error-desc">{{ msg('cm.page.not.found.description') }}</p>
      <button class="btn btn-primary" @click="router.push('/dashboard')">시스템 현황으로 이동</button>
    </div>
  </div>
</template>

<style scoped>
.not-found-view {
  min-height: 70vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.not-found-card {
  width: 100%;
  max-width: 460px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px 32px;
  background: var(--ds-surface);
  border: 1px solid var(--ds-rule);
  border-radius: var(--ds-radius-panel);
  box-shadow: none;
  box-sizing: border-box;
}

.error-code {
  display: inline-flex;
  align-items: center;
  padding: 6px 14px;
  font-family: var(--ds-font-brand);
  font-size: 38px;
  font-weight: 600;
  letter-spacing: 0.12em;
  font-variant-numeric: tabular-nums;
  line-height: 1;
  color: var(--ds-foil);
  background: var(--ds-slate-900);
  border-radius: 2px;
}

.error-title {
  margin: 8px 0 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--ds-ink);
}

.error-desc {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--ds-ink-2);
}

.btn:focus-visible {
  outline: none;
  box-shadow: var(--ds-focus);
}
</style>
