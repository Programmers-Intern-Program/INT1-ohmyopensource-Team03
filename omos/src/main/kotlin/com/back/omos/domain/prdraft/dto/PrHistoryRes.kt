package com.back.omos.domain.prdraft.dto

import com.back.omos.domain.prdraft.entity.PrDraft
import java.time.LocalDateTime

/**
 * PR 초안 목록 조회 결과를 응답으로 전달하는 DTO입니다.
 *
 * <p>
 * 사용자가 생성한 PR 초안의 제목, 본문 및 생성 시각을 포함합니다.
 *
 * @property id PR 초안 ID
 * @property repoFullName 이슈가 속한 레포지토리 전체 이름 (예: owner/repo)
 * @property issueTitle 연결된 이슈 제목
 * @property title AI가 생성한 PR 제목
 * @property body AI가 생성한 PR 본문
 * @property createdAt PR 초안 생성 시각
 *
 * @author 5h6vm
 * @since 2026-04-27
 */
data class PrHistoryRes(
    val id: Long,
    val repoFullName: String,
    val issueTitle: String,
    val title: String,
    val body: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(prDraft: PrDraft) = PrHistoryRes(
            id = prDraft.id!!,
            repoFullName = prDraft.issue.repoFullName,
            issueTitle = prDraft.issue.title,
            title = prDraft.prTitle,
            body = prDraft.prBody,
            createdAt = prDraft.createdAt
        )
    }
}
