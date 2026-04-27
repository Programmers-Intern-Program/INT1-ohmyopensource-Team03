package com.back.omos.domain.prdraft.ai

/**
 * PR 초안 생성을 위한 AI 호출 기능을 추상화한 인터페이스입니다.
 *
 * <p>
 * 서비스 계층은 이 인터페이스를 통해 AI 모델에 프롬프트를 전달하고,
 * 생성된 PR 제목 및 본문 결과를 전달받습니다.
 * </p>
 *
 * <p>
 * 지금은 Mock 구현체로 테스트할 수 있습니다.
 * 이후 실제 OpenAI 연동 구현체로 교체할 수 있도록 확장성을 고려한 구조입니다.
 * </p>
 *
 * @author 5h6vm
 * @since 2026-04-22
 */
interface AiClient {

    /**
     * 전달받은 프롬프트를 기반으로 PR 초안을 생성합니다.
     *
     * @param prompt AI에게 전달할 PR 생성 프롬프트
     * @return AI가 생성한 PR 제목 및 본문
     */
    fun generatePrDraft(prompt: String): AiPrResult
}
