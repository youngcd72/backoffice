/// <reference types="vite/client" />
/*****************************************************************************************
	Program ID   : env.d
	Program Type : ts
	Program Name : 환경 타입 선언
	Description  : Vite 환경변수(ImportMetaEnv) 및 *.vue 모듈 타입 선언
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-09-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/


declare module '*.vue' {
  import type { DefineComponent } from 'vue';
  const component: DefineComponent<any, any, any>;
  export default component;
}

interface ImportMetaEnv {
  readonly VITE_APP_ENV: 'local' | 'develop' | 'production';
  readonly VITE_API_BASE_URL: string;
  /** coder-batch 관리 API 주소 (비우면 같은 출처, 로컬은 vite proxy) */
  readonly VITE_BATCH_API_BASE_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
