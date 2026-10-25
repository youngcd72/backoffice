/*****************************************************************************************
	Program ID   : useDialog
	Program Type : ts
	Program Name : 다이얼로그 컴포저블
	Description  : Alert(에러) / Confirm(확인) 메시지 창을 Promise 기반으로 띄우는 공통 다이얼로그 상태 관리
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-09-29  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { reactive } from 'vue';

export type DialogKind = 'alert' | 'confirm';
export type DialogIcon = 'default' | 'error' | 'information' | 'question' | 'warning';

export interface DialogOptions {
  title?: string;
  message: string;
  /** 여러 줄 메시지의 첫 줄 앞에 표시할 부가 정보 (예: 에러코드) */
  detail?: string;
  icon?: DialogIcon;
  confirmText?: string;
  cancelText?: string;
}

interface DialogState extends DialogOptions {
  visible: boolean;
  kind: DialogKind;
}

const state = reactive<DialogState>({
  visible: false,
  kind: 'alert',
  message: '',
});

let resolver: ((result: boolean) => void) | null = null;

/**
 * openDialog : 다이얼로그를 열고 사용자의 선택 결과를 Promise 로 반환 (이미 열린 다이얼로그는 취소 처리 후 교체)
 * @param   kind    - 다이얼로그 종류 (alert / confirm)
 * @param   options - 표시 옵션 또는 메시지 문자열
 * @return  확인이면 true, 취소·닫기면 false 로 resolve 되는 Promise
 */
const openDialog = (kind: DialogKind, options: DialogOptions | string): Promise<boolean> => {
  // 이미 열려 있는 다이얼로그는 취소 처리 후 교체
  resolver?.(false);

  const opts = typeof options === 'string' ? { message: options } : options;
  Object.assign(state, {
    title: undefined,
    detail: undefined,
    icon: undefined,
    confirmText: undefined,
    cancelText: undefined,
    ...opts,
    kind,
    visible: true,
  });

  return new Promise<boolean>((resolve) => {
    resolver = resolve;
  });
};

/**
 * closeDialog : 다이얼로그를 닫고 대기 중인 Promise 를 결과값으로 resolve
 * @param   result - 확인이면 true, 취소·닫기면 false
 * @return  N/A
 */
const closeDialog = (result: boolean) => {
  state.visible = false;
  resolver?.(result);
  resolver = null;
};

/**
 * useDialog : Alert(에러) / Confirm(확인) 메시지 창을 Promise 기반으로 띄우는 컴포저블
 * @return  state, alert, confirm, closeDialog 를 담은 객체
 * @example await useDialog().alert('저장되었습니다.')
 */
export function useDialog() {
  return {
    state,
    /** 확인 버튼만 있는 에러 메시지 창. 닫히면 resolve. */
    alert: async (options: DialogOptions | string) => {
      await openDialog('alert', options);
    },
    /** 확인/취소 창. 확인이면 true, 취소·닫기면 false. */
    confirm: (options: DialogOptions | string) => openDialog('confirm', options),
    closeDialog,
  };
}
