/*****************************************************************************************
	Program ID   : rowFlag
	Program Type : ts
	Program Name : 그리드 행 상태 플래그 유틸
	Description  : 그리드 행 상태 플래그(_flag: 등록/수정/삭제) 관리 유틸
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-05  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

/**
 * 그리드 행 상태 플래그 (_flag)
 * - '' : 조회 후 변경 없음
 * - 'I': 등록 (행추가)
 * - 'U': 수정
 * - 'D': 삭제 (저장 시 삭제, 그리드에는 분홍색으로 남음)
 */
export type RowFlag = '' | 'I' | 'U' | 'D'

export interface FlaggedRow {
  _flag: RowFlag
}

/**
 * markRowUpdated : 조회된 행을 수정 상태로 표시 (등록·삭제 행은 그대로 둠)
 * @param   row - 상태 플래그를 가진 행
 * @return  N/A
 */
export function markRowUpdated(row: FlaggedRow): void {
  if (row._flag === '') row._flag = 'U'
}

/**
 * deleteFlaggedRows : 행삭제: 등록(I) 행은 목록에서 바로 제거하고, 나머지는 D로 표시
 * @param   rows    - 전체 행 목록
 * @param   targets - 삭제할 행 목록
 * @return  남은 행 목록
 */
export function deleteFlaggedRows<T extends FlaggedRow>(rows: T[], targets: T[]): T[] {
  const targetSet = new Set(targets)
  targetSet.forEach((row) => {
    if (row._flag !== 'I') row._flag = 'D'
  })
  return rows.filter((row) => !(targetSet.has(row) && row._flag === 'I'))
}

/**
 * hasRowChanges : 변경된 행(등록/수정/삭제)이 하나라도 있는지 판별
 * @param   rows - 행 목록
 * @return  변경 행이 있으면 true
 */
export function hasRowChanges(rows: FlaggedRow[]): boolean {
  return rows.some((row) => row._flag !== '')
}

/**
 * isDeletedRow : 삭제 표시된 행인지 판별
 * @param   row - 상태 플래그를 가진 행
 * @return  삭제(D) 상태이면 true
 */
export function isDeletedRow(row: FlaggedRow): boolean {
  return row._flag === 'D'
}

/** 그리드 행 고유 ID 채번용 순번 (화면 전체에서 겹치지 않음) */
let rowSeq = 0

/**
 * nextRowId : 그리드 행 고유 ID 채번
 * @return  증가된 행 ID
 */
export function nextRowId(): number {
  rowSeq += 1
  return rowSeq
}

/**
 * renumber : 그리드 행의 NO 순번 재부여 (1부터)
 * @param   rows - NO를 부여할 행 목록
 * @return  N/A
 */
export function renumber(rows: Array<{ no?: number }>): void {
  rows.forEach((row, idx) => {
    row.no = idx + 1
  })
}

/**
 * splitRowsByFlag : 행 목록을 등록(I)/수정(U)/삭제(D) 저장 목록으로 나눔
 * @param   rows   - 전체 행 목록
 * @param   mapper - 행을 저장용 데이터로 변환하는 함수
 * @return  { insertList, updateList, deleteList }
 * @example saveMenuListApi(splitRowsByFlag(allRows.value, toMenu))
 */
export function splitRowsByFlag<T extends FlaggedRow, R>(rows: T[], mapper: (row: T) => R) {
  const toList = (flag: RowFlag) => rows.filter((row) => row._flag === flag).map(mapper)
  return { insertList: toList('I'), updateList: toList('U'), deleteList: toList('D') }
}
