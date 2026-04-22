package com.back.omos.domain.prdraft.dto

/**
 * PR 생성 요청 정보를 담는 DTO입니다.
 *
 * <p>
 * 사용자가 선택한 Issue와 코드 변경 내용(diff)을 기반으로
 * PR 제목과 본문 생성을 요청할 때 사용됩니다.
 *
 * @author 5h6vm
 * @since 2026-04-21
 */
data class CreatePrReq(
    val repositoryId: Long,
    val issueId: Long,
    val diffContent: String
)
