package com.back.omos.domain.issue.dto

/**
 * 추천된 이슈 정보를 응답으로 전달하는 DTO입니다.
 *
 * <p>
 * AI 기반의 유사도 검색을 통해 추천된 이슈들의 목록을 반환합니다.
 *
 * @property issueId 이슈의 시스템 내부 식별자
 * @property issueNumber 레포지토리 내에서의 이슈 번호
 * @property title 이슈 제목
 * @property summary 이슈 본문의 요약본 (AI 생성)
 * @property score 추천 점수 (0.0 ~ 1.0)
 * @property labels 이슈에 부여된 라벨들
 * @property status 이슈 상태 (OPEN/CLOSED)
 *
 * @author 유재원
 * @since 2026-04-22
 */
data class RecommendIssueRes(
    val issueId: Long,
    val issueNumber: Int,
    val title: String,
    val summary: String,
    val score: Float,
    val labels: List<String>?,
    val status: String  // "OPEN" or "CLOSED"
)