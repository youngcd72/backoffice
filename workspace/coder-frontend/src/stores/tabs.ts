/*****************************************************************************************
	Program ID   : tabs
	Program Type : ts
	Program Name : MDI 탭 스토어
	Description  : MDI 방식 화면 탭 목록과 활성 탭을 관리하는 Pinia 스토어
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-09-20  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export interface MdiTab {
  id: string;
  label: string;
  path: string;
  topId: string;
}

/**
 * useTabsStore : MDI 탭 Pinia 스토어 정의: 탭 목록·활성 탭 상태와 열기/활성화/닫기 동작 제공
 * @return  N/A
 */
export const useTabsStore = defineStore('tabs', () => {
  const tabs = ref<MdiTab[]>([]);
  const activeTabId = ref('');

  const activeTab = computed(() => tabs.value.find((t) => t.id === activeTabId.value) || null);

  /**
   * openTab : 탭을 열고 활성화 (이미 열려 있으면 활성화만 수행)
   * @param   tab - 열 탭 정보
   * @return  N/A
   */
  function openTab(tab: MdiTab): void {
    if (!tabs.value.some((t) => t.id === tab.id)) {
      tabs.value.push(tab);
    }
    activeTabId.value = tab.id;
  }

  /**
   * activateTab : 열려 있는 탭을 활성화
   * @param   id - 활성화할 탭 ID
   * @return  N/A
   */
  function activateTab(id: string): void {
    if (tabs.value.some((t) => t.id === id)) activeTabId.value = id;
  }

  /**
   * closeTab : 탭을 닫고, 활성 탭이었다면 인접 탭을 활성화
   * @param   id - 닫을 탭 ID
   * @return  새로 활성화된 탭 (없으면 null)
   */
  function closeTab(id: string): MdiTab | null {
    const idx = tabs.value.findIndex((t) => t.id === id);
    if (idx < 0) return activeTab.value;
    const wasActive = activeTabId.value === id;
    tabs.value.splice(idx, 1);
    if (wasActive) {
      const next = tabs.value[idx] || tabs.value[idx - 1] || null;
      activeTabId.value = next ? next.id : '';
      return next;
    }
    return activeTab.value;
  }

  /**
   * closeAllTabs : 모든 탭을 닫고 활성 탭을 초기화
   * @return  N/A
   */
  function closeAllTabs(): void {
    tabs.value = [];
    activeTabId.value = '';
  }

  /**
   * clearActiveTab : 탭은 그대로 두고 활성 탭만 해제 (홈 탭으로 이동할 때 사용)
   * @return  N/A
   */
  function clearActiveTab(): void {
    activeTabId.value = '';
  }

  return { tabs, activeTabId, activeTab, openTab, activateTab, closeTab, closeAllTabs, clearActiveTab };
});
