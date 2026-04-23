package com.back.omos.domain.issue.dto

import com.back.omos.domain.issue.entity.Issue

/**
 * 이슈 생성 요청 정보를 담는 DTO입니다.
 *
 * <p>
 * 새로운 이슈를 생성하기 위해 필요한 정보를 담고 있습니다.
 * @property repositoryId 이슈를 생성할 레포지토리의 식별자
 * @property issueNumber 레포지토리 내에서의 이슈 번호
 * @property title 이슈 제목
 * @property content 이슈 본문
 * @property labels 이슈에 부여할 라벨들
 * @property status 이슈 상태 (기본값: OPEN)
 *
 * @author 유재원
 * @since 2026-04-22
 */
data class CreateIssueReq(
    val repositoryId: Long,
    val issueNumber: Long,
    val title: String,
    val content: String? = null,
    val labels: List<String>? = null,
    val status: String = "OPEN"  // "OPEN" or "CLOSED"
) {
    /**
     * [CreateIssueReq] DTO를 [Issue] 엔티티로 변환합니다.
     *
     * @return 생성된 Issue 엔티티
     */
    fun toEntity(): Issue {
        return Issue(
            repositoryId = repositoryId,
            issueNumber = issueNumber,
            title = title,
            content = content,
            labels = labels,
            status = Issue.IssueStatus.valueOf(status)
        )
    }
}