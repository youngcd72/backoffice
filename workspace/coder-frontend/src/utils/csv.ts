/*****************************************************************************************
	Program ID   : csv
	Program Type : ts
	Program Name : CSV 유틸
	Description  : CSV 값 이스케이프 및 엑셀에서 열 수 있는 CSV(UTF-8 BOM) 다운로드 유틸
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-05  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

/**
 * escapeCsvValue : CSV 값에 쉼표·따옴표·줄바꿈이 있으면 따옴표로 감싸 이스케이프
 * @param   value - CSV 셀 값
 * @return  이스케이프된 값
 */
function escapeCsvValue(value: string): string {
  return /[",\r\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value
}

/**
 * downloadCsv : 엑셀에서 바로 열 수 있는 CSV(UTF-8 BOM) 파일을 내려받음
 * @param   fileName - 저장할 파일명
 * @param   header   - 헤더 컬럼명 목록
 * @param   rows     - 데이터 행 목록
 * @return  N/A
 * @example downloadCsv('user_' + formatToday() + '.csv', ['ID', '이름'], [['u1', '홍길동']])
 */
export function downloadCsv(fileName: string, header: string[], rows: string[][]): void {
  const csv = [header, ...rows].map((cols) => cols.map(escapeCsvValue).join(',')).join('\r\n')
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = fileName
  link.click()
  URL.revokeObjectURL(link.href)
}
