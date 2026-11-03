<script setup lang="ts">
/*****************************************************************************************
	Program ID   : SYLK010
	Program Type : vue
	Program Name : API관리
	Description  : 대외 연계 API(coder-api) 관리 화면. 연계기관·클라이언트 키·허용 인터페이스(①),
	               인터페이스(②), 호출이력(③)을 한 화면의 탭으로 관리한다. coder-backend 관리 API(api/sy/link.ts)를 호출한다.
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-11-03  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { ref, onMounted, nextTick } from 'vue'
import { Tabs } from '@/components/common'
import { getCommCdOptions } from '@/api/common/commonCode'
import type { CodeOption } from '@/utils/code'
import LinkOrgTab from './api/LinkOrgTab.vue'
import InterfaceTab from './api/InterfaceTab.vue'
import InterfaceCallTab from './api/InterfaceCallTab.vue'

type TabKey = 'org' | 'infc' | 'call'

const PAGE_TITLE = 'API관리'
const PAGE_ID = 'SYLK010'

const TAB_ITEMS = [
  { key: 'org', label: '연계기관' },
  { key: 'infc', label: '인터페이스' },
  { key: 'call', label: '호출이력' },
]

// 공통코드 (탭에 전달)
const chnlClsOptions = ref<CodeOption[]>([])
const keyStatOptions = ref<CodeOption[]>([])
const wrkMdulOptions = ref<CodeOption[]>([])

// 탭: 본문은 v-show 로 전환해 탭을 오가도 검색조건·선택 행·입력 중인 폼을 유지한다
const activeTab = ref<TabKey>('org')
const loadedTabs = new Set<TabKey>()
const orgTabRef = ref<InstanceType<typeof LinkOrgTab> | null>(null)
const infcTabRef = ref<InstanceType<typeof InterfaceTab> | null>(null)
const callTabRef = ref<InstanceType<typeof InterfaceCallTab> | null>(null)

/**
 * loadTab : 처음 활성화되는 탭이면 최초 조회
 * @param   tab - 탭 키
 * @return  N/A
 */
async function loadTab(tab: TabKey): Promise<void> {
  if (loadedTabs.has(tab)) return
  loadedTabs.add(tab)
  await nextTick()
  if (tab === 'org') await orgTabRef.value?.initTab()
  if (tab === 'infc') await infcTabRef.value?.initTab()
  if (tab === 'call') await callTabRef.value?.initTab()
}

// ---------------------------------------------------------------------
// 이벤트 핸들러
// ---------------------------------------------------------------------
/**
 * onTabMainChange : 탭 변경: 처음 여는 탭이면 최초 조회
 * @param   tab - 선택한 탭 키
 * @return  N/A
 */
async function onTabMainChange(tab: TabKey): Promise<void> {
  await loadTab(tab)
}

/**
 * onOrgTabCallView : 연계기관 탭의 호출이력 보기: 호출이력 탭으로 이동해 선택 기관으로 조회
 * @param   lnkOrgId - 연계기관ID
 * @return  N/A
 */
async function onOrgTabCallView(lnkOrgId: string): Promise<void> {
  activeTab.value = 'call'
  await loadTab('call')
  await callTabRef.value?.searchByOrg(lnkOrgId)
}

/**
 * onOrgTabSaved : 연계기관 저장: 호출이력 탭의 기관 옵션 갱신
 * @return  N/A
 */
async function onOrgTabSaved(): Promise<void> {
  if (loadedTabs.has('call')) await callTabRef.value?.reloadOptions()
}

/**
 * onInfcTabSaved : 인터페이스 저장: 연계기관 탭의 허용 인터페이스, 호출이력 탭의 인터페이스 옵션 갱신
 * @return  N/A
 */
async function onInfcTabSaved(): Promise<void> {
  if (loadedTabs.has('org')) await orgTabRef.value?.reloadOrgInterfaces()
  if (loadedTabs.has('call')) await callTabRef.value?.reloadOptions()
}

/**
 * onOrgTabInfcSaved : 허용 인터페이스 저장: 인터페이스 탭의 허용기관 수 갱신
 * @return  N/A
 */
async function onOrgTabInfcSaved(): Promise<void> {
  if (loadedTabs.has('infc')) await infcTabRef.value?.searchInterfaceList(false)
}

// ---------------------------------------------------------------------
// 페이지 라이프사이클
// ---------------------------------------------------------------------
/**
 * fetchPageData : 페이지 참조 데이터(채널구분·키상태·업무모듈 공통코드) 조회
 * @return  N/A
 */
async function fetchPageData(): Promise<void> {
  ;[chnlClsOptions.value, keyStatOptions.value, wrkMdulOptions.value] = await Promise.all([
    getCommCdOptions('SY', 'CHNL_CLS_CD'),
    getCommCdOptions('SY', 'KEY_STAT_CD'),
    getCommCdOptions('SY', 'WRK_MDUL_CD'),
  ])
}

/**
 * initPageUi : 첫 탭(연계기관)을 활성화하고 최초 조회
 * @return  N/A
 */
async function initPageUi(): Promise<void> {
  activeTab.value = 'org'
  loadedTabs.clear()
  await loadTab('org')
}

onMounted(async () => {
  await fetchPageData()
  await initPageUi()
})
</script>

<template>
  <section class="page-root">
    <header class="page-title-bar">
      <h2 class="page-title"><span class="page-plate">{{ PAGE_ID }}</span>{{ PAGE_TITLE }}</h2>
    </header>

    <Tabs id="tabMain" v-model="activeTab" :items="TAB_ITEMS" class="main-tabs" @change="onTabMainChange" />

    <LinkOrgTab
      v-show="activeTab === 'org'"
      ref="orgTabRef"
      :chnl-cls-options="chnlClsOptions"
      :key-stat-options="keyStatOptions"
      @call-view="onOrgTabCallView"
      @saved="onOrgTabSaved"
      @infc-saved="onOrgTabInfcSaved"
    />
    <InterfaceTab
      v-show="activeTab === 'infc'"
      ref="infcTabRef"
      :wrk-mdul-options="wrkMdulOptions"
      @saved="onInfcTabSaved"
    />
    <InterfaceCallTab v-show="activeTab === 'call'" ref="callTabRef" />
  </section>
</template>

<style scoped src="@/assets/page.css"></style>

<style scoped>
/* 탭 머리글만 쓰고 본문은 화면이 v-show 로 전환한다 */
.main-tabs :deep(.tabs-body) {
  display: none;
}

.main-tabs {
  margin-bottom: 4px;
}
</style>
