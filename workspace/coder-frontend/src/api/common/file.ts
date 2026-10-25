/*****************************************************************************************
	Program ID   : file
	Program Type : ts
	Program Name : 공통 첨부파일 API
	Description  : 공통 첨부파일 API

	               대응 테이블: TB_APND_FILE_M (ERD SY0050M 시스템_첨부파일기본), FILE_ID 채번: SQ_TB_APND_FILE_M_FILE_ID

	               REST 경로 (백엔드 구현 예정)
	               - POST   /api/files                         업로드 + 파일정보 저장 (multipart)
	               - GET    /api/files?fileId=&apndFileMngeNo=  목록
	               - DELETE /api/files/{fileId}?fileSeqs=      삭제
	               - GET    /api/files/{fileId}/{fileSeq}       다운로드

	               백엔드 구현 전까지 브라우저 메모리 목업으로 동작한다 (새로고침하면 사라짐).
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-05  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { msg } from '@/utils/message'
import type { ApiResponse } from '../client'

export interface FileInfo {
  fileId: string
  fileSeq: number
  fileClsCd: string
  /** 원본 파일명 (FILE_ORGN_NM) */
  fileOrgnNm: string
  fileSize: number
  /** 확장자 (FILE_EXPS_NM) */
  fileExpsNm: string
  /** 업무 첨부파일관리번호 (APND_FILE_MNGE_NO) */
  apndFileMngeNo: string
  regrId: string
}

export interface FileUploadRequest {
  fileClsCd: string
  /** 기존 파일ID. 비어 있으면 새로 채번 */
  fileId: string
  apndFileMngeNo: string
  files: File[]
}

export interface FileUploadResult {
  fileId: string
  apndFileMngeNo: string
  files: FileInfo[]
}

// ---------------------------------------------------------------------
// 목업 저장소 (백엔드 연동 전)
// ---------------------------------------------------------------------
interface StoredFile {
  info: FileInfo
  blob: Blob
}

const mockStore = new Map<string, StoredFile[]>()
let mockFileIdSeq = 1000

/**
 * getExtension : 파일명에서 확장자를 소문자로 추출
 * @param   fileName - 파일명
 * @return  확장자 (없으면 빈 문자열)
 */
function getExtension(fileName: string): string {
  const pos = fileName.lastIndexOf('.')
  return pos < 0 ? '' : fileName.slice(pos + 1).toLowerCase()
}

/**
 * listInfos : 목업 저장소에서 파일ID에 해당하는 파일정보 목록을 복사하여 반환
 * @param   fileId - 파일ID
 * @return  파일정보 목록
 */
function listInfos(fileId: string): FileInfo[] {
  return (mockStore.get(fileId) ?? []).map((stored) => ({ ...stored.info }))
}

/**
 * getFileListApi : 첨부파일 목록 조회 (GET /api/files)
 * @param   fileId         - 파일ID (있으면 해당 파일ID 기준 조회)
 * @param   apndFileMngeNo - 업무 첨부파일관리번호 (파일ID가 없을 때 기준)
 * @return  파일정보 목록을 담은 API 응답
 */
export async function getFileListApi(fileId: string, apndFileMngeNo = ''): Promise<ApiResponse<FileInfo[]>> {
  if (fileId) return { success: true, data: listInfos(fileId) }
  if (!apndFileMngeNo) return { success: true, data: [] }
  const data = [...mockStore.values()]
    .flat()
    .filter((stored) => stored.info.apndFileMngeNo === apndFileMngeNo)
    .map((stored) => ({ ...stored.info }))
  return { success: true, data }
}

/**
 * uploadFilesApi : 파일 업로드 및 파일정보 저장 (POST /api/files, multipart)
 * @param   request - 업로드 요청 (파일구분코드, 파일ID, 관리번호, 파일 목록)
 * @param   regrId  - 등록자ID
 * @return  파일ID와 저장된 파일정보 목록을 담은 API 응답
 */
export async function uploadFilesApi(request: FileUploadRequest, regrId = ''): Promise<ApiResponse<FileUploadResult>> {
  const fileId = request.fileId || String(++mockFileIdSeq)
  const stored = mockStore.get(fileId) ?? []
  let fileSeq = stored.reduce((max, item) => Math.max(max, item.info.fileSeq), 0)
  request.files.forEach((file) => {
    fileSeq += 1
    stored.push({
      info: {
        fileId,
        fileSeq,
        fileClsCd: request.fileClsCd,
        fileOrgnNm: file.name,
        fileSize: file.size,
        fileExpsNm: getExtension(file.name),
        apndFileMngeNo: request.apndFileMngeNo,
        regrId,
      },
      blob: file,
    })
  })
  mockStore.set(fileId, stored)
  return { success: true, data: { fileId, apndFileMngeNo: request.apndFileMngeNo, files: listInfos(fileId) } }
}

/**
 * deleteFilesApi : 첨부파일 삭제 (DELETE /api/files/{fileId}?fileSeqs=)
 * @param   fileId   - 파일ID
 * @param   fileSeqs - 삭제할 파일순번 목록
 * @return  삭제된 건수를 담은 API 응답
 */
export async function deleteFilesApi(fileId: string, fileSeqs: number[]): Promise<ApiResponse<number>> {
  const stored = mockStore.get(fileId) ?? []
  const remain = stored.filter((item) => !fileSeqs.includes(item.info.fileSeq))
  mockStore.set(fileId, remain)
  return { success: true, data: stored.length - remain.length }
}

/**
 * updateApndFileMngeNoApi : 파일ID에 속한 파일의 업무 관리번호 갱신 (원본 fn_updateMngeNo, PUT /api/files/{fileId}/mnge-no)
 * @param   fileId         - 파일ID
 * @param   apndFileMngeNo - 새 업무 첨부파일관리번호
 * @return  갱신된 건수를 담은 API 응답
 */
export async function updateApndFileMngeNoApi(fileId: string, apndFileMngeNo: string): Promise<ApiResponse<number>> {
  const stored = mockStore.get(fileId) ?? []
  stored.forEach((item) => {
    item.info.apndFileMngeNo = apndFileMngeNo
  })
  return { success: true, data: stored.length }
}

/**
 * downloadFileApi : 파일 다운로드 (GET /api/files/{fileId}/{fileSeq})
 * @param   file - 다운로드할 파일정보
 * @return  N/A (파일이 없으면 Error 발생)
 */
export async function downloadFileApi(file: FileInfo): Promise<void> {
  const stored = mockStore.get(file.fileId)?.find((item) => item.info.fileSeq === file.fileSeq)
  if (!stored) throw new Error(msg('cm.file.not.found'))
  const link = document.createElement('a')
  link.href = URL.createObjectURL(stored.blob)
  link.download = file.fileOrgnNm
  link.click()
  URL.revokeObjectURL(link.href)
}
