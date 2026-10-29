<template>
  <div 
    class="custom-card" 
    :class="{ 
      'is-hoverable': hoverable, 
      'is-bordered': bordered 
    }"
  >
    <!-- 카드 헤더 -->
    <div v-if="$slots.header || title" class="card-header">
      <div class="header-titles">
        <slot name="header">
          <h4 v-if="title" class="card-title">{{ title }}</h4>
          <p v-if="subtitle" class="card-subtitle">{{ subtitle }}</p>
        </slot>
      </div>

      <!-- 우측 액션 슬롯 (버튼, 배지 등) -->
      <div v-if="$slots.extra" class="header-extra">
        <slot name="extra"></slot>
      </div>
    </div>

    <!-- 카드 본문 -->
    <div class="card-body" :style="bodyStyle">
      <slot></slot>
    </div>

    <!-- 카드 푸터 -->
    <div v-if="$slots.footer" class="card-footer">
      <slot name="footer"></slot>
    </div>
  </div>
</template>

<script setup>
/*****************************************************************************************
	Program ID   : Card
	Program Type : vue
	Program Name : 카드
	Description  : 제목과 본문 영역을 가진 공통 카드 컨테이너
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-09-07  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
defineProps({
  title: {
    type: String,
    default: ''
  },
  subtitle: {
    type: String,
    default: ''
  },
  hoverable: {
    type: Boolean,
    default: false
  },
  bordered: {
    type: Boolean,
    default: true
  },
  bodyStyle: {
    type: Object,
    default: () => ({})
  }
});
</script>

<style scoped>
.custom-card {
  background: var(--ds-surface);
  border-radius: var(--ds-radius-panel);
  color: var(--ds-ink);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: none;
}

.is-bordered {
  border: 1px solid var(--ds-rule);
}

/* hoverable: v2 에서는 떠오름·테두리 변화 없음 (props 호환을 위해 클래스만 유지) */

/* 헤더 */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--ds-rule);
}

.header-titles {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.card-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--ds-ink);
}

.card-subtitle {
  margin: 0;
  font-size: 12.5px;
  color: var(--ds-ink-3);
}

.header-extra {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 바디 */
.card-body {
  padding: 16px;
  flex: 1;
}

/* 푸터 */
.card-footer {
  padding: 10px 16px;
  border-top: 1px solid var(--ds-rule);
  background: var(--ds-slate-050);
}
</style>
