package com.back.omos.domain.analysis.github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * GitHub API의 이슈 라벨 정보를 담는 DTO입니다.
 *
 * <p>
 * GitHub Issues API 응답의 labels 배열의 각 요소를 매핑합니다.
 * 실제 응답에는 id, color 등 다양한 필드가 있지만,
 * 서비스에서 필요한 name 필드만 매핑합니다.
 *
 * @author Jaewon Ryu
 * @since 2026-04-24
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitHubLabel(
    val name: String
)