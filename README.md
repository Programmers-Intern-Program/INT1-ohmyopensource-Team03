# Oh My OpenSource (OMOS)

> GitHub 오픈소스 기여의 첫 걸음을 AI가 함께합니다.
> 
> GitHub 활동을 분석해 맞춤형 이슈를 추천하고, 코드 수정 가이드부터 PR 초안 작성까지 AI가 안내하는 오픈소스 기여 가이드 서비스

---

## 목차

- [프로젝트 소개](#프로젝트-소개)
- [핵심 기능](#핵심-기능)
- [시스템 아키텍처](#시스템-아키텍처)
- [기술 스택](#기술-스택)
- [폴더 구조](#폴더-구조)
- [시작하기](#시작하기)
- [환경 변수 설정](#환경-변수-설정)
- [API 명세](#api-명세)
- [AI 기능 상세](#ai-기능-상세)
- [팀 소개](#팀-소개)

---

## 프로젝트 소개

기여 의지가 있는 주니어 개발자들이 적합한 이슈 탐색, 코드 파악, 컨벤션 진입의 어려움으로 첫 PR도 내보지 못하고 이탈하는 문제를 해결하기 위해 기획된 서비스입니다.

**GitHub ID 하나만 있으면:**

1. 내 GitHub 활동을 분석해 **내 기술 스택에 맞는 Good First Issue**를 추천
2. 이슈를 어떻게 해결해야 할지 **AI 코드 수정 가이드** 제공
3. 코드 수정 후 **PR 제목과 본문을 자동 생성** (프로젝트 컨벤션 반영)
4. 필요시 **영어 PR로 자동 번역**

웹 서비스 외에 **Chrome 익스텐션**으로 GitHub 페이지에서 직접 사용할 수 있습니다.

---

## 핵심 기능

### 1. GitHub 프로필 분석 및 벡터화
- GitHub OAuth 로그인 후 커밋 이력, 사용 언어, 레포지토리 토픽 수집
- Gemini Embedding 2 모델로 3072차원 사용자 프로필 벡터 생성
- PostgreSQL pgvector에 저장하여 코사인 유사도 기반 매칭 수행

### 2. 맞춤형 이슈 추천
- Good First Issue 기반 이슈 크롤링 (GitHub Search API)
- 이슈 내용을 임베딩 후 사용자 벡터와 유사도 비교
- 사용자별 맞춤 이슈 3개 추천 + 추천 사유 제공

### 3. AI 코드 수정 가이드
- 이슈 해결에 필요한 파일 선별 (최대 10개)
- 수정 위치, 구체적 로직 가이드, 사이드 이펙트 분석
- 의사 코드(Pseudo Code) 제공
- 캐싱 전략으로 재분석 비용 절감, 일일 분석 요청 제한(5회)

### 4. PR 초안 자동 생성
- 코드 diff 분석 + CONTRIBUTING.md 파싱 + 기존 PR 컨벤션 학습
- 프로젝트 톤앤매너에 맞는 PR 제목/본문 자동 생성
- Markdown 포맷 유지, 커밋 컨벤션 자동 반영
- 생성 후 수정, 영어 번역 기능 제공

### 5. Chrome 익스텐션
- GitHub 이슈 목록에 OMOS 추천 뱃지 자동 삽입
- 이슈 상세 페이지에서 코드 가이드/의사 코드 사이드바 표시
- PR 생성 페이지에서 제목/본문 자동 입력 및 번역 버튼 제공

---

## 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                        클라이언트                         │
│                                                         │
│   ┌──────────────────┐    ┌──────────────────────┐      │
│   │  Web (Next.js)   │    │  Chrome Extension    │      │
│   │  localhost:3000  │    │  (GitHub 페이지 주입) │      │
│   └────────┬─────────┘    └──────────┬───────────┘      │
└────────────┼──────────────────────────┼──────────────────┘
             │ HTTP (JWT Bearer)         │ HTTP (JWT Bearer)
             ▼                          ▼
┌─────────────────────────────────────────────────────────┐
│                   백엔드 (Spring Boot)                    │
│                    localhost:8080                        │
│                                                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │  Auth    │ │  User    │ │  Issue   │ │ PrDraft  │  │
│  │ (OAuth2) │ │(Profiling│ │(Recommend│ │(PR 생성) │  │
│  │  + JWT)  │ │+ Vector) │ │+ Guide)  │ │          │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘  │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │                  External API 연동                 │  │
│  │  GitHub API  │  GLM-4.5  │  Gemini Embedding 2   │  │
│  └──────────────────────────────────────────────────┘  │
└──────────────────────┬──────────────────────────────────┘
                       │
          ┌────────────┼────────────┐
          ▼            ▼            ▼
   ┌────────────┐ ┌─────────┐ ┌──────────┐
   │ PostgreSQL │ │  Redis  │ │ Langfuse │
   │ + pgvector │ │(캐싱/   │ │(LLM 모니 │
   │  (벡터 DB) │ │Rate Lmt)│ │ 터링)    │
   └────────────┘ └─────────┘ └──────────┘
```

---

## 기술 스택

### Backend
| 항목 | 기술 |
|------|------|
| 언어 | Kotlin |
| 프레임워크 | Spring Boot 3.5 |
| 인증 | Spring Security + GitHub OAuth2 + JWT |
| ORM | Spring Data JPA |
| API 통신 | Spring WebFlux (WebClient) |
| AI 통합 | Spring AI 1.1.4 |
| AI 모델 | GLM-4.5 (PR 생성, 이슈 분석), Gemini Embedding 2 (벡터화) |
| 데이터베이스 | PostgreSQL 16 + pgvector |
| 모니터링 | Langfuse (LLM 성능 추적) |
| API 문서 | SpringDoc OpenAPI (Swagger UI) |
| 빌드 | Gradle |
| 테스트 | JUnit 5, Mockito, OkHttp MockWebServer |

### Frontend
| 항목 | 기술 |
|------|------|
| 프레임워크 | Next.js 14 |
| 언어 | TypeScript |
| 스타일링 | Tailwind CSS (GitHub 다크 테마) |
| 인증 | localStorage 기반 JWT |

### Chrome Extension
| 항목 | 기술 |
|------|------|
| Manifest | V3 |
| 언어 | Vanilla JavaScript |
| 호스트 권한 | `https://github.com/*` |

### Infrastructure
| 항목 | 기술 |
|------|------|
| 컨테이너 | Docker Compose |
| 데이터베이스 | PostgreSQL 16 + pgvector 커스텀 이미지 |
| LLM 모니터링 | Langfuse 2 |
| 포트 | 백엔드: 8080, 프론트: 3000, Langfuse: 3001, DB: 5432 |

---

## 폴더 구조

```
INT1-ohmyopensource-Team03/
├── omos/                                    # 백엔드 (Spring Boot + Kotlin)
│   ├── src/main/kotlin/com/back/omos/
│   │   ├── domain/
│   │   │   ├── analysis/                    # 코드 분석 모듈
│   │   │   │   ├── ai/                      # GLM API 클라이언트, 프롬프트 빌더
│   │   │   │   ├── controller/              # ContextAnalyzerController
│   │   │   │   ├── service/                 # 분석 비즈니스 로직, 캐싱 전략
│   │   │   │   ├── entity/                  # AnalysisResult, UserAnalysisRequest
│   │   │   │   └── repository/
│   │   │   ├── issue/                       # 이슈 추천 모듈
│   │   │   │   ├── ai/                      # 이슈 GLM 클라이언트
│   │   │   │   ├── controller/              # IssueController
│   │   │   │   ├── service/                 # IssueService, RecommendService
│   │   │   │   ├── entity/                  # Issue, UserRecommendedIssue
│   │   │   │   ├── github/                  # GitHub API 클라이언트
│   │   │   │   └── repository/
│   │   │   ├── prdraft/                     # PR 초안 생성 모듈
│   │   │   │   ├── ai/                      # Spring AI 클라이언트
│   │   │   │   ├── controller/              # PrDraftController
│   │   │   │   ├── service/                 # PrDraftService, 번역 서비스
│   │   │   │   ├── entity/                  # PrDraft
│   │   │   │   ├── github/                  # diff, CONTRIBUTING.md 수집
│   │   │   │   └── repository/
│   │   │   ├── user/                        # 사용자 정보 관리
│   │   │   │   ├── controller/              # UserController
│   │   │   │   ├── service/                 # UserService, UserVectorService
│   │   │   │   ├── entity/                  # User (profile_vector 포함)
│   │   │   │   └── repository/
│   │   │   └── repo/                        # GitHub 저장소 정보
│   │   └── global/
│   │       ├── auth/                        # OAuth2, JWT 인증
│   │       ├── config/                      # Spring 설정 클래스
│   │       ├── exception/                   # 예외 처리
│   │       ├── response/                    # 공통 응답 형식
│   │       └── ai/                          # AI 통합 설정 (Langfuse 등)
│   ├── src/main/resources/
│   │   ├── application.yaml                 # 공통 설정
│   │   ├── application-dev.yaml             # 개발 환경 설정
│   │   └── templates/
│   │       └── pr-default-template.md       # PR 기본 템플릿
│   ├── build.gradle
│   └── docker-compose.yml                   # PostgreSQL, pgvector, Langfuse
│
├── front/                                   # 프론트엔드 (Next.js)
│   ├── src/
│   │   ├── app/
│   │   │   ├── login/                       # 로그인 페이지
│   │   │   ├── oauth/callback/              # OAuth 콜백
│   │   │   ├── dashboard/                   # 메인 대시보드 (3단계 플로우)
│   │   │   ├── profile/                     # 프로필 벡터 업데이트
│   │   │   ├── issues/                      # 이슈 추천 목록 + 상세
│   │   │   ├── pr-draft/                    # PR 초안 생성
│   │   │   └── pr/[id]/                     # PR 상세 조회/수정
│   │   ├── components/
│   │   │   ├── Navbar.tsx
│   │   │   └── ProtectedLayout.tsx          # 인증 필수 페이지 래퍼
│   │   └── lib/
│   │       ├── auth.ts                      # JWT 토큰 관리
│   │       └── api.ts                       # API 호출 함수 + 타입 정의
│   ├── package.json
│   └── tailwind.config.ts
│
└── extension/                               # Chrome 익스텐션
    ├── manifest.json                        # Manifest V3
    ├── popup.html / popup.js                # 팝업 UI + 추천 이력
    ├── background.js                        # 서비스 워커 (API 통신)
    ├── content.js                           # GitHub 페이지 사이드바 주입
    └── content.css                          # 사이드바 스타일
```

---

## 시작하기

### 사전 요구사항

- JDK 21+
- Node.js 18+
- Docker & Docker Compose
- GitHub OAuth App 등록 (Client ID, Secret 필요)
- GLM API Key (Grepp Sandbox)
- Google Gemini API Key

### 1. 저장소 클론

```bash
git clone https://github.com/Programmers-Intern-Program/INT1-ohmyopensource-Team03.git
cd INT1-ohmyopensource-Team03
```

### 2. 환경 변수 설정

`omos/` 디렉토리에 `.env` 파일 생성 (아래 [환경 변수 설정](#환경-변수-설정) 참고)

### 3. 인프라 실행 (Docker Compose)

```bash
cd omos
docker-compose up -d
```

PostgreSQL, pgvector, Langfuse가 실행됩니다.

### 4. 백엔드 실행

```bash
cd omos
./gradlew bootRun
```

`http://localhost:8080` 에서 실행됩니다.  
Swagger UI: `http://localhost:8080/swagger-ui.html`

### 5. 프론트엔드 실행

```bash
cd front
npm install
npm run dev
```

`http://localhost:3000` 에서 실행됩니다.

### 6. Chrome 익스텐션 설치

1. Chrome에서 `chrome://extensions` 접속
2. 우측 상단 **개발자 모드** 활성화
3. **압축 해제된 확장 프로그램 로드** 클릭
4. `extension/` 폴더 선택

---

## 환경 변수 설정

`omos/.env` 파일을 생성하고 아래 값을 입력합니다.

```env
# GitHub API
GITHUB_TOKEN=ghp_xxx
GITHUB_CLIENT_ID=xxx
GITHUB_CLIENT_SECRET=xxx

# AI API
GLM_API_KEY=xxx          # Grepp Sandbox GLM API Key
GEMINI_API_KEY=xxx       # Google Gemini API Key

# 데이터베이스
DB_NAME=oh_my_opensource
DB_USER=postgres
DB_PASSWORD=xxx
DB_PORT=5432

# JWT
JWT_SECRET=xxx           # 최소 32자 이상 랜덤 문자열

# OAuth2 리다이렉트
OAUTH2_REDIRECT_URI=http://localhost:3000/oauth/callback

# Langfuse LLM 모니터링
LANGFUSE_HOST=http://localhost:3001
LANGFUSE_PUBLIC_KEY=xxx
LANGFUSE_SECRET_KEY=xxx
LANGFUSE_NEXTAUTH_SECRET=xxx
LANGFUSE_SALT=xxx
```

`front/` 디렉토리에 `.env.local` 파일 생성:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_BACKEND_OAUTH_URL=http://localhost:8080/oauth2/authorization/github
```

---

## API 명세

전체 API 문서는 Swagger UI에서 확인할 수 있습니다: `http://localhost:8080/swagger-ui.html`

### 인증

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| GET | `/oauth2/authorization/github` | GitHub OAuth 로그인 시작 |

### 사용자 (`/api/v1/users`)

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| GET | `/me` | 현재 로그인 사용자 정보 조회 |
| GET | `/{githubId}` | 특정 사용자 정보 조회 |
| PATCH | `/me` | 프로필 업데이트 (name, email) |
| POST | `/me/vector` | GitHub 기반 프로필 벡터 생성/업데이트 |

### 이슈 추천 (`/api/v1/issues`)

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| GET | `/recommend` | AI 기반 맞춤형 이슈 추천 (3개) |
| GET | `/recommend/history` | 사용자의 추천 이력 조회 |
| GET | `/{issueId}/guide` | 이슈 코드 수정 가이드 (AI 분석) |
| GET | `/{issueId}/pseudo` | 이슈 의사 코드 (AI 분석) |
| POST | `/crawl/search?q={query}` | GitHub 검색으로 이슈 크롤링 |
| GET | `/lookup?repo={repo}&number={number}` | GitHub 이슈 번호로 OMOS ID 조회 |

### PR 초안 (`/api/v1/pr`)

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| POST | `/` | PR 초안 생성 (diff 분석 기반) |
| GET | `/{id}` | PR 초안 상세 조회 |
| GET | `/history?page={page}&size={size}` | PR 생성 이력 (페이지네이션) |
| PATCH | `/{id}` | PR 초안 수정 (title, body) |
| POST | `/{id}/translate` | PR 영어 번역 |
| DELETE | `/{id}` | PR 초안 삭제 |

---

## AI 기능 상세

### PR 초안 생성 프롬프트
- **모델**: GLM-4.5 (OpenAI API 호환)
- **입력**: code diff, CONTRIBUTING.md 내용, 기존 PR 예시, 이슈 정보
- **출력**: JSON `{ title, body }`
- **특징**: 커밋 컨벤션(feat/fix/refactor 등) 강제, 한국어 작성, Markdown 포맷 유지

### 이슈 코드 분석 프롬프트
- **모델**: GLM-4.5
- **2단계 처리**:
  1. 이슈 해결에 필요한 파일 최대 10개 선별
  2. 선별된 파일 분석 → 가이드, 의사 코드, 사이드 이펙트 생성
- **출력**: JSON `{ guideline, pseudoCode, sideEffects }`
- **캐싱**: 사용자별 캐시 → 이슈별 캐시 → 신규 AI 분석 순서

### 번역 프롬프트
- **모델**: GLM-4.5
- **입력**: 한국어 PR 제목 + 본문
- **출력**: JSON `{ title, body }`
- **규칙**: Markdown 헤더 레벨 유지, 클래스명/메서드명/파일 경로는 번역 제외

### 임베딩 및 벡터 검색
- **모델**: Gemini Embedding 2 (3072차원)
- **저장소**: PostgreSQL pgvector (코사인 유사도 검색)
- **대상**: 사용자 프로필 벡터, 이슈 내용 벡터

### LLM 모니터링 (Langfuse)
- 프롬프트 버전별 응답 시간, 토큰 사용량, 점수 추적
- `http://localhost:3001` 에서 대시보드 확인 가능

---

## 데이터베이스 스키마

```
users
├── id, github_id (UK)
├── name, email
├── profile_vector (vector 3072)     ← Gemini Embedding
├── primary_languages (jsonb)
└── vector_updated_at

issues
├── id, repo_full_name, issue_number
├── title, content
├── labels (jsonb)
├── issue_vector (vector 3072)       ← Gemini Embedding
└── status (OPEN/CLOSED)

analysis_results
├── id, issue_id (FK, UK)
├── file_paths, guideline, pseudo_code, side_effects

user_recommended_issues
├── id, user_id (FK), issue_id (FK)
├── summary (추천 사유)
└── score (유사도 점수)

pr_draft
├── id, user_id (FK), issue_id (FK, nullable)
├── diff_content
├── pr_title, pr_body
└── base_branch, head_branch, fork_owner
```

---

## 팀 소개

**프로그래머스 인턴십 1기 - Team03**

| 역할 | 이름 | 담당 |
|------|------|------|
| 팀장 | 김연수 | GitHub 프로필 분석, User Profiling AI |
| 팀원 | 유재원 | 이슈 수집, Semantic Matcher AI |
| 팀원 | 류재원 | 코드 수정 가이드, Context Analyzer AI |
| 팀원 | 김민지 | PR 생성, PR Architect AI |

---

## License

This project is for educational purposes as part of the Programmers Internship Program.
