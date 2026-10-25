/*****************************************************************************************
	Program ID   : commonCode
	Program Type : ts
	Program Name : 공통코드 조회 API
	Description  : 화면 콤보 등에 쓰는 공통코드 상세 목록 조회 (TB_COMM_CD_M + TB_COMM_CD_D)
	               - 조회: GET /api/v1/common-codes/{wrkMdulCd}/{commCtgCd}/details?useYn=Y (사용 중인 코드, 정렬순번 순)
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-17  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import apiClient, { type ApiResponse } from '@/api/client'
import type { CodeOption } from '@/utils/code'

/** 공통코드 상세 (TB_COMM_CD_D) */
export interface CommCdDtlItem {
  commCtgDtlCd: string
  commCtgDtlNm: string
  alinSeq: number | null
}

/**
 * getCommCdDtlItemListApi : 공통분류의 사용 중인 상세코드 목록 조회 (GET /api/v1/common-codes/{wrkMdulCd}/{commCtgCd}/details?useYn=Y)
 * @param   wrkMdulCd - 업무모듈코드 (예: SY)
 * @param   commCtgCd - 공통분류코드 (예: PRGM_CRS_CD)
 * @return  상세코드 목록을 담은 API 응답
 */
export async function getCommCdDtlItemListApi(wrkMdulCd: string, commCtgCd: string): Promise<ApiResponse<CommCdDtlItem[]>> {
  const response = await apiClient.get<ApiResponse<CommCdDtlItem[]>>(
    `/api/v1/common-codes/${encodeURIComponent(wrkMdulCd)}/${encodeURIComponent(commCtgCd)}/details`,
    { params: { useYn: 'Y' } },
  )
  return response.data
}

/**
 * getCommCdOptions : 공통분류의 상세코드를 콤보 옵션(label: 코드명, value: 코드)으로 조회
 * @param   wrkMdulCd - 업무모듈코드
 * @param   commCtgCd - 공통분류코드
 * @return  콤보 옵션 목록
 * @example getCommCdOptions('SY', 'PRGM_CRS_CD')
 */
export async function getCommCdOptions(wrkMdulCd: string, commCtgCd: string): Promise<CodeOption[]> {
  const res = await getCommCdDtlItemListApi(wrkMdulCd, commCtgCd)
  return (res.data ?? []).map((item) => ({ label: item.commCtgDtlNm, value: item.commCtgDtlCd }))
}
