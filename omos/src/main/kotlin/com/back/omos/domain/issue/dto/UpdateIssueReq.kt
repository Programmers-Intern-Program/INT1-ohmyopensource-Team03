package com.back.omos.domain.issue.dto

/**
 * 이슈 수정 요청 정보를 담는 DTO입니다.
 *
 * 식별자인 repositoryId와 issueNumber는 제외하고,
 * 변경 가능한 정보(제목, 본문, 라벨, 상태)만 받습니다.
 *
 * @author 유재원
 * @since 2026-04-22
 */
data class UpdateIssueReq(
    val title: String,
    val content: String? = null,
    val labels: List<String>? = null,
    val status: String // "OPEN" or "CLOSED"
)