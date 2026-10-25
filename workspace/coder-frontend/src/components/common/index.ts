/*****************************************************************************************
	Program ID   : index
	Program Type : ts
	Program Name : 공통 컴포넌트 등록
	Description  : 공통 UI 컴포넌트(Button, Input, Select, Modal, DataTable, DatePicker, 코드서치, 파일업로드 등)를 일괄 export
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-05  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import ButtonComp from './button/Button.vue';
import InputComp from './input/Input.vue';
import SelectComp from './select/Select.vue';
import ModalComp from './modal/Modal.vue';
import CardComp from './card/Card.vue';
import BadgeComp from './badge/Badge.vue';
import DataTableComp from './table/DataTable.vue';
import TabsComp from './tabs/Tabs.vue';
import ListboxComp from './listbox/Listbox.vue';
import ProgressBarComp from './progress/ProgressBar.vue';
import DatePickerComp from './datepicker/DatePicker.vue';
import MessageDialogComp from './dialog/MessageDialog.vue';
import DialogHostComp from './dialog/DialogHost.vue';
import ToastHostComp from './toast/ToastHost.vue';
import PopupWindowComp from './dialog/PopupWindow.vue';
import TreeViewComp from './tree/TreeView.vue';
import BaseSearchCodeComp from './search-code/BaseSearchCode.vue';
import ComSearchCodeComp from './search-code/ComSearchCode.vue';
import ComSearchCodePopupComp from './search-code/ComSearchCodePopup.vue';
import ComFileUploadComp from './file-upload/ComFileUpload.vue';

export const Button = ButtonComp as any;
export const Input = InputComp as any;
export const Select = SelectComp as any;
export const Modal = ModalComp as any;
export const Card = CardComp as any;
export const Badge = BadgeComp as any;
export const DataTable = DataTableComp as any;
export const Tabs = TabsComp as any;
export const Listbox = ListboxComp as any;
export const ProgressBar = ProgressBarComp as any;
export const DatePicker = DatePickerComp as any;
export const MessageDialog = MessageDialogComp as any;
export const DialogHost = DialogHostComp as any;
export const ToastHost = ToastHostComp as any;
export const PopupWindow = PopupWindowComp as any;
export const TreeView = TreeViewComp as any;
export const BaseSearchCode = BaseSearchCodeComp as any;
export const ComSearchCode = ComSearchCodeComp as any;
export const ComSearchCodePopup = ComSearchCodePopupComp as any;
export const ComFileUpload = ComFileUploadComp as any;
export type { FileInfo } from '@/api/common/file';
export type { SearchCodeRow, SearchCodeParams } from '@/api/common/searchCode';
export type { TreeNode } from './tree/TreeView.vue';
export { useDialog } from '@/composables/useDialog';
export { useToast } from '@/composables/useToast';
