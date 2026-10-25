/*****************************************************************************************
	Program ID   : code
	Program Type : ts
	Program Name : 코드 옵션 유틸
	Description  : 콤보(Select)용 코드 옵션 타입과 공통 옵션(사용여부), 코드명 조회 유틸
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-20  ywkim     최초생성 (화면별 코드 옵션·함수 통합)
------------------------------------------------------------------------------------------
*****************************************************************************************/

/** 콤보 옵션 (라벨, 코드값) */
export interface CodeOption {
  label: string
  value: string
}

/** 사용여부 옵션 (Y/N 표시) */
export const YN_OPTIONS: CodeOption[] = [
  { label: 'Y', value: 'Y' },
  { label: 'N', value: 'N' },
]

/** 사용여부 옵션 (사용/미사용 표시) */
export const USE_YN_OPTIONS: CodeOption[] = [
  { label: '사용', value: 'Y' },
  { label: '미사용', value: 'N' },
]

/**
 * codeName : 옵션 목록에서 코드값에 해당하는 명칭 조회
 * @param   options - 코드 옵션 목록
 * @param   value   - 코드값
 * @return  코드명 (없으면 코드값 그대로, 값이 없으면 빈 문자열)
 */
export function codeName(options: CodeOption[], value: string | null | undefined): string {
  if (!value) return ''
  return options.find((option) => option.value === value)?.label ?? value
}

/**
 * withAll : 옵션 목록 맨 앞에 전체(빈 값) 항목을 추가
 * @param   options - 코드 옵션 목록
 * @return  전체 항목이 추가된 옵션 목록
 */
export function withAll(options: CodeOption[]): CodeOption[] {
  return [{ label: '전체', value: '' }, ...options]
}
