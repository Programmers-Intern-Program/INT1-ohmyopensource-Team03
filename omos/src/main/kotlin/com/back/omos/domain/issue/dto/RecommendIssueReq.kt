package com.back.omos.domain.issue.dto

/**
 * 사용자 맞춤 이슈 추천 요청 정보를 담는 DTO입니다.
 *
 * <p>
 * 특정 레포지토리에서 사용자에게 맞춤 이슈를 추천받기 위해 사용됩니다.
 *
 * @property userId 사용자 고유 식별자 (추천 알고리즘에서 사용)
 * @property repositoryId 이슈를 추천받을 레포지토리 ID
 * @property limit 추천받을 이슈 개수 (기본값: 5)
 *
 * @author 유재원
 * @since 2026-04-22
 */
data class RecommendIssueReq(
    val userId: Long,
    val repoFullName: String,
    val limit: Int = 5
)