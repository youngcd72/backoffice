/*****************************************************************************************
	Program ID   : format
	Program Type : ts
	Program Name : 표시 포맷 유틸
	Description  : 숫자 등 화면 표시용 포맷 유틸
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-20  ywkim     최초생성 (화면별 formatCount 통합)
------------------------------------------------------------------------------------------
*****************************************************************************************/

/**
 * formatCount : 건수를 천 단위 구분 문자열로 변환
 * @param   value - 건수
 * @return  포맷된 문자열 (값이 없으면 빈 문자열)
 */
export function formatCount(value: number | null | undefined): string {
  return value === null || value === undefined ? '' : value.toLocaleString()
}
