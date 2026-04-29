package com.back.omos.domain.issue.dto

import com.back.omos.domain.analysis.entity.UserAnalysisRequest
import com.back.omos.domain.issue.entity.UserRecommendedIssue

/**
 * 사용자의 이슈 추천 이력 응답 DTO입니다.
 *
 * [RecommendIssueRes]의 기본 필드에 분석 여부 정보를 추가로 포함합니다.
 * [isAnalyzed]가 true인 경우 [analysisResultId]를 통해 분석 결과를 조회할 수 있습니다.
 *
 * @property id 이슈의 시스템 내부 식별자
 * @property repoFullName 이슈가 속한 레포지토리의 전체 이름 (예: owner/repo)
 * @property issueNumber 레포지토리 내에서의 이슈 번호
 * @property title 이슈 제목
 * @property summary AI가 생성한 추천 사유
 * @property score 추천 점수 (0.0 ~ 1.0)
 * @property labels 이슈에 부여된 라벨 목록
 * @property status 이슈 상태 (OPEN/CLOSED)
 * @property isAnalyzed 해당 이슈에 대해 사용자가 분석을 완료했는지 여부
 * @property analysisResultId 완료된 분석 결과의 식별자, [isAnalyzed]가 false이면 null
 *
 * @author MintyU
 * @since 2026-04-29
 */
data class RecommendIssueHistoryRes(
    val id: Long,
    val repoFullName: String,
    val issueNumber: Long,
    val title: String,
    val summary: String,
    val score: Float,
    val labels: List<String>?,
    val status: String,
    val isAnalyzed: Boolean,
    val analysisResultId: Long?
) {
    companion object {

        /**
         * [UserRecommendedIssue]와 해당 이슈의 분석 요청 정보로 DTO를 생성합니다.
         *
         * @param recommended 추천 이력 엔티티
         * @param analysisRequest 해당 이슈에 대한 사용자의 분석 요청, 없으면 null
         */
        fun from(recommended: UserRecommendedIssue, analysisRequest: UserAnalysisRequest?): RecommendIssueHistoryRes {
            return RecommendIssueHistoryRes(
                id = recommended.issue.id ?: 0L,
                repoFullName = recommended.issue.repoFullName,
                issueNumber = recommended.issue.issueNumber,
                title = recommended.issue.title,
                summary = recommended.summary ?: "요약 정보 없음",
                score = 0.0f,
                labels = recommended.issue.labels,
                status = recommended.issue.status.name,
                isAnalyzed = analysisRequest != null,
                analysisResultId = analysisRequest?.analysisResult?.id
            )
        }
    }
}
