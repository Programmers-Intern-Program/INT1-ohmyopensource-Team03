package com.back.omos.domain.prdraft.github

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * GitHub Contents API 응답을 담는 DTO입니다.
 *
 * <p>
 * GitHub API의 파일 내용은 base64로 인코딩되어 전달됩니다.
 * {@link GitHubClientImpl}에서 디코딩하여 사용합니다.
 *
 * @author 5h6vm
 * @since 2026-04-23
 * @see GitHubClientImpl
 */
data class GitHubContentsRes(

    /**
     * base64로 인코딩된 파일 내용입니다.
     */
    @JsonProperty("content")
    val content: String?
)
