/*****************************************************************************************
	Program ID   : permission
	Program Type : ts
	Program Name : 컴포넌트 권한 디렉티브
	Description  : v-permission — 로그인 사용자에게 컴포넌트 권한이 없으면 엘리먼트를 숨김
	               - 컴포넌트ID: 디렉티브 값 또는 엘리먼트 id (TB_PRGM_CMPN_M.CMPN_ID)
	               - 프로그램ID: 디렉티브 인자 또는 현재 라우트 이름 (TB_PRGM_CMPN_M.PRGM_ID)
	               - 권한: 권한그룹(TB_AUTH_GRP_CMPN_R)·사용자(TB_USR_CMPN_R) 중 하나라도 AUTH_YN='Y' 이면 노출
	               - TB_PRGM_CMPN_M 에 사용 중으로 등록되지 않은 컴포넌트는 항상 노출
	               - 사용 예: <Button id="btnSave" v-permission>, <Button v-permission="'btnSave'">, <Button id="btnSave" v-permission:SYPG010>
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-19  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { watchEffect, type Directive, type WatchStopHandle } from 'vue';
import router from '@/router';
import { useComponentAuthStore } from '@/stores/componentAuth';

/** 엘리먼트별 권한 감시 해제 함수 */
const stopMap = new WeakMap<HTMLElement, WatchStopHandle>();

/**
 * vPermission : 컴포넌트 권한이 없으면 엘리먼트를 숨기는 디렉티브 (권한 조회 전에도 숨김)
 * @return  N/A
 */
export const vPermission: Directive<HTMLElement, string | undefined> = {
  mounted(el, binding) {
    const cmpnId = binding.value || el.id;
    // <script setup> 컴포넌트는 binding.instance 에 $route 가 노출되지 않으므로 라우터에서 현재 라우트를 읽는다
    const prgmId = binding.arg || String(router.currentRoute.value.name ?? '');
    if (!cmpnId || !prgmId) {
      console.warn('[v-permission] 컴포넌트ID 또는 프로그램ID를 알 수 없어 권한을 적용하지 않습니다.', el);
      return;
    }

    const componentAuthStore = useComponentAuthStore();
    const originDisplay = el.style.display;
    void componentAuthStore.loadComponentAuth(prgmId);
    stopMap.set(el, watchEffect(() => {
      el.style.display = componentAuthStore.isComponentAllowed(prgmId, cmpnId) === true ? originDisplay : 'none';
    }));
  },
  unmounted(el) {
    stopMap.get(el)?.();
    stopMap.delete(el);
  },
};

declare module 'vue' {
  interface GlobalDirectives {
    vPermission: typeof vPermission;
  }
}
