<script setup lang="ts">
/*****************************************************************************************
	Program ID   : ComFileUpload
	Program Type : vue
	Program Name : 공통 첨부파일
	Description  : 파일 선택/드래그앤드롭 업로드, 목록 조회, 삭제, 다운로드를 제공하는 첨부파일 컴포넌트
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-05  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
import { msg } from '@/utils/message'
import { ref, computed, watch } from 'vue'
import { useDialog } from '@/composables/useDialog'
import { useAuthStore } from '@/stores/auth'
import {
  getFileListApi,
  uploadFilesApi,
  deleteFilesApi,
  updateApndFileMngeNoApi,
  downloadFileApi,
  type FileInfo,
} from '@/api/common/file'

/**
 * 공통 첨부파일
 *
 * 사용 예)
 *   <ComFileUpload id="fupAttach" file-cls-cd="SY" v-model:file-id="form.fileId" :editable="isEditMode" />
 *
 * 동작
 * - 파일첨부(선택·드래그앤드롭) 즉시 업로드하고 파일정보를 저장한다. 결과 파일ID를 v-model:file-id 로 돌려준다.
 * - 파일삭제는 확인 후 즉시 삭제한다.
 * - 파일명 옆 [열기]로 다운로드한다.
 * - 업로드 전 검사: 업무구분 필수, 개수, 파일당 용량, 파일명 100byte, 확장자 필수·허용 확장자, 중복 파일명
 */

/** 업로드 유형별 기본 허용 확장자·용량 (원본 fn_init) */
const FILE_TYPE_PRESETS: Record<string, { extensions: string[]; maxSize: number }> = {
  IMG: { extensions: ['jpeg', 'jpg', 'png', 'gif', 'bmp'], maxSize: 1024 * 1024 },
  DOC: {
    extensions: ['hwp', 'doc', 'docx', 'xls', 'xlsx', 'xlt', 'xlsm', 'ppt', 'pptx', 'pdf', 'zip', 'tiff', 'txt'],
    maxSize: 5 * 1024 * 1024,
  },
  DEFAULT: {
    extensions: ['hwp', 'doc', 'docx', 'xls', 'xlsx', 'xlt', 'xlsm', 'ppt', 'pptx', 'pdf', 'zip', 'jpg', 'png', 'tiff', 'txt'],
    maxSize: 5 * 1024 * 1024,
  },
}

const MAX_FILE_NAME_BYTES = 100

const props = withDefaults(
  defineProps<{
    /** 업로드 업무구분 (업무약어, 예: SY) — 필수 */
    fileClsCd: string
    /** 파일ID (업로드 후 채번된 값) */
    fileId?: string
    /** 업무 첨부파일관리번호 */
    apndFileMngeNo?: string
    /** 업로드 유형: IMG(이미지) / DOC(문서) / 그 외 기본 */
    type?: string
    /** 허용 확장자 (예: ['pdf', 'xlsx']). 없으면 유형별 기본값 */
    extensions?: string[]
    /** 파일당 최대 용량(byte). 없으면 유형별 기본값 */
    maxSize?: number
    /** 최대 첨부 개수 (원본 컴포넌트 기본 1) */
    maxCount?: number
    /** 수정 가능 여부 (원본 bEnable). false 면 조회·열기만 가능 */
    editable?: boolean
    /** 파일 목록 영역 폭 */
    width?: string
  }>(),
  {
    fileId: '',
    apndFileMngeNo: '',
    type: '',
    extensions: undefined,
    maxSize: undefined,
    maxCount: 1,
    editable: true,
    width: '280px',
  },
)

const emit = defineEmits<{
  (e: 'update:fileId', value: string): void
  (e: 'update:apndFileMngeNo', value: string): void
  /** 첨부 목록이 바뀜 (업로드·삭제·조회 후) */
  (e: 'change', files: FileInfo[]): void
}>()

const { alert, confirm } = useDialog()
const authStore = useAuthStore()

const fileInputRef = ref<HTMLInputElement | null>(null)
const files = ref<FileInfo[]>([])
const checkedSeqs = ref<number[]>([])
const isBusy = ref(false)
const isDragOver = ref(false)

const preset = computed(() => FILE_TYPE_PRESETS[props.type.toUpperCase()] ?? FILE_TYPE_PRESETS.DEFAULT)
const allowedExtensions = computed(() => (props.extensions ?? preset.value.extensions).map((ext) => ext.toLowerCase()))
const maxFileSize = computed(() => props.maxSize ?? preset.value.maxSize)
const acceptAttr = computed(() => allowedExtensions.value.map((ext) => `.${ext}`).join(','))
const isMultiple = computed(() => props.maxCount > 1)
const canAdd = computed(() => props.editable && files.value.length < props.maxCount)
const canDelete = computed(() => props.editable && files.value.length > 0)

/**
 * formatSize : 바이트 수를 KB/MB 단위 문자열로 변환
 * @param   bytes - 파일 크기(byte)
 * @return  표시용 크기 문자열
 */
function formatSize(bytes: number): string {
  if (bytes >= 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)}MB`
  return `${Math.max(1, Math.round(bytes / 1024))}KB`
}

/**
 * getUtf8Bytes : 문자열의 UTF-8 바이트 수를 계산
 * @param   text - 대상 문자열
 * @return  UTF-8 바이트 수
 */
function getUtf8Bytes(text: string): number {
  return new TextEncoder().encode(text).length
}

/**
 * validateFiles : 첨부 전 검사 (업무구분, 개수, 확장자, 용량, 파일명 길이, 중복 파일명)
 * 원본: fn_fileAttachAdd
 * @param   selected - 선택된 파일 목록
 * @return  오류 메시지 (문제 없으면 빈 문자열)
 */
function validateFiles(selected: File[]): string {
  if (!props.fileClsCd) return msg('cm.file.class.code.missing')
  if (files.value.length + selected.length > props.maxCount) {
    return msg('cm.file.count.exceeded', props.maxCount)
  }
  const names = new Set(files.value.map((file) => file.fileOrgnNm))
  for (const file of selected) {
    const pos = file.name.lastIndexOf('.')
    if (pos < 0) return msg('cm.file.extension.missing', file.name)
    if (!allowedExtensions.value.includes(file.name.slice(pos + 1).toLowerCase())) {
      return msg('cm.file.extension.not.allowed', file.name, allowedExtensions.value.join(', '))
    }
    if (file.size > maxFileSize.value) {
      return msg('cm.file.size.exceeded', formatSize(maxFileSize.value), file.name)
    }
    if (getUtf8Bytes(file.name) > MAX_FILE_NAME_BYTES) return msg('cm.file.name.length.exceeded', MAX_FILE_NAME_BYTES, file.name)
    if (names.has(file.name)) return msg('cm.file.duplicated', file.name)
    names.add(file.name)
  }
  return ''
}

/**
 * setFiles : 첨부 목록을 갱신하고 체크 상태를 초기화한 뒤 change 이벤트를 발생
 * @param   list - 새 첨부 목록
 * @return  N/A
 */
function setFiles(list: FileInfo[]): void {
  files.value = list
  checkedSeqs.value = []
  emit('change', list)
}

/**
 * reload : 파일ID/첨부관리번호로 첨부 목록을 다시 조회
 * 원본: fn_search
 * @return  N/A
 */
async function reload(): Promise<void> {
  if (!props.fileId && !props.apndFileMngeNo) {
    setFiles([])
    return
  }
  isBusy.value = true
  try {
    const res = await getFileListApi(props.fileId, props.apndFileMngeNo)
    setFiles(res.data)
  } finally {
    isBusy.value = false
  }
}

/**
 * uploadFiles : 검사 후 파일을 업로드하고 파일정보를 저장
 * 원본: fn_fileUpload → fn_fileInfoSave
 * @param   selected - 업로드할 파일 목록
 * @return  N/A
 */
async function uploadFiles(selected: File[]): Promise<void> {
  if (selected.length === 0) return
  const errorMessage = validateFiles(selected)
  if (errorMessage) {
    await alert({ message: errorMessage, icon: 'warning' })
    return
  }
  isBusy.value = true
  try {
    const res = await uploadFilesApi(
      { fileClsCd: props.fileClsCd.toUpperCase(), fileId: props.fileId, apndFileMngeNo: props.apndFileMngeNo, files: selected },
      authStore.user?.username ?? '',
    )
    if (res.data.fileId !== props.fileId) emit('update:fileId', res.data.fileId)
    if (res.data.apndFileMngeNo !== props.apndFileMngeNo) emit('update:apndFileMngeNo', res.data.apndFileMngeNo)
    setFiles(res.data.files)
  } catch {
    await alert({ message: msg('cm.file.upload.failed'), icon: 'error' })
  } finally {
    isBusy.value = false
  }
}

/**
 * removeFiles : 서버에서 파일을 삭제하고 목록을 다시 조회
 * @param   targets - 삭제할 파일 목록
 * @return  N/A
 */
async function removeFiles(targets: FileInfo[]): Promise<void> {
  if (targets.length === 0) return
  isBusy.value = true
  try {
    await deleteFilesApi(targets[0].fileId, targets.map((file) => file.fileSeq))
    await reload()
  } finally {
    isBusy.value = false
  }
}

// ---------------------------------------------------------------------
// 이벤트 핸들러
// ---------------------------------------------------------------------
/**
 * onBtnFileAddClick : 파일첨부 버튼 클릭 시 파일 선택창을 염
 * @return  N/A
 */
function onBtnFileAddClick(): void {
  fileInputRef.value?.click()
}

/**
 * onFileInputChange : 파일 선택창에서 파일이 선택되면 업로드
 * @param   event - 파일 input 의 change 이벤트
 * @return  N/A
 */
async function onFileInputChange(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const selected = Array.from(input.files ?? [])
  input.value = '' // 같은 파일 다시 선택 가능하도록
  await uploadFiles(selected)
}

/**
 * onBtnFileDelClick : 파일삭제 버튼 클릭 시 확인 후 선택(또는 전체) 파일을 삭제
 * @return  N/A
 */
async function onBtnFileDelClick(): Promise<void> {
  const targets = isMultiple.value ? files.value.filter((file) => checkedSeqs.value.includes(file.fileSeq)) : files.value
  if (targets.length === 0) {
    await alert({ message: msg('cm.file.delete.required'), icon: 'warning' })
    return
  }
  const ok = await confirm({ message: msg('cm.confirm.delete.file'), icon: 'question' })
  if (ok) await removeFiles(targets)
}

/**
 * onBtnFileOpenClick : 열기 클릭 시 파일을 다운로드
 * @param   file - 다운로드할 파일 정보
 * @return  N/A
 */
async function onBtnFileOpenClick(file: FileInfo): Promise<void> {
  try {
    await downloadFileApi(file)
  } catch {
    await alert({ message: msg('cm.file.download.failed'), icon: 'error' })
  }
}

/**
 * onDragOver : 파일을 끌어 올리면 드롭 가능 상태로 표시
 * @param   event - 드래그 이벤트
 * @return  N/A
 */
function onDragOver(event: DragEvent): void {
  if (!canAdd.value || !event.dataTransfer?.types.includes('Files')) return
  event.preventDefault()
  isDragOver.value = true
}

/**
 * onDrop : 파일을 놓으면 업로드
 * @param   event - 드롭 이벤트
 * @return  N/A
 */
async function onDrop(event: DragEvent): Promise<void> {
  isDragOver.value = false
  if (!canAdd.value) return
  event.preventDefault()
  await uploadFiles(Array.from(event.dataTransfer?.files ?? []))
}

// fileId / apndFileMngeNo 가 바뀌면 다시 조회 (원본 fn_init → fn_search)
watch(
  () => [props.fileId, props.apndFileMngeNo],
  ([fileId], prev) => {
    // 방금 업로드로 채번된 fileId 는 이미 목록을 갖고 있으므로 다시 조회하지 않는다
    if (fileId && !prev?.[0] && files.value.some((file) => file.fileId === fileId)) return
    reload()
  },
  { immediate: true },
)

defineExpose({
  reload,
  /** 첨부 목록 (원본 dsMain) */
  getFiles: () => files.value,
  /** 화면 값 초기화 (원본 fn_clear) — 서버 파일은 지우지 않음 */
  clear: () => {
    emit('update:fileId', '')
    emit('update:apndFileMngeNo', '')
    setFiles([])
  },
  /** 모든 첨부파일 삭제 (원본 fn_deleteAllFiles) */
  deleteAll: () => removeFiles(files.value),
  /** 임시 관리번호를 업무 저장 후 채번된 번호로 갱신 (원본 fn_updateMngeNo) */
  updateApndFileMngeNo: async (newMngeNo: string) => {
    if (props.fileId) await updateApndFileMngeNoApi(props.fileId, newMngeNo)
    emit('update:apndFileMngeNo', newMngeNo)
  },
})
</script>

<template>
  <div class="file-upload" :class="{ 'is-busy': isBusy }">
    <div
      class="file-list"
      :class="{ 'is-drag-over': isDragOver, 'is-empty': files.length === 0 }"
      :style="{ width }"
      @dragover="onDragOver"
      @dragleave="isDragOver = false"
      @drop="onDrop"
    >
      <div v-for="file in files" :key="file.fileSeq" class="file-row">
        <input
          v-if="isMultiple && editable"
          v-model="checkedSeqs"
          type="checkbox"
          class="file-check"
          :value="file.fileSeq"
          :aria-label="`${file.fileOrgnNm} 선택`"
        />
        <span class="file-name" :title="`${file.fileOrgnNm} (${formatSize(file.fileSize)})`">{{ file.fileOrgnNm }}</span>
        <button type="button" class="file-btn file-open-btn" @click="onBtnFileOpenClick(file)">열기</button>
      </div>
      <div v-if="files.length === 0" class="file-row file-placeholder">
        {{ canAdd ? msg('cm.file.drop.guide') : msg('cm.file.empty') }}
      </div>
    </div>

    <div v-if="editable" class="file-actions">
      <button v-if="canAdd" type="button" class="file-btn" :disabled="isBusy" @click="onBtnFileAddClick">파일첨부</button>
      <button v-if="canDelete" type="button" class="file-btn" :disabled="isBusy" @click="onBtnFileDelClick">파일삭제</button>
    </div>

    <input
      ref="fileInputRef"
      type="file"
      class="file-input"
      :accept="acceptAttr"
      :multiple="isMultiple"
      tabindex="-1"
      aria-hidden="true"
      @change="onFileInputChange"
    />
  </div>
</template>

<style scoped>
.file-upload {
  --fu-row-height: 28px;
  --fu-border: var(--ds-rule-strong);
  --fu-bg: var(--ds-surface);
  --fu-bg-muted: var(--ds-surface-muted);
  --fu-btn-bg: var(--ds-surface);
  --fu-btn-bg-hover: var(--ds-pine-050);
  --fu-text: var(--ds-ink);
  --fu-text-muted: var(--ds-ink-3);
  --fu-accent: var(--ds-pine-700);

  display: inline-flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 13px;
  color: var(--fu-text);
  vertical-align: middle;
}

.file-list {
  box-sizing: border-box;
  max-height: calc(var(--fu-row-height) * 5 + 2px);
  overflow-y: auto;
  background: var(--fu-bg);
  border: 1px solid var(--fu-border);
  border-radius: var(--ds-radius-control);
  transition: background-color var(--ds-ease), border-color var(--ds-ease);
}

.file-list.is-drag-over {
  border-color: var(--fu-accent);
  background: var(--ds-pine-050);
  box-shadow: var(--ds-focus-field);
}

.file-row {
  display: flex;
  align-items: center;
  gap: 6px;
  height: var(--fu-row-height);
  padding: 0 4px 0 8px;
}

.file-row + .file-row {
  border-top: 1px solid var(--ds-rule);
}

.file-placeholder {
  color: var(--fu-text-muted);
  background: var(--fu-bg-muted);
}

.file-check {
  margin: 0;
  accent-color: var(--ds-pine-700);
}

.file-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.file-actions {
  display: flex;
  gap: 4px;
}

.file-btn {
  box-sizing: border-box;
  height: var(--ds-control-height-sm);
  padding: 0 10px;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  color: var(--fu-text);
  background: var(--fu-btn-bg);
  border: 1px solid var(--fu-border);
  border-radius: var(--ds-radius-control);
  cursor: pointer;
  transition: background-color var(--ds-ease), border-color var(--ds-ease), color var(--ds-ease);
}

.file-btn:hover:not(:disabled) {
  background: var(--fu-btn-bg-hover);
  border-color: var(--ds-pine-700);
  color: var(--ds-pine-900);
}

.file-btn:focus-visible {
  outline: none;
  box-shadow: var(--ds-focus);
}

.file-btn:disabled {
  background: var(--ds-surface-muted);
  border-color: var(--ds-rule);
  color: var(--fu-text-muted);
  cursor: wait;
}

.file-open-btn {
  height: 22px;
  padding: 0 8px;
  font-size: 12px;
}

.file-input {
  display: none;
}

.is-busy .file-list {
  opacity: 0.6;
}
</style>
