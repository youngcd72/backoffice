/*****************************************************************************************
	Program ID   : useGridSave
	Program Type : ts
	Program Name : 그리드 저장 흐름 컴포저블
	Description  : 행 상태(I/U/D) 그리드 화면의 공통 저장·조회 흐름
	               - 저장: 변경 없음 안내 → 검증 → 저장 확인 → API 호출 → 실패 시 서버 메시지 안내 → 저장 완료 안내 → 후처리(재조회)
	               - 조회: 저장하지 않은 변경이 있으면 버리고 조회할지 확인
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-20  ywkim     최초생성 (그리드 화면별 저장 흐름 통합)
------------------------------------------------------------------------------------------
*****************************************************************************************/

import type { ApiResponse } from '@/api/client'
import type { DialogIcon } from '@/composables/useDialog'
import { useDialog } from '@/composables/useDialog'
import { useToast } from '@/composables/useToast'
import { msg } from '@/utils/message'

export interface GridSaveOptions {
  /** 저장할 변경이 있는지 (false 면 "변경 없음" 안내) */
  hasChanges: boolean
  /** 저장 전 검증: 오류 메시지 반환 (문제 없으면 빈 문자열) */
  validate?: () => string | Promise<string>
  /** 저장 확인 문구 (기본: cm.confirm.save) */
  confirmMessage?: string
  /** 저장 확인 아이콘 (기본: question) */
  confirmIcon?: DialogIcon
  /** 저장 API 호출 */
  save: () => Promise<ApiResponse<unknown>>
  /** 저장 성공 후 처리 (재조회 등) */
  afterSave?: () => unknown
  /** 저장 완료 안내 방식 (기본: alert) */
  savedNotice?: 'alert' | 'toast'
}

/**
 * useGridSave : 그리드 화면의 공통 저장·조회 확인 흐름 제공
 * @return  saveChanges, confirmDiscardChanges
 * @example const { saveChanges, confirmDiscardChanges } = useGridSave()
 */
export function useGridSave() {
  const { alert, confirm } = useDialog()
  const toast = useToast()

  /**
   * confirmDiscardChanges : 저장하지 않은 변경이 있으면 버리고 조회할지 확인
   * @param   hasChanges - 저장하지 않은 변경이 있는지
   * @return  계속 진행하면 true
   */
  async function confirmDiscardChanges(hasChanges: boolean): Promise<boolean> {
    if (!hasChanges) return true
    return confirm({ message: msg('cm.confirm.discard.and.search') })
  }

  /**
   * saveChanges : 변경 확인 → 검증 → 저장 확인 → 저장 → 결과 안내 → 후처리
   * @param   options - 저장 흐름 옵션
   * @return  저장에 성공하면 true
   */
  async function saveChanges(options: GridSaveOptions): Promise<boolean> {
    if (!options.hasChanges) {
      await alert({ message: msg('cm.info.no.changes'), icon: 'information' })
      return false
    }
    const errorMessage = options.validate ? await options.validate() : ''
    if (errorMessage) {
      await alert({ message: errorMessage, icon: 'warning' })
      return false
    }
    const ok = await confirm({ message: options.confirmMessage ?? msg('cm.confirm.save'), icon: options.confirmIcon ?? 'question' })
    if (!ok) return false

    const res = await options.save()
    if (!res.success) {
      await alert({ message: res.message ?? msg('cm.error.save.failed'), icon: 'error' })
      return false
    }
    if (options.savedNotice === 'toast') toast.success(msg('cm.info.saved'))
    else await alert({ message: msg('cm.info.saved'), icon: 'information' })
    await options.afterSave?.()
    return true
  }

  return { saveChanges, confirmDiscardChanges }
}
