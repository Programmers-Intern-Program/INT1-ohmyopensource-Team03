package com.back.omos.domain.prdraft.service

import com.back.omos.domain.issue.repository.IssueRepository
import com.back.omos.domain.prdraft.dto.CreatePrReq
import com.back.omos.domain.prdraft.dto.PrDetailRes
import com.back.omos.domain.prdraft.dto.PrHistoryRes
import com.back.omos.domain.prdraft.dto.PrInfoRes
import com.back.omos.domain.prdraft.dto.PrPageRes
import com.back.omos.domain.prdraft.dto.PrTranslateRes
import com.back.omos.domain.prdraft.dto.UpdatePrReq
import org.springframework.data.domain.Pageable
import com.back.omos.domain.prdraft.ai.AiClient
import com.back.omos.domain.prdraft.entity.PrDraft
import com.back.omos.domain.prdraft.github.GitHubClient
import com.back.omos.domain.prdraft.repository.PrDraftRepository
import com.back.omos.domain.user.repository.UserRepository
import com.back.omos.global.exception.errorCode.AuthErrorCode
import com.back.omos.global.exception.errorCode.IssueErrorCode
import com.back.omos.global.exception.errorCode.PrDraftErrorCode
import com.back.omos.global.exception.exceptions.AuthException
import com.back.omos.global.exception.exceptions.IssueException
import com.back.omos.global.exception.exceptions.PrDraftException
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * PR 초안 생성, 조회, 수정, 삭제 기능의 구현체입니다.
 *
 * <p>
 * diffContent와 Issue 정보를 기반으로 AI를 호출하여 PR 제목과 본문을 생성하고,
 * 생성된 초안의 단건/목록 조회, 수정 및 삭제 로직을 담당합니다.
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
     * GitHub Compare API로 diff를 가져와 AI를 호출하여 PR 초안을 생성하고 저장합니다.
     *
     * @param githubId 요청한 사용자의 GitHub ID
     * @param request PR 생성 요청 DTO (upstreamRepo, githubIssueNumber, baseBranch, headBranch 포함)
     * @return 생성된 PR 제목, 본문
     * @throws AuthException 존재하지 않는 githubId이거나 GitHub 로그인명이 없는 경우
     * @throws IssueException 존재하지 않는 issue인 경우
     */
    override fun create(githubId: String, request: CreatePrReq): PrInfoRes {
        // 정보 조회
        val user = userRepository.findByGithubId(githubId)
            .orElseThrow { AuthException(AuthErrorCode.USER_NOT_FOUND) }
        val issue = issueRepository.findByRepoFullNameAndIssueNumber(request.upstreamRepo, request.githubIssueNumber)
            ?: throw IssueException(IssueErrorCode.ISSUE_NOT_FOUND)

        // GitHub Compare API로 diff 가져오기 (forkOwner는 GitHub 로그인명)
        val forkOwner = user.name ?: throw AuthException(AuthErrorCode.USER_NOT_FOUND)
        val diffContent = gitHubClient.fetchDiff(request.upstreamRepo, request.baseBranch, forkOwner, request.headBranch)

        // prompt에게 줄 pr 형식 정보
        val contributing = gitHubClient.fetchContributing(request.upstreamRepo)
        val prs = if (contributing == null) gitHubClient.fetchMergedPrs(request.upstreamRepo) else emptyList()

        // prompt 작성
        val prompt = prDraftPromptBuilder.build(diffContent, contributing, prs)

        // AI 호출
        val aiResult = aiClient.generatePrDraft(prompt)

        val saved = prDraftRepository.save(PrDraft(
            user = user,
            issue = issue,
            diffContent = diffContent,
            prTitle = aiResult.title,
            prBody = aiResult.body,
            baseBranch = request.baseBranch,
            headBranch = request.headBranch,
            forkOwner = forkOwner
        ))

        return PrInfoRes(
            id = saved.id!!,
            title = aiResult.title,
            body = aiResult.body
        )
    }

    /**
     * PR 초안 단건을 조회합니다.
     *
     * @param githubId 요청한 사용자의 GitHub ID
     * @param prDraftId 조회할 PR 초안 ID
     * @return PR 초안 상세 정보 (diffContent 포함)
     * @throws PrDraftException 존재하지 않는 PR 초안이거나 본인 소유가 아닌 경우
     */
    override fun getOne(githubId: String, prDraftId: Long): PrDetailRes {
        val prDraft = prDraftRepository.findByIdWithIssueAndUserGithubId(prDraftId, githubId)
            ?: throw PrDraftException(PrDraftErrorCode.PR_DRAFT_NOT_FOUND)
        return PrDetailRes.from(prDraft)
    }

    /**
     * 사용자가 생성한 PR 초안 목록을 최신순으로 조회합니다.
     *
     * @param githubId 조회할 사용자의 GitHub ID
     * @return PR 초안 목록 (최신순)
     */
    override fun getHistory(githubId: String, pageable: Pageable): PrPageRes<PrHistoryRes> {
        return PrPageRes.from(
            prDraftRepository.findAllWithIssueByUserGithubId(githubId, pageable)
                .map { PrHistoryRes.from(it) }
        )
    }

    /**
     * PR 초안의 제목과 본문을 수정합니다.
     *
     * @param githubId 요청한 사용자의 GitHub ID
     * @param prDraftId 수정할 PR 초안 ID
     * @param request 수정할 제목과 본문 (null인 필드는 기존 값 유지)
     * @return 수정된 PR 초안 상세 정보
     * @throws PrDraftException 존재하지 않는 PR 초안이거나 본인 소유가 아닌 경우
     */
    override fun update(githubId: String, prDraftId: Long, request: UpdatePrReq): PrDetailRes {
        val prDraft = prDraftRepository.findByIdWithIssueAndUserGithubId(prDraftId, githubId)
            ?: throw PrDraftException(PrDraftErrorCode.PR_DRAFT_NOT_FOUND)

        prDraft.prTitle = request.title ?: prDraft.prTitle
        prDraft.prBody = request.body ?: prDraft.prBody

        return PrDetailRes.from(prDraftRepository.save(prDraft))
    }

    /**
     * PR 초안의 제목과 본문을 영어로 번역하고 GitHub PR 작성 URL을 반환합니다.
     *
     * @param githubId 요청한 사용자의 GitHub ID
     * @param prDraftId 번역할 PR 초안 ID
     * @return 영어 제목, 본문, GitHub URL
     * @throws PrDraftException 존재하지 않는 PR 초안이거나 본인 소유가 아닌 경우
     */
    override fun translate(githubId: String, prDraftId: Long): PrTranslateRes {
        // PR 초안 조회 (소유권 확인)
        val prDraft = prDraftRepository.findByIdWithIssueAndUserGithubId(prDraftId, githubId)
            ?: throw PrDraftException(PrDraftErrorCode.PR_DRAFT_NOT_FOUND)

        // AI로 제목/본문 영어 번역
        val translated = aiClient.translate(prDraft.prTitle, prDraft.prBody)

        // GitHub URL 빌드
        val githubUrl = buildGithubUrl(prDraft.issue.repoFullName, prDraft.baseBranch, prDraft.forkOwner, prDraft.headBranch, translated.title, translated.body)

        return PrTranslateRes(
            titleEn = translated.title,
            bodyEn = translated.body,
            githubUrl = githubUrl
        )
    }

    /**
     * 번역된 제목과 본문을 GitHub PR 작성 페이지 URL로 조합합니다.
     *
     * @param fullName 레포지토리 전체 이름 (예: owner/repo)
     * @param title URL에 삽입할 PR 제목
     * @param body URL에 삽입할 PR 본문
     * @return pre-fill된 GitHub PR 생성 URL
     */
    private fun buildGithubUrl(fullName: String, baseBranch: String, forkOwner: String, headBranch: String, title: String, body: String): String {
        val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8).replace("+", "%20")
        val encodedBody = URLEncoder.encode(body, StandardCharsets.UTF_8).replace("+", "%20")
        return "https://github.com/$fullName/compare/$baseBranch...$forkOwner:$headBranch?quick_pull=1&title=$encodedTitle&body=$encodedBody"
    }

    /**
     * PR 초안을 삭제합니다.
     *
     * @param githubId 요청한 사용자의 GitHub ID
     * @param prDraftId 삭제할 PR 초안 ID
     * @throws PrDraftException 존재하지 않는 PR 초안이거나 본인 소유가 아닌 경우
     */
    override fun delete(githubId: String, prDraftId: Long) {
        val prDraft = prDraftRepository.findByIdAndUserGithubId(prDraftId, githubId)
            ?: throw PrDraftException(PrDraftErrorCode.PR_DRAFT_NOT_FOUND)

        prDraftRepository.delete(prDraft)
    }

}
