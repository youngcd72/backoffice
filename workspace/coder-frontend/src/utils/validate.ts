/*****************************************************************************************
	Program ID   : validate
	Program Type : ts
	Program Name : 입력값 검증 유틸
	Description  : 입력값 검증 유틸
	               
	               1) 컴포넌트 validate 속성 검증 — validateForm(영역)
	                  공통 컴포넌트(Input, Select, DatePicker, BaseSearchCode, ComSearchCode)에 validate 속성으로 규칙을 지정한다.
	                  (required 속성은 validate="required" 의 줄임말)
	               
	                    <Input v-model="form.email"  validate="required|email" field-name="이메일" />
	                    <Input v-model="form.juminNo" :validate="['juminno']"   field-name="주민등록번호" />
	               
	                  컴포넌트는 루트 요소에 아래 속성만 표시하고, 실제 검사는 이 파일에서 한다.
	                    data-validate="required email"   적용할 규칙
	                    data-empty="true"                현재 값이 비어 있음
	                    data-field-name="…"              메시지에 쓸 항목명 (field-name > label > placeholder 순)
	               
	                  if (!(await validateForm(frmSrchRef.value))) return
	               
	               2) 값 단위 검증 — validateFields([{ label, value, rules, maxBytes, focus }])
	                  그리드 셀처럼 컴포넌트로 표시되지 않는 값이나, 길이까지 함께 검사할 때 사용한다.
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-05  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { msg, getLang } from '@/utils/message'
import { useDialog } from '@/composables/useDialog'

export const VALIDATE_ATTR = 'data-validate'
export const EMPTY_ATTR = 'data-empty'
export const FIELD_NAME_ATTR = 'data-field-name'

// ---------------------------------------------------------------------
// 공통 함수
// ---------------------------------------------------------------------
/**
 * isEmptyValue : null·undefined·빈 문자열(공백만 포함)·빈 배열을 빈 값으로 판별
 * @param   value - 검사할 값
 * @return  빈 값이면 true
 */
export function isEmptyValue(value: unknown): boolean {
  if (value === null || value === undefined) return true
  if (typeof value === 'string') return value.trim() === ''
  if (Array.isArray(value)) return value.length === 0 || value.every(isEmptyValue)
  return false
}

/**
 * getByteLength : UTF-8 바이트 수 계산 (DB AL32UTF8 기준: 한글 3byte)
 * @param   value - 문자열
 * @return  바이트 수
 */
export function getByteLength(value: string): number {
  return new TextEncoder().encode(value).length
}

/**
 * withJosa : 한국어 조사 선택: 받침 유무에 따라 은/는, 이/가, 을/를 중 하나를 단어에 붙임
 * @param   word - 대상 단어
 * @param   josa - 조사 쌍 (은는 / 이가 / 을를)
 * @return  조사가 붙은 단어
 * @example withJosa('이메일', '은는')
 */
export function withJosa(word: string, josa: '은는' | '이가' | '을를'): string {
  const code = word.trim().slice(-1).charCodeAt(0) - 0xac00
  if (code < 0 || code > 11171) return `${word}${josa[0]}(${josa[1]})`
  return `${word}${code % 28 !== 0 ? josa[0] : josa[1]}`
}

/**
 * withLangJosa : 한국어일 때만 조사를 붙이고, 그 외 언어는 단어를 그대로 반환 (메시지 인자용)
 * @param   word - 대상 단어
 * @param   josa - 조사 쌍 (은는 / 이가 / 을를)
 * @return  조사가 붙은 단어 (한국어 외에는 원래 단어)
 */
function withLangJosa(word: string, josa: '은는' | '이가' | '을를'): string {
  return getLang() === 'ko' ? withJosa(word, josa) : word
}

/**
 * isValidYmd : YYYYMMDD (또는 YYYY-MM-DD) 가 실제 존재하는 날짜인지 판별
 * @param   value - 날짜 문자열
 * @return  유효한 날짜이면 true
 */
export function isValidYmd(value: string): boolean {
  const ymd = value.replace(/-/g, '')
  if (!/^\d{8}$/.test(ymd)) return false
  const y = Number(ymd.slice(0, 4))
  const m = Number(ymd.slice(4, 6))
  const d = Number(ymd.slice(6, 8))
  const date = new Date(y, m - 1, d)
  return date.getFullYear() === y && date.getMonth() === m - 1 && date.getDate() === d
}

/**
 * isValidJuminNo : 주민등록번호(외국인등록번호 포함) 형식 검사: 13자리(하이픈 허용), 생년월일, 성별·세기 자리 (2020.10 이후 발급분은 뒷자리가 임의 번호라 검증번호는 검사하지 않음)
 * @param   value - 주민등록번호 문자열
 * @return  형식이 올바르면 true
 */
export function isValidJuminNo(value: string): boolean {
  const digits = value.replace(/-/g, '')
  if (!/^\d{13}$/.test(digits) || !/^\d{6}-?\d{7}$/.test(value.trim())) return false
  const genderDigit = Number(digits[6])
  const century = { 1: 1900, 2: 1900, 5: 1900, 6: 1900, 3: 2000, 4: 2000, 7: 2000, 8: 2000, 9: 1800, 0: 1800 }[genderDigit]
  if (century === undefined) return false
  return isValidYmd(`${century + Number(digits.slice(0, 2))}${digits.slice(2, 6)}`)
}

// ---------------------------------------------------------------------
// 검증 규칙
// ---------------------------------------------------------------------
export interface ValidateRuleDef {
  /** 값이 있을 때 형식 검사 (required 는 빈 값 여부로 따로 검사) */
  test?: (value: string) => boolean
  message: (label: string) => string
}

export const VALIDATE_RULES: Record<string, ValidateRuleDef> = {
  required: { message: (label) => msg('cm.validate.required', withLangJosa(label || msg('cm.validate.label.item'), '은는')) },
  email: {
    test: (v) => /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(v.trim()),
    message: (label) => msg('cm.validate.email.format', label || msg('cm.validate.label.email')),
  },
  juminno: {
    test: isValidJuminNo,
    message: (label) => msg('cm.validate.juminno.format', label || msg('cm.validate.label.juminno')),
  },
  number: { test: (v) => /^-?\d+(\.\d+)?$/.test(v.trim()), message: (label) => msg('cm.validate.number', withLangJosa(label, '은는')) },
  integer: { test: (v) => /^-?\d+$/.test(v.trim()), message: (label) => msg('cm.validate.integer', withLangJosa(label, '은는')) },
  alnum: { test: (v) => /^[A-Za-z0-9_]+$/.test(v.trim()), message: (label) => msg('cm.validate.alnum', withLangJosa(label, '은는')) },
  date: { test: isValidYmd, message: (label) => msg('cm.validate.date', withLangJosa(label, '은는')) },
}

export type ValidateRuleName = keyof typeof VALIDATE_RULES

/**
 * normalizeValidateRules : 컴포넌트의 validate·required 속성을 규칙 목록으로 정리 (validate: "required|email" 또는 ['required', 'email'], 공백·쉼표 구분도 허용)
 * @param   validate - 검증 규칙 문자열 또는 배열
 * @param   required - 필수 여부 (true 이면 required 규칙을 맨 앞에 추가)
 * @return  중복 제거된 유효 규칙명 목록
 */
export function normalizeValidateRules(validate?: string | string[] | null, required = false): string[] {
  const list = Array.isArray(validate) ? validate : (validate ?? '').split(/[|,\s]+/)
  const rules = list.map((rule) => rule.trim().toLowerCase()).filter(Boolean)
  if (required) rules.unshift('required')
  const unknown = rules.filter((rule) => !VALIDATE_RULES[rule])
  if (unknown.length > 0) console.warn(`[validate] 알 수 없는 검증 규칙: ${unknown.join(', ')}`)
  return [...new Set(rules.filter((rule) => VALIDATE_RULES[rule]))]
}

/**
 * checkRules : 값 하나를 규칙 목록으로 검사
 * @param   label - 메시지에 쓸 항목명
 * @param   value - 검사할 값
 * @param   rules - 적용할 규칙명 목록
 * @return  오류 메시지 (문제 없으면 빈 문자열)
 */
export function checkRules(label: string, value: unknown, rules: string[]): string {
  if (isEmptyValue(value)) return rules.includes('required') ? VALIDATE_RULES.required.message(label) : ''
  const text = Array.isArray(value) ? value.join('') : String(value)
  const failed = rules.find((rule) => VALIDATE_RULES[rule]?.test && !VALIDATE_RULES[rule].test!(text))
  return failed ? VALIDATE_RULES[failed].message(label) : ''
}

// ---------------------------------------------------------------------
// 1) 컴포넌트 validate 속성 검증 (DOM 표시 기반)
// ---------------------------------------------------------------------
export interface FormFieldError {
  label: string
  message: string
  el: HTMLElement
}

/**
 * isVisible : 요소가 화면에 보이는지 판별
 * @param   el - 검사할 요소
 * @return  보이면 true
 */
function isVisible(el: HTMLElement): boolean {
  return el.offsetParent !== null || el.getClientRects().length > 0
}

/**
 * readFieldText : 형식 검사에 쓸 화면 값 조회: 컴포넌트 안의 첫 텍스트 입력칸 값
 * @param   el - 컴포넌트 루트 요소
 * @return  입력칸 값 (없으면 빈 문자열)
 */
function readFieldText(el: HTMLElement): string {
  const input = el.querySelector<HTMLInputElement>('input:not([type="hidden"]):not([type="checkbox"]):not([type="radio"]), textarea')
  return input?.value ?? ''
}

/**
 * findFocusTarget : 요소 안에서 포커스를 받을 첫 입력 요소 탐색
 * @param   el - 탐색 대상 요소
 * @return  포커스 대상 요소 (없으면 null)
 */
function findFocusTarget(el: HTMLElement): HTMLElement | null {
  if (el.matches('input, select, textarea, [tabindex]:not([tabindex="-1"])')) return el
  return el.querySelector<HTMLElement>(
    'input:not([readonly]):not([disabled]):not([type="hidden"]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
  )
}

/**
 * findFormErrors : 영역 안에서 검증 규칙을 통과하지 못한 항목 조회 (화면에 보이고 비활성이 아닌 것만)
 * @param   root - 검사할 영역 요소
 * @return  검증 오류 목록
 */
export function findFormErrors(root: HTMLElement | null | undefined): FormFieldError[] {
  if (!root) return []
  return Array.from(root.querySelectorAll<HTMLElement>(`[${VALIDATE_ATTR}]`))
    .filter((el) => isVisible(el) && !el.matches('.is-disabled'))
    .flatMap((el) => {
      const rules = (el.getAttribute(VALIDATE_ATTR) ?? '').split(/\s+/).filter(Boolean)
      const label = el.getAttribute(FIELD_NAME_ATTR) ?? ''
      const empty = el.getAttribute(EMPTY_ATTR) === 'true'
      // 값이 있지만 텍스트 입력칸이 없는 컴포넌트(Select 등)는 형식 검사를 건너뛴다
      const text = empty ? '' : readFieldText(el)
      const message = empty || text ? checkRules(label, text, rules) : ''
      return message ? [{ label, message, el }] : []
    })
}

/**
 * focusField : 요소를 화면에 보이게 스크롤하고 입력 요소에 포커스 이동
 * @param   el - 포커스할 요소
 * @return  N/A
 */
export function focusField(el: HTMLElement): void {
  el.scrollIntoView({ block: 'nearest' })
  findFocusTarget(el)?.focus()
}

/**
 * validateForm : 영역 안 컴포넌트들의 validate 규칙 검사: 통과하지 못한 첫 항목을 알리고 그 항목으로 포커스 이동
 * @param   root - 검사할 영역 요소
 * @return  모두 통과하면 true
 * @example if (!(await validateForm(frmSrchRef.value))) return
 */
export async function validateForm(root: HTMLElement | null | undefined): Promise<boolean> {
  const [first] = findFormErrors(root)
  if (!first) return true
  await useDialog().alert({ message: first.message, icon: 'warning' })
  focusField(first.el)
  return false
}

/** @deprecated validateForm 사용 (required 외 규칙도 함께 검사) */
export const validateRequired = validateForm

// ---------------------------------------------------------------------
// 2) 값 단위 검증
// ---------------------------------------------------------------------
export interface FieldRule {
  /** 메시지에 쓸 항목명 */
  label: string
  value: unknown
  /** 검증 규칙 ("required|email" 또는 배열) */
  rules?: string | string[]
  required?: boolean
  /** 최대 글자 수 */
  maxLength?: number
  /** 최대 바이트 수 (UTF-8, DB 컬럼 길이 검사용) */
  maxBytes?: number
  /** 추가 검사: 오류 메시지를 반환하면 실패 */
  custom?: (value: unknown) => string | undefined
  /** 오류 시 포커스 이동 */
  focus?: () => void
}

/**
 * checkField : 값 단위 규칙 1건 검사 (규칙, 최대 글자 수, 최대 바이트 수, 사용자 정의 검사 순)
 * @param   rule - 검사 규칙
 * @return  오류 메시지 (문제 없으면 빈 문자열)
 */
export function checkField(rule: FieldRule): string {
  const message = checkRules(rule.label, rule.value, normalizeValidateRules(rule.rules, rule.required))
  if (message || isEmptyValue(rule.value)) return message
  const text = String(rule.value)
  if (rule.maxLength !== undefined && text.length > rule.maxLength) {
    return msg('cm.validate.length.exceeded', withLangJosa(rule.label, '은는'), rule.maxLength)
  }
  if (rule.maxBytes !== undefined && getByteLength(text) > rule.maxBytes) {
    return msg('cm.validate.bytes.exceeded', withLangJosa(rule.label, '은는'), rule.maxBytes)
  }
  return rule.custom?.(rule.value) ?? ''
}

/**
 * validateFields : 규칙 목록을 순서대로 검사해 첫 오류를 알리고 포커스 이동
 * @param   rules - 검사 규칙 목록
 * @return  모두 통과하면 true
 */
export async function validateFields(rules: FieldRule[]): Promise<boolean> {
  for (const rule of rules) {
    const message = checkField(rule)
    if (message) {
      await useDialog().alert({ message, icon: 'warning' })
      rule.focus?.()
      return false
    }
  }
  return true
}
