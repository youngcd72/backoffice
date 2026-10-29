<template>
  <span class="brand-logo" :class="[`is-${layout}`, { 'is-drawn': animated }]" role="img" aria-label="CODER">
    <svg
      class="brand-mark"
      viewBox="0 0 60 56"
      :width="markSize"
      :height="markSize * 56 / 60"
      fill="none"
      stroke="currentColor"
      :stroke-width="strokeWidth"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
    >
      <!-- 외곽선만 그린 굵은 C -->
      <path
        class="mark-line"
        pathLength="1"
        d="M41.56 14.44 A22 22 0 1 0 41.56 45.56 L35.19 39.19 A13 13 0 1 1 35.19 20.81 Z"
      />
      <!-- C 띠를 가르는 사선 -->
      <path class="mark-line mark-cut" pathLength="1" d="M24.87 42.95 L16.7 49.94" />
      <!-- 중괄호 } -->
      <path
        class="mark-line mark-brace"
        pathLength="1"
        d="M47.5 3 q5 0 5 5 v7 q0 4 4 4 q-4 0 -4 4 v7 q0 5 -5 5"
      />
    </svg>
    <span v-if="layout !== 'mark'" class="brand-text">
      <span class="brand-wordmark">CODER</span>
      <span v-if="tagline" class="brand-tagline">{{ tagline }}</span>
    </span>
  </span>
</template>

<script setup lang="ts">
/*****************************************************************************************
	Program ID   : BrandLogo
	Program Type : vue
	Program Name : 브랜드 로고
	Description  : CODER 라인아트 로고(C + 중괄호) 및 워드마크. 색상은 currentColor를 따른다
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-29  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
import { computed } from 'vue';

const props = withDefaults(defineProps<{
  /** mark: 심볼만, inline: 심볼 + 워드마크 가로, stacked: 심볼 위 워드마크 */
  layout?: 'mark' | 'inline' | 'stacked';
  /** 심볼 너비(px) */
  size?: number;
  /** 워드마크 아래 보조 문구 */
  tagline?: string;
  /** 선을 그려 나가는 진입 애니메이션 (로그인 화면 1회용) */
  animated?: boolean;
}>(), {
  layout: 'inline',
  size: 32,
  tagline: '',
  animated: false,
});

const markSize = computed(() => props.size);
// 작은 크기에서도 선이 뭉개지지 않도록 크기에 반비례해 굵기를 보정한다
const strokeWidth = computed(() => (props.size <= 24 ? 3 : props.size <= 48 ? 2.2 : 1.6));
</script>

<style scoped>
.brand-logo {
  display: inline-flex;
  align-items: center;
  gap: 0.6em;
  color: inherit;
  line-height: 1;
}

.brand-logo.is-stacked {
  flex-direction: column;
  gap: 18px;
  text-align: center;
}

.is-stacked .brand-text {
  align-items: center;
}

.brand-mark {
  flex: none;
  overflow: visible;
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 0.5em;
}

/* 로고 서체: 기하학 산세리프 + 넓은 자간 (대문자 고유명사이므로 자간을 벌린다) */
.brand-wordmark {
  font-family: var(--ds-font-brand);
  font-weight: 500;
  letter-spacing: 0.16em;
  /* 자간이 오른쪽에만 붙으므로 시각 중심을 맞춘다 */
  margin-right: -0.16em;
}

.brand-tagline {
  font-family: var(--ds-font-brand);
  font-size: max(11px, 0.32em);
  font-weight: 500;
  letter-spacing: 0.08em;
  opacity: 0.72;
}

/* ---------- 진입 애니메이션: 펜으로 긋듯 C → 사선 → 중괄호 순서 ---------- */
.is-drawn .mark-line {
  stroke-dasharray: 1;
  stroke-dashoffset: 1;
  animation: brand-draw 1.1s cubic-bezier(0.65, 0, 0.35, 1) both;
}

.is-drawn .mark-cut {
  animation-delay: 0.9s;
  animation-duration: 0.25s;
}

.is-drawn .mark-brace {
  animation-delay: 1.05s;
  animation-duration: 0.6s;
}

.is-drawn .brand-text {
  opacity: 0;
  animation: brand-fade 0.5s ease 1.4s forwards;
}

/* 둥근 선끝이 그리기 전부터 점으로 보이지 않도록 시작 순간까지 숨긴다 */
@keyframes brand-draw {
  from { stroke-dashoffset: 1; opacity: 0; }
  1% { opacity: 1; }
  to { stroke-dashoffset: 0; opacity: 1; }
}

@keyframes brand-fade {
  to { opacity: 1; }
}
</style>
