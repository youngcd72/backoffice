<template>
  <div ref="alrmCenterRoot" class="alarm-center">
    <button
      id="btnAlrm"
      ref="btnAlrm"
      type="button"
      class="alarm-btn"
      :class="{ open: isOpen }"
      title="알림 센터"
      aria-haspopup="dialog"
      :aria-expanded="isOpen"
      @click="onBtnAlrmClick"
    >
      <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
        <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
      </svg>
      <span v-if="alarmStore.unconfirmedCnt > 0" id="lblAlrmCnt" class="alarm-count">{{ countLabel }}</span>
    </button>

    <div v-if="isOpen" class="alarm-panel" role="dialog" aria-label="알림 센터" @keydown.esc="closePanel">
      <div class="alarm-panel-header">
        <span class="alarm-panel-title">알림</span>
        <span class="alarm-panel-count">미확인 {{ alarmStore.unconfirmedCnt }}건</span>
        <button
          id="btnAlrmCfmAll"
          type="button"
          class="alarm-cfm-all-btn"
          :disabled="alarmStore.unconfirmedCnt === 0"
          @click="onBtnAlrmCfmAllClick"
        >모두 읽음</button>
      </div>

      <ul class="alarm-list">
        <li v-if="alarmStore.isListLoading && !alarmStore.isListLoaded" class="alarm-empty">{{ msg('cm.alarm.loading') }}</li>
        <li v-else-if="listError" class="alarm-empty">{{ listError }}</li>
        <li v-else-if="alarmStore.alarmList.length === 0" class="alarm-empty">{{ msg('cm.alarm.empty') }}</li>
        <template v-else>
          <li
            v-for="alarm in alarmStore.alarmList"
            :key="alarm.alrmSn"
            class="alarm-item"
            :class="{ unconfirmed: alarm.cfmYn === 'N' }"
            tabindex="0"
            @click="onAlarmItemClick(alarm)"
            @keydown.enter="onAlarmItemClick(alarm)"
          >
            <div class="alarm-item-head">
              <span class="alarm-type">{{ alrmTypeNameMap[alarm.alrmTypeCd] || alarm.alrmTypeCd }}</span>
              <span class="alarm-time">{{ formatAlarmTime(alarm.firsRegDt) }}</span>
            </div>
            <div class="alarm-title">{{ alarm.alrmTitlNm }}</div>
            <div v-if="alarm.alrmCont" class="alarm-cont">{{ alarm.alrmCont }}</div>
          </li>
        </template>
      </ul>

      <div v-if="!alarmStore.isConnected" class="alarm-panel-footer">실시간 알림 연결 중…</div>
    </div>
  </div>
</template>

<script setup lang="ts">
/*****************************************************************************************
	Program ID   : AlarmCenter
	Program Type : vue
	Program Name : 알림 센터
	Description  : 상단 GNB 의 알림(벨) 버튼과 알람 목록 패널
	               - 미확인 건수는 stores/alarm.ts 의 SSE 연결로 실시간 갱신
	               - 알람을 클릭하면 읽음 처리 후 open-program 이벤트로 연결된 화면 열기를 요청
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-17  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
import { msg } from '@/utils/message';
import { formatDate } from '@/utils/date';
import { computed, onUnmounted, ref } from 'vue';
import { useAlarmStore } from '@/stores/alarm';
import { useToast } from '@/composables/useToast';
import { getCommCdOptions } from '@/api/common/commonCode';
import type { Alarm } from '@/api/common/alarm';

const emit = defineEmits<{
  (e: 'open-program', alarm: Alarm): void;
}>();

const alarmStore = useAlarmStore();
const toast = useToast();

const alrmCenterRoot = ref<HTMLElement | null>(null);
const btnAlrm = ref<HTMLButtonElement | null>(null);
const isOpen = ref(false);
const listError = ref('');
/** 알람유형코드 → 코드명 (SY/ALRM_TYPE_CD, 처음 열 때 한 번 조회) */
const alrmTypeNameMap = ref<Record<string, string>>({});
let isAlrmTypeLoaded = false;

/** 버튼 배지 표시 (100건 이상은 99+) */
const countLabel = computed(() => (alarmStore.unconfirmedCnt > 99 ? '99+' : String(alarmStore.unconfirmedCnt)));

/**
 * fetchAlrmTypeNames : 알람유형 공통코드명을 조회 (실패하면 코드를 그대로 표시)
 * @return  N/A
 */
const fetchAlrmTypeNames = async () => {
  if (isAlrmTypeLoaded) return;
  try {
    const options = await getCommCdOptions('SY', 'ALRM_TYPE_CD');
    alrmTypeNameMap.value = Object.fromEntries(options.map((option) => [option.value, option.label]));
    isAlrmTypeLoaded = true;
  } catch {
    // 코드명을 못 가져와도 알람 표시는 계속한다
  }
};

/**
 * openPanel : 알림 패널을 열고 알람 목록을 다시 조회
 * @return  N/A
 */
const openPanel = async () => {
  isOpen.value = true;
  document.addEventListener('mousedown', onDocumentMousedown);
  listError.value = '';
  fetchAlrmTypeNames();
  try {
    await alarmStore.fetchAlarmList();
  } catch {
    listError.value = msg('cm.alarm.load.failed');
  }
};

/**
 * closePanel : 알림 패널을 닫음
 * @return  N/A
 */
const closePanel = () => {
  if (!isOpen.value) return;
  isOpen.value = false;
  document.removeEventListener('mousedown', onDocumentMousedown);
  btnAlrm.value?.focus();
};

/**
 * onDocumentMousedown : 패널 바깥을 누르면 패널을 닫음
 * @param   event - 마우스 이벤트
 * @return  N/A
 */
const onDocumentMousedown = (event: MouseEvent) => {
  if (alrmCenterRoot.value && !alrmCenterRoot.value.contains(event.target as Node)) {
    isOpen.value = false;
    document.removeEventListener('mousedown', onDocumentMousedown);
  }
};

/**
 * onBtnAlrmClick : 알림 버튼 클릭 시 패널을 열거나 닫음
 * @return  N/A
 */
const onBtnAlrmClick = () => {
  if (isOpen.value) closePanel();
  else openPanel();
};

/**
 * onBtnAlrmCfmAllClick : 모두 읽음 버튼 클릭 시 미확인 알람 전체를 읽음 처리
 * @return  N/A
 */
const onBtnAlrmCfmAllClick = async () => {
  try {
    await alarmStore.confirmAllAlarms();
  } catch {
    toast.error(msg('cm.alarm.confirm.failed'));
  }
};

/**
 * onAlarmItemClick : 알람 클릭 시 읽음 처리하고, 연결된 화면이 있으면 열기를 요청
 * @param   alarm - 클릭한 알람
 * @return  N/A
 */
const onAlarmItemClick = async (alarm: Alarm) => {
  try {
    await alarmStore.confirmAlarm(alarm);
  } catch {
    toast.error(msg('cm.alarm.confirm.failed'));
  }
  if (alarm.prgmId) {
    closePanel();
    emit('open-program', alarm);
  }
};

/**
 * formatAlarmTime : 발송일시를 경과 시간(방금, N분 전, N시간 전) 또는 MM-DD HH:mm 으로 표시
 * @param   value - 발송일시 (yyyy-MM-ddTHH:mm:ss)
 * @return  표시 문자열
 * @example formatAlarmTime('2026-10-17T09:30:00')
 */
const formatAlarmTime = (value: string): string => {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value ?? '';
  const diffMinutes = Math.floor((Date.now() - date.getTime()) / 60000);
  if (diffMinutes < 1) return '방금';
  if (diffMinutes < 60) return `${diffMinutes}분 전`;
  if (diffMinutes < 60 * 24) return `${Math.floor(diffMinutes / 60)}시간 전`;
  return formatDate(date, 'MM-DD HH:mm');
};

onUnmounted(() => {
  document.removeEventListener('mousedown', onDocumentMousedown);
});
</script>

<style scoped>
.alarm-center {
  position: relative;
  flex-shrink: 0;
  margin-left: 6px;
}

.alarm-btn {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: var(--ds-radius-control);
  background: transparent;
  color: var(--ds-pine-100);
  cursor: pointer;
  transition: background-color var(--ds-ease), color var(--ds-ease);
}

.alarm-btn:hover,
.alarm-btn.open {
  background: var(--ds-pine-500);
  color: var(--ds-ink-inverse);
}

.alarm-count {
  position: absolute;
  top: 1px;
  right: 0;
  min-width: 15px;
  height: 15px;
  padding: 0 4px;
  box-sizing: border-box;
  border-radius: 8px;
  background: var(--ds-stamp);
  color: var(--ds-ink-inverse);
  font-size: 10px;
  font-weight: 700;
  line-height: 15px;
  text-align: center;
  font-variant-numeric: tabular-nums;
  box-shadow: 0 0 0 1.5px var(--ds-pine-700);
}

.alarm-panel {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 300;
  display: flex;
  flex-direction: column;
  width: 360px;
  max-height: 460px;
  background: var(--ds-surface);
  border: 1px solid var(--ds-rule-strong);
  border-radius: var(--ds-radius-overlay);
  box-shadow: var(--ds-shadow-overlay);
  overflow: hidden;
}

.alarm-panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-bottom: 1px solid var(--ds-rule);
}

.alarm-panel-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--ds-ink);
}

.alarm-panel-count {
  font-size: 12px;
  color: var(--ds-ink-3);
  font-variant-numeric: tabular-nums;
}

.alarm-cfm-all-btn {
  margin-left: auto;
  padding: 2px 6px;
  border: none;
  border-radius: var(--ds-radius-control);
  background: transparent;
  color: var(--ds-pine-500);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.alarm-cfm-all-btn:hover:not(:disabled) {
  background: var(--ds-pine-050);
}

.alarm-cfm-all-btn:disabled {
  color: var(--ds-ink-3);
  cursor: default;
}

.alarm-list {
  flex: 1;
  margin: 0;
  padding: 0;
  list-style: none;
  overflow-y: auto;
}

.alarm-item {
  position: relative;
  padding: 9px 12px 9px 20px;
  border-bottom: 1px solid var(--ds-rule);
  cursor: pointer;
}

.alarm-item:last-child {
  border-bottom: none;
}

.alarm-item:hover,
.alarm-item:focus-visible {
  background: var(--ds-pine-050);
  outline: none;
}

/* 미확인 알람: 왼쪽 점 + 굵은 제목 */
.alarm-item.unconfirmed::before {
  content: '';
  position: absolute;
  top: 15px;
  left: 8px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--ds-stamp);
}

.alarm-item-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 2px;
}

.alarm-type {
  font-size: 11px;
  font-weight: 600;
  color: var(--ds-pine-500);
}

.alarm-time {
  margin-left: auto;
  font-size: 11px;
  color: var(--ds-ink-3);
  font-variant-numeric: tabular-nums;
}

.alarm-title {
  font-size: 13px;
  color: var(--ds-ink-2);
}

.alarm-item.unconfirmed .alarm-title {
  font-weight: 600;
  color: var(--ds-ink);
}

.alarm-cont {
  margin-top: 2px;
  font-size: 12px;
  color: var(--ds-ink-3);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  white-space: pre-line;
}

.alarm-empty {
  padding: 24px 12px;
  font-size: 13px;
  color: var(--ds-ink-3);
  text-align: center;
}

.alarm-panel-footer {
  padding: 6px 12px;
  border-top: 1px solid var(--ds-rule);
  font-size: 11px;
  color: var(--ds-ink-3);
}
</style>
