package com.back.omos.domain.analysis.github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * GitHub Issues API 응답을 담는 DTO입니다.
 *
 * <p>
 * GET /repos/{owner}/{repo}/issues/{issueNumber} 응답에서
 * 서비스에 필요한 필드만 매핑합니다.
 *
 * <p><b>주요 필드 활용:</b><br>
 * - title: 관련 소스코드 검색 키워드로 활용 <br>
 * - body: GLM API 프롬프트의 이슈 맥락 정보로 활용 <br>
 * - labels: GLM API 프롬프트의 이슈 분류 정보로 활용
 *
 * @author Jaewon Ryu
 * @since 2026-04-24
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitHubIssueRes(

    /**
     * GitHub 이슈 번호입니다.
     * Issue 엔티티의 issueNumber와 동일한 값입니다.
     */
    val number: Int,

    /**
     * 이슈 제목입니다.
     * searchCode()의 키워드로 활용됩니다.
     */
    val title: String,

    /**
     * 이슈 본문입니다.
     * 작성되지 않은 이슈의 경우 null일 수 있습니다.
     */
    val body: String?,

    /**
     * 이슈에 부여된 라벨 목록입니다.
     * GitHub 응답의 labels 배열을 GitHubLabel 리스트로 매핑합니다.
     */
    val labels: List<GitHubLabel>
)