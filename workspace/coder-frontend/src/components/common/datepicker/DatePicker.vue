<template>
  <div 
    class="custom-datepicker-wrapper" 
    :class="[
      `size-${size}`,
      { 'is-open': isOpen, 'is-disabled': disabled, 'is-readonly': readonly, 'is-inline': inline, 'is-range': range, 'is-required': isRequired }
    ]"
    :data-validate="validateRuleText"
    :data-empty="validateRuleText ? String(isEmptyValue) : undefined"
    :data-field-name="validateRuleText ? (fieldName || placeholder) : undefined"
    ref="pickerWrapperRef"
  >
    <!-- 인풋 트리거 영역 (inline 모드가 아닐 때 표시) -->
    <div 
      v-if="!inline" 
      class="datepicker-input-box" 
      :class="{ 'is-focused': isOpen, 'has-value': hasValue }"
      @click="toggleOpen"
      tabindex="0"
      @keydown.esc="closeCalendar"
      @keydown.enter.prevent="toggleOpen"
      @keydown.space.prevent="toggleOpen"
    >
      <span class="input-icon calendar-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24" width="18" height="18" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
          <line x1="16" y1="2" x2="16" y2="6"></line>
          <line x1="8" y1="2" x2="8" y2="6"></line>
          <line x1="3" y1="10" x2="21" y2="10"></line>
        </svg>
      </span>

      <span class="formatted-text" :class="{ 'placeholder': !hasValue }">
        {{ displayText }}
      </span>

      <!-- 클리어 버튼 & 열림/닫힘 화살표 -->
      <div class="input-actions" @click.stop>
        <button
          v-if="clearable && hasValue && !disabled && !readonly"
          type="button"
          class="action-btn clear-btn"
          title="날짜 지우기"
          @click="clearValue"
        >
          <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
          </svg>
        </button>
        <span class="chevron-icon" :class="{ 'rotate': isOpen }">
          <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="6 9 12 15 18 9"></polyline>
          </svg>
        </span>
      </div>
    </div>

    <!-- 달력 팝업 드롭다운 / 인라인 뷰 -->
    <Transition name="datepicker-dropdown">
      <div 
        v-if="inline || isOpen" 
        class="datepicker-popup" 
        :class="{ 'with-presets': showPresets }"
      >
        <!-- 빠른 프리셋 단축 영역 (단일 또는 범위 모드 지원) -->
        <aside v-if="showPresets" class="preset-sidebar">
          <div class="preset-header">단축 선택</div>
          <div class="preset-list">
            <button
              v-for="(p, idx) in resolvedPresets"
              :key="idx"
              type="button"
              class="preset-item"
              @click="applyPreset(p)"
            >
              {{ p.label }}
            </button>
          </div>
        </aside>

        <!-- 메인 달력 콘텐츠 -->
        <div class="datepicker-main">
          <!-- 달력 헤더 네비게이션 -->
          <div class="calendar-header">
            <div class="header-nav-group left">
              <!-- 이전 년도 -->
              <button 
                type="button" 
                class="nav-btn double-prev" 
                @click="navigateYear(-1)" 
                title="이전 년도"
              >
                <svg viewBox="0 0 24 24" width="15" height="15" stroke="currentColor" stroke-width="2.2" fill="none" stroke-linecap="round">
                  <polyline points="11 17 6 12 11 7"></polyline>
                  <polyline points="18 17 13 12 18 7"></polyline>
                </svg>
              </button>
              <!-- 이전 달 -->
              <button 
                v-if="currentView === 'days'" 
                type="button" 
                class="nav-btn prev-month" 
                @click="navigateMonth(-1)" 
                title="이전 달"
              >
                <svg viewBox="0 0 24 24" width="15" height="15" stroke="currentColor" stroke-width="2.2" fill="none" stroke-linecap="round">
                  <polyline points="15 18 9 12 15 6"></polyline>
                </svg>
              </button>
            </div>

            <!-- 년/월 전환 제목 -->
            <div class="header-titles">
              <button 
                type="button" 
                class="title-btn year-title" 
                @click="toggleView('years')"
              >
                {{ viewYearTitle }}
              </button>
              <button 
                v-if="currentView === 'days'" 
                type="button" 
                class="title-btn month-title" 
                @click="toggleView('months')"
              >
                {{ viewMonth + 1 }}월
              </button>
            </div>

            <div class="header-nav-group right">
              <!-- 다음 달 -->
              <button 
                v-if="currentView === 'days'" 
                type="button" 
                class="nav-btn next-month" 
                @click="navigateMonth(1)" 
                title="다음 달"
              >
                <svg viewBox="0 0 24 24" width="15" height="15" stroke="currentColor" stroke-width="2.2" fill="none" stroke-linecap="round">
                  <polyline points="9 18 15 12 9 6"></polyline>
                </svg>
              </button>
              <!-- 다음 년도 -->
              <button 
                type="button" 
                class="nav-btn double-next" 
                @click="navigateYear(1)" 
                title="다음 년도"
              >
                <svg viewBox="0 0 24 24" width="15" height="15" stroke="currentColor" stroke-width="2.2" fill="none" stroke-linecap="round">
                  <polyline points="13 17 18 12 13 7"></polyline>
                  <polyline points="6 17 11 12 6 7"></polyline>
                </svg>
              </button>
            </div>
          </div>

          <!-- 1. 일(Day) 선택 뷰 -->
          <div v-if="currentView === 'days'" class="calendar-days-view">
            <!-- 요일 헤더 -->
            <div class="weekdays-grid">
              <span 
                v-for="(w, idx) in weekDays" 
                :key="w" 
                class="weekday-cell" 
                :class="{ 'sun': idx === 0, 'sat': idx === 6 }"
              >
                {{ w }}
              </span>
            </div>

            <!-- 날짜 셀 그리드 (6행 7열 = 42일) -->
            <div class="days-grid" @mouseleave="hoveredDate = null">
              <button
                v-for="cell in calendarDays"
                :key="cell.dateString"
                type="button"
                class="day-cell"
                :class="getDayClasses(cell)"
                :disabled="cell.disabled"
                @click="handleDayClick(cell)"
                @mouseenter="handleDayHover(cell)"
              >
                <span class="day-text">{{ cell.dayNumber }}</span>
                <span v-if="cell.isToday" class="today-indicator"></span>
              </button>
            </div>
          </div>

          <!-- 2. 월(Month) 선택 뷰 -->
          <div v-else-if="currentView === 'months'" class="calendar-months-view">
            <div class="months-grid">
              <button
                v-for="(mName, mIdx) in monthNames"
                :key="mIdx"
                type="button"
                class="month-cell"
                :class="{
                  'is-current-month': isCurrentMonth(mIdx),
                  'is-selected': isMonthSelected(mIdx)
                }"
                @click="selectMonth(mIdx)"
              >
                {{ mName }}
              </button>
            </div>
          </div>

          <!-- 3. 연도(Year) 선택 뷰 -->
          <div v-else-if="currentView === 'years'" class="calendar-years-view">
            <div class="years-grid">
              <button
                v-for="y in yearList"
                :key="y"
                type="button"
                class="year-cell"
                :class="{
                  'is-current-year': y === todayYear,
                  'is-selected': isYearSelected(y)
                }"
                @click="selectYear(y)"
              >
                {{ y }}
              </button>
            </div>
          </div>

          <!-- 팝업 푸터 (오늘/초기화 빠른 액션) -->
          <div class="calendar-footer">
            <button 
              type="button" 
              class="footer-action-btn today-btn" 
              @click="goToToday"
            >
              오늘로 이동
            </button>
            <div class="footer-status-text" v-if="range">
              <span v-if="!tempRangeStart">{{ msg('cm.info.select.start.date') }}</span>
              <span v-else-if="!tempRangeEnd">{{ msg('cm.info.select.end.date') }}</span>
              <span v-else class="success-text">{{ msg('cm.info.select.completed') }}</span>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
/*****************************************************************************************
	Program ID   : DatePicker
	Program Type : vue
	Program Name : 날짜 선택기
	Description  : 단일/기간 선택, 월/연 전환, 프리셋을 지원하는 공통 달력 입력
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-09-07  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
import { msg } from '@/utils/message';
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue';
import { normalizeValidateRules } from '@/utils/validate';

const props = defineProps({
  modelValue: {
    type: [String, Date, Array],
    default: null
  },
  range: {
    type: Boolean,
    default: false
  },
  placeholder: {
    type: String,
    default: ''
  },
  format: {
    type: String,
    default: 'YYYY-MM-DD'
  },
  disabled: {
    type: Boolean,
    default: false
  },
  readonly: {
    type: Boolean,
    default: false
  },
  // 필수 입력 (utils/validate.ts validateRequired 대상)
  required: {
    type: Boolean,
    default: false
  },
  // 검증 규칙: "required|email|juminno" 또는 배열 (utils/validate.ts validateForm 대상)
  validate: {
    type: [String, Array],
    default: ''
  },
  // 검증 메시지에 쓸 항목명 (없으면 placeholder)
  fieldName: {
    type: String,
    default: ''
  },
  clearable: {
    type: Boolean,
    default: true
  },
  minDate: {
    type: [String, Date],
    default: null
  },
  maxDate: {
    type: [String, Date],
    default: null
  },
  disabledDates: {
    type: Function,
    default: null
  },
  presets: {
    type: [Boolean, Array],
    default: true
  },
  inline: {
    type: Boolean,
    default: false
  },
  size: {
    type: String,
    default: 'medium', // 'small' | 'medium' | 'large'
    validator: (v) => ['small', 'medium', 'large'].includes(v)
  }
});

const emit = defineEmits(['update:modelValue', 'change', 'clear', 'open', 'close']);

// 래퍼 참조 (외부 클릭 감지용)
const pickerWrapperRef = ref(null);

// 검증 규칙 (required 속성은 validate="required" 의 줄임말)
const validateRules = computed(() => normalizeValidateRules(props.validate, props.required));
const validateRuleText = computed(() => validateRules.value.join(' ') || undefined);
const isRequired = computed(() => validateRules.value.includes('required'));

// 검증용 빈 값 여부 (range 는 시작·종료 중 하나라도 비면 빈 값)
const isEmptyValue = computed(() => {
  const v = props.modelValue;
  if (Array.isArray(v)) return v.length < 2 || !v[0] || !v[1];
  return v === null || v === undefined || v === '';
});

// 달력 열림/닫힘 상태
const isOpen = ref(false);

// 달력 탐색 상태
const today = new Date();
const todayYear = today.getFullYear();
const todayMonth = today.getMonth();
const todayDate = today.getDate();

const viewYear = ref(todayYear);
const viewMonth = ref(todayMonth); // 0-indexed
const currentView = ref('days'); // 'days' | 'months' | 'years'
const hoveredDate = ref(null); // hover 시 범위 미리보기용

// 범위 선택을 위한 임시 저장소
const tempRangeStart = ref(null);
const tempRangeEnd = ref(null);

// 요일 및 월 이름 상수
const weekDays = ['일', '월', '화', '수', '목', '금', '토'];
const monthNames = [
  '1월', '2월', '3월', '4월', '5월', '6월',
  '7월', '8월', '9월', '10월', '11월', '12월'
];

// --- 날짜 유틸 함수 ---
/**
 * padZero : 한 자리 숫자 앞에 0 을 붙여 두 자리 문자열로 변환
 * @param   n - 변환할 숫자
 * @return  두 자리 문자열
 */
const padZero = (n) => (n < 10 ? `0${n}` : `${n}`);

/**
 * formatDate : Date 를 지정한 형식(YYYY, MM, DD)의 문자열로 변환
 * @param   d   - 변환할 Date
 * @param   fmt - 날짜 형식 (기본값은 props.format)
 * @return  날짜 문자열 (유효하지 않으면 빈 문자열)
 */
const formatDate = (d, fmt = props.format) => {
  if (!d || !(d instanceof Date) || isNaN(d.getTime())) return '';
  const y = d.getFullYear();
  const m = padZero(d.getMonth() + 1);
  const date = padZero(d.getDate());
  return fmt
    .replace('YYYY', `${y}`)
    .replace('MM', m)
    .replace('DD', date);
};

/**
 * parseDateString : 문자열 또는 Date 를 시간 정보 없는 Date 로 변환
 * @param   val - 날짜 문자열(YYYY-MM-DD 등) 또는 Date
 * @return  Date (변환 불가 시 null)
 */
const parseDateString = (val) => {
  if (!val) return null;
  if (val instanceof Date) {
    return isNaN(val.getTime()) ? null : new Date(val.getFullYear(), val.getMonth(), val.getDate());
  }
  if (typeof val === 'string') {
    // "YYYY-MM-DD" 혹은 호환 가능한 포맷 파싱
    const parts = val.trim().split(/[-/.]/);
    if (parts.length >= 3) {
      const y = parseInt(parts[0], 10);
      const m = parseInt(parts[1], 10) - 1;
      const d = parseInt(parts[2], 10);
      const parsed = new Date(y, m, d);
      return isNaN(parsed.getTime()) ? null : parsed;
    }
  }
  return null;
};

/**
 * isSameDay : 두 날짜가 같은 날인지 비교
 * @param   d1 - 날짜 1
 * @param   d2 - 날짜 2
 * @return  같은 날 여부
 */
const isSameDay = (d1, d2) => {
  if (!d1 || !d2) return false;
  return (
    d1.getFullYear() === d2.getFullYear() &&
    d1.getMonth() === d2.getMonth() &&
    d1.getDate() === d2.getDate()
  );
};

/**
 * isBeforeDay : 첫 번째 날짜가 두 번째 날짜보다 이전 날인지 비교
 * @param   d1 - 기준 날짜
 * @param   d2 - 비교 날짜
 * @return  이전 날 여부
 */
const isBeforeDay = (d1, d2) => {
  if (!d1 || !d2) return false;
  const t1 = new Date(d1.getFullYear(), d1.getMonth(), d1.getDate()).getTime();
  const t2 = new Date(d2.getFullYear(), d2.getMonth(), d2.getDate()).getTime();
  return t1 < t2;
};

/**
 * isBetweenDays : 대상 날짜가 시작일과 종료일 사이(양끝 제외)에 있는지 판별
 * @param   target - 대상 날짜
 * @param   start  - 시작일
 * @param   end    - 종료일
 * @return  범위 내 여부
 */
const isBetweenDays = (target, start, end) => {
  if (!target || !start || !end) return false;
  const t = new Date(target.getFullYear(), target.getMonth(), target.getDate()).getTime();
  const s = new Date(start.getFullYear(), start.getMonth(), start.getDate()).getTime();
  const e = new Date(end.getFullYear(), end.getMonth(), end.getDate()).getTime();
  return t > s && t < e;
};

// --- 값 파싱 및 표시 텍스트 계산 ---
const parsedSingleValue = computed(() => {
  if (props.range) return null;
  return parseDateString(props.modelValue);
});

const parsedRangeValue = computed(() => {
  if (!props.range) return [null, null];
  if (Array.isArray(props.modelValue) && props.modelValue.length >= 2) {
    return [
      parseDateString(props.modelValue[0]),
      parseDateString(props.modelValue[1])
    ];
  }
  return [null, null];
});

const hasValue = computed(() => {
  if (props.range) {
    const [start, end] = parsedRangeValue.value;
    return Boolean(start && end);
  }
  return Boolean(parsedSingleValue.value);
});

const displayText = computed(() => {
  if (props.range) {
    const [start, end] = parsedRangeValue.value;
    if (start && end) {
      return `${formatDate(start)} ~ ${formatDate(end)}`;
    }
    return props.placeholder || '시작일 ~ 종료일 선택';
  }

  if (parsedSingleValue.value) {
    return formatDate(parsedSingleValue.value);
  }
  return props.placeholder || '날짜를 선택하세요';
});

// 달력 헤더 타이틀 표시
const viewYearTitle = computed(() => {
  if (currentView.value === 'years') {
    const startYear = Math.floor(viewYear.value / 12) * 12;
    return `${startYear}년 - ${startYear + 11}년`;
  }
  return `${viewYear.value}년`;
});

// 연도 선택 그리드 (12개)
const yearList = computed(() => {
  const startYear = Math.floor(viewYear.value / 12) * 12;
  const list = [];
  for (let i = 0; i < 12; i++) {
    list.push(startYear + i);
  }
  return list;
});

// 프리셋 리스트 계산
const resolvedPresets = computed(() => {
  if (!props.presets) return [];
  if (Array.isArray(props.presets)) return props.presets;

  // 기본 제공 프리셋
  if (props.range) {
    return [
      {
        label: '오늘',
        value: () => {
          const now = new Date();
          return [now, now];
        }
      },
      {
        label: '최근 7일',
        value: () => {
          const end = new Date();
          const start = new Date();
          start.setDate(end.getDate() - 6);
          return [start, end];
        }
      },
      {
        label: '최근 30일',
        value: () => {
          const end = new Date();
          const start = new Date();
          start.setDate(end.getDate() - 29);
          return [start, end];
        }
      },
      {
        label: '이번 달',
        value: () => {
          const now = new Date();
          const start = new Date(now.getFullYear(), now.getMonth(), 1);
          const end = new Date(now.getFullYear(), now.getMonth() + 1, 0);
          return [start, end];
        }
      },
      {
        label: '지난 달',
        value: () => {
          const now = new Date();
          const start = new Date(now.getFullYear(), now.getMonth() - 1, 1);
          const end = new Date(now.getFullYear(), now.getMonth(), 0);
          return [start, end];
        }
      }
    ];
  } else {
    return [
      {
        label: '오늘',
        value: () => new Date()
      },
      {
        label: '어제',
        value: () => {
          const d = new Date();
          d.setDate(d.getDate() - 1);
          return d;
        }
      },
      {
        label: '내일',
        value: () => {
          const d = new Date();
          d.setDate(d.getDate() + 1);
          return d;
        }
      },
      {
        label: '일주일 뒤',
        value: () => {
          const d = new Date();
          d.setDate(d.getDate() + 7);
          return d;
        }
      }
    ];
  }
});

const showPresets = computed(() => {
  return Boolean(props.presets && resolvedPresets.value.length > 0);
});

// 달력 날짜 그리드 (42일 계산: 이전 달 잔여 + 현재 달 + 다음 달 잔여)
const calendarDays = computed(() => {
  const year = viewYear.value;
  const month = viewMonth.value;

  const firstDayOfMonth = new Date(year, month, 1);
  const firstDayWeekday = firstDayOfMonth.getDay(); // 0(일) ~ 6(토)
  const lastDayOfMonth = new Date(year, month + 1, 0);
  const daysInMonth = lastDayOfMonth.getDate();

  const daysInPrevMonth = new Date(year, month, 0).getDate();

  const minD = parseDateString(props.minDate);
  const maxD = parseDateString(props.maxDate);

  const days = [];

  // 이전 달의 끝부분 채우기
  for (let i = firstDayWeekday - 1; i >= 0; i--) {
    const dayNum = daysInPrevMonth - i;
    const dateObj = new Date(year, month - 1, dayNum);
    days.push(createDayCell(dateObj, false, minD, maxD));
  }

  // 이번 달 일 채우기
  for (let i = 1; i <= daysInMonth; i++) {
    const dateObj = new Date(year, month, i);
    days.push(createDayCell(dateObj, true, minD, maxD));
  }

  // 다음 달 시작부분 채우기 (총 42칸 = 6주 완성)
  const remaining = 42 - days.length;
  for (let i = 1; i <= remaining; i++) {
    const dateObj = new Date(year, month + 1, i);
    days.push(createDayCell(dateObj, false, minD, maxD));
  }

  return days;
});

/**
 * createDayCell : 달력의 날짜 셀 정보를 생성 (비활성 여부, 오늘 여부 포함)
 * @param   dateObj        - 셀의 날짜
 * @param   isCurrentMonth - 현재 표시 월에 속하는지 여부
 * @param   minD           - 선택 가능 최소일
 * @param   maxD           - 선택 가능 최대일
 * @return  날짜 셀 객체
 */
const createDayCell = (dateObj, isCurrentMonth, minD, maxD) => {
  const dateString = formatDate(dateObj, 'YYYY-MM-DD');
  let disabled = false;

  if (minD && isBeforeDay(dateObj, minD)) {
    disabled = true;
  }
  if (maxD && isBeforeDay(maxD, dateObj)) {
    disabled = true;
  }
  if (props.disabledDates && typeof props.disabledDates === 'function') {
    if (props.disabledDates(dateObj)) {
      disabled = true;
    }
  }

  const isToday =
    dateObj.getFullYear() === todayYear &&
    dateObj.getMonth() === todayMonth &&
    dateObj.getDate() === todayDate;

  return {
    date: dateObj,
    dateString,
    dayNumber: dateObj.getDate(),
    isCurrentMonth,
    isToday,
    disabled
  };
};

// 각 날짜 셀의 CSS 클래스 계산
/**
 * getDayClasses : 날짜 셀의 상태(선택, 범위, 오늘, 비활성 등)에 따른 CSS 클래스 객체를 계산
 * @param   cell - 날짜 셀
 * @return  CSS 클래스 객체
 */
const getDayClasses = (cell) => {
  const target = cell.date;
  const classes = {
    'other-month': !cell.isCurrentMonth,
    'is-today': cell.isToday,
    'is-disabled': cell.disabled
  };

  if (!props.range) {
    if (parsedSingleValue.value && isSameDay(target, parsedSingleValue.value)) {
      classes['is-selected'] = true;
    }
    return classes;
  }

  // 범위(range) 모드 클래스 계산
  const effectiveStart = tempRangeStart.value || parsedRangeValue.value[0];
  const effectiveEnd = tempRangeEnd.value || parsedRangeValue.value[1];

  const isStart = effectiveStart && isSameDay(target, effectiveStart);
  const isEnd = effectiveEnd && isSameDay(target, effectiveEnd);

  if (isStart) classes['range-start'] = true;
  if (isEnd) classes['range-end'] = true;
  if (isStart || isEnd) classes['is-selected'] = true;

  // 선택 완료된 범위 내부
  if (effectiveStart && effectiveEnd && isBetweenDays(target, effectiveStart, effectiveEnd)) {
    classes['in-range'] = true;
  }

  // 범위 선택 중 hover 미리보기
  if (tempRangeStart.value && !tempRangeEnd.value && hoveredDate.value) {
    let s = tempRangeStart.value;
    let e = hoveredDate.value;
    if (isBeforeDay(e, s)) {
      [s, e] = [e, s];
    }
    if (isBetweenDays(target, s, e)) {
      classes['in-range-hover'] = true;
    }
    if (isSameDay(target, hoveredDate.value)) {
      classes['hover-target'] = true;
    }
  }

  return classes;
};

// --- 액션 핸들러 ---
/**
 * toggleOpen : 달력 열림/닫힘을 전환 (비활성, 읽기전용, 인라인이면 무시)
 * @return  N/A
 */
const toggleOpen = () => {
  if (props.disabled || props.readonly || props.inline) return;
  if (isOpen.value) {
    closeCalendar();
  } else {
    openCalendar();
  }
};

/**
 * openCalendar : 달력을 열고 선택값에 맞춰 표시 월을 맞춤
 * @return  N/A
 */
const openCalendar = () => {
  isOpen.value = true;
  currentView.value = 'days';
  syncViewWithSelectedDate();
  emit('open');
};

/**
 * closeCalendar : 달력을 닫고 임시 범위 선택 상태를 초기화
 * @return  N/A
 */
const closeCalendar = () => {
  if (isOpen.value) {
    isOpen.value = false;
    tempRangeStart.value = null;
    tempRangeEnd.value = null;
    hoveredDate.value = null;
    emit('close');
  }
};

/**
 * clearValue : 선택된 날짜를 비우고 clear 이벤트를 발생
 * @return  N/A
 */
const clearValue = () => {
  if (props.disabled || props.readonly) return;
  const emptyVal = props.range ? [] : '';
  tempRangeStart.value = null;
  tempRangeEnd.value = null;
  emit('update:modelValue', emptyVal);
  emit('change', emptyVal);
  emit('clear');
};

/**
 * syncViewWithSelectedDate : 선택된 날짜(없으면 오늘)에 맞춰 달력 표시 연/월을 동기화
 * @return  N/A
 */
const syncViewWithSelectedDate = () => {
  if (props.range) {
    const [start] = parsedRangeValue.value;
    if (start) {
      viewYear.value = start.getFullYear();
      viewMonth.value = start.getMonth();
      return;
    }
  } else if (parsedSingleValue.value) {
    viewYear.value = parsedSingleValue.value.getFullYear();
    viewMonth.value = parsedSingleValue.value.getMonth();
    return;
  }

  viewYear.value = todayYear;
  viewMonth.value = todayMonth;
};

/**
 * navigateMonth : 표시 월을 이동 (연도 경계 처리)
 * @param   direction - 이동 방향 (-1 이전달, 1 다음달)
 * @return  N/A
 */
const navigateMonth = (direction) => {
  let m = viewMonth.value + direction;
  let y = viewYear.value;
  if (m < 0) {
    m = 11;
    y -= 1;
  } else if (m > 11) {
    m = 0;
    y += 1;
  }
  viewMonth.value = m;
  viewYear.value = y;
};

/**
 * navigateYear : 표시 연도를 이동 (연도 뷰에서는 12년 단위)
 * @param   direction - 이동 방향 (-1 이전, 1 다음)
 * @return  N/A
 */
const navigateYear = (direction) => {
  if (currentView.value === 'years') {
    viewYear.value += direction * 12;
  } else {
    viewYear.value += direction;
  }
};

/**
 * toggleView : 월/연 선택 뷰를 전환 (이미 해당 뷰면 일 뷰로 복귀)
 * @param   view - 전환할 뷰 (months, years)
 * @return  N/A
 */
const toggleView = (view) => {
  if (currentView.value === view) {
    currentView.value = 'days';
  } else {
    currentView.value = view;
  }
};

/**
 * selectMonth : 월을 선택하고 일 뷰로 복귀
 * @param   mIdx - 월 인덱스 (0~11)
 * @return  N/A
 */
const selectMonth = (mIdx) => {
  viewMonth.value = mIdx;
  currentView.value = 'days';
};

/**
 * isCurrentMonth : 표시 연도 기준으로 해당 월이 오늘이 속한 월인지 판별
 * @param   mIdx - 월 인덱스 (0~11)
 * @return  오늘이 속한 월 여부
 */
const isCurrentMonth = (mIdx) => {
  return viewYear.value === todayYear && mIdx === todayMonth;
};

/**
 * isMonthSelected : 표시 연도 기준으로 해당 월이 선택된 날짜의 월인지 판별
 * @param   mIdx - 월 인덱스 (0~11)
 * @return  선택된 월 여부
 */
const isMonthSelected = (mIdx) => {
  const selDate = props.range ? parsedRangeValue.value[0] : parsedSingleValue.value;
  return selDate && selDate.getFullYear() === viewYear.value && selDate.getMonth() === mIdx;
};

/**
 * selectYear : 연도를 선택하고 월 뷰로 이동
 * @param   y - 선택한 연도
 * @return  N/A
 */
const selectYear = (y) => {
  viewYear.value = y;
  currentView.value = 'months';
};

/**
 * isYearSelected : 해당 연도가 선택된 날짜의 연도인지 판별
 * @param   y - 확인할 연도
 * @return  선택된 연도 여부
 */
const isYearSelected = (y) => {
  const selDate = props.range ? parsedRangeValue.value[0] : parsedSingleValue.value;
  return selDate && selDate.getFullYear() === y;
};

/**
 * goToToday : 오늘이 속한 연/월의 일 뷰로 이동
 * @return  N/A
 */
const goToToday = () => {
  viewYear.value = todayYear;
  viewMonth.value = todayMonth;
  currentView.value = 'days';
};

// 날짜 클릭 이벤트 처리
/**
 * handleDayClick : 날짜 클릭 시 단일 값 또는 기간(시작일/종료일) 선택을 처리
 * @param   cell - 클릭된 날짜 셀
 * @return  N/A
 */
const handleDayClick = (cell) => {
  if (cell.disabled || props.readonly) return;

  // 단일 선택 모드
  if (!props.range) {
    const formatted = formatDate(cell.date);
    emit('update:modelValue', formatted);
    emit('change', formatted);
    if (!props.inline) {
      closeCalendar();
    }
    return;
  }

  // 범위 선택 모드
  if (!tempRangeStart.value || (tempRangeStart.value && tempRangeEnd.value)) {
    // 1번째 클릭: 시작일 지정
    tempRangeStart.value = cell.date;
    tempRangeEnd.value = null;
  } else {
    // 2번째 클릭: 종료일 지정
    let start = tempRangeStart.value;
    let end = cell.date;

    if (isBeforeDay(end, start)) {
      [start, end] = [end, start];
    }

    const rangeFormatted = [formatDate(start), formatDate(end)];
    tempRangeStart.value = null;
    tempRangeEnd.value = null;

    emit('update:modelValue', rangeFormatted);
    emit('change', rangeFormatted);

    if (!props.inline) {
      closeCalendar();
    }
  }
};

/**
 * handleDayHover : 기간 선택 중 날짜 셀에 마우스를 올리면 미리보기용 날짜를 갱신
 * @param   cell - 마우스가 올라간 날짜 셀
 * @return  N/A
 */
const handleDayHover = (cell) => {
  if (!props.range || !tempRangeStart.value || tempRangeEnd.value) return;
  hoveredDate.value = cell.date;
};

// 빠른 프리셋 적용
/**
 * applyPreset : 빠른 선택 프리셋을 적용해 값을 갱신
 * @param   preset - 프리셋 (value 는 날짜 또는 기간을 반환하는 함수)
 * @return  N/A
 */
const applyPreset = (preset) => {
  if (typeof preset.value !== 'function') return;
  const result = preset.value();

  if (props.range && Array.isArray(result)) {
    const formatted = [formatDate(result[0]), formatDate(result[1])];
    emit('update:modelValue', formatted);
    emit('change', formatted);
    viewYear.value = result[0].getFullYear();
    viewMonth.value = result[0].getMonth();
  } else if (!props.range && result instanceof Date) {
    const formatted = formatDate(result);
    emit('update:modelValue', formatted);
    emit('change', formatted);
    viewYear.value = result.getFullYear();
    viewMonth.value = result.getMonth();
  }

  if (!props.inline) {
    closeCalendar();
  }
};

// 외부 클릭 시 닫기
/**
 * handleOutsideClick : 달력 바깥 클릭 시 닫음 (인라인 모드 제외)
 * @param   e - 클릭 이벤트
 * @return  N/A
 */
const handleOutsideClick = (e) => {
  if (!pickerWrapperRef.value || props.inline) return;
  if (!pickerWrapperRef.value.contains(e.target)) {
    closeCalendar();
  }
};

watch(() => props.modelValue, () => {
  syncViewWithSelectedDate();
});

onMounted(() => {
  document.addEventListener('click', handleOutsideClick);
  syncViewWithSelectedDate();
});

onBeforeUnmount(() => {
  document.removeEventListener('click', handleOutsideClick);
});
</script>

<style scoped>
/* ============================================================
   DatePicker — CODER 디자인 시스템 v2
   로컬 --dp-* 변수는 전역 --ds-* 토큰에 연결한다.
============================================================ */
.custom-datepicker-wrapper {
  position: relative;
  display: inline-block;
  font-family: inherit;
  user-select: none;
  --dp-primary: var(--ds-pine-700);
  --dp-primary-hover: var(--ds-pine-900);
  --dp-secondary: var(--ds-pine-500);
  --dp-bg: var(--ds-surface);
  --dp-surface: var(--ds-surface);
  --dp-surface-hover: var(--ds-pine-050);
  --dp-border: var(--ds-rule-strong);
  --dp-border-hover: var(--ds-ink-3);
  --dp-border-focus: var(--ds-pine-700);
  --dp-text: var(--ds-ink);
  --dp-text-muted: var(--ds-ink-3);
  --dp-text-dimmed: var(--ds-rule-strong);
  --dp-range-bg: var(--ds-pine-100);
  --dp-range-hover-bg: var(--ds-pine-050);
  --dp-shadow: var(--ds-shadow-overlay);
  --dp-radius: var(--ds-radius-control);
}

/* 크기 변형 */
.size-small {
  --dp-height: var(--ds-control-height-sm);
  --dp-font-size: 13px;
  --dp-cell-size: 30px;
}
.size-medium {
  --dp-height: var(--ds-control-height);
  --dp-font-size: 14px;
  --dp-cell-size: 34px;
}
.size-large {
  --dp-height: 40px;
  --dp-font-size: 15px;
  --dp-cell-size: 38px;
}

/* --- 인풋 박스 영역 --- */
.datepicker-input-box {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: var(--dp-height);
  min-width: 220px;
  padding: 0 10px;
  background: var(--dp-surface);
  border: 1px solid var(--dp-border);
  border-radius: var(--dp-radius);
  color: var(--dp-text);
  font-size: var(--dp-font-size);
  font-variant-numeric: tabular-nums;
  cursor: pointer;
  outline: none;
  transition: border-color var(--ds-ease), box-shadow var(--ds-ease);
}

.is-range .datepicker-input-box {
  min-width: 260px;
}

.datepicker-input-box:hover:not(.is-disabled) {
  border-color: var(--dp-border-hover);
}

.datepicker-input-box:focus-visible,
.datepicker-input-box.is-focused,
.datepicker-input-box.is-focused:hover:not(.is-disabled) {
  border-color: var(--dp-border-focus);
  box-shadow: var(--ds-focus-field);
}

/* 필수 항목 표시 : 입력 박스 왼쪽 안쪽 빨간 줄 (비활성/읽기전용 제외) */
.is-required:not(.is-disabled):not(.is-readonly) .datepicker-input-box {
  box-shadow: inset 2px 0 0 var(--ds-stamp);
}

.is-required:not(.is-disabled):not(.is-readonly) .datepicker-input-box:focus-visible,
.is-required:not(.is-disabled):not(.is-readonly) .datepicker-input-box.is-focused {
  box-shadow: inset 2px 0 0 var(--ds-stamp), var(--ds-focus-field);
}

.input-icon {
  display: flex;
  align-items: center;
  color: var(--dp-text-muted);
  flex-shrink: 0;
  transition: color var(--ds-ease);
}

.datepicker-input-box.is-focused .input-icon {
  color: var(--dp-primary);
}

.formatted-text {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 400;
}

.formatted-text.placeholder {
  color: var(--dp-text-muted);
  font-weight: 400;
}

.input-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.action-btn.clear-btn {
  background: none;
  border: none;
  padding: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--dp-text-muted);
  border-radius: 2px;
  cursor: pointer;
  transition: background-color var(--ds-ease), color var(--ds-ease);
}

.action-btn.clear-btn:hover {
  background: var(--ds-stamp-tint);
  color: var(--ds-stamp);
}

.chevron-icon {
  display: flex;
  align-items: center;
  color: var(--dp-text-muted);
}

.chevron-icon.rotate {
  transform: rotate(180deg);
  color: var(--dp-primary);
}

/* 비활성화 상태 */
.is-disabled {
  cursor: not-allowed;
}
.is-disabled .datepicker-input-box {
  cursor: not-allowed;
  pointer-events: none;
  background: var(--ds-surface-muted);
  border-color: var(--ds-rule);
  color: var(--ds-ink-3);
}

/* --- 팝업 드롭다운 --- */
.datepicker-popup {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  z-index: 1000;
  display: flex;
  background: var(--dp-bg);
  border: 1px solid var(--ds-rule-strong);
  border-radius: var(--ds-radius-overlay);
  box-shadow: var(--dp-shadow);
  overflow: hidden;
}

/* 인라인 모드일 때 포지셔닝 해제 */
.is-inline .datepicker-popup {
  position: static;
  box-shadow: none;
  border-color: var(--ds-rule);
  border-radius: var(--ds-radius-panel);
  display: inline-flex;
}

/* 공통: 팝업 내부 버튼 키보드 포커스 */
.preset-item:focus-visible,
.nav-btn:focus-visible,
.title-btn:focus-visible,
.day-cell:focus-visible,
.month-cell:focus-visible,
.year-cell:focus-visible,
.footer-action-btn:focus-visible,
.action-btn.clear-btn:focus-visible {
  outline: none;
  box-shadow: var(--ds-focus);
  z-index: 4;
}

/* --- 사이드바 프리셋 --- */
.preset-sidebar {
  width: 110px;
  background: var(--ds-pine-050);
  border-right: 1px solid var(--ds-rule);
  padding: 12px 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.preset-header {
  font-size: 12px;
  font-weight: 600;
  color: var(--ds-ink-2);
  padding: 0 6px 6px;
  border-bottom: 1px solid var(--ds-rule);
}

.preset-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.preset-item {
  background: none;
  border: none;
  padding: 6px 8px;
  border-radius: var(--ds-radius-control);
  font-size: 12.5px;
  color: var(--dp-text);
  text-align: left;
  cursor: pointer;
  transition: background-color var(--ds-ease), color var(--ds-ease);
  font-family: inherit;
}

.preset-item:hover {
  background: var(--ds-pine-100);
  color: var(--ds-pine-900);
}

/* --- 메인 캘린더 바디 --- */
.datepicker-main {
  padding: 12px;
  min-width: 260px;
}

/* 캘린더 헤더 */
.calendar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  padding: 0 2px;
}

.header-nav-group {
  display: flex;
  align-items: center;
  gap: 2px;
}

.nav-btn {
  background: none;
  border: none;
  color: var(--ds-ink-2);
  width: 28px;
  height: 28px;
  border-radius: var(--ds-radius-control);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color var(--ds-ease), color var(--ds-ease);
}

.nav-btn:hover {
  background: var(--dp-surface-hover);
  color: var(--ds-pine-900);
}

.header-titles {
  display: flex;
  align-items: center;
  gap: 4px;
}

.title-btn {
  background: none;
  border: none;
  font-size: 15px;
  font-weight: 600;
  color: var(--dp-text);
  font-variant-numeric: tabular-nums;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--ds-radius-control);
  transition: background-color var(--ds-ease), color var(--ds-ease);
  font-family: inherit;
}

.title-btn:hover {
  background: var(--dp-surface-hover);
  color: var(--ds-pine-900);
}

/* --- 요일 헤더 그리드 --- */
.weekdays-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  margin-bottom: 4px;
  text-align: center;
  border-bottom: 1px solid var(--ds-rule);
}

.weekday-cell {
  font-size: 12px;
  font-weight: 600;
  color: var(--ds-ink-2);
  padding: 4px 0;
}

.weekday-cell.sun {
  color: var(--ds-stamp);
}

.weekday-cell.sat {
  color: var(--ds-pine-500);
}

/* --- 일자 그리드 --- */
.days-grid {
  display: grid;
  grid-template-columns: repeat(7, var(--dp-cell-size));
  grid-auto-rows: var(--dp-cell-size);
  gap: 2px 0;
  justify-content: center;
}

.day-cell {
  position: relative;
  background: none;
  border: none;
  border-radius: var(--ds-radius-control);
  color: var(--dp-text);
  font-size: 13px;
  font-variant-numeric: tabular-nums;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color var(--ds-ease), color var(--ds-ease);
  font-family: inherit;
  outline: none;
  padding: 0;
}

.day-text {
  position: relative;
  z-index: 2;
  font-weight: 400;
}

.day-cell:hover:not(.is-disabled):not(.is-selected) {
  background: var(--dp-surface-hover);
  color: var(--ds-pine-900);
}

/* 오늘 표시: pine-500 1px 링 */
.day-cell.is-today {
  box-shadow: inset 0 0 0 1px var(--ds-pine-500);
}

.today-indicator {
  display: none;
}

/* 다른 달 날짜 */
.day-cell.other-month {
  color: var(--ds-ink-3);
}

/* 범위 선택 관련 스타일 */
.day-cell.in-range {
  background: var(--dp-range-bg);
  border-radius: 0;
  color: var(--ds-pine-900);
}

.day-cell.in-range-hover {
  background: var(--dp-range-hover-bg);
  border-radius: 0;
}

.day-cell.hover-target {
  background: var(--ds-pine-100);
  box-shadow: inset 0 0 0 1px var(--ds-pine-700);
  border-radius: var(--ds-radius-control);
}

/* 선택된 날짜 (단일 or 시작/종료일) */
.day-cell.is-selected,
.day-cell.is-selected:hover {
  background: var(--dp-primary);
  color: var(--ds-ink-inverse);
  font-weight: 600;
  z-index: 3;
}

.day-cell.is-selected .day-text {
  font-weight: 600;
}

.day-cell.is-selected.is-today {
  box-shadow: inset 0 0 0 1px var(--dp-primary);
}

.day-cell.range-start {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
}

.day-cell.range-end {
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
}

.day-cell.range-start.range-end {
  border-radius: var(--ds-radius-control);
}

/* 비활성화 셀 */
.day-cell.is-disabled {
  color: var(--dp-text-dimmed);
  cursor: not-allowed;
  pointer-events: none;
}

/* --- 월 / 연도 선택 그리드 --- */
.months-grid,
.years-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  padding: 8px 0;
}

.month-cell,
.year-cell {
  background: none;
  border: 1px solid transparent;
  border-radius: var(--ds-radius-control);
  color: var(--dp-text);
  font-size: 13px;
  font-weight: 400;
  font-variant-numeric: tabular-nums;
  padding: 12px 6px;
  cursor: pointer;
  transition: background-color var(--ds-ease), border-color var(--ds-ease), color var(--ds-ease);
  font-family: inherit;
}

.month-cell:hover,
.year-cell:hover {
  background: var(--dp-surface-hover);
  border-color: var(--ds-rule);
  color: var(--ds-pine-900);
}

.month-cell.is-current-month,
.year-cell.is-current-year {
  border-color: var(--ds-pine-500);
}

.month-cell.is-selected,
.year-cell.is-selected {
  background: var(--dp-primary);
  border-color: var(--dp-primary);
  color: var(--ds-ink-inverse);
  font-weight: 600;
}

/* --- 푸터 영역 --- */
.calendar-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--ds-rule);
  font-size: 12px;
}

.footer-action-btn {
  background: none;
  border: none;
  color: var(--ds-pine-700);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: var(--ds-radius-control);
  transition: background-color var(--ds-ease), color var(--ds-ease);
  font-family: inherit;
}

.footer-action-btn:hover {
  background: var(--dp-surface-hover);
  color: var(--ds-pine-900);
}

.footer-status-text {
  color: var(--dp-text-muted);
  font-size: 11.5px;
}

.footer-status-text .success-text {
  color: var(--ds-pine-700);
  font-weight: 600;
}

/* --- 드롭다운 트랜지션 --- */
.datepicker-dropdown-enter-active,
.datepicker-dropdown-leave-active {
  transition: opacity var(--ds-ease);
}

.datepicker-dropdown-enter-from,
.datepicker-dropdown-leave-to {
  opacity: 0;
}
</style>
