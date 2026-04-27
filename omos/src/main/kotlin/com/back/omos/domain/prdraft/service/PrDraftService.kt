package com.back.omos.domain.prdraft.service

import com.back.omos.domain.prdraft.dto.CreatePrReq
import com.back.omos.domain.prdraft.dto.PrHistoryRes
import com.back.omos.domain.prdraft.dto.PrInfoRes

/**
 * PR 생성 기능을 제공하는 Service 인터페이스입니다.
 *
 * <p>
 * 사용자의 코드 변경 내용(diff)을 기반으로 PR 제목과 본문을 생성하고, 생성 이력을 조회하는 기능을 정의합니다.
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
     * @return 생성된 PR 제목, 본문, GitHub URL
     */
    fun create(githubId: String, request: CreatePrReq): PrInfoRes

    /**
     * 사용자가 생성한 PR 초안 목록을 최신순으로 조회합니다.
     *
     * @param githubId 조회할 사용자의 GitHub ID
     * @return PR 초안 목록 (최신순)
     */
    fun getHistory(githubId: String): List<PrHistoryRes>

    /**
     * PR 초안을 삭제합니다.
     *
     * @param githubId 요청한 사용자의 GitHub ID
     * @param prDraftId 삭제할 PR 초안 ID
     * @throws PrDraftException 존재하지 않는 PR 초안이거나 본인 소유가 아닌 경우
     */
    fun delete(githubId: String, prDraftId: Long)
}