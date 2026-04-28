package com.back.omos.domain.prdraft.dto

/**
 * PR 초안 수정 요청 정보를 담는 DTO입니다.
 *
 * <p>
 * 사용자가 직접 수정할 PR 제목과 본문을 전달할 때 사용됩니다.
 * null인 필드는 기존 값을 유지합니다.
 *
 * @author 5h6vm
 * @since 2026-04-28
 */
data class UpdatePrReq(
    val title: String?,
    val body: String?
)
