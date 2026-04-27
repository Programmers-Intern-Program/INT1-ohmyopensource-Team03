package com.back.omos.domain.analysis.ai

/**
 * GLM API 분석 결과를 담는 DTO입니다.
 *
 * @author Jaewon Ryu
 * @since 2026-04-25
 */
data class GlmAnalysisRes(
    val guideline: String,
    val pseudoCode: String,
    val sideEffects: String
)