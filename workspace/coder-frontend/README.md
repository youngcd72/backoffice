# CODER Frontend

CODER Core Framework 관리 콘솔 및 모니터링 프론트엔드 (Vue 3 + Vite + TypeScript)

## 🛠️ 기술 스택

- **Framework**: Vue 3 (Composition API / `<script setup>`)
- **Build Tool**: Vite 8
- **Language**: TypeScript 6
- **Routing**: Vue Router 5
- **State Management**: Pinia 4
- **HTTP Client**: Axios 1.19
- **Design System**: CODER Dark/Light Enterprise Theme (Vanilla CSS)

## 📁 디렉터리 구조

```text
coder-frontend/
├── src/
│   ├── api/             # Axios 인스턴스 및 인터셉터 설정 (CODER Backend Proxy)
│   ├── assets/          # CODER 디자인 시스템 스타일 (main.css)
│   ├── components/      # 공통 컴포넌트 (HeaderNav, SidebarNav, MetricCard, StatusBadge)
│   ├── layouts/         # 레이아웃 셸 (DefaultLayout)
│   ├── router/          # Vue Router 라우팅 설정
│   ├── stores/          # Pinia 전역 상태 관리 (Auth, Session)
│   ├── views/           # 페이지 뷰 (Dashboard, Modules, Login, Settings)
│   ├── App.vue          # 루트 컴포넌트
│   └── main.ts          # 애플리케이션 진입점
├── index.html           # HTML 엔트리포인트
├── vite.config.ts       # Vite 설정 (API Proxy: http://localhost:8080)
├── tsconfig.json        # TypeScript 설정
└── package.json         # 의존성 및 스크립트
```

## 🚀 실행 방법

```bash
# 패키지 설치 (이미 설치되어 있는 경우 생략)
npm install

# 로컬 개발 서버 실행 (기본 포트: 3000)
npm run dev

# 프로덕션 빌드
npm run build

# 빌드 미리보기
npm run preview
```

## 🔗 백엔드 연동

- 개발 서버 프록시 설정이 `vite.config.ts`에 포함되어 있어 `/api` 요청이 `http://localhost:8080`으로 자동 프록시됩니다.
