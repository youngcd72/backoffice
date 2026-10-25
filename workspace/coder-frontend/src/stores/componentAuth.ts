/*****************************************************************************************
	Program ID   : componentAuth
	Program Type : ts
	Program Name : 컴포넌트 권한 스토어
	Description  : 프로그램별 로그인 사용자의 컴포넌트(버튼 등) 권한을 조회·캐시하는 Pinia 스토어 (v-permission)
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-19  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { defineStore } from 'pinia';
import { ref } from 'vue';
import { getComponentAuthListApi } from '@/api/common/componentAuth';

/**
 * useComponentAuthStore : 컴포넌트 권한 Pinia 스토어 정의: 프로그램별 권한 캐시와 조회/판별 동작 제공
 * @return  N/A
 */
export const useComponentAuthStore = defineStore('componentAuth', () => {
  /** 프로그램ID → (컴포넌트ID → 권한여부). 키가 없으면 아직 조회 전 */
  const authMap = ref<Record<string, Record<string, boolean>>>({});
  const pendingMap = new Map<string, Promise<void>>();

  /**
   * loadComponentAuth : 프로그램의 컴포넌트 권한을 조회해 캐시 (이미 조회했거나 조회 중이면 다시 호출하지 않음)
   * @param   prgmId - 프로그램ID
   * @return  N/A
   */
  function loadComponentAuth(prgmId: string): Promise<void> {
    if (authMap.value[prgmId]) return Promise.resolve();
    const pending = pendingMap.get(prgmId);
    if (pending) return pending;

    const request = getComponentAuthListApi(prgmId)
      .then((list) => {
        authMap.value[prgmId] = Object.fromEntries(list.map((item) => [item.cmpnId, item.authYn === 'Y']));
      })
      .catch((err) => {
        // 조회 실패 시 통제 대상이 없는 것으로 보고 모두 노출 (화면 버튼이 전부 사라지지 않도록)
        console.warn(`[CODER] 컴포넌트 권한을 불러오지 못했습니다. (${prgmId})`, err);
        authMap.value[prgmId] = {};
      })
      .finally(() => pendingMap.delete(prgmId));
    pendingMap.set(prgmId, request);
    return request;
  }

  /**
   * isComponentAllowed : 컴포넌트 노출 여부 판별 (TB_PRGM_CMPN_M 에 등록되지 않은 컴포넌트는 노출)
   * @param   prgmId - 프로그램ID
   * @param   cmpnId - 컴포넌트ID
   * @return  노출하면 true, 숨기면 false, 아직 조회 전이면 null
   */
  function isComponentAllowed(prgmId: string, cmpnId: string): boolean | null {
    const cmpnAuth = authMap.value[prgmId];
    if (!cmpnAuth) return null;
    return cmpnAuth[cmpnId] ?? true;
  }

  /**
   * clearComponentAuth : 캐시한 컴포넌트 권한을 모두 삭제 (로그인/로그아웃 시)
   * @return  N/A
   */
  function clearComponentAuth(): void {
    authMap.value = {};
    pendingMap.clear();
  }

  return { authMap, loadComponentAuth, isComponentAllowed, clearComponentAuth };
});
