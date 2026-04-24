package com.back.omos.domain.analysis.github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * GitHub Contents API 응답을 담는 DTO입니다.
 *
 * <p>
 * GET /repos/{owner}/{repo}/contents/{path} 응답에서
 * Base64 인코딩된 파일 내용만 매핑합니다.
 *
 * <p><b>사용 범위:</b><br>
 * {@link GitHubClientImpl#fetchFileContent}에서 내부적으로만 사용되며,
 * 디코딩된 String으로 변환 후 반환되므로 서비스 계층에 노출되지 않습니다.
 *
 * @author Jaewon Ryu
 * @since 2026-04-24
 * @see GitHubClientImpl
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitHubFileContentRes(

    /**
     * Base64로 인코딩된 파일 내용입니다.
     * GitHub API가 줄바꿈(\n)을 포함하여 응답하므로
     * 디코딩 전 replace("\n", "")로 제거해야 합니다.
     * 파일이 비어있거나 바이너리 파일인 경우 null일 수 있습니다.
     */
    val content: String?
)