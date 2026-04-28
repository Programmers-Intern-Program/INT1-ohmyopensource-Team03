package com.back.omos.domain.issue.dto

/**
 * AI 모델이 생성한 이슈 추천 결과를 구조화하여 담는 데이터 객체(DTO)입니다.
 * <p>
 * AI가 후보군 중 선정한 이슈의 핵심 식별 정보와
 * 기술 스택 기반의 분석 사유를 매핑하여 서비스 계층으로 전달하는 역할을 수행합니다.
 *
 * @author 유재원
 * @since 2026-04-28
 * @see com.back.omos.domain.recommend.client.IssueGlmClient
 */
data class AIRecommendationResult(
    val title: String,    // 이슈 제목
    val repoName: String, // 레포지토리 이름
    val reason: String    // AI가 생성한 추천 사유
)