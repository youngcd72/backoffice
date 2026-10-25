/*****************************************************************************************
	Program ID   : message
	Program Type : ts
	Program Name : 다국어 메시지 유틸
	Description  : coder-backend 에서 내려받은 메시지를 보관하고 키로 문구를 조회
	               - 메시지 원본: coder-backend/src/main/resources/messages/messages_{언어}.properties
	               - 앱 시작 시 main.ts 에서 loadMessages() 로 한 번 내려받는다
	               - 인자 치환은 Java MessageFormat 과 같은 {0}, {1} 형식
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { reactive, ref } from 'vue'
import { getMessageListApi } from '@/api/common/message'

const LANG_STORAGE_KEY = 'coder_lang'
const DEFAULT_LANG = 'ko'

const messages = reactive<Record<string, string>>({})
const currentLang = ref(DEFAULT_LANG)

/**
 * getSavedLang : 브라우저에 저장된 언어 코드를 조회 (없거나 저장소를 쓸 수 없으면 기본 언어)
 * @return  언어 코드 (ko, en)
 */
function getSavedLang(): string {
  try {
    return localStorage.getItem(LANG_STORAGE_KEY) || DEFAULT_LANG
  } catch {
    return DEFAULT_LANG
  }
}

/**
 * getLang : 현재 적용된 언어 코드를 조회 (API 요청의 Accept-Language 에 사용)
 * @return  언어 코드 (ko, en)
 */
export function getLang(): string {
  return currentLang.value
}

/**
 * loadMessages : coder-backend 에서 메시지 전체를 내려받아 교체
 *                실패해도 화면은 뜨도록 예외를 던지지 않고, 이 경우 msg() 는 키를 그대로 반환한다.
 * @param   lang - 언어 코드 (생략 시 저장된 언어 또는 ko)
 * @return  N/A
 * @example await loadMessages('en')
 */
export async function loadMessages(lang: string = getSavedLang()): Promise<void> {
  try {
    const loaded = await getMessageListApi(lang)
    Object.keys(messages).forEach((key) => delete messages[key])
    Object.assign(messages, loaded)
    currentLang.value = lang
    try {
      localStorage.setItem(LANG_STORAGE_KEY, lang)
    } catch {
      // 저장소를 쓸 수 없는 환경이면 이번 세션에만 적용
    }
  } catch (error) {
    console.warn('[CODER] 메시지를 불러오지 못했습니다.', error)
  }
}

/**
 * msg : 메시지 키로 현재 언어의 문구를 조회하고 {0}, {1} 자리에 인자를 치환
 *       키가 없으면 키 자체를 반환한다 (backend MessageSource 의 useCodeAsDefaultMessage 와 동일).
 * @param   key  - 메시지 키 (예: cm.info.saved)
 * @param   args - {0}, {1} ... 에 들어갈 값
 * @return  치환된 메시지 문구
 * @example msg('cm.info.no.data') / msg('sy.batch.not.found', dsptId)
 */
export function msg(key: string, ...args: unknown[]): string {
  const template = messages[key] ?? key
  if (args.length === 0) return template
  return template.replace(/\{(\d+)\}/g, (match, index) => {
    const value = args[Number(index)]
    return value === undefined || value === null ? match : String(value)
  })
}
