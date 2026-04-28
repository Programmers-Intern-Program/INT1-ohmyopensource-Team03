package com.back.omos.domain.analysis.github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * GitHub Git Tree API 응답 최상위 객체입니다.
 *
 * GET /repos/{owner}/{repo}/git/trees/{tree_sha}?recursive=1
 *
 * @property tree 트리에 포함된 파일/디렉토리 항목 목록
 * @property truncated 응답이 잘렸는지 여부.
 *           레포 크기가 크면 GitHub가 일부 항목만 반환하고 true로 설정합니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitHubTreeRes(
    val tree: List<GitHubTreeItem>,
    val truncated: Boolean = false
)

/**
 * GitHub Git Tree API 응답의 개별 항목입니다.
 *
 * @property path 파일 또는 디렉토리의 경로 (예: "src/main/kotlin/Foo.kt")
 * @property type 항목 유형. "blob" = 파일, "tree" = 디렉토리
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitHubTreeItem(
    val path: String,
    val type: String
)