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
        const val PROMPT_VERSION = "v4"
    }


    /**
     * PR 초안 생성을 위한 프롬프트 문자열을 구성합니다.
     *
     * @param diffContent GitHub Compare API로 가져온 diff 내용
     * @param contributing CONTRIBUTING.md 내용 (없으면 null)
     * @param prs 참고용 기존 병합 PR 목록
     * @return AI에 전달할 프롬프트 문자열
     */
    fun build(diffContent: String, contributing: String?, prs: List<GitHubPrRes>): String {
        val contextSection = when {
            contributing != null -> "\n[CONTRIBUTING.md]\n$contributing\n"
            prs.isNotEmpty() -> {
                val examples = prs.joinToString("\n---\n") { "제목: ${it.title}\n본문: ${it.body}" }
                "\n[기존 PR 예시 - PR들의 톤앤매너가 일관적이라면 이를 참고하여 작성하고, 일관적이지 않다면 아래 기본 템플릿 형식에 맞춰 작성하세요]\n$examples\n\n[기본 템플릿]\n$defaultTemplate\n"
            }
            else -> "\n[아래 기본 템플릿 형식에 맞춰 작성하세요]\n$defaultTemplate\n"
        }

        return """
            [System Message]
            당신은 오픈소스 커뮤니티의 소통을 돕는 테크니컬 라이터입니다.
            수정된 코드와 맥락을 결합하여, 메인테이너가 한눈에 이해할 수 있는 전문적인 Pull Request 본문을 작성합니다.
            반드시 제목과 본문 모두 한국어로 작성하세요.

            [Input]
            $contextSection
            - 변경된 코드 내역:
            $diffContent

            [Instruction]
            1. 제목은 feat:, fix:, refactor:, chore:, docs:, test: 중 하나의 커밋 컨벤션을 따르고 50자 이내로 작성하세요.
            2. 본문은 '변경 이유(Why)', '수정 내용(What)'의 2단 구조로 작성하세요. 변경 이유가 diff에서 추론되지 않으면 '(작성 필요)'로 남겨두세요.
            3. 불필요한 미사여구는 배제하고, 엔지니어링 관점에서 간결하고 명확한 어조를 유지하세요.
            4. 타입 변경·메서드 시그니처·어노테이션 위치 등 기술적 세부사항이 있다면 본문에 명시하세요.
            5. 테스트 방법 섹션은 개발자가 직접 작성할 수 있도록 아래와 같이 placeholder로 남겨두세요.
               ## 테스트 방법
               <!-- 직접 작성 필요 -->

            반드시 아래 JSON 형식으로만 응답하세요.
            {
              "title": "PR 제목",
              "body": "PR 본문"
            }
        """.trimIndent()
    }
}
