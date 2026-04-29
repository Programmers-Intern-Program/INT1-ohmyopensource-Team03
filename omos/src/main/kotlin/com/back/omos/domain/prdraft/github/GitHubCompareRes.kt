package com.back.omos.domain.prdraft.github

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * GitHub Compare API 응답을 담는 DTO입니다.
 *
 * <p>
 * 두 브랜치 간의 변경된 파일 목록을 포함하며,
 * 각 파일의 diff(patch)를 추출하는 데 사용됩니다.
 *
 * @author 5h6vm
 * @since 2026-04-29
 * @see GitHubClientImpl
 */
data class GitHubCompareRes(

    @JsonProperty("files")
    val files: List<GitHubFileRes>?
)

/**
 * GitHub Compare API 응답 내 개별 파일 변경 정보를 담는 DTO입니다.
 *
 * <p>
 * 바이너리 파일이나 변경량이 너무 큰 파일의 경우 patch가 null로 반환됩니다.
 */
data class GitHubFileRes(

    @JsonProperty("filename")
    val filename: String,

    @JsonProperty("patch")
    val patch: String?
)
