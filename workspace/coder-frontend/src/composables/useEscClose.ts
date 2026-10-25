/*****************************************************************************************
	Program ID   : useEscClose
	Program Type : ts
	Program Name : ESC 닫기 컴포저블
	Description  : 열린 팝업/다이얼로그를 스택으로 관리하고, ESC 입력 시 가장 위에 열린 것만 닫는다
	               (팝업 위에 alert 가 떠 있을 때 ESC 한 번에 둘 다 닫히지 않도록)
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-20  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { onBeforeUnmount, watch, type WatchSource } from 'vue';

type EscHandler = () => void;

/** 열린 순서대로 쌓인 ESC 처리 함수 (마지막 요소가 가장 위) */
const escStack: EscHandler[] = [];

/**
 * onWindowKeydown : ESC 입력 시 스택 맨 위의 처리 함수만 실행
 * @param   e - 키보드 이벤트
 * @return  N/A
 */
function onWindowKeydown(e: KeyboardEvent): void {
  if (e.key !== 'Escape' || escStack.length === 0) return;
  e.stopPropagation();
  escStack[escStack.length - 1]();
}

/**
 * removeHandler : 스택에서 처리 함수를 제거하고, 비면 window 리스너도 해제
 * @param   handler - 제거할 처리 함수
 * @return  N/A
 */
function removeHandler(handler: EscHandler): void {
  const idx = escStack.lastIndexOf(handler);
  if (idx < 0) return;
  escStack.splice(idx, 1);
  if (escStack.length === 0) window.removeEventListener('keydown', onWindowKeydown);
}

/**
 * useEscClose : 열려 있는 동안 ESC 스택에 등록하고, 가장 위일 때만 ESC 처리 함수를 실행
 * @param   isOpen - 열림 여부
 * @param   onEsc  - ESC 입력 시 실행할 함수 (닫지 않으려면 아무 것도 하지 않는 함수를 넘긴다)
 * @return  N/A
 * @example useEscClose(() => props.modelValue, () => { if (props.closeOnEsc) close() })
 */
export function useEscClose(isOpen: WatchSource<boolean>, onEsc: EscHandler): void {
  const handler: EscHandler = () => onEsc();

  watch(
    isOpen,
    (open) => {
      if (open) {
        if (escStack.length === 0) window.addEventListener('keydown', onWindowKeydown);
        escStack.push(handler);
      } else {
        removeHandler(handler);
      }
    },
    { immediate: true },
  );

  onBeforeUnmount(() => removeHandler(handler));
}
