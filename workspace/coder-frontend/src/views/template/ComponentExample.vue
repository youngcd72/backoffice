<script setup lang="ts">
/*****************************************************************************************
	Program ID   : ComponentExample
	Program Type : vue
	Program Name : 컴포넌트 사용예제
	Description  : 공통 컴포넌트(팝업, 토스트, 코드검색, 첨부파일, 검증) 사용 예제
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-05  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { msg } from '@/utils/message'
import { ref } from 'vue'
import { Button, Input, Select, DatePicker, PopupWindow, ComSearchCode, ComFileUpload, useDialog, useToast, type SearchCodeRow, type FileInfo } from '@/components/common'
import { validateForm, validateFields } from '@/utils/validate'

const PAGE_TITLE = '컴포넌트 사용예제'
const PAGE_ID = 'COMPONENT_EXAMPLE'

// alert / confirm / 팝업 창 예제
const { alert, confirm } = useDialog()
const toast = useToast()
const dialogResult = ref('-')
const isPopupOpen = ref(false)

/**
 * onBtnToastClick : 토스트 예제 버튼 클릭 시 유형별 토스트 표시
 * @param   type - 토스트 유형 (success / info / warning / error)
 * @return  N/A
 */
function onBtnToastClick(type: 'success' | 'info' | 'warning' | 'error'): void {
  const messages = {
    success: msg('cm.info.saved'),
    info: msg('cm.example.toast.info'),
    warning: msg('cm.example.toast.warning'),
    error: msg('cm.example.toast.error'),
  }
  toast[type](messages[type])
}

/**
 * onBtnAlertClick : alert 예제 버튼 클릭 시 알림창 표시
 * @return  N/A
 */
async function onBtnAlertClick(): Promise<void> {
  await alert({
    detail: msg('cm.example.alert.detail'),
    message: msg('cm.example.alert.message'),
  })
  dialogResult.value = msg('cm.example.alert.closed')
}

/**
 * onBtnConfirmClick : confirm 예제 버튼 클릭 시 확인창 표시 후 결과 기록
 * @return  N/A
 */
async function onBtnConfirmClick(): Promise<void> {
  const ok = await confirm({
    title: 'CONFIRM MESSAGE',
    message: msg('cm.confirm.delete.selected'),
  })
  dialogResult.value = msg('cm.example.confirm.result', ok ? msg('cm.common.confirm') : msg('cm.common.cancel'))
}

/**
 * onBtnPopupClick : 팝업 열기 버튼 클릭
 * @return  N/A
 */
function onBtnPopupClick(): void {
  isPopupOpen.value = true
}

/**
 * onBtnPopupCloseClick : 팝업 닫기 버튼 클릭
 * @return  N/A
 */
function onBtnPopupCloseClick(): void {
  isPopupOpen.value = false
}

// 공통 코드서치 예제 (ComSearchCode)
const cdsForm = ref({ usrId: '', usrNm: '', prgmId: '', prgmNm: '' })
const cdsResult = ref('-')

/**
 * onCdsUsrChange : 사용자 코드검색 값 변경 시 선택 결과 표시
 * @param   row - 선택된 사용자 행 (해제 시 null)
 * @return  N/A
 */
function onCdsUsrChange(row: SearchCodeRow | null): void {
  cdsResult.value = row ? msg('cm.example.search.user.selected', row.CD, row.NM, row.EMAIL) : msg('cm.example.search.user.cleared')
}

// 공통 첨부파일 예제 (ComFileUpload)
// 필수 검증 예제 (utils/validate.ts)
const frmReqRef = ref<HTMLElement | null>(null)
const reqForm = ref({ usrNm: '', usrClsCd: '', stDt: '', mngrId: '', mngrNm: '', email: '', juminNo: '', rmk: '' })
const reqResult = ref('-')
const reqClsOptions = [
  { label: '직영', value: '1' },
  { label: '에이전트', value: '3' },
]

/**
 * onBtnReqCheckClick : 필수 항목 및 비고 길이(byte) 검증 실행
 * @return  N/A
 */
async function onBtnReqCheckClick(): Promise<void> {
  if (!(await validateForm(frmReqRef.value))) {
    reqResult.value = msg('cm.example.validate.required.missing')
    return
  }
  // 값 단위 검증: 길이(byte)·형식
  const ok = await validateFields([{ label: '비고', value: reqForm.value.rmk, maxBytes: 30 }])
  reqResult.value = ok ? msg('cm.example.validate.passed') : msg('cm.example.validate.length.exceeded')
}

const fupForm = ref({ attachFileId: '', imgFileId: '' })
const fupEditable = ref(true)
const fupResult = ref('-')

/**
 * onFupChange : 첨부파일 변경 시 결과 표시
 * @param   label - 첨부 영역 라벨
 * @param   files - 변경 후 파일 목록
 * @return  N/A
 */
function onFupChange(label: string, files: FileInfo[]): void {
  fupResult.value = `${label}: ${files.length}건${files.length ? ` (${files.map((f) => f.fileOrgnNm).join(', ')})` : ''}`
}

/**
 * onBtnFupModeClick : 첨부파일 편집/조회 모드 전환
 * @return  N/A
 */
function onBtnFupModeClick(): void {
  fupEditable.value = !fupEditable.value
}

/**
 * onCdsPrgmChange : 프로그램 코드검색 값 변경 시 선택 결과 표시
 * @param   row - 대표 선택 행 (해제 시 null)
 * @param   rows - 선택된 전체 행 목록
 * @return  N/A
 */
function onCdsPrgmChange(row: SearchCodeRow | null, rows: SearchCodeRow[]): void {
  cdsResult.value = row ? msg('cm.example.search.program.selected', rows.length, rows.map((r) => r.CD).join(', ')) : msg('cm.example.search.program.cleared')
}
</script>

<template>
  <section class="template-page">
    <header class="page-title-bar">
      <h2 class="page-title"><span class="page-plate">{{ PAGE_ID }}</span>{{ PAGE_TITLE }}</h2>
    </header>

    <!-- Alert / Confirm / Popup 예제 -->
    <div class="dialog-area">
      <span class="field-label">메시지 창</span>
      <Button id="btnAlert" variant="outline" @click="onBtnAlertClick">Alert</Button>
      <Button id="btnConfirm" variant="outline" @click="onBtnConfirmClick">Confirm</Button>
      <Button id="btnPopup" variant="outline" @click="onBtnPopupClick">Popup</Button>
      <span class="dialog-result">결과: <strong id="lblDialogResult">{{ dialogResult }}</strong></span>
    </div>

    <!-- 토스트 예제 (useToast) -->
    <div class="dialog-area">
      <span class="field-label">토스트</span>
      <Button id="btnToastSuccess" variant="outline" @click="onBtnToastClick('success')">Success</Button>
      <Button id="btnToastInfo" variant="outline" @click="onBtnToastClick('info')">Info</Button>
      <Button id="btnToastWarning" variant="outline" @click="onBtnToastClick('warning')">Warning</Button>
      <Button id="btnToastError" variant="outline" @click="onBtnToastClick('error')">Error</Button>
    </div>

    <!-- 필수 검증 예제 (required + utils/validate.ts) -->
    <div id="frmReq" ref="frmReqRef" class="dialog-area req-area">
      <span class="field-label">필수 검증</span>
      <Input id="txtReqUsrNm" v-model="reqForm.usrNm" placeholder="사용자명" size="small" required field-name="사용자명" />
      <Select id="cboReqUsrClsCd" v-model="reqForm.usrClsCd" :options="reqClsOptions" size="small" required field-name="사용자 유형" />
      <DatePicker id="dtpReqStDt" v-model="reqForm.stDt" :presets="false" size="small" required field-name="시작일자" />
      <ComSearchCode
        id="cdsReqMngr"
        type="USR"
        v-model:code="reqForm.mngrId"
        v-model:name="reqForm.mngrNm"
        required
        field-name="담당자"
      />
      <Input id="txtReqEmail" v-model="reqForm.email" placeholder="이메일" size="small" validate="required|email" field-name="이메일" />
      <Input id="txtReqJuminNo" v-model="reqForm.juminNo" placeholder="주민등록번호(선택)" size="small" :validate="['juminno']" field-name="주민등록번호" />
      <Input id="txtReqRmk" v-model="reqForm.rmk" placeholder="비고(30byte)" size="small" />
      <Button id="btnReqCheck" size="small" @click="onBtnReqCheckClick">검증</Button>
      <span class="dialog-result">결과: <strong id="lblReqResult">{{ reqResult }}</strong></span>
    </div>

    <!-- 공통 첨부파일 예제 (ComFileUpload) -->
    <div class="dialog-area fup-area">
      <span class="field-label">첨부파일</span>
      <span class="cds-example">
        <span class="cds-example-label">단건</span>
        <ComFileUpload
          id="fupAttach"
          file-cls-cd="SY"
          v-model:file-id="fupForm.attachFileId"
          :editable="fupEditable"
          @change="onFupChange('단건', $event)"
        />
      </span>
      <span class="cds-example">
        <span class="cds-example-label">이미지(최대 3)</span>
        <ComFileUpload
          id="fupImg"
          file-cls-cd="SY"
          type="IMG"
          :max-count="3"
          width="240px"
          v-model:file-id="fupForm.imgFileId"
          :editable="fupEditable"
          @change="onFupChange('이미지', $event)"
        />
      </span>
      <Button id="btnFupMode" variant="outline" size="small" @click="onBtnFupModeClick">
        {{ fupEditable ? '조회모드로' : '수정모드로' }}
      </Button>
      <span class="dialog-result">
        결과: <strong id="lblFupResult">{{ fupResult }}</strong> / fileId: <strong id="lblFupFileId">{{ fupForm.attachFileId || '-' }}</strong>
      </span>
    </div>

    <!-- 공통 코드서치 예제 (ComSearchCode) -->
    <div class="dialog-area">
      <span class="field-label">코드서치</span>
      <span class="cds-example">
        <span class="cds-example-label">사용자</span>
        <ComSearchCode
          id="cdsUsr"
          type="USR"
          v-model:code="cdsForm.usrId"
          v-model:name="cdsForm.usrNm"
          required
          @change="onCdsUsrChange"
        />
      </span>
      <span class="cds-example">
        <span class="cds-example-label">프로그램(다건)</span>
        <ComSearchCode
          id="cdsPrgm"
          type="PRGM"
          v-model:code="cdsForm.prgmId"
          v-model:name="cdsForm.prgmNm"
          multiple
          @change="onCdsPrgmChange"
        />
      </span>
      <span class="dialog-result">결과: <strong id="lblCdsResult">{{ cdsResult }}</strong></span>
    </div>

    <PopupWindow v-model="isPopupOpen" title="POPUP WINDOW" :width="480">
      <p>{{ msg('cm.example.popup.body') }}</p>
      <template #footer>
        <Button id="btnPopupClose" @click="onBtnPopupCloseClick">닫기</Button>
      </template>
    </PopupWindow>
  </section>
</template>

<style scoped>
.template-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.page-title-bar {
  padding-bottom: 8px;
  border-bottom: 2px solid var(--ds-slate-900);
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--ds-ink);
}

.dialog-area {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--ds-surface);
  border: 1px solid var(--ds-rule);
  border-radius: var(--ds-radius-panel);
}

.cds-example {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-right: 12px;
}

.cds-example-label {
  font-size: 13px;
  color: var(--ds-ink-2);
}

.req-area {
  flex-wrap: wrap;
}

.req-area > :deep(.custom-input-group),
.req-area > :deep(.custom-select-group) {
  width: 150px;
}

.fup-area {
  flex-wrap: wrap;
  align-items: flex-start;
}

.dialog-result {
  margin-left: 12px;
  font-size: 13px;
  color: var(--ds-ink-2);
}

.field-label {
  min-width: 64px;
  font-size: 13px;
  font-weight: 600;
  color: var(--ds-ink-2);
}
</style>
