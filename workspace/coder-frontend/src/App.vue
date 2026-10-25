<script setup lang="ts">
/*****************************************************************************************
	Program ID   : App
	Program Type : vue
	Program Name : 앱 루트
	Description  : 로그인 화면은 단독으로, 그 외 화면은 MainLayout 안에 표시하는 최상위 컴포넌트
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { MainLayout } from '@/layouts';
import { useAuthStore } from '@/stores/auth';
import { DialogHost, ToastHost } from '@/components/common';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

// 로그인 화면 경로인 경우 MainLayout 없이 단독 표시
const isAuthRoute = computed(() => route.path === '/login');

/**
 * handleLogout : 로그아웃 처리 후 로그인 화면으로 이동
 * @return  N/A
 */
const handleLogout = () => {
  authStore.logout();
  router.push('/login');
};

/**
 * handleSelectTopMenu : 상위 메뉴 선택 시 메뉴 ID에 해당하는 화면 경로로 이동
 * @param   menuId - 선택된 상위 메뉴 ID
 * @return  N/A
 */
const handleSelectTopMenu = (menuId: string) => {
  if (menuId === 'template') router.push('/template/component-example');
};
</script>

<template>
  <div class="app-root">
    <!-- 1. 로그인 라우트인 경우: 로그인 뷰포트 단독 표시 -->
    <RouterView v-if="isAuthRoute" />

    <!-- 2. 인증된 일반 라우트인 경우: Top / Left / Center / Bottom 레이아웃 적용 -->
    <MainLayout
      v-else
      :user="authStore.user"
      @logout="handleLogout"
      @select-top-menu="handleSelectTopMenu"
    >
      <RouterView />
    </MainLayout>

    <!-- 전역 alert / confirm 창 (useDialog) -->
    <DialogHost />

    <!-- 전역 토스트 알림 (useToast) -->
    <ToastHost />
  </div>
</template>

<style>
html, body, #app, .app-root {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  background: var(--ds-ground);
  color: var(--ds-ink);
  overflow: hidden;
}
</style>
