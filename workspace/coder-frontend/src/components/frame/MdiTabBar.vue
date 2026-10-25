<script setup lang="ts">
/*****************************************************************************************
	Program ID   : MdiTabBar
	Program Type : vue
	Program Name : MDI 탭바
	Description  : 열린 화면 탭 목록 표시, 스크롤, 선택/닫기/홈/전체닫기 이벤트 제공
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-09-20  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
import { ref, watch, nextTick } from 'vue'
import type { MdiTab } from '@/stores/tabs'

const props = defineProps<{
  tabs: MdiTab[]
  activeTabId: string
  /** 홈(대시보드) 화면이 열려 있으면 true — 홈 탭을 활성 탭으로 표시 */
  homeActive?: boolean
}>()

const emit = defineEmits<{
  (e: 'home'): void
  (e: 'select', tab: MdiTab): void
  (e: 'close', id: string): void
  (e: 'close-all'): void
}>()

const SCROLL_STEP = 160
const tabScroller = ref<HTMLElement | null>(null)

/**
 * scrollTabs : 탭 영역을 좌우로 부드럽게 스크롤
 * @param   delta - 스크롤 이동량(px, 음수는 왼쪽)
 * @return  N/A
 */
function scrollTabs(delta: number): void {
  tabScroller.value?.scrollBy({ left: delta, behavior: 'smooth' })
}

/**
 * onBtnTabPrevClick : 이전 버튼 클릭 시 탭 영역을 왼쪽으로 스크롤
 * @return  N/A
 */
function onBtnTabPrevClick(): void {
  scrollTabs(-SCROLL_STEP)
}

/**
 * onBtnTabNextClick : 다음 버튼 클릭 시 탭 영역을 오른쪽으로 스크롤
 * @return  N/A
 */
function onBtnTabNextClick(): void {
  scrollTabs(SCROLL_STEP)
}

/**
 * onBtnTabHomeClick : 홈 버튼 클릭 시 home 이벤트를 발생
 * @return  N/A
 */
function onBtnTabHomeClick(): void {
  emit('home')
}

/**
 * onBtnTabCloseAllClick : 전체 닫기 버튼 클릭 시 close-all 이벤트를 발생
 * @return  N/A
 */
function onBtnTabCloseAllClick(): void {
  emit('close-all')
}

/**
 * onTabItemClick : 탭 클릭 시 select 이벤트를 발생
 * @param   tab - 클릭된 탭
 * @return  N/A
 */
function onTabItemClick(tab: MdiTab): void {
  emit('select', tab)
}

/**
 * onBtnTabCloseClick : 탭 닫기 버튼 클릭 시 close 이벤트를 발생
 * @param   id - 닫을 탭 ID
 * @return  N/A
 */
function onBtnTabCloseClick(id: string): void {
  emit('close', id)
}

// 활성 탭이 바뀌면 보이는 영역으로 스크롤한다.
watch(
  () => [props.activeTabId, props.tabs.length],
  async () => {
    await nextTick()
    tabScroller.value?.querySelector('.mdi-tab.active')?.scrollIntoView({ block: 'nearest', inline: 'nearest' })
  },
)
</script>

<template>
  <div class="mdi-tab-bar">
    <!-- 홈(대시보드) 고정 탭: 닫기 버튼 없음 -->
    <button
      id="btnTabHome"
      type="button"
      class="mdi-home-btn"
      :class="{ active: homeActive }"
      :aria-current="homeActive ? 'page' : undefined"
      title="홈"
      @click="onBtnTabHomeClick"
    >
      <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
        <path d="M12 3 2 12h3v8h5v-6h4v6h5v-8h3z" />
      </svg>
    </button>

    <div ref="tabScroller" class="mdi-tab-scroller">
      <div
        v-for="tab in tabs"
        :key="tab.id"
        class="mdi-tab"
        :class="{ active: tab.id === activeTabId }"
        :title="tab.label"
        @click="onTabItemClick(tab)"
      >
        <span class="mdi-tab-label">{{ tab.label }}</span>
        <button
          type="button"
          class="mdi-tab-close"
          title="닫기"
          @click.stop="onBtnTabCloseClick(tab.id)"
        >×</button>
      </div>
    </div>

    <div class="mdi-tab-nav">
      <button id="btnTabPrev" type="button" class="mdi-nav-btn" title="이전 탭" @click="onBtnTabPrevClick">‹</button>
      <button id="btnTabNext" type="button" class="mdi-nav-btn" title="다음 탭" @click="onBtnTabNextClick">›</button>
      <button id="btnTabCloseAll" type="button" class="mdi-nav-btn" title="모든 탭 닫기" @click="onBtnTabCloseAllClick">
        <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.6">
          <line x1="6" y1="6" x2="18" y2="18" /><line x1="18" y1="6" x2="6" y2="18" />
        </svg>
      </button>
    </div>
  </div>
</template>

<style scoped>
.mdi-tab-bar {
  height: 40px;
  min-height: 40px;
  display: flex;
  align-items: flex-end;
  gap: 4px;
  padding: 0 12px;
  background: var(--ds-ground);
  border-bottom: 1px solid var(--ds-rule-strong);
  box-sizing: border-box;
}

.mdi-home-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  margin-bottom: 4px;
  border: 0;
  border-radius: var(--ds-radius-control);
  background: transparent;
  color: var(--ds-ink-2);
  cursor: pointer;
  transition: color var(--ds-ease);
}
.mdi-home-btn:hover { color: var(--ds-pine-700); }
/* 활성 탭은 탭 바 아래 괘선 위에 선다 (본문 바탕이 ground 라 괘선을 덮어 잇지 않는다) */
.mdi-home-btn.active {
  height: 32px;
  width: 40px;
  margin-bottom: 0;
  border-radius: 0;
  color: var(--ds-pine-900);
  background: var(--ds-surface);
  box-shadow: inset 0 3px 0 var(--ds-sign);
}

.mdi-tab-scroller {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: flex-end;
  gap: 0;
  overflow-x: hidden;
}

.mdi-tab {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 32px;
  min-width: 96px;
  max-width: 180px;
  padding: 0 8px 0 12px;
  font-size: 13px;
  color: var(--ds-ink-2);
  background: transparent;
  border: 0;
  border-radius: 0;
  cursor: pointer;
  box-sizing: border-box;
  transition: background-color var(--ds-ease), color var(--ds-ease);
}
.mdi-tab:hover { background: var(--ds-pine-050); color: var(--ds-ink); }
.mdi-tab.active {
  font-weight: 600;
  color: var(--ds-pine-900);
  background: var(--ds-surface);
  box-shadow: inset 0 3px 0 var(--ds-sign);
}

.mdi-tab-label {
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.mdi-tab-close {
  width: 16px;
  height: 16px;
  padding: 0;
  line-height: 14px;
  font-size: 14px;
  border: 0;
  border-radius: 2px;
  background: transparent;
  color: var(--ds-ink-3);
  cursor: pointer;
  transition: background-color var(--ds-ease), color var(--ds-ease);
}
.mdi-tab-close:hover { background: var(--ds-stamp); color: var(--ds-ink-inverse); }

.mdi-tab-nav {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-bottom: 7px;
}

.mdi-nav-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  font-size: 16px;
  line-height: 1;
  border: 1px solid var(--ds-rule);
  border-radius: var(--ds-radius-control);
  background: var(--ds-surface);
  color: var(--ds-ink-2);
  cursor: pointer;
  transition: background-color var(--ds-ease), border-color var(--ds-ease), color var(--ds-ease);
}
.mdi-nav-btn:hover {
  background: var(--ds-pine-050);
  border-color: var(--ds-rule-strong);
  color: var(--ds-ink);
}

</style>
