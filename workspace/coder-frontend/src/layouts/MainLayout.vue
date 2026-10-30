<template>
  <div class="app-layout" :class="{ 'sidebar-collapsed': isSidebarCollapsed }">
    <!-- 1. TOP BAR : 브랜드 + 사용자 정보 / 로그아웃 -->
    <div class="top-bar">
      <div class="brand" @click="handleBrandClick">
        <BrandLogo :size="20" class="brand-title" />
        <span class="brand-subtitle">Freshly brewed code</span>
      </div>

      <div class="top-bar-right">
        <span class="welcome-text"><strong>{{ user?.name }}</strong>{{ msg('cm.common.welcome.suffix') }}</span>
        <button type="button" class="top-text-btn" @click="handleLogout" title="로그아웃">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="11" width="18" height="10" rx="2" ry="2"></rect>
            <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
          </svg>
          <span>로그아웃</span>
        </button>
      </div>
    </div>

    <!-- 2. GNB BAR : 대분류 메뉴 + 검색 -->
    <div class="gnb-bar">
      <button
        type="button"
        class="sidebar-toggle-btn"
        :title="isSidebarCollapsed ? '사이드바 펼치기' : '사이드바 접기'"
        @click="toggleSidebar"
      >
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="3" y1="12" x2="21" y2="12"></line>
          <line x1="3" y1="6" x2="21" y2="6"></line>
          <line x1="3" y1="18" x2="21" y2="18"></line>
        </svg>
      </button>

      <nav class="gnb-nav">
        <button
          v-for="cat in topCategories"
          :key="cat.id"
          type="button"
          class="gnb-item"
          :class="{ active: !isHomeActive && selectedTopCategoryId === cat.id }"
          @click="selectTopCategory(cat.id)"
        >
          <span class="gnb-label">{{ cat.label }}</span>
          <span v-if="cat.badge" class="gnb-badge">{{ cat.badge }}</span>
        </button>
      </nav>

      <!-- 메뉴 검색: 권한 있는 메뉴의 메뉴명으로 검색, 선택하면 대메뉴·왼쪽 메뉴를 선택하고 화면을 연다 -->
      <div class="gnb-search-wrap">
        <div class="gnb-search">
          <input
            id="txtMenuSrch"
            v-model="menuSrchKeyword"
            type="text"
            placeholder="메뉴명을 입력하세요."
            class="gnb-search-input"
            autocomplete="off"
            role="combobox"
            aria-controls="menuSrchResult"
            :aria-expanded="isMenuSrchOpen"
            @input="onTxtMenuSrchInput"
            @focus="onTxtMenuSrchFocus"
            @blur="onTxtMenuSrchBlur"
            @keydown="onTxtMenuSrchKeydown"
          />
          <button id="btnMenuSrch" type="button" class="gnb-search-btn" title="메뉴 검색" @click="onBtnMenuSrchClick">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.4">
              <circle cx="11" cy="11" r="7"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
          </button>
        </div>

        <ul v-if="isMenuSrchOpen" id="menuSrchResult" class="gnb-search-result" role="listbox">
          <li v-if="isMenuLoading" class="gnb-search-empty">{{ msg('cm.menu.search.loading') }}</li>
          <li v-else-if="menuLoadError" class="gnb-search-empty">{{ msg('cm.menu.search.load.failed') }}</li>
          <li v-else-if="menuSrchResults.length === 0" class="gnb-search-empty">{{ msg('cm.menu.search.empty') }}</li>
          <template v-else>
            <li
              v-for="(item, idx) in menuSrchResults"
              :key="item.menuId"
              class="gnb-search-item"
              :class="{ active: idx === menuSrchActiveIndex }"
              role="option"
              :aria-selected="idx === menuSrchActiveIndex"
              @mousedown.prevent="selectSearchMenu(item)"
              @mouseenter="menuSrchActiveIndex = idx"
            >
              <span class="gnb-search-item-label">{{ item.menuNm }}</span>
              <span class="gnb-search-item-path">{{ item.pathLabel }}</span>
            </li>
          </template>
        </ul>
      </div>

      <!-- 알림 센터: SSE 로 미확인 건수를 실시간 표시, 알람 클릭 시 연결된 화면을 연다 -->
      <AlarmCenter @open-program="onAlarmCenterOpenProgram" />
    </div>

    <!-- 3. BODY : Left (Sidebar) + Center (Main Content) -->
    <div class="layout-body">
      <!-- 3-1. LEFT (사이드바) -->
      <!-- 접으면 사이드바 전체를 숨긴다 (아이콘·약어만 남기지 않음) -->
      <aside v-show="!isSidebarCollapsed" class="layout-sidebar">
        <div class="sidebar-tabs" role="tablist">
          <button
            id="tabSidebarMenu"
            type="button"
            role="tab"
            class="sidebar-tab"
            :class="{ active: sidebarTab === 'menu' }"
            :aria-selected="sidebarTab === 'menu'"
            @click="onTabSidebarClick('menu')"
          >메뉴</button>
          <button
            id="tabSidebarFavorite"
            type="button"
            role="tab"
            class="sidebar-tab"
            :class="{ active: sidebarTab === 'favorite' }"
            :aria-selected="sidebarTab === 'favorite'"
            @click="onTabSidebarClick('favorite')"
          >즐겨찾기</button>
        </div>

        <!-- 즐겨찾기 탭: TB_PRGM_FAV_L 에서 로그인 사용자의 즐겨찾기 메뉴를 조회해 표시 -->
        <template v-if="sidebarTab === 'favorite'">
          <div class="sidebar-header">
            <span class="sidebar-header-dot"></span>
            <span class="sidebar-title">즐겨찾기</span>
            <span class="sidebar-count">{{ favorites.length }}개</span>
          </div>

          <nav class="sidebar-nav">
            <div v-if="isFavoriteLoading" class="sidebar-empty">{{ msg('cm.favorite.loading') }}</div>
            <div v-else-if="favoriteError" class="sidebar-empty">{{ favoriteError }}</div>
            <div v-else-if="favorites.length === 0" class="sidebar-empty">{{ msg('cm.favorite.empty') }}</div>
            <template v-else>
              <button
                v-for="fav in favorites"
                :key="fav.menuId"
                type="button"
                class="sidebar-item"
                :class="{ active: selectedSubMenuId === findMenuEntry(fav)?.sub.id }"
                @click="onFavoriteItemClick(fav)"
              >
                <span class="menu-label">{{ fav.menuNm || fav.menuId }}</span>
              </button>
            </template>
          </nav>
        </template>

        <div v-if="sidebarTab === 'menu'" class="sidebar-header">
          <span class="sidebar-header-dot"></span>
          <span class="sidebar-title">{{ currentTopCategory?.label || '메뉴' }}</span>
          <span class="sidebar-count">{{ currentSubMenus.length }}개</span>
        </div>

        <!-- 메뉴 탭: 로그인 사용자에게 권한이 있는 메뉴(권한그룹 + 사용자 메뉴 권한)만 표시 -->
        <nav v-if="sidebarTab === 'menu'" class="sidebar-nav">
          <div v-if="isMenuLoading" class="sidebar-empty">{{ msg('cm.menu.loading') }}</div>
          <div v-else-if="menuLoadError" class="sidebar-empty">{{ msg('cm.menu.load.failed') }}</div>
          <div v-else-if="topCategories.length === 0" class="sidebar-empty">{{ msg('cm.menu.empty') }}</div>
          <template v-else>
            <div
              v-for="menu in currentSubMenus"
              :key="menu.id"
              class="nav-group"
              :class="{ 'has-group-title': menu.groupTitle }"
            >
              <div v-if="menu.groupTitle" class="group-title">
                {{ menu.groupTitle }}
              </div>

              <button
                type="button"
                class="sidebar-item"
                :class="{ active: selectedSubMenuId === menu.id }"
                @click="selectSubMenu(menu.id)"
              >
                <span class="menu-label">{{ menu.label }}</span>
              </button>
            </div>
          </template>
        </nav>
      </aside>

      <!-- 3-2. CENTER (메인 화면 영역) -->
      <main class="layout-content">
        <MdiTabBar
          :tabs="tabsStore.tabs"
          :active-tab-id="isHomeActive ? '' : tabsStore.activeTabId"
          :home-active="isHomeActive"
          @home="onTabHome"
          @select="onTabSelect"
          @close="onTabClose"
          @close-all="onTabCloseAll"
        />

        <div class="content-viewport">
          <slot>
            <div class="default-placeholder">
              <p class="placeholder-path">{{ currentTopCategory?.label }} &gt; {{ currentSubMenu?.label }}</p>
              <p class="placeholder-text">{{ msg('cm.info.select.menu') }}</p>
            </div>
          </slot>
        </div>
      </main>
    </div>

    <!-- 4. BOTTOM (하단 상태바) -->
    <footer class="layout-footer">
      <div class="footer-left">
        <div class="status-indicator online">
          <span class="status-dot"></span>
          <span class="status-label">CODER Core Backend 연결 정상</span>
        </div>
        <span class="footer-sep">|</span>
        <span class="status-detail">네트워크 응답: 14ms</span>
        <span class="footer-sep">|</span>
        <span class="status-detail">보안 세션: 암호화 적용됨</span>
      </div>

      <div class="footer-center">
        <span class="system-notice">CODER Freshly brewed code</span>
      </div>

      <div class="footer-right">
        <span class="current-time">{{ currentTimeString }}</span>
        <span class="footer-sep">|</span>
        <span class="version-tag">CODER-CORE v1.2.0</span>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
/*****************************************************************************************
	Program ID   : MainLayout
	Program Type : vue
	Program Name : 메인 레이아웃
	Description  : 상위/하위 메뉴, MDI 탭, 사이드바, 현재 시각을 포함한 업무 화면 공통 프레임
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
import { msg } from '@/utils/message';
import { formatDttm } from '@/utils/date';
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import MdiTabBar from '@/components/frame/MdiTabBar.vue';
import AlarmCenter from '@/components/frame/AlarmCenter.vue';
import { BrandLogo } from '@/components';
import { useTabsStore, type MdiTab } from '@/stores/tabs';
import { useAlarmStore } from '@/stores/alarm';
import type { Alarm } from '@/api/common/alarm';
import { useDialog } from '@/composables/useDialog';
import { getFavoriteListApi, type Favorite } from '@/api/common/favorite';
import { getAuthMenuListApi, type AuthMenu } from '@/api/common/authMenu';

interface UserInfo {
  name?: string;
  role?: string;
  email?: string;
}

/** 상단 메뉴 (TB_MENU_M 최상위 메뉴) */
interface TopMenu {
  id: string;
  label: string;
  badge?: string;
}

/** 사이드바 화면 메뉴 (TB_MENU_M 에서 프로그램이 연결된 메뉴) */
interface SubMenu {
  /** 메뉴ID */
  id: string;
  label: string;
  prgmId: string;
  /** 화면 경로 (라우트 이름 = 프로그램ID) */
  path: string;
  /** 중분류 메뉴명: 같은 중분류의 첫 번째 메뉴에만 표시 */
  groupTitle?: string;
  /** 상위 메뉴 경로 (예: 시스템관리 > LOG관리) */
  pathLabel: string;
}

withDefaults(
  defineProps<{
    user?: UserInfo | null;
  }>(),
  {
    user: null,
  }
);

const emit = defineEmits<{
  (e: 'logout'): void;
  (e: 'select-top-menu', id: string): void;
  (e: 'select-sub-menu', id: string): void;
}>();

const route = useRoute();
const router = useRouter();

// 로그인 사용자의 권한 메뉴 (TB_MENU_M, 권한 있는 화면 메뉴와 그 상위 메뉴)
const authMenuList = ref<AuthMenu[]>([]);
const isMenuLoading = ref(false);
const menuLoadError = ref(false);

/**
 * resolveProgramPath : 프로그램ID에 해당하는 화면 경로를 찾음 (라우트 이름 = 프로그램ID, SYPG010_T01 처럼 접미사가 붙으면 화면 ID 부분으로 대조)
 * @param   prgmId - 프로그램ID
 * @return  화면 경로 (등록된 화면이 없으면 undefined)
 */
const resolveProgramPath = (prgmId: string): string | undefined => {
  const name = [prgmId, prgmId.split('_')[0]].find((id) => router.hasRoute(id));
  return name ? router.resolve({ name }).path : undefined;
};

/**
 * 권한 메뉴로 구성한 상단 메뉴와 상단 메뉴별 화면 메뉴
 * 최상위 메뉴 → 상단 메뉴, 그 아래 화면 메뉴 → 사이드바 (중간 단계 메뉴는 중분류 제목), 화면이 등록되지 않은 메뉴는 제외
 */
const menuTree = computed(() => {
  const childrenMap = new Map<string, AuthMenu[]>();
  const roots: AuthMenu[] = [];
  for (const menu of authMenuList.value) {
    if (!menu.upprMenuId) {
      roots.push(menu);
      continue;
    }
    const children = childrenMap.get(menu.upprMenuId) ?? [];
    children.push(menu);
    childrenMap.set(menu.upprMenuId, children);
  }
  const bySeq = (a: AuthMenu, b: AuthMenu) => (a.alinSeq ?? Number.MAX_SAFE_INTEGER) - (b.alinSeq ?? Number.MAX_SAFE_INTEGER);

  const topMenus: TopMenu[] = [];
  const subMenuMap: Record<string, SubMenu[]> = {};
  for (const root of [...roots].sort(bySeq)) {
    const subs: SubMenu[] = [];
    let lastGroupTitle = '';
    const visited = new Set<string>([root.menuId]);
    const walk = (parent: AuthMenu, groups: AuthMenu[]) => {
      for (const menu of [...(childrenMap.get(parent.menuId) ?? [])].sort(bySeq)) {
        if (visited.has(menu.menuId)) continue;
        visited.add(menu.menuId);
        if (!menu.prgmId) {
          walk(menu, [...groups, menu]);
          continue;
        }
        const path = resolveProgramPath(menu.prgmId);
        if (!path) continue;
        const groupTitle = groups.map((g) => g.menuNm).join(' > ');
        subs.push({
          id: menu.menuId,
          label: menu.menuNm,
          prgmId: menu.prgmId,
          path,
          groupTitle: groupTitle && groupTitle !== lastGroupTitle ? groupTitle : undefined,
          pathLabel: [root, ...groups].map((m) => m.menuNm).join(' > '),
        });
        lastGroupTitle = groupTitle;
      }
    };
    walk(root, []);
    if (subs.length === 0) continue;
    topMenus.push({ id: root.menuId, label: root.menuNm });
    subMenuMap[root.menuId] = subs;
  }
  return { topMenus, subMenuMap };
});

const topCategories = computed(() => menuTree.value.topMenus);
const subMenuMap = computed(() => menuTree.value.subMenuMap);
const selectedTopCategoryId = ref('');

const currentSubMenus = computed(() => {
  return subMenuMap.value[selectedTopCategoryId.value] || [];
});

const selectedSubMenuId = ref('');

const currentTopCategory = computed(() => {
  return topCategories.value.find((c) => c.id === selectedTopCategoryId.value);
});

const currentSubMenu = computed(() => {
  return currentSubMenus.value.find((m) => m.id === selectedSubMenuId.value) || currentSubMenus.value[0];
});

const isSidebarCollapsed = ref(false);
/**
 * toggleSidebar : 사이드바 접힘/펼침 상태를 전환
 * @return  N/A
 */
const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value;
};

const tabsStore = useTabsStore();

// 홈(대시보드) 탭: MDI 탭 맨 앞의 고정 탭으로, 닫을 수 없다
const HOME_PATH = '/dashboard';
const isHomeActive = computed(() => route.path === HOME_PATH);

/**
 * goToPath : 현재 경로와 다른 경우에만 해당 경로로 이동
 * @param   path - 이동할 화면 경로
 * @return  N/A
 */
const goToPath = (path: string) => {
  if (route.path !== path) router.push(path);
};

/**
 * openMenuTab : 서브메뉴에 해당하는 MDI 탭을 연다 (이미 열려 있으면 활성화)
 * @param   topId - 상위 메뉴 ID
 * @param   sub   - 탭으로 열 서브메뉴 (id, label, path)
 * @return  N/A
 */
const openMenuTab = (topId: string, sub: SubMenu) => {
  tabsStore.openTab({
    id: sub.id,
    label: sub.label,
    path: sub.path,
    topId,
  });
};

/**
 * selectTopCategory : 상위 메뉴를 선택하고 첫 번째 서브메뉴의 탭을 열어 화면 이동
 * @param   catId - 선택할 상위 메뉴 ID
 * @return  N/A
 */
const selectTopCategory = (catId: string) => {
  selectedTopCategoryId.value = catId;
  emit('select-top-menu', catId);
  const subList = subMenuMap.value[catId] || [];
  if (subList.length > 0) {
    selectedSubMenuId.value = subList[0].id;
    openMenuTab(catId, subList[0]);
    goToPath(subList[0].path);
    emit('select-sub-menu', subList[0].id);
  }
};

/**
 * selectSubMenu : 서브메뉴를 선택하고 해당 탭을 열어 화면 이동
 * @param   subId - 선택할 서브메뉴 ID
 * @return  N/A
 */
const selectSubMenu = (subId: string) => {
  selectedSubMenuId.value = subId;
  const sub = currentSubMenus.value.find((m) => m.id === subId);
  if (sub) {
    openMenuTab(selectedTopCategoryId.value, sub);
    goToPath(sub.path);
  }
  emit('select-sub-menu', subId);
};

/**
 * onTabSelect : MDI 탭 선택 시 탭을 활성화하고 메뉴 선택 상태와 화면 경로를 맞춤
 * @param   tab - 선택된 MDI 탭
 * @return  N/A
 */
const onTabSelect = (tab: MdiTab) => {
  tabsStore.activateTab(tab.id);
  selectedTopCategoryId.value = tab.topId;
  selectedSubMenuId.value = tab.id;
  goToPath(tab.path);
};

/**
 * onTabClose : MDI 탭 닫기 시 탭을 닫고 다음 탭으로 이동 (없으면 대시보드)
 * @param   id - 닫을 탭 ID
 * @return  N/A
 */
const onTabClose = (id: string) => {
  const next = tabsStore.closeTab(id);
  if (next) onTabSelect(next);
  else onTabHome();
};

/**
 * onTabCloseAll : MDI 탭 전체 닫기 시 모든 탭을 닫고 대시보드로 이동
 * @return  N/A
 */
const onTabCloseAll = () => {
  tabsStore.closeAllTabs();
  onTabHome();
};

/**
 * onTabHome : MDI 탭 홈 클릭 시 첫 번째 상위 메뉴를 선택하고 대시보드로 이동
 * @return  N/A
 */
const onTabHome = () => {
  tabsStore.clearActiveTab();
  selectedSubMenuId.value = '';
  goToPath(HOME_PATH);
};

const dialog = useDialog();

// 사이드바 탭 (메뉴 / 즐겨찾기)
const sidebarTab = ref<'menu' | 'favorite'>('menu');
const favorites = ref<Favorite[]>([]);
const isFavoriteLoading = ref(false);
const favoriteError = ref('');

/**
 * loadFavorites : 로그인 사용자의 즐겨찾기 메뉴 목록을 backend(TB_PRGM_FAV_L)에서 조회
 * @return  N/A
 */
const loadFavorites = async () => {
  isFavoriteLoading.value = true;
  favoriteError.value = '';
  try {
    favorites.value = await getFavoriteListApi();
  } catch {
    favorites.value = [];
    favoriteError.value = msg('cm.favorite.load.failed');
  } finally {
    isFavoriteLoading.value = false;
  }
};

/**
 * onTabSidebarClick : 사이드바 탭 클릭 시 탭을 전환 (즐겨찾기 탭은 열 때마다 최신 목록을 다시 조회)
 * @param   tab - 전환할 탭 (menu / favorite)
 * @return  N/A
 */
const onTabSidebarClick = (tab: 'menu' | 'favorite') => {
  sidebarTab.value = tab;
  if (tab === 'favorite') loadFavorites();
};

/**
 * findMenuEntry : 즐겨찾기·알림 메뉴에 해당하는 사이드바 메뉴(상위 메뉴 + 서브메뉴)를 찾음 (권한 있는 메뉴만)
 *                 메뉴ID → 프로그램ID → 프로그램ID의 화면 ID 부분(SYPG010_T01 → SYPG010) 순으로 대조
 * @param   fav - 즐겨찾기 또는 알람 (메뉴ID, 프로그램ID)
 * @return  상위 메뉴 ID와 서브메뉴 (없으면 undefined)
 */
const findMenuEntry = (fav: Pick<Favorite, 'menuId' | 'prgmId'>) => {
  const candidates = [fav.menuId, fav.prgmId, fav.prgmId?.split('_')[0]].filter(Boolean) as string[];
  for (const id of candidates) {
    for (const [topId, subs] of Object.entries(subMenuMap.value)) {
      const sub = subs.find((m) => m.id === id || m.prgmId === id);
      if (sub) return { topId, sub };
    }
  }
  return undefined;
};

/**
 * onFavoriteItemClick : 즐겨찾기 메뉴 클릭 시 해당 화면의 탭을 열고 이동 (연결된 화면이 없으면 안내)
 * @param   fav - 클릭한 즐겨찾기 메뉴
 * @return  N/A
 */
const onFavoriteItemClick = async (fav: Favorite) => {
  const entry = findMenuEntry(fav);
  if (!entry) {
    await dialog.alert(msg('cm.favorite.screen.not.found', fav.menuNm || fav.menuId));
    return;
  }
  selectedTopCategoryId.value = entry.topId;
  selectedSubMenuId.value = entry.sub.id;
  openMenuTab(entry.topId, entry.sub);
  goToPath(entry.sub.path);
};

// ---------------------------------------------------------------------
// 알림 센터 (TB_ALRM_L, SSE)
// ---------------------------------------------------------------------
const alarmStore = useAlarmStore();

/**
 * onAlarmCenterOpenProgram : 알림 센터에서 알람 클릭 시 알람에 연결된 화면의 탭을 열고 이동 (화면이 없으면 안내)
 * @param   alarm - 클릭한 알람 (프로그램ID)
 * @return  N/A
 */
const onAlarmCenterOpenProgram = async (alarm: Alarm) => {
  const entry = findMenuEntry({ menuId: alarm.prgmId ?? '', prgmId: alarm.prgmId });
  if (!entry) {
    await dialog.alert(msg('cm.alarm.screen.not.found', alarm.alrmTitlNm));
    return;
  }
  selectedTopCategoryId.value = entry.topId;
  selectedSubMenuId.value = entry.sub.id;
  openMenuTab(entry.topId, entry.sub);
  goToPath(entry.sub.path);
};

// ---------------------------------------------------------------------
// GNB 메뉴 검색 (권한 메뉴)
// ---------------------------------------------------------------------
/** 메뉴 검색 결과 1건 */
interface MenuSrchResult {
  menuId: string;
  menuNm: string;
  /** 상위 메뉴 경로 (예: 시스템관리 > LOG관리) */
  pathLabel: string;
  topId: string;
  sub: SubMenu;
}

/** 메뉴 검색 결과 최대 표시 건수 */
const MENU_SRCH_MAX_ROWS = 20;

const menuSrchKeyword = ref('');
const isMenuSrchOpen = ref(false);
const menuSrchActiveIndex = ref(-1);

/**
 * 검색어가 포함된 권한 메뉴 중 화면이 연결된 메뉴 (메뉴명 대소문자·공백 무시, 상단 메뉴·정렬순번 순)
 */
const menuSrchResults = computed<MenuSrchResult[]>(() => {
  const keyword = menuSrchKeyword.value.replace(/\s+/g, '').toUpperCase();
  if (!keyword) return [];
  const results: MenuSrchResult[] = [];
  for (const [topId, subs] of Object.entries(subMenuMap.value)) {
    for (const sub of subs) {
      if (!sub.label.replace(/\s+/g, '').toUpperCase().includes(keyword)) continue;
      results.push({ menuId: sub.id, menuNm: sub.label, pathLabel: sub.pathLabel, topId, sub });
      if (results.length >= MENU_SRCH_MAX_ROWS) return results;
    }
  }
  return results;
});

/**
 * loadAuthMenuList : 로그인 사용자의 권한 메뉴를 backend 에서 조회하고, 상단 메뉴가 선택되지 않았으면 첫 번째 상단 메뉴를 선택
 * @return  N/A
 */
const loadAuthMenuList = async () => {
  isMenuLoading.value = true;
  menuLoadError.value = false;
  try {
    authMenuList.value = await getAuthMenuListApi();
  } catch {
    authMenuList.value = [];
    menuLoadError.value = true;
  } finally {
    isMenuLoading.value = false;
  }
  if (!subMenuMap.value[selectedTopCategoryId.value]) {
    selectedTopCategoryId.value = topCategories.value[0]?.id || '';
  }
};

/**
 * openMenuSrch : 검색어가 있으면 검색 결과 목록을 열고 첫 번째 결과를 선택 상태로 둠
 * @return  N/A
 */
const openMenuSrch = () => {
  if (!menuSrchKeyword.value.trim()) {
    isMenuSrchOpen.value = false;
    return;
  }
  isMenuSrchOpen.value = true;
  menuSrchActiveIndex.value = 0;
};

/**
 * closeMenuSrch : 검색 결과 목록을 닫음
 * @return  N/A
 */
const closeMenuSrch = () => {
  isMenuSrchOpen.value = false;
  menuSrchActiveIndex.value = -1;
};

/**
 * selectSearchMenu : 검색 결과 메뉴를 선택: 대메뉴·왼쪽 메뉴를 선택하고 해당 화면의 탭을 열어 이동
 * @param   item - 선택한 검색 결과
 * @return  N/A
 */
const selectSearchMenu = (item: MenuSrchResult) => {
  selectedTopCategoryId.value = item.topId;
  selectedSubMenuId.value = item.sub.id;
  sidebarTab.value = 'menu';
  isSidebarCollapsed.value = false;
  emit('select-top-menu', item.topId);
  openMenuTab(item.topId, item.sub);
  goToPath(item.sub.path);
  emit('select-sub-menu', item.sub.id);

  menuSrchKeyword.value = '';
  closeMenuSrch();
  (document.getElementById('txtMenuSrch') as HTMLInputElement | null)?.blur();
};

/**
 * onTxtMenuSrchInput : 메뉴 검색어 입력 시 검색 결과 목록 갱신
 * @return  N/A
 */
const onTxtMenuSrchInput = async () => {
  await openMenuSrch();
};

/**
 * onTxtMenuSrchFocus : 메뉴 검색창 포커스 시 검색어가 있으면 결과 목록을 다시 열음
 * @return  N/A
 */
const onTxtMenuSrchFocus = async () => {
  await openMenuSrch();
};

/**
 * onTxtMenuSrchBlur : 메뉴 검색창 포커스 해제 시 결과 목록을 닫음
 * @return  N/A
 */
const onTxtMenuSrchBlur = () => {
  closeMenuSrch();
};

/**
 * onTxtMenuSrchKeydown : 메뉴 검색창 키 입력 - 위/아래 화살표로 결과 이동, Enter로 선택, Esc로 닫기
 * @param   event - 키보드 이벤트
 * @return  N/A
 */
const onTxtMenuSrchKeydown = async (event: KeyboardEvent) => {
  if (event.isComposing) return;
  const count = menuSrchResults.value.length;
  switch (event.key) {
    case 'ArrowDown':
      event.preventDefault();
      if (!isMenuSrchOpen.value) await openMenuSrch();
      else if (count > 0) menuSrchActiveIndex.value = (menuSrchActiveIndex.value + 1) % count;
      break;
    case 'ArrowUp':
      event.preventDefault();
      if (count > 0) menuSrchActiveIndex.value = (menuSrchActiveIndex.value - 1 + count) % count;
      break;
    case 'Enter':
      event.preventDefault();
      await onBtnMenuSrchClick();
      break;
    case 'Escape':
      closeMenuSrch();
      break;
  }
};

/**
 * onBtnMenuSrchClick : 메뉴 검색 버튼 클릭(또는 Enter) - 선택된 결과(없으면 첫 번째 결과)를 열고, 결과가 없으면 목록에 안내 표시
 * @return  N/A
 */
const onBtnMenuSrchClick = async () => {
  if (!menuSrchKeyword.value.trim()) {
    document.getElementById('txtMenuSrch')?.focus();
    return;
  }
  const results = menuSrchResults.value;
  const target = results[menuSrchActiveIndex.value] ?? results[0];
  if (isMenuSrchOpen.value && target) {
    selectSearchMenu(target);
    return;
  }
  await openMenuSrch();
};

/**
 * handleLogout : 로그아웃 버튼 클릭 시 확인 후 logout 이벤트를 발생
 * @return  N/A
 */
const handleLogout = async () => {
  if (await dialog.confirm({ message: msg('cm.confirm.logout') })) {
    emit('logout');
  }
};

/**
 * handleBrandClick : 브랜드(로고) 클릭 시 첫 번째 상위 메뉴를 선택
 * @return  N/A
 */
const handleBrandClick = () => {
  onTabHome();
};

const currentTimeString = ref('');
let timer: any = null;

/**
 * updateTime : 현재 일시를 YYYY-MM-DD HH:mm:ss 형식으로 갱신
 * @return  N/A
 */
const updateTime = () => {
  currentTimeString.value = formatDttm();
};

onMounted(async () => {
  updateTime();
  timer = setInterval(updateTime, 1000);
  // 알림 센터 실시간 연결 (로그아웃·레이아웃 해제 시 끊는다)
  alarmStore.connect();

  await loadAuthMenuList();
  // 홈이 아닌 화면으로 바로 들어온 경우(새로고침 등) 현재 경로의 메뉴 탭을 연다
  if (tabsStore.tabs.length === 0 && !isHomeActive.value) {
    for (const [topId, subs] of Object.entries(subMenuMap.value)) {
      const sub = subs.find((m) => m.path === route.path);
      if (sub) {
        selectedTopCategoryId.value = topId;
        selectedSubMenuId.value = sub.id;
        openMenuTab(topId, sub);
        break;
      }
    }
  }
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
  alarmStore.disconnect();
});
</script>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  width: 100vw;
  height: 100vh;
  background-color: var(--ds-ground);
  color: var(--ds-ink);
  font-family: var(--ds-font);
  overflow: hidden;
  user-select: none;
}

/* ===== 1. TOP BAR ===== */
.top-bar {
  height: 34px;
  min-height: 34px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: var(--ds-slate-900);
  z-index: 101;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

/* 슬레이트 위 흰 라인아트 로고 */
.brand .brand-title {
  font-size: 13px;
  gap: 8px;
  color: var(--ds-ink-inverse);
}

.brand-subtitle {
  padding-left: 12px;
  border-left: 1px solid var(--ds-slate-500);
  font-size: 12px;
  font-weight: 400;
  color: var(--ds-ink-inverse);
  opacity: 0.7;
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.welcome-text {
  font-size: 12px;
  color: var(--ds-slate-100);
  margin-right: 8px;
}

.welcome-text strong {
  color: var(--ds-ink-inverse);
  font-weight: 600;
}

.top-text-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 26px;
  padding: 0 10px;
  border: none;
  background: transparent;
  color: var(--ds-slate-100);
  font-size: 12px;
  cursor: pointer;
  border-radius: var(--ds-radius-control);
  transition: background-color var(--ds-ease), color var(--ds-ease);
}

.top-text-btn:hover {
  background: var(--ds-slate-700);
  color: var(--ds-ink-inverse);
}

/* ===== 2. GNB BAR ===== */
.gnb-bar {
  height: 46px;
  min-height: 46px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0 12px;
  background: var(--ds-slate-700);
  border-bottom: 1px solid var(--ds-slate-900);
  z-index: 100;
}

.sidebar-toggle-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: var(--ds-radius-control);
  background: transparent;
  color: var(--ds-slate-100);
  cursor: pointer;
  transition: background-color var(--ds-ease), color var(--ds-ease);
  flex-shrink: 0;
}

.sidebar-toggle-btn:hover {
  background: var(--ds-slate-500);
  color: var(--ds-ink-inverse);
}

.gnb-nav {
  display: flex;
  align-items: stretch;
  gap: 0;
  height: 100%;
  flex: 1;
  overflow-x: auto;
}

.gnb-item {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 100%;
  padding: 0 16px;
  border: none;
  border-radius: 0;
  background: transparent;
  color: var(--ds-slate-100);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color var(--ds-ease), color var(--ds-ease);
  white-space: nowrap;
}

.gnb-item:hover {
  background: var(--ds-slate-500);
  color: var(--ds-ink-inverse);
}

.gnb-item.active {
  background: transparent;
  color: var(--ds-ink-inverse);
  font-weight: 600;
  box-shadow: inset 0 -3px 0 var(--ds-foil);
}

.gnb-item.active:hover {
  background: var(--ds-slate-500);
}

.gnb-badge {
  font-size: 11px;
  font-weight: 600;
  line-height: 16px;
  padding: 0 5px;
  background: var(--ds-foil);
  color: var(--ds-slate-900);
  border-radius: 2px;
}

.gnb-search-wrap {
  position: relative;
  flex-shrink: 0;
}

/* 메뉴 검색 결과 (검색창 바로 아래) */
.gnb-search-result {
  position: absolute;
  top: calc(100% + 4px);
  right: 0;
  z-index: 300;
  width: 320px;
  max-height: 360px;
  margin: 0;
  padding: 4px 0;
  overflow-y: auto;
  list-style: none;
  background: var(--ds-surface);
  border: 1px solid var(--ds-rule-strong);
  border-radius: var(--ds-radius-overlay);
  box-shadow: var(--ds-shadow-overlay);
}

.gnb-search-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 6px 12px;
  cursor: pointer;
}

.gnb-search-item.active {
  background: var(--ds-slate-050);
}

.gnb-search-item-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--ds-ink);
}

.gnb-search-item-path {
  font-size: 12px;
  color: var(--ds-ink-3);
}

.gnb-search-empty {
  padding: 10px 12px;
  font-size: 13px;
  color: var(--ds-ink-3);
}

.gnb-search {
  display: flex;
  align-items: center;
  background: var(--ds-surface);
  border-radius: var(--ds-radius-control);
  overflow: hidden;
  height: var(--ds-control-height-sm);
  flex-shrink: 0;
}

.gnb-search:focus-within {
  box-shadow: var(--ds-focus);
}

.gnb-search-input {
  border: none;
  outline: none;
  padding: 0 10px;
  height: 100%;
  width: 200px;
  font-size: 13px;
  color: var(--ds-ink);
  background: transparent;
}

/* 포커스 표시는 래퍼(.gnb-search:focus-within)가 담당 */
.gnb-search-input:focus-visible {
  box-shadow: none;
}

.gnb-search-input::placeholder {
  color: var(--ds-ink-3);
}

.gnb-search-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 100%;
  border: none;
  border-radius: 0;
  background: var(--ds-slate-900);
  color: var(--ds-ink-inverse);
  cursor: pointer;
  flex-shrink: 0;
  transition: background-color var(--ds-ease);
}

.gnb-search-btn:hover {
  background: var(--ds-slate-500);
}

/* ===== 3. BODY ===== */
.layout-body {
  flex: 1;
  display: flex;
  min-height: 0;
  position: relative;
}

.layout-sidebar {
  width: 240px;
  min-width: 240px;
  background: var(--ds-surface);
  border-right: 1px solid var(--ds-rule);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-tabs {
  display: flex;
  border-bottom: 1px solid var(--ds-rule);
}

.sidebar-tab {
  flex: 1;
  height: 36px;
  border: none;
  background: var(--ds-surface);
  color: var(--ds-ink-3);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: color var(--ds-ease);
}

.sidebar-tab:hover {
  color: var(--ds-ink);
}

.sidebar-tab.active {
  color: var(--ds-slate-900);
  box-shadow: inset 0 -2px 0 var(--ds-slate-700);
}

.sidebar-header {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid var(--ds-rule);
}

.sidebar-header-dot {
  width: 3px;
  height: 14px;
  background: var(--ds-slate-700);
  flex-shrink: 0;
}

.sidebar-title {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  color: var(--ds-ink);
}

.sidebar-count {
  font-size: 12px;
  color: var(--ds-ink-3);
  font-variant-numeric: tabular-nums;
}

.sidebar-nav {
  flex: 1;
  padding: 8px 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.nav-group.has-group-title:not(:first-child) {
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px solid var(--ds-rule);
}

/* 중분류: 메뉴 항목보다 진하고 굵게, 사각 마커로 그룹 머리임을 표시 */
.group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px 4px;
  font-size: 13px;
  font-weight: 700;
  color: var(--ds-slate-900);
}

.group-title::before {
  content: '';
  width: 6px;
  height: 6px;
  background: var(--ds-slate-700);
  flex-shrink: 0;
}

/* 중분류 아래 메뉴는 마커 뒤 글자 위치에 맞춰 들여쓴다 */
.nav-group .sidebar-item {
  padding-left: 30px;
}

.sidebar-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 34px;
  padding: 0 16px;
  border: none;
  border-radius: 0;
  background: transparent;
  color: var(--ds-ink-2);
  box-shadow: inset 3px 0 0 transparent;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color var(--ds-ease), color var(--ds-ease);
  white-space: nowrap;
}

.sidebar-item:hover {
  background: var(--ds-slate-050);
  color: var(--ds-ink);
}

.sidebar-item.active {
  background: var(--ds-slate-100);
  color: var(--ds-slate-900);
  font-weight: 600;
  box-shadow: inset 3px 0 0 var(--ds-slate-700);
}

.sidebar-item:focus-visible {
  box-shadow: var(--ds-focus);
}

.sidebar-empty {
  padding: 24px 16px;
  font-size: 13px;
  color: var(--ds-ink-3);
  text-align: center;
}

.menu-label {
  flex: 1;
  text-align: left;
  overflow: hidden;
  text-overflow: ellipsis;
}

.menu-badge {
  font-size: 11px;
  font-weight: 600;
  line-height: 16px;
  padding: 0 5px;
  border-radius: 2px;
  color: var(--ds-ink-3);
  background: var(--ds-surface-muted);
}

.menu-badge.primary {
  background: var(--ds-slate-100);
  color: var(--ds-slate-900);
  border: 1px solid var(--ds-rule-strong);
}

.layout-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--ds-ground);
  position: relative;
  overflow: hidden;
}


.content-viewport {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  box-sizing: border-box;
}

.default-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  text-align: center;
}

.placeholder-path {
  font-size: 14px;
  font-weight: 600;
  color: var(--ds-ink-2);
}

.placeholder-text {
  font-size: 13px;
  color: var(--ds-ink-3);
}

/* ===== 4. FOOTER ===== */
.layout-footer {
  height: 28px;
  min-height: 28px;
  background: var(--ds-surface);
  border-top: 1px solid var(--ds-rule);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  font-size: 12px;
  color: var(--ds-ink-3);
  z-index: 100;
}

.footer-left, .footer-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.footer-sep {
  color: var(--ds-rule-strong);
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--ds-slate-500);
}

.status-label {
  color: var(--ds-ink-2);
  font-weight: 500;
}

.status-detail {
  color: var(--ds-ink-3);
}

.footer-center {
  color: var(--ds-ink-3);
}

.current-time {
  color: var(--ds-ink-2);
  font-variant-numeric: tabular-nums;
}

.version-tag {
  color: var(--ds-ink-3);
}
</style>
