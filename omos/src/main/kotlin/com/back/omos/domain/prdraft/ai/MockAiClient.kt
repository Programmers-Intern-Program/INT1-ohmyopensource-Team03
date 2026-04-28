package com.back.omos.domain.prdraft.ai

import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * AI 연동 전 전체 PR 초안 생성 흐름을 테스트하기 위한 Mock 구현체입니다.
 *
 * <p>
 * 실제 AI 호출 대신 고정된 PR 제목과 본문을 반환하여,
 * Controller → Service → PromptBuilder → AiClient → Response 흐름이
 * 정상적으로 동작하는지 검증하는 용도로 사용됩니다.
 * </p>
 *
 * @author 5h6vm
 * @since 2026-04-22
 */
@Component
@Profile("test")
@Primary
class MockAiClient : AiClient {

    override fun generatePrDraft(prompt: String): AiPrResult {
        return AiPrResult(
            title = "fix: mock PR 제목",
            body = """
                ## 변경 사항
                - diff 내용을 바탕으로 PR 초안을 생성했습니다.

                ## 테스트 방법
                - 관련 기능을 직접 확인했습니다.

                ## 관련 이슈
                - close #123
            """.trimIndent()
        )
    }

    override fun translate(title: String, body: String): AiPrResult {
        return AiPrResult(
            title = "fix: mock PR title",
            body = """
                ## Changes
                - Generated PR draft based on diff content.

                ## How to Test
                - Verified the related feature directly.

                ## Related Issue
                - close #123
            """.trimIndent()
        )
    }
}
