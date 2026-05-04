# Langfuse 설정 가이드

PR 초안 생성 AI 호출의 응답시간·토큰 사용량·품질 점수를 추적하기 위해 Langfuse를 사용합니다.

## 로컬 실행

```bash
docker-compose up -d
```

Langfuse UI: http://localhost:3001

> `DB_USER`, `DB_PASSWORD`는 기존 앱 DB와 공유합니다.  
> `LANGFUSE_NEXTAUTH_SECRET`, `LANGFUSE_SALT`는 아무 랜덤 문자열이면 됩니다. (`openssl rand -base64 32`로 생성 가능)

## 키 발급

1. http://localhost:3001 에서 회원가입 후 프로젝트 생성
2. **Settings → API Keys → Create new API key**
3. 발급된 키를 환경변수에 추가

```env
LANGFUSE_PUBLIC_KEY=pk-lf-...
LANGFUSE_SECRET_KEY=sk-lf-...
# LANGFUSE_HOST는 기본값이 http://localhost:3001 이므로 로컬에선 생략 가능
```

키가 없어도 앱은 정상 동작합니다. 키가 없으면 AI 호출 기록만 건너뜁니다.

## 추적 항목

| 항목 | 설명 |
|---|---|
| `pr-draft-vN` | PR 초안 생성 — 응답시간, 토큰 수, 프롬프트/응답 원문 |
| `pr-translate-vN` | 번역 — 응답시간, 토큰 수 |
| `quality` score | LLM-as-judge가 채점한 0~10점 품질 점수 (PR 초안 생성에만 적용) |

## 프롬프트 버전 관리

프롬프트를 수정할 때 반드시 두 곳의 버전을 함께 올려야 Langfuse에서 버전별 성능 비교가 가능합니다.

| 파일 | 상수 |
|---|---|
| `PrDraftPromptBuilder.kt` | `PROMPT_VERSION` |
| `SpringAiClient.kt` | `GENERATION_PR_DRAFT` |

예: `v2` → `v3`으로 올릴 때 두 상수를 같이 변경합니다.
