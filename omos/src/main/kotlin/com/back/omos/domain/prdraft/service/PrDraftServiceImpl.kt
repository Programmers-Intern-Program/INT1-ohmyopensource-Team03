package com.back.omos.domain.prdraft.service

import com.back.omos.domain.issue.repository.IssueRepository
import com.back.omos.domain.prdraft.dto.CreatePrReq
import com.back.omos.domain.prdraft.dto.PrInfoRes
import com.back.omos.domain.prdraft.repository.PrDraftRepository
import com.back.omos.domain.repo.repository.RepoRepository
import jakarta.transaction.Transactional
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
    private val aiClient: AiClient
) : PrDraftService {

    @Transactional
    override fun create(request: CreatePrReq): PrInfoRes {
        // TODO: 에러처리 추가해야함
        val issue = issueRepository.findById(request.issueId)
        val repo = repoRepository.findById(request.repositoryId)

        // TODO: 규칙 정리하기
        require(request.diffContent.isNotBlank())   //변경사항이 존재함

        // TODO: 프롬프트 만들기
        val prompt = prDraftPromptBuilder.build(request)

        // TODO: AI 호출하기
        val aiResult = aiClient.generatePrDraft(prompt)

        // TODO: 응답 반환하기
        return PrInfoRes(
            title = aiResult.title,
            body = aiResult.body
        )
    }
}