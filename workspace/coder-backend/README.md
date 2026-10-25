# CODER Backend Application

`coder-backend`는 **CODER Core Framework**(`coder-core-common`, `coder-core-db`, `coder-core-redis`, `coder-core-security`)를 기반 라이브러리로 활용하여 구축된 Spring Boot 3.3 기반 백엔드 애플리케이션입니다. **`coder-frontend`** 관리 콘솔과 연동되어 REST API 및 실시간 모니터링/인증/캐시 기능을 제공합니다.

---

## 🛠️ 기술 스택 & 프레임워크

- **Java**: 21 (LTS)
- **Spring Boot**: 3.3.5
- **Build Tool**: Gradle 8.x (Composite Build)
- **Base Libraries (`coder-core`)**:
  - `coder-core-common`: 표준 `ApiResponse<T>` 응답 래퍼, `GlobalExceptionHandler`, 비즈니스 예외 처리
  - `coder-core-db`: HikariCP 커넥션 풀, MyBatis 3.0.3 세션 매핑, `AuditInterceptor` (생성일시/수정일시 자동 주입)
  - `coder-core-redis`: `RedisTemplate` 기반 분산 캐시 및 세션 처리
  - `coder-core-security`: Spring Security 6, OAuth2 Resource Server, SpringDoc OpenAPI (Swagger UI)
- **Database**: PostgreSQL 18 (DB `coder`, Schema `coder`, 계정 `coder`)

---

## 📁 디렉터리 구조

```text
coder-backend/
├── src/
│   ├── main/
│   │   ├── java/kr/or/coder/
│   │   │   ├── biz/                           # 업무(비즈니스) 패키지 kr.or.coder.biz
│   │   │   │   └── CoderBackendApplication.java # Spring Boot 진입점 (@SpringBootApplication)
│   │   │   └── common/                        # 공통 패키지 kr.or.coder.common (code, exception, frame)
│   │   │       ├── frame/                     # 프레임 패키지 kr.or.coder.common.frame
│   │   │       │   ├── controller/                # REST API 컨트롤러 (System, Auth, Module, Cache)
│   │   │       │   ├── dto/                       # Request / Response DTO
│   │   │       │   ├── mapper/                    # MyBatis 매퍼 인터페이스 (UserMapper)
│   │   │       │   └── service/                   # 비즈니스 로직 서비스 레이어
│   │   │       ├── code/
│   │   │       └── exception/
│   │   └── resources/
│   │       ├── application.yml                # CODER 프레임워크 통합 설정값
│   │       └── mapper/frame/auths/AuthMapper.xml  # MyBatis XML 매퍼 (인증·사용자)
│   └── test/                                  # 단위 및 통합 테스트
├── settings.gradle                            # includeBuild('../coder-core')
├── build.gradle                               # 의존성 및 플러그인 설정
└── README.md
```

---

## 🚀 주요 REST API 명세

모든 API 응답은 `coder-core-common`의 `ApiResponse<T>` 규격을 따릅니다.

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/ping` | 프론트엔드 연동 헬스체크 핑 |
| `GET` | `/api/v1/system/status` | JVM 메모리, 가동시간, 서브시스템 진단 상태 |
| `GET` | `/api/v1/modules` | `coder-core` 서브모듈 아키텍처 및 설정 정보 목록 |
| `GET` | `/api/v1/modules/{id}` | 특정 모듈 세부정보 조회 |
| `POST` | `/api/v1/auth/login` | 사용자 로그인 및 토큰 발급 (`admin / coder1234`) |
| `GET` | `/api/v1/users/profile` | 사용자 프로필 조회 |
| `GET` | `/api/v1/users` | DB(MyBatis)에서 등록된 사용자 목록 조회 |
| `POST` | `/api/v1/cache/set` | Redis 캐시 저장 |
| `GET` | `/api/v1/cache/{key}` | Redis 캐시 값 조회 |
| `GET` | `/api/v1/cache/keys` | 활성 캐시 키 목록 조회 |
| `GET` | `/actuator/health` | Spring Boot Actuator 헬스체크 |
| `GET` | `/swagger-ui.html` | SpringDoc OpenAPI Swagger 문서 UI |

---

## 💻 실행 방법

### 1. 백엔드 실행 (Spring Boot)

```bash
# Gradle 빌드 및 실행
gradle bootRun
# 또는 gradlew 사용 시
./gradlew bootRun
```

서버가 실행되면 `http://localhost:8080` 포트에서 대기합니다.
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Database**: `jdbc:postgresql://localhost:5432/coder?currentSchema=coder` (User: `coder`, 비밀번호는 환경변수 `CODER_DB_PASSWORD`로 변경 가능)

### 2. 프론트엔드(`coder-frontend`)와 연동

`coder-frontend` 개발 서버를 실행하면 Vite 프록시(`http://localhost:8080`)를 통해 `coder-backend`와 즉시 연동됩니다.

```bash
cd ../coder-frontend
npm run dev
```
브라우저에서 `http://localhost:3000`으로 접속하여 Dashboard의 **"Ping CODER Core"** 버튼 및 모듈 현황을 확인할 수 있습니다.
