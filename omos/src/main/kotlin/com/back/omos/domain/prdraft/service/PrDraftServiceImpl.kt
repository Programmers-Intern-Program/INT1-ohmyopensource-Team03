package com.back.omos.domain.prdraft.service

import com.back.omos.domain.prdraft.dto.CreatePrReq
import com.back.omos.domain.prdraft.dto.PrInfoRes
import com.back.omos.domain.prdraft.repository.PrDraftRepository
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
    private val prDraftPromptBuilder: PrDraftPromptBuilder
) : PrDraftService {

    @Transactional
    override fun create(request: CreatePrReq): PrInfoRes {
        // TODO: val issue = issueRepository.findById
        // TODO: val repo = repoRepository.findById
        // TODO: issue의 레포와 repo가 동일한지 확인

        // TODO: 규칙 정리하기
        // TODO: 프롬프트 만들기
        val prompt = prDraftPromptBuilder.build(request)

        // TODO: AI 호출하기
        // TODO: 응답 반환하기

        return PrInfoRes(
            title = "feat: 임시 제목",
            body = prompt
        )
    }
}