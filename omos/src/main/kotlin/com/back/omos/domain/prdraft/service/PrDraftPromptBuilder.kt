package com.back.omos.domain.prdraft.service

import com.back.omos.domain.prdraft.github.GitHubPrRes
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

/**
 * PR 초안 생성을 위한 AI 프롬프트를 구성하는 클래스입니다.
 *
 * <p>
 * 사용자가 전달한 diff 정보를 기반으로,
 * AI에게 PR 제목과 본문 생성을 요청하기 위한 입력 문자열(prompt)을 생성합니다.
 * </p>
 *
 * <p>
 * 생성된 프롬프트는 다음과 같은 역할을 수행합니다:
 * <ul>
 *   <li>AI의 역할을 "PR 작성 도우미"로 명확히 지정</li>
 *   <li>코드 변경 내용(diff)을 전달하여 변경 의도를 이해하도록 유도</li>
 *   <li>응답 형식을 JSON으로 강제하여 파싱 가능하도록 제한</li>
 * </ul>
 * </p>
 *
 * <p><b>동작 방식:</b><br>
 * {@link CreatePrReq}로부터 diff 내용을 추출하여,
 * PR 제목과 본문 생성을 위한 프롬프트 문자열을 구성합니다.
 * </p>
 *
 * <p><b>빈 관리:</b><br>
 * {@link org.springframework.stereotype.Component}로 등록되어
 * 서비스 계층에서 주입받아 사용됩니다.
 * </p>
 *
 * @author 5h6vm
 * @since 2026-04-22
 */
@Component
class PrDraftPromptBuilder {

    private val defaultTemplate = ClassPathResource("templates/pr-default-template.md")
        .inputStream.bufferedReader().readText()


    companion object {
        // 프롬프트 내용을 변경할 때 이 버전도 함께 올려야 Langfuse에서 버전별 성능 비교가 가능합니다.
        const val PROMPT_VERSION = "v7.3"
        const val PROMPT_VERSION_TRANSLATE = "v2.2"
    }


    /**
     * PR 초안 생성을 위한 프롬프트 문자열을 구성합니다.
     *
     * @param diffContent GitHub Compare API로 가져온 diff 내용
     * @param contributing CONTRIBUTING.md 내용 (없으면 null)
     * @param prs 참고용 기존 병합 PR 목록
     * @param issueTitle 연결된 이슈 제목 (없으면 null)
     * @param issueContent 연결된 이슈 본문 (없으면 null)
     * @param prTemplate 레포지토리의 PR 템플릿 내용 (없으면 null)
     * @return AI에 전달할 프롬프트 문자열
     */
    fun build(
        diffContent: String,
        contributing: String?,
        prs: List<GitHubPrRes>,
        issueTitle: String? = null,
        issueContent: String? = null,
        issueGuideline: String? = null,
        issueNumber: Long? = null,
        prTemplate: String? = null
    ): String {
        val contextSection = when {
            prTemplate != null -> {
                val contributingPart = if (contributing != null) "\n[CONTRIBUTING.md]\n$contributing\n" else ""
                "$contributingPart\n[PR 템플릿]\n$prTemplate\n"
            }
            contributing != null -> {
                "\n[CONTRIBUTING.md]\n$contributing\n\n[기본 템플릿]\n$defaultTemplate\n"
            }
            prs.isNotEmpty() -> {
                val examples = prs.joinToString("\n---\n") { "제목: ${it.title}\n본문: ${it.body}" }
                "\n[기존 PR 예시]\n$examples\n\n[기본 템플릿]\n$defaultTemplate\n"
            }
            else -> "\n[기본 템플릿]\n$defaultTemplate\n"
        }

        val structureInstruction = when {
            prTemplate != null ->
                "[PR 템플릿]의 섹션 구조와 체크박스(- [ ])를 그대로 유지하세요. HTML 주석(<!-- -->)은 모두 삭제하세요. 변경 이유는 이슈 내용을 우선 참고하고, 이슈에도 없으면 '(작성 필요)'로 남겨두세요."
            contributing != null ->
                "CONTRIBUTING.md의 PR 본문 형식을 따르세요. PR 형식이 없거나 모호한 경우 '변경 이유(Why)', '수정 내용(What)'의 2단 구조로 작성하세요. 변경 이유는 이슈 내용을 우선 참고하고, 이슈에도 없으면 '(작성 필요)'로 남겨두세요."
            prs.isNotEmpty() ->
                "PR 예시에서 일관된 구조를 파악해 따르세요. 규칙이 모호한 경우 '변경 이유(Why)', '수정 내용(What)'의 2단 구조로 작성하세요. 변경 이유는 이슈 내용을 우선 참고하고, 이슈에도 없으면 '(작성 필요)'로 남겨두세요."
            else ->
                "본문은 '변경 이유(Why)', '수정 내용(What)'의 2단 구조로 작성하세요. 변경 이유는 이슈 내용을 우선 참고하고, 이슈에도 없으면 '(작성 필요)'로 남겨두세요."
        }

        val issueSection = if (issueTitle != null) {
            val numberLine = if (issueNumber != null) "번호: #$issueNumber\n" else ""
            "\n[이슈]\n${numberLine}제목: $issueTitle\n내용: ${issueContent ?: "(본문 없음)"}\n"
        } else ""

        val guidelineSection = issueGuideline
            ?.take(500)
            ?.let { "\n[분석 가이드라인]\n$it\n" }
            ?: ""

        return """
            [System Message]
            당신은 오픈소스 커뮤니티의 소통을 돕는 테크니컬 라이터입니다.
            수정된 코드와 맥락을 결합하여, 메인테이너가 한눈에 이해할 수 있는 전문적인 Pull Request 본문을 작성합니다.
            반드시 제목과 본문 모두 한국어로 작성하세요.

            [Input]
            $issueSection
            $guidelineSection
            $contextSection
            - 변경된 코드 내역:
            $diffContent

            [Instruction]
            1. 제목은 feat:, fix:, refactor:, chore:, docs:, test: 중 하나의 커밋 컨벤션을 따르고 50자 이내로 작성하세요.
            2. $structureInstruction
            3. 불필요한 미사여구는 배제하고, 엔지니어링 관점에서 간결하고 명확한 어조를 유지하세요.
            4. 타입 변경·메서드 시그니처·어노테이션 위치 등 기술적 세부사항이 있다면 본문에 명시하세요.
            5. 테스트 방법 섹션은 개발자가 직접 작성할 수 있도록 아래와 같이 placeholder로 남겨두세요.
               ## 테스트 방법
               <!-- 직접 작성 필요 -->
            6. 분석 가이드라인은 변경 맥락 파악을 위한 참고용입니다. PR은 실제 diff에 반영된 변경 사항만 기술하고, 가이드라인에만 언급된 내용은 포함하지 마세요.
            ${if (issueNumber != null) "7. 본문에 이미 close 항목이 있으면 `Closes #$issueNumber`로 채우고 중복 추가하지 마세요. 없으면 본문 마지막에 추가하세요." else ""}

            반드시 아래 JSON 형식으로만 응답하세요.
            {
              "title": "PR 제목",
              "body": "PR 본문"
            }
        """.trimIndent()
    }

    /**
     * 한국어 PR 제목과 본문을 영어로 번역하기 위한 프롬프트를 구성합니다.
     *
     * @param title 번역할 PR 제목 (한국어)
     * @param body 번역할 PR 본문 (한국어)
     * @return AI에 전달할 번역 프롬프트 문자열
     */
    fun buildTranslatePrompt(title: String, body: String): String {
        return """
            Translate the following Korean PR title and body into natural English.

            Rules:
            1. Preserve the exact markdown header level (## must stay ##, ### must stay ###).
            2. Translate section headers naturally: 변경 이유 → Why, 수정 내용 → Changes, 테스트 방법 → How to Test.
            3. Translate <!-- --> comment content to English (e.g., <!-- 직접 작성 필요 --> → <!-- Fill in manually -->).
            4. Do not translate class names, method names, annotations, or file paths.
            5. Write natural English, not a word-for-word literal translation.

            Return only the JSON below with no extra text.
            {
              "title": "translated title",
              "body": "translated body"
            }

            [Korean Title]
            $title

            [Korean Body]
            $body
        """.trimIndent()
    }
}
