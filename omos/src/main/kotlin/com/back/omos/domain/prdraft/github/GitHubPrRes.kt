package com.back.omos.domain.prdraft.github

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * GitHub PR 목록 API 응답을 담는 DTO입니다.
 *
 * <p>
 * CONTRIBUTING.md가 없는 레포지토리에서 기존 PR의 톤앤매너를 분석하기 위해 사용됩니다.
 * merged된 PR만 프롬프트에 활용하기 위해 mergedAt 필드를 포함합니다.
 *
 * @author 5h6vm
 * @since 2026-04-24
 * @see GitHubClientImpl
 */
data class GitHubPrRes(

    @JsonProperty("title")
    val title: String,

    @JsonProperty("body")
    val body: String?,

    @JsonProperty("merged_at")
    val mergedAt: String?
)
