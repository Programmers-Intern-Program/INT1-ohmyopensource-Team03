package com.back.omos.domain.issue.dto

import com.back.omos.domain.issue.entity.Issue

/**
 * 이슈 생성/조회 결과를 응답으로 전달하는 DTO입니다.
 *
 * <p>
 * 이슈의 기본 정보를 포함하며, 클라이언트에게 이슈 생성 결과나 상세 정보를 반환합니다.
 *
 * @property id 이슈의 시스템 내부 식별자
 * @property repositoryId 이슈가 속한 레포지토리의 식별자
 * @property issueNumber 레포지토리 내에서의 이슈 번호
 * @property title 이슈 제목
 * @property content 이슈 본문
 * @property labels 이슈에 부여된 라벨들
 * @property status 이슈 상태 (OPEN/CLOSED)
 *
 * @author 유재원
 * @since 2026-04-22
 */
data class IssueInfoRes(
    val id: Long,
    val repoFullName: String,
    val issueNumber: Long,
    val title: String,
    val content: String?,
    val labels: List<String>?,
    val status: Issue.IssueStatus  // "OPEN" or "CLOSED"
) {
    companion object {
        /**
         * [Issue] 엔티티를 [IssueInfoRes] DTO로 변환합니다.
         *
         * @param issue 변환할 Issue 엔티티
         * @return 변환된 IssueInfoRes DTO
         */
        fun from(issue: Issue): IssueInfoRes {
            return IssueInfoRes(
                id = issue.id!!,
                repoFullName = issue.repoFullName,
                issueNumber = issue.issueNumber,
                title = issue.title,
                content = issue.content,
                labels = issue.labels,
                status = issue.status
            )
        }
    }
}