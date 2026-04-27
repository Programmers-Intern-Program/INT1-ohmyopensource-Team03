package com.back.omos.domain.issue.service

import com.back.omos.domain.issue.dto.CreateIssueReq
import com.back.omos.domain.issue.dto.IssueInfoRes
import com.back.omos.domain.issue.dto.RecommendIssueRes
import com.back.omos.domain.issue.dto.UpdateIssueReq
import com.back.omos.domain.issue.entity.Issue
import com.back.omos.domain.issue.github.GithubClient
import com.back.omos.domain.issue.repository.IssueRepository
import com.back.omos.global.ai.GeminiEmbeddingModel
import com.back.omos.global.exception.errorCode.IssueErrorCode
import com.back.omos.global.exception.exceptions.IssueException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.ai.document.Document

/**
 * 이슈 관련 비즈니스 로직을 처리하는 서비스 구현체입니다.
 *
 * @author 유재원
 * @since 2026-04-22
 */
@Service
class IssueServiceImpl(
    private val issueRepository: IssueRepository,
    private val githubClient: GithubClient,
    private val embeddingModel: GeminiEmbeddingModel
) : IssueService {

    @Transactional
    override fun createIssue(request: CreateIssueReq): IssueInfoRes {
        if(issueRepository.existsByRepoFullNameAndIssueNumber(request.repoFullName, request.issueNumber)){
            throw IssueException(IssueErrorCode.ISSUE_ALREADY_EXIST)
        }

        val issue = issueRepository.save(request.toEntity())
        return IssueInfoRes.from(issue)
    }

    @Transactional(readOnly = true)
    override fun getIssue(issueId: Long): IssueInfoRes {
        val issue = issueRepository.findByIdOrNull(issueId)
            ?: throw IssueException(IssueErrorCode.ISSUE_NOT_FOUND)
        return IssueInfoRes.from(issue)
    }

    @Transactional
    override fun updateIssue(issueId: Long, request: UpdateIssueReq) {
        val issue = issueRepository.findByIdOrNull(issueId)
            ?: throw IssueException(IssueErrorCode.ISSUE_NOT_FOUND)

        issue.title = request.title
        issue.content = request.content
        issue.labels = request.labels
        issue.status = Issue.IssueStatus.valueOf(request.status)

        val updatedIssue = issueRepository.save(issue)
    }

    @Transactional
    override fun deleteIssue(issueId: Long) {
        val issue = issueRepository.findByIdOrNull(issueId)
            ?: throw IssueException(IssueErrorCode.ISSUE_NOT_FOUND)
        issueRepository.delete(issue);
    }

    override fun recommendIssues(userId: Long, repositoryId: Long, limit: Int): List<RecommendIssueRes> {
        TODO("추천 로직 구현 필요")
    }


    @Transactional
    override fun crawlAndSaveByQuery(query: String): List<Issue> {
        val githubIssues = githubClient.searchIssues(query)

        return githubIssues.map { dto ->
            val fullName = dto.repositoryUrl.substringAfter("repos/")

            // 이미 존재하는 이슈인지 확인
            val existingIssue = issueRepository.findByRepoFullNameAndIssueNumber(fullName, dto.number)

            if (existingIssue != null) {
                existingIssue // 이미 있다면 그대로 반환
            } else {
                //  신규 이슈라면 임베딩용 텍스트 조립
                val textToEmbed = buildString {
                    appendLine("GitHub Issue Information")
                    appendLine("Title: ${dto.title}")
                    appendLine("Labels: ${dto.labels.joinToString { it.name }}")
                    if (!dto.body.isNullOrBlank()) {
                        // 본문 1000자 제한
                        appendLine("Content: ${dto.body.take(1000)}")
                    }
                }

                // AI 임베딩 모델 호출
                val floatArray = embeddingModel.embed(Document(textToEmbed))

                // FloatArray -> DoubleArray 변환
                val vector = if (floatArray.isNotEmpty()) {
                    floatArray.map { it.toDouble() }.toDoubleArray()
                } else {
                    null
                }

                // DB 저장
                issueRepository.save(
                    Issue(
                        repoFullName = fullName,
                        issueNumber = dto.number,
                        title = dto.title,
                        content = dto.body,
                        labels = dto.labels.map { it.name },
                        issueVector = vector,
                        status = Issue.IssueStatus.OPEN
                    )
                )
            }
        }
    }

}