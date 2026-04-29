package com.back.omos.domain.prdraft.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

/**
 * PR 생성 요청 정보를 담는 DTO입니다.
 *
 * @author 5h6vm
 * @since 2026-04-21
 */
data class CreatePrReq(
    @field:NotBlank(message = "upstreamRepo는 비어 있을 수 없습니다.")
    val upstreamRepo: String,
    @field:Positive(message = "githubIssueNumber는 1 이상이어야 합니다.")
    val githubIssueNumber: Long,
    @field:NotBlank(message = "baseBranch는 비어 있을 수 없습니다.")
    val baseBranch: String,
    @field:NotBlank(message = "headBranch는 비어 있을 수 없습니다.")
    val headBranch: String
)
