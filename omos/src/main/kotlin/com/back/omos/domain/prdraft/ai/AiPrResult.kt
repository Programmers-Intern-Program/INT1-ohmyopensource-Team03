package com.back.omos.domain.prdraft.ai

/**
 * AI가 생성한 PR 초안 결과를 담는 내부 DTO입니다.
 *
 * <p>
 * PR 제목과 본문을 구조화하여 서비스 계층에서 사용할 수 있도록 표현합니다.
 * 외부 API 응답인 {@code PrInfoRes}로 변환되기 전, AI 응답을 내부적으로 다루기 위해 사용됩니다.
 * </p>
 *
 * @property title AI가 생성한 PR 제목
 * @property body AI가 생성한 PR 본문
 *
 * @author 5h6vm
 * @since 2026-04-22
 */
data class AiPrResult(
    val title: String,
    val body: String
)
