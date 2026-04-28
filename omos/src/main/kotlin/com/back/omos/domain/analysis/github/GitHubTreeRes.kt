package com.back.omos.domain.analysis.github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitHubTreeRes(
    val tree: List<GitHubTreeItem>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitHubTreeItem(
    val path: String,
    val type: String  // "blob" = 파일, "tree" = 디렉토리
)