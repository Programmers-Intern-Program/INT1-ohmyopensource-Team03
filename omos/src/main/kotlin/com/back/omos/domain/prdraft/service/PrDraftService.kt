package com.back.omos.domain.prdraft.service

import com.back.omos.domain.prdraft.dto.CreatePrReq
import com.back.omos.domain.prdraft.dto.PrDetailRes
import com.back.omos.domain.prdraft.dto.PrHistoryRes
import com.back.omos.domain.prdraft.dto.PrInfoRes
import com.back.omos.domain.prdraft.dto.UpdatePrReq

/**
 * PR 초안 생성, 조회, 수정, 삭제 기능을 제공하는 Service 인터페이스입니다.
 *
 * <p>
 * 사용자의 코드 변경 내용(diff)을 기반으로 PR 제목과 본문을 생성하고, 단건/목록 조회, 수정 및 삭제 기능을 정의합니다.
 *
 * <p><b>상속 정보:</b><br>
 * 별도의 상속 없이 Service 역할을 정의하는 인터페이스입니다.
 *
 * @author 5h6vm
 * @since 2026-04-22
 */
interface PrDraftService {

    /**
     * diff 내용과 이슈 정보를 기반으로 AI를 호출하여 PR 초안을 생성하고 저장합니다.
     *
     * @param githubId 요청한 사용자의 GitHub ID
     * @param request PR 생성 요청 DTO (issueId, diffContent 포함)
     * @return 생성된 PR 제목, 본문
     */
    fun create(githubId: String, request: CreatePrReq): PrInfoRes

    /**
     * PR 초안 단건을 조회합니다.
     *
     * @param githubId 요청한 사용자의 GitHub ID
     * @param prDraftId 조회할 PR 초안 ID
     * @return PR 초안 상세 정보 (diffContent 포함)
     * @throws PrDraftException 존재하지 않는 PR 초안이거나 본인 소유가 아닌 경우
     */
    fun getOne(githubId: String, prDraftId: Long): PrDetailRes

    /**
     * 사용자가 생성한 PR 초안 목록을 최신순으로 조회합니다.
     *
     * @param githubId 조회할 사용자의 GitHub ID
     * @return PR 초안 목록 (최신순)
     */
    fun getHistory(githubId: String): List<PrHistoryRes>

    /**
     * PR 초안의 제목과 본문을 수정합니다.
     *
     * @param githubId 요청한 사용자의 GitHub ID
     * @param prDraftId 수정할 PR 초안 ID
     * @param request 수정할 제목과 본문 (null인 필드는 기존 값 유지)
     * @return 수정된 PR 초안 상세 정보
     * @throws PrDraftException 존재하지 않는 PR 초안이거나 본인 소유가 아닌 경우
     */
    fun update(githubId: String, prDraftId: Long, request: UpdatePrReq): PrDetailRes

    /**
     * PR 초안을 삭제합니다.
     *
     * @param githubId 요청한 사용자의 GitHub ID
     * @param prDraftId 삭제할 PR 초안 ID
     * @throws PrDraftException 존재하지 않는 PR 초안이거나 본인 소유가 아닌 경우
     */
    fun delete(githubId: String, prDraftId: Long)
}