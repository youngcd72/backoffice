/*****************************************************************************************
	Program ID   : main
	Program Type : ts
	Program Name : 애플리케이션 진입점
	Description  : Vue 앱 생성 및 Pinia, Router 플러그인 등록 후 coder-backend 메시지를 내려받고 #app 에 마운트
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-09-20  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { loadMessages } from './utils/message'
import { vPermission } from './directives/permission'
import './assets/main.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.directive('permission', vPermission)
// 다국어 메시지는 coder-backend 에서 관리 — 내려받은 뒤 마운트 (실패해도 화면은 띄움)
loadMessages().finally(() => app.mount('#app'))
