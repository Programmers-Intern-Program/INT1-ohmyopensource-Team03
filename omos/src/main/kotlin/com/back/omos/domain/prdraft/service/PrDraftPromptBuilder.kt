package com.back.omos.domain.prdraft.service

import com.back.omos.domain.prdraft.dto.CreatePrReq
import com.back.omos.domain.prdraft.github.GitHubPrRes
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

    fun build(req: CreatePrReq, contributing: String?, prs: List<GitHubPrRes>): String {
        val contextSection = when {
            contributing != null -> "\n[CONTRIBUTING.md]\n$contributing\n"
            prs.isNotEmpty() -> {
                val examples = prs.joinToString("\n---\n") { "제목: ${it.title}\n본문: ${it.body}" }
                "\n[기존 PR 예시 - 아래 PR들의 톤앤매너에 맞춰 작성하세요]\n$examples\n"
            }
            else -> "\n[작성 가이드가 없으므로 일반적인 오픈소스 PR 형식으로 작성하세요]\n"
        }

        return """
            당신은 오픈소스 프로젝트의 PR 초안 작성 도우미입니다.
            아래 diff를 바탕으로 PR 제목과 본문을 작성하세요.
            $contextSection
            [Diff]
            ${req.diffContent}

            반드시 아래 JSON 형식으로만 응답하세요.
            {
              "title": "PR 제목",
              "body": "PR 본문"
            }
        """.trimIndent()
    }
}