/*****************************************************************************************
	Program ID   : index
	Program Type : ts
	Program Name : 라우터 설정
	Description  : 화면 라우트 정의 및 JWT 기반 네비게이션 가드(인증 필요 화면/로그인 화면 이동 제어)
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import DashboardView from '@/views/main/DashboardView.vue';
import LoginView from '@/views/main/LoginView.vue';
import ComponentExample from '@/views/template/ComponentExample.vue';
import ProgramManageView from '@/views/system/menu/ProgramManageView.vue';
import MenuManageView from '@/views/system/menu/MenuManageView.vue';
import AuthGroupManageView from '@/views/system/auths/AuthGroupManageView.vue';
import UserAuthManageView from '@/views/system/auths/UserAuthManageView.vue';
import AuthGroupUserManageView from '@/views/system/auths/AuthGroupUserManageView.vue';
import CommonCodeManageView from '@/views/master/basic/CommonCodeManageView.vue';
import BatchManageView from '@/views/system/links/BatchManageView.vue';
import SystemLogManageView from '@/views/system/logs/SystemLogManageView.vue';
import NotFoundView from '@/views/errors/NotFoundView.vue';
import { isValidJwt } from '@/stores/auth';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard',
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: DashboardView,
    meta: { title: '대시보드 - CODER', requiresAuth: true },
  },
  {
    path: '/template/component-example',
    name: 'ComponentExample',
    component: ComponentExample,
    meta: { title: '컴포넌트 사용예제 - CODER', requiresAuth: true },
  },
  {
    path: '/sy/sypg020',
    name: 'SYPG020',
    component: MenuManageView,
    meta: { title: '메뉴관리 - CODER', requiresAuth: true },
  },
  {
    path: '/sy/sypg010',
    name: 'SYPG010',
    component: ProgramManageView,
    meta: { title: '프로그램관리 - CODER', requiresAuth: true },
  },
  {
    path: '/sy/syau010',
    name: 'SYAU010',
    component: UserAuthManageView,
    meta: { title: '사용자권한관리 - CODER', requiresAuth: true },
  },
  {
    path: '/sy/syau020',
    name: 'SYAU020',
    component: AuthGroupManageView,
    meta: { title: '그룹권한관리 - CODER', requiresAuth: true },
  },
  {
    path: '/sy/syau030',
    name: 'SYAU030',
    component: AuthGroupUserManageView,
    meta: { title: '그룹별사용자관리 - CODER', requiresAuth: true },
  },
  {
    path: '/ms/msbs010',
    name: 'MSBS010',
    component: CommonCodeManageView,
    meta: { title: '공통코드관리 - CODER', requiresAuth: true },
  },
  {
    path: '/sy/sylo030',
    name: 'SYLO030',
    component: SystemLogManageView,
    meta: { title: '시스템로그관리 - CODER', requiresAuth: true },
  },
  {
    path: '/sy/sylk050',
    name: 'SYLK050',
    component: BatchManageView,
    meta: { title: '배치관리 - CODER', requiresAuth: true },
  },
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
    meta: { title: '로그인 - CODER', guestOnly: true },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFoundView,
    meta: { title: '페이지를 찾을 수 없음 - CODER' },
  },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
});

// Navigation Guard: 올바른 JWT 검증
router.beforeEach((to) => {
  if (to.meta.title) {
    document.title = to.meta.title as string;
  }

  const token = localStorage.getItem('coder_token');
  const hasValidJwt = isValidJwt(token);

  // 로그인 페이지 접속 시 이미 유효한 JWT가 있는 경우 대시보드로 이동
  if (to.path === '/login' && hasValidJwt) {
    return '/dashboard';
  }

  // 인증이 필요한 페이지 접속 시 올바른 JWT가 없는 경우 로그인 화면으로 이동
  if (to.path !== '/login' && !hasValidJwt) {
    return '/login';
  }

  return true;
});

export default router;
