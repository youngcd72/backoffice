/*****************************************************************************************
	Program ID   : useToast
	Program Type : ts
	Program Name : 토스트 컴포저블
	Description  : 화면 흐름을 막지 않고 잠시 표시되는 알림(토스트) 메시지의 전역 상태 관리
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { reactive } from 'vue';

export type ToastType = 'success' | 'info' | 'warning' | 'error';

export interface ToastOptions {
  message: string;
  title?: string;
  /** 표시 시간(ms). 0 이면 사용자가 닫을 때까지 유지 */
  duration?: number;
}

export interface ToastItem extends ToastOptions {
  id: number;
  type: ToastType;
}

/** 동시에 표시할 최대 토스트 수 (초과 시 가장 오래된 것부터 제거) */
const MAX_TOASTS = 5;

/** 유형별 기본 표시 시간(ms) — 오류는 읽을 시간을 더 준다 */
const DEFAULT_DURATION: Record<ToastType, number> = {
  success: 3000,
  info: 3000,
  warning: 4000,
  error: 5000,
};

const toasts = reactive<ToastItem[]>([]);
const timers = new Map<number, ReturnType<typeof setTimeout>>();
let seq = 0;

/**
 * removeToast : 지정한 토스트를 목록에서 제거하고 자동 닫힘 타이머를 해제
 * @param   id - 제거할 토스트 ID
 * @return  N/A
 */
const removeToast = (id: number) => {
  const timer = timers.get(id);
  if (timer) {
    clearTimeout(timer);
    timers.delete(id);
  }
  const index = toasts.findIndex((toast) => toast.id === id);
  if (index > -1) toasts.splice(index, 1);
};

/**
 * showToast : 토스트를 목록에 추가하고 표시 시간이 지나면 자동으로 제거
 * @param   type    - 토스트 유형 (success / info / warning / error)
 * @param   options - 표시 옵션 또는 메시지 문자열
 * @return  추가된 토스트 ID (removeToast 로 직접 닫을 때 사용)
 */
const showToast = (type: ToastType, options: ToastOptions | string): number => {
  const opts = typeof options === 'string' ? { message: options } : options;
  const id = ++seq;
  const duration = opts.duration ?? DEFAULT_DURATION[type];

  toasts.push({ ...opts, id, type, duration });
  while (toasts.length > MAX_TOASTS) removeToast(toasts[0].id);

  if (duration > 0) timers.set(id, setTimeout(() => removeToast(id), duration));
  return id;
};

/**
 * useToast : 화면 흐름을 막지 않는 알림(토스트)을 띄우는 컴포저블
 * @return  toasts, success, info, warning, error, removeToast 를 담은 객체
 * @example useToast().success('저장되었습니다.')
 */
export function useToast() {
  return {
    toasts,
    /** 성공 알림 (저장·삭제 완료 등) */
    success: (options: ToastOptions | string) => showToast('success', options),
    /** 일반 안내 */
    info: (options: ToastOptions | string) => showToast('info', options),
    /** 주의 안내 (처리는 되었으나 확인이 필요한 경우) */
    warning: (options: ToastOptions | string) => showToast('warning', options),
    /** 오류 알림 (사용자 확인이 반드시 필요하면 useDialog().alert 사용) */
    error: (options: ToastOptions | string) => showToast('error', options),
    removeToast,
  };
}
