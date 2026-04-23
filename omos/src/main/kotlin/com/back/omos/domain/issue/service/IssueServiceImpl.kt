package com.back.omos.domain.issue.service

import com.back.omos.domain.issue.dto.CreateIssueReq
import com.back.omos.domain.issue.dto.IssueInfoRes
import com.back.omos.domain.issue.dto.RecommendIssueRes
import com.back.omos.domain.issue.dto.UpdateIssueReq
import com.back.omos.domain.issue.entity.Issue
import com.back.omos.domain.issue.repository.IssueRepository
import com.back.omos.global.exception.errorCode.IssueErrorCode
import com.back.omos.global.exception.exceptions.IssueException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

/**
 * 이슈 관련 비즈니스 로직을 처리하는 서비스 구현체입니다.
 *
 * @author 유재원
 * @since 2026-04-22
 */
@Service
class IssueServiceImpl(
    private val issueRepository: IssueRepository
) : IssueService {

    override fun createIssue(request: CreateIssueReq): IssueInfoRes {
        if(issueRepository.existsByRepositoryIdAndIssueNumber(request.repositoryId, request.issueNumber)){
            throw IssueException(IssueErrorCode.ISSUE_ALREADY_EXIST)
        }

        val issue = issueRepository.save(request.toEntity())
        return IssueInfoRes.from(issue)
    }

    override fun getIssue(issueId: Long): IssueInfoRes {
        val issue = issueRepository.findByIdOrNull(issueId)
            ?: throw IssueException(IssueErrorCode.ISSUE_NOT_FOUND)
        return IssueInfoRes.from(issue)
    }

    override fun updateIssue(issueId: Long, request: UpdateIssueReq) {
        val issue = issueRepository.findByIdOrNull(issueId)
            ?: throw IssueException(IssueErrorCode.ISSUE_NOT_FOUND)

        issue.title = request.title
        issue.content = request.content
        issue.labels = request.labels
        issue.status = Issue.IssueStatus.valueOf(request.status)

        val updatedIssue = issueRepository.save(issue)
    }

    override fun deleteIssue(issueId: Long) {
        val issue = issueRepository.findByIdOrNull(issueId)
            ?: throw IssueException(IssueErrorCode.ISSUE_NOT_FOUND)
        issueRepository.delete(issue);
    }

    override fun recommendIssues(userId: Long, repositoryId: Long, limit: Int): List<RecommendIssueRes> {
        TODO("추천 로직 구현 필요")
    }

}