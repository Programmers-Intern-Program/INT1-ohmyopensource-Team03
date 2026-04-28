package com.back.omos.domain.analysis.github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * GitHub Code Search API 응답을 담는 DTO입니다.
 *
 * <p>
 * GET /search/code?q={keyword}+repo:{owner}/{repo} 응답에서
 * 서비스에 필요한 필드만 매핑합니다.
 *
 * <p><b>주요 필드 활용:</b><br>
 * - totalCount: 검색 결과가 없을 때 분기 처리에 활용 <br>
 * - items: 각 item의 path로 fetchFileContent() 호출하여 실제 코드 fetch
 *
 * @author Jaewon Ryu
 * @since 2026-04-24
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitHubCodeSearchRes(

    /**
     * 검색된 파일의 총 개수입니다.
     * 0이면 관련 파일이 없는 것으로 판단합니다.
     * JSON 키가 "total_count"이므로 @JsonProperty로 매핑합니다.
     */
    @JsonProperty("total_count")
    val totalCount: Int,

    /**
     * 검색된 파일 목록입니다.
     * 각 item의 path를 fetchFileContent()의 파라미터로 활용합니다.
     */
    val items: List<GitHubCodeSearchItem>
)