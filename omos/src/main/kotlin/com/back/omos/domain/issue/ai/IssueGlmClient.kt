package com.back.omos.domain.issue.ai

import com.back.omos.domain.issue.dto.AIRecommendationResult
import com.back.omos.domain.issue.entity.Issue

/**
 * Generative AI 모델을 사용하여 오픈소스 이슈 추천 로직을 수행하는 클라이언트 인터페이스입니다.
 * <p>
 * 사용자 프로필과 벡터 검색으로 추출된 후보군 이슈들을 결합하여(Augmentation),
 * AI 모델에게 전달하고 가장 적합한 추천 항목과 그 근거를 생성(Generation)하는 역할을 담당합니다.
 *
 * <p><b>빈 관리:</b><br>
 * 해당 인터페이스의 구현체는 Spring Context에 의해 빈(Bean)으로 관리되며,
 * 비즈니스 로직 계층(Service)에서 주입받아 사용됩니다.
 *
 * <p><b>외부 모듈:</b><br>
 * Spring AI 모듈 및 외부 LLM(GLM, Gemini 등) API와 연동하여 자연어 처리 및 분석을 수행합니다.
 *
 * @author 유재원
 * @since 2026-04-27
 * @see com.back.omos.domain.recommend.client.IssueGlmClientImpl
 */
interface IssueGlmClient {
    /**
     * 유저 프로필과 유사도 기반으로 검색된 후보 이슈들을 분석하여
     * 최적의 이슈를 선정하고 추천 사유를 생성합니다.
     */
    fun generateRecommendationReasons(userProfile: String, candidateIssues: List<Issue>): List<AIRecommendationResult>
}