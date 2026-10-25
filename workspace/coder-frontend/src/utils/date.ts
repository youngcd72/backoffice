/*****************************************************************************************
	Program ID   : date
	Program Type : ts
	Program Name : 날짜 유틸
	Description  : 일자/일시 포맷 및 형식 변환 유틸
	               - 서버·DB 일자 형식: YYYYMMDD
	               - DatePicker 일자 형식: YYYY-MM-DD
	               - 화면 표시 일시 형식: YYYY-MM-DD HH:mm:ss
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-20  ywkim     최초생성 (utils/csv.ts, 화면별 날짜 함수 통합)
------------------------------------------------------------------------------------------
*****************************************************************************************/

const DAY_MS = 86_400_000

/**
 * pad2 : 숫자를 2자리 문자열로 변환 (앞을 0으로 채움)
 * @param   n - 변환할 숫자
 * @return  2자리 문자열
 */
export function pad2(n: number): string {
  return String(n).padStart(2, '0')
}

/**
 * formatDate : 일시를 패턴 문자열로 변환 (YYYY, MM, DD, HH, mm, ss 치환)
 * @param   value   - Date 또는 밀리초 시각 (기본: 현재)
 * @param   pattern - 출력 패턴 (기본: YYYY-MM-DD)
 * @return  패턴에 맞춘 문자열
 * @example formatDate(Date.now(), 'MM-DD HH:mm')
 */
export function formatDate(value: Date | number = Date.now(), pattern = 'YYYY-MM-DD'): string {
  const d = value instanceof Date ? value : new Date(value)
  const tokens: Record<string, string> = {
    YYYY: String(d.getFullYear()),
    MM: pad2(d.getMonth() + 1),
    DD: pad2(d.getDate()),
    HH: pad2(d.getHours()),
    mm: pad2(d.getMinutes()),
    ss: pad2(d.getSeconds()),
  }
  return pattern.replace(/YYYY|MM|DD|HH|mm|ss/g, (token) => tokens[token])
}

/**
 * formatYmd : 일시를 YYYYMMDD 형식 문자열로 변환
 * @param   value - Date 또는 밀리초 시각 (기본: 현재)
 * @return  YYYYMMDD 형식 문자열
 */
export function formatYmd(value: Date | number = Date.now()): string {
  return formatDate(value, 'YYYYMMDD')
}

/**
 * formatDttm : 일시를 YYYY-MM-DD HH:mm:ss 형식 문자열로 변환
 * @param   value - Date 또는 밀리초 시각 (기본: 현재)
 * @return  YYYY-MM-DD HH:mm:ss 형식 문자열
 */
export function formatDttm(value: Date | number = Date.now()): string {
  return formatDate(value, 'YYYY-MM-DD HH:mm:ss')
}

/**
 * todayYmd : 오늘 일자를 YYYYMMDD 형식으로 반환
 * @return  YYYYMMDD 형식 오늘 일자
 */
export function todayYmd(): string {
  return formatYmd()
}

/**
 * todayDash : 오늘 기준 YYYY-MM-DD 문자열 반환 (DatePicker 형식)
 * @param   offsetDays - 오늘로부터의 가감 일수 (기본 0)
 * @return  YYYY-MM-DD 형식 문자열
 * @example todayDash(-7)
 */
export function todayDash(offsetDays = 0): string {
  const d = new Date()
  d.setDate(d.getDate() + offsetDays)
  return formatDate(d)
}

/**
 * dashToYmd : YYYY-MM-DD 형식(DatePicker)을 YYYYMMDD 로 변환
 * @param   value - YYYY-MM-DD 형식 문자열
 * @return  YYYYMMDD 형식 문자열 (값이 없으면 빈 문자열)
 */
export function dashToYmd(value: string | null | undefined): string {
  return value ? value.replace(/-/g, '') : ''
}

/**
 * ymdToDash : YYYYMMDD 형식을 YYYY-MM-DD(DatePicker) 로 변환
 * @param   ymd - YYYYMMDD 형식 문자열
 * @return  YYYY-MM-DD 형식 문자열 (8자리 숫자가 아니면 입력값 그대로, 값이 없으면 빈 문자열)
 */
export function ymdToDash(ymd: string | null | undefined): string {
  if (!ymd) return ''
  return /^\d{8}$/.test(ymd) ? `${ymd.slice(0, 4)}-${ymd.slice(4, 6)}-${ymd.slice(6, 8)}` : ymd
}

/**
 * daysUntil : 일시 문자열 기준 남은 일수 계산 (음수면 지남)
 * @param   dttm - YYYY-MM-DD HH:mm:ss 형식 일시
 * @return  현재부터 해당 일시까지 남은 일수
 */
export function daysUntil(dttm: string): number {
  return Math.floor((new Date(dttm.replace(' ', 'T')).getTime() - Date.now()) / DAY_MS)
}
