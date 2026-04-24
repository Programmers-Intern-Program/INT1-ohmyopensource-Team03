package com.back.omos.domain.prdraft.service

import com.back.omos.domain.issue.repository.IssueRepository
import com.back.omos.domain.prdraft.dto.CreatePrReq
import com.back.omos.domain.prdraft.dto.PrInfoRes
import com.back.omos.domain.prdraft.ai.AiClient
import com.back.omos.domain.prdraft.github.GitHubClient
import com.back.omos.domain.prdraft.repository.PrDraftRepository
import com.back.omos.domain.repo.repository.RepoRepository
import com.back.omos.global.exception.errorCode.IssueErrorCode
import com.back.omos.global.exception.errorCode.RepoErrorCode
import com.back.omos.global.exception.exceptions.IssueException
import com.back.omos.global.exception.exceptions.RepoException
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service

/**
 * PR 생성 기능의 구현체입니다.
 *
 * <p>
 * diffContent와 Issue 정보를 기반으로 AI를 호출하여
 * PR 제목과 본문을 생성하는 로직을 담당합니다.
 *
 * <p><b>상속 정보:</b><br>
 * {@link PrDraftService}를 구현합니다.
 *
 * <p><b>외부 모듈:</b><br>
 * AI 모델(GLM 등)을 활용하여 PR 내용을 생성합니다.
 *
 * @author 5h6vm
 * @since 2026-04-22
 * @see PrDraftService
 */
@Service
class PrDraftServiceImpl(
    private val prDraftRepository: PrDraftRepository,
    private val issueRepository: IssueRepository,
    private val repoRepository: RepoRepository,
    private val prDraftPromptBuilder: PrDraftPromptBuilder,
    private val aiClient: AiClient,
    private val gitHubClient: GitHubClient
) : PrDraftService {

    @Transactional
    override fun create(request: CreatePrReq): PrInfoRes {
        // 이슈, 레포 조회
        val issue = issueRepository.findById(request.issueId)
            .orElseThrow { IssueException(IssueErrorCode.ISSUE_NOT_FOUND) }
        val repo = repoRepository.findById(request.repositoryId)
            .orElseThrow { RepoException(RepoErrorCode.REPO_NOT_FOUND) }

        // prompt에게 줄 pr 형식 정보
        val contributing = gitHubClient.fetchContributing(repo.fullName)
        val prs = if (contributing == null) gitHubClient.fetchMergedPrs(repo.fullName) else emptyList()

        // prompt 작성
        val prompt = prDraftPromptBuilder.build(request, contributing, prs)

        // AI 호출
        val aiResult = aiClient.generatePrDraft(prompt)

        return PrInfoRes(
            title = aiResult.title,
            body = aiResult.body
        )
    }
}
