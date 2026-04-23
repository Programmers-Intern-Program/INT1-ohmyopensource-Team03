package com.back.omos.domain.prdraft.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

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
    @field:Positive(message = "repositoryId는 1 이상이어야 합니다.")
    val repositoryId: Long,
    @field:Positive(message = "issueId는 1 이상이어야 합니다.")
    val issueId: Long,
    @field:NotBlank(message = "diffContent는 비어 있을 수 없습니다.")
    val diffContent: String
)
