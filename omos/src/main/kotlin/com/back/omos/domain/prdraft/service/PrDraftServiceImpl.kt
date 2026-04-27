package com.back.omos.domain.prdraft.service

import com.back.omos.domain.issue.repository.IssueRepository
import com.back.omos.domain.prdraft.dto.CreatePrReq
import com.back.omos.domain.prdraft.dto.PrHistoryRes
import com.back.omos.domain.prdraft.dto.PrInfoRes
import com.back.omos.domain.prdraft.ai.AiClient
import com.back.omos.domain.prdraft.entity.PrDraft
import com.back.omos.domain.prdraft.github.GitHubClient
import com.back.omos.domain.prdraft.repository.PrDraftRepository
import com.back.omos.domain.user.repository.UserRepository
import com.back.omos.global.exception.errorCode.AuthErrorCode
import com.back.omos.global.exception.errorCode.IssueErrorCode
import com.back.omos.global.exception.exceptions.AuthException
import com.back.omos.global.exception.exceptions.IssueException
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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
    private val userRepository: UserRepository,
    private val issueRepository: IssueRepository,
    private val prDraftPromptBuilder: PrDraftPromptBuilder,
    private val aiClient: AiClient,
    private val gitHubClient: GitHubClient
) : PrDraftService {

    /**
     * diff 내용과 이슈 정보를 기반으로 AI를 호출하여 PR 초안을 생성하고 저장합니다.
     *
     * @param githubId 요청한 사용자의 GitHub ID
     * @param request PR 생성 요청 DTO (issueId, diffContent 포함)
     * @return 생성된 PR 제목, 본문, GitHub URL
     * @throws AuthException 존재하지 않는 githubId인 경우
     * @throws IssueException 존재하지 않는 issueId인 경우
     */
    override fun create(githubId: String, request: CreatePrReq): PrInfoRes {
        // 정보 조회
        val user = userRepository.findByGithubId(githubId)
            .orElseThrow { AuthException(AuthErrorCode.USER_NOT_FOUND) }
        val issue = issueRepository.findById(request.issueId)
            .orElseThrow { IssueException(IssueErrorCode.ISSUE_NOT_FOUND) }

        // prompt에게 줄 pr 형식 정보
        val contributing = gitHubClient.fetchContributing(issue.repoFullName)
        val prs = if (contributing == null) gitHubClient.fetchMergedPrs(issue.repoFullName) else emptyList()

        // prompt 작성
        val prompt = prDraftPromptBuilder.build(request, contributing, prs)

        // AI 호출
        val aiResult = aiClient.generatePrDraft(prompt)

        val githubUrl = buildGithubUrl(issue.repoFullName, aiResult.title, aiResult.body)

        prDraftRepository.save(PrDraft(
            user = user,
            issue = issue,
            diffContent = request.diffContent,
            prTitle = aiResult.title,
            prBody = aiResult.body
        ))

        return PrInfoRes(
            title = aiResult.title,
            body = aiResult.body,
            githubUrl = githubUrl
        )
    }

    /**
     * 사용자가 생성한 PR 초안 목록을 최신순으로 조회합니다.
     *
     * @param githubId 조회할 사용자의 GitHub ID
     * @return PR 초안 목록 (최신순)
     */
    override fun getHistory(githubId: String): List<PrHistoryRes> {
        return prDraftRepository.findAllWithIssueByUserGithubId(githubId)
            .map { PrHistoryRes.from(it) }
    }

    private fun buildGithubUrl(fullName: String, title: String, body: String): String {
        val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8).replace("+", "%20")
        val encodedBody = URLEncoder.encode(body, StandardCharsets.UTF_8).replace("+", "%20")
        return "https://github.com/$fullName/compare?quick_pull=1&title=$encodedTitle&body=$encodedBody"
    }
}
