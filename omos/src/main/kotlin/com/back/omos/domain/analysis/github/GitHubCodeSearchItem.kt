package com.back.omos.domain.analysis.github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * GitHub Code Search API 응답의 items 배열 각 요소를 담는 DTO입니다.
 *
 * <p>
 * GET /search/code 응답의 items 배열에서
 * 서비스에 필요한 필드만 매핑합니다.
 *
 * <p><b>주요 필드 활용:</b><br>
 * - path: fetchFileContent() 호출 시 파일 경로로 활용 <br>
 * - name: ANALYSIS_RESULT.file_paths 저장 시 활용 <br>
 * - htmlUrl: 프론트엔드 파일 링크로 활용
 *
 * @author Jaewon Ryu
 * @since 2026-04-24
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitHubCodeSearchItem(

    /**
     * 파일명입니다.
     * 예: "UserService.kt"
     */
    val name: String,

    /**
     * 레포지토리 내 파일 전체 경로입니다.
     * fetchFileContent() 호출 시 path 파라미터로 사용됩니다.
     * 예: "src/main/kotlin/com/example/UserService.kt"
     */
    val path: String,

    /**
     * GitHub 파일 페이지 URL입니다.
     * JSON 키가 "html_url"이므로 @JsonProperty로 매핑합니다.
     * 예: "https://github.com/spring-projects/spring-boot/blob/main/src/..."
     */
    @JsonProperty("html_url")
    val htmlUrl: String
)