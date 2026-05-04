# Langfuse 설정 가이드

AI 호출의 응답시간·토큰 사용량·품질 점수를 추적하기 위해 Langfuse를 사용합니다.

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

아래는 현재 PR 파트 기준입니다. 각 파트에서 연동하면 항목이 추가됩니다.

| 항목 | 설명 |
|---|---|
| `pr-draft-vN` | PR 초안 생성 — 응답시간, 토큰 수, 프롬프트/응답 원문 |
| `pr-translate-vN` | 번역 — 응답시간, 토큰 수 |
| `quality` score | LLM-as-judge가 채점한 0~10점 품질 점수 (PR 초안 생성에만 적용) |

## 다른 AI 파트에서 Langfuse 연동하기

`LangfuseClient`는 전역 빈으로 등록되어 있어서 어디서든 주입받아 사용할 수 있습니다.

### 기본 사용법 — AI 호출 기록

```kotlin
@Component
class MyAiClient(
    private val chatModel: ChatModel,
    private val langfuseClient: LangfuseClient
) {
    companion object {
        // 프롬프트를 수정할 때마다 버전을 올려야 Langfuse에서 버전별 비교가 가능합니다.
        private const val GENERATION_NAME = "my-feature-v1"
    }

    fun callAi(prompt: String): String {
        val startTime = Instant.now()
        val response = chatModel.call(prompt)
        val endTime = Instant.now()

        langfuseClient.recordGeneration(
            name = GENERATION_NAME,
            input = prompt,
            output = response,
            startTime = startTime,
            endTime = endTime,
        )

        return response
    }
}
```

### 토큰 수도 같이 기록하고 싶다면

`ChatModel.call(Prompt(...))`으로 호출하면 usage 메타데이터에서 토큰 수를 꺼낼 수 있습니다.

```kotlin
val chatResponse = chatModel.call(Prompt(prompt))
val usage = chatResponse.metadata.usage

langfuseClient.recordGeneration(
    name = GENERATION_NAME,
    input = prompt,
    output = chatResponse.result.output.text ?: "",
    startTime = startTime,
    endTime = endTime,
    inputTokens = usage?.promptTokens?.toInt(),
    outputTokens = usage?.completionTokens?.toInt(),
)
```

### 품질 점수도 기록하고 싶다면

`recordGeneration()`이 반환하는 `traceId`로 점수를 붙일 수 있습니다.

```kotlin
val traceId = langfuseClient.recordGeneration(...)

if (traceId != null) {
    langfuseClient.recordScore(traceId, score = 8.5, reason = "채점 근거")
}
```

> Langfuse 키가 설정되지 않은 환경에서는 `recordGeneration()`이 `null`을 반환하고 기록을 건너뜁니다. 메인 로직에는 영향이 없습니다.

---

## 프롬프트 버전 관리

프롬프트를 수정할 때 `GENERATION_NAME` 상수의 버전을 함께 올려야 Langfuse에서 버전별 성능 비교가 가능합니다.

**네이밍 컨벤션:** `{기능명}-v{N}`

PR 파트의 경우 `SpringAiClient.kt`에서 이렇게 관리합니다:

```kotlin
companion object {
    private const val GENERATION_PR_DRAFT = "pr-draft-v2"  // 프롬프트 수정 시 v3, v4... 으로 올릴 것
    private const val GENERATION_TRANSLATE = "pr-translate-v1"
}
```

버전을 올리지 않으면 Langfuse에서 이전 버전과 현재 버전의 데이터가 섞여 비교가 어렵습니다.
