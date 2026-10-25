<script setup lang="ts">
/*****************************************************************************************
	Program ID   : DashboardView
	Program Type : vue
	Program Name : 대시보드
	Description  : 검색창 하나만 있는 대시보드 (질의는 추후 MCP 서버로 전달 예정)
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-09-20  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
import { ref, onMounted } from 'vue'
import { useToast } from '@/composables/useToast'
import { msg } from '@/utils/message'

const toast = useToast()
const txtSearch = ref<HTMLInputElement | null>(null)
const keyword = ref('')

/** 예시 질문: 처음 쓰는 운영자에게 무엇을 물어볼 수 있는지 보여준다 (MCP 연동 시 실제 범위에 맞춰 조정) */
const EXAMPLE_QUERIES = ['오늘 실패한 배치', '최근 시스템 에러 로그', 'SYLK050']

/**
 * search : 검색어를 MCP 서버에 질의 (MCP 서버 연동 전까지는 준비 중 안내만 표시)
 * @param   query - 검색어
 * @return  N/A
 */
function search(query: string): void {
  // TODO: MCP 서버 연동 시 이 함수에서 질의를 전달하고 결과 화면을 표시한다
  toast.info(msg('cm.dashboard.search.not.ready', query))
}

/**
 * onFrmSearchSubmit : 검색 폼 제출(Enter / 검색 버튼) 시 검색어가 있으면 검색 실행
 * @return  N/A
 */
function onFrmSearchSubmit(): void {
  const query = keyword.value.trim()
  if (!query) {
    txtSearch.value?.focus()
    return
  }
  search(query)
}

/**
 * onBtnClearClick : 지우기 버튼 클릭 시 검색어를 비우고 입력창에 포커스
 * @return  N/A
 */
function onBtnClearClick(): void {
  keyword.value = ''
  txtSearch.value?.focus()
}

/**
 * onBtnExampleClick : 예시 질문 클릭 시 검색창에 채우고 바로 검색
 * @param   query - 예시 질문
 * @return  N/A
 */
function onBtnExampleClick(query: string): void {
  keyword.value = query
  search(query)
}

onMounted(() => {
  txtSearch.value?.focus()
})
</script>

<template>
  <section class="dashboard-search">
    <div class="search-hero">
      <span class="search-logo" aria-hidden="true">CODER</span>

      <form id="frmSearch" class="search-form" role="search" @submit.prevent="onFrmSearchSubmit">
        <label for="txtSearch" class="sr-only">검색</label>
        <div class="search-box">
          <svg class="search-icon" viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
            <circle cx="11" cy="11" r="7" fill="none" stroke="currentColor" stroke-width="2" />
            <line x1="16.5" y1="16.5" x2="21" y2="21" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
          </svg>
          <input
            id="txtSearch"
            ref="txtSearch"
            v-model="keyword"
            type="search"
            class="search-input"
            placeholder="배치명 또는 질문을 입력하세요"
            autocomplete="off"
            enterkeyhint="search"
          />
          <button
            v-if="keyword"
            id="btnClear"
            type="button"
            class="search-clear"
            aria-label="지우기"
            @click="onBtnClearClick"
          >×</button>
          <button id="btnSearch" type="submit" class="search-btn">검색</button>
        </div>

        <ul class="search-examples" aria-label="예시 질문">
          <li v-for="(query, idx) in EXAMPLE_QUERIES" :key="query">
            <button :id="`btnExample${idx + 1}`" type="button" class="search-example" @click="onBtnExampleClick(query)">
              {{ query }}
            </button>
          </li>
        </ul>
      </form>
    </div>
  </section>
</template>

<style scoped>
.dashboard-search {
  height: 100%;
  overflow: auto;
  display: flex;
  justify-content: center;
  background: var(--ds-ground);
}

/* 화면 위쪽 1/3 지점에 로고와 검색창을 둔다 */
.search-hero {
  width: min(600px, 100%);
  padding: 18vh 16px 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 28px;
}

/* 로고 플레이트: 화면ID 플레이트와 같은 표지 (pine-900 바탕 + 사인 옐로) */
.search-logo {
  display: inline-flex;
  align-items: center;
  padding: 8px 18px;
  font-size: 56px;
  font-weight: 700;
  letter-spacing: 0.06em;
  line-height: 1;
  color: var(--ds-sign);
  background: var(--ds-pine-900);
  border-radius: 2px;
  user-select: none;
}

.search-form {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

/* 검색창 + 검색 버튼을 한 덩어리로: 다른 화면의 조회 영역과 같은 컨트롤 언어 */
.search-box {
  width: 100%;
  height: 48px;
  display: flex;
  align-items: stretch;
  background: var(--ds-surface);
  border: 1px solid var(--ds-rule-strong);
  border-radius: var(--ds-radius-control);
  overflow: hidden;
  transition: border-color var(--ds-ease), box-shadow var(--ds-ease);
}

.search-box:hover {
  border-color: var(--ds-ink-3);
}

/* 키보드·마우스 포커스: 녹색 테두리 + 옅은 녹색 링 (hover 와 구분된다) */
.search-box:focus-within {
  border-color: var(--ds-pine-700);
  box-shadow: var(--ds-focus-field);
}

.search-icon {
  flex: 0 0 auto;
  align-self: center;
  margin: 0 10px 0 14px;
  color: var(--ds-ink-3);
}

.search-input {
  flex: 1 1 auto;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  color: var(--ds-ink);
  font: inherit;
  font-size: 16px;
}

.search-input::placeholder {
  color: var(--ds-ink-3);
}

/* 입력창 포커스는 바깥 .search-box 가 표시한다 */
.search-input:focus-visible {
  box-shadow: none;
}

/* 브라우저 기본 검색창 지우기(x) 버튼은 숨기고 직접 만든 버튼을 사용 */
.search-input::-webkit-search-cancel-button {
  display: none;
}

.search-clear {
  flex: 0 0 auto;
  align-self: center;
  width: 32px;
  height: 32px;
  margin-right: 6px;
  border: none;
  border-radius: var(--ds-radius-control);
  background: transparent;
  color: var(--ds-ink-3);
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
}

.search-clear:hover {
  background: var(--ds-surface-muted);
  color: var(--ds-ink);
}

.search-btn {
  flex: 0 0 auto;
  min-width: 84px;
  padding: 0 20px;
  border: none;
  background: var(--ds-pine-700);
  color: var(--ds-ink-inverse);
  font: inherit;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color var(--ds-ease);
}

.search-btn:hover {
  background: var(--ds-pine-900);
}

/* .search-box 가 overflow:hidden 이라 바깥 포커스 링이 잘리므로 안쪽 링을 쓴다 */
.search-clear:focus-visible {
  box-shadow: inset 0 0 0 2px var(--ds-pine-700);
}

.search-btn:focus-visible {
  box-shadow: inset 0 0 0 3px var(--ds-sign);
}

/* 예시 질문: 버튼처럼 꾸미지 않은 글자 링크 */
.search-examples {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px 20px;
  list-style: none;
}

.search-example {
  padding: 2px 2px;
  border: none;
  border-radius: 2px;
  background: none;
  color: var(--ds-ink-2);
  font: inherit;
  font-size: 14px;
  cursor: pointer;
}

.search-example:hover {
  color: var(--ds-pine-700);
  text-decoration: underline;
  text-underline-offset: 3px;
}

.search-example:focus-visible {
  box-shadow: var(--ds-focus);
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 640px) {
  .search-hero {
    padding-top: 12vh;
  }

  .search-logo {
    font-size: 40px;
    padding: 6px 14px;
  }

  .search-btn {
    min-width: 64px;
    padding: 0 14px;
  }
}
</style>
