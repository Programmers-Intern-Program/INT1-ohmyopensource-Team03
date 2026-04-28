package com.back.omos.domain.analysis.github

/**
 * GitHub API 호출 기능을 추상화한 인터페이스입니다.
 *
 * 서비스 계층은 이 인터페이스를 통해 이슈 정보 조회([fetchIssue]),
 * 코드 검색([searchCode]), 파일 내용 조회([fetchFileContent]),
 * 파일 트리 조회([fetchTree]) 기능을 사용합니다.
 *
 * @author Jaewon Ryu
 * @since 2026-04-24
 * @see GitHubClientImpl
 */
interface GitHubClient {

    /**
     * 특정 이슈의 상세 정보를 조회합니다.
     *
     * @param owner 레포지토리 소유자 (예: `"spring-projects"`)
     * @param repo 레포지토리명 (예: `"spring-boot"`)
     * @param issueNumber 조회할 이슈 번호
     * @return 이슈 제목·본문·라벨을 포함한 [GitHubIssueRes]
     * @throws com.back.omos.global.exception.exceptions.AnalysisException
     *         GITHUB_API_FAIL — API 호출 실패 또는 이슈를 찾을 수 없는 경우
     */
    fun fetchIssue(owner: String, repo: String, issueNumber: Int): GitHubIssueRes

    /**
     * 이슈 키워드를 기반으로 레포지토리 내 관련 소스코드를 검색합니다.
     *
     * @param keyword 검색 키워드 (이슈 제목 기반)
     * @param owner 레포지토리 소유자
     * @param repo 레포지토리명
     * @return 키워드와 매칭된 파일 목록을 담은 [GitHubCodeSearchRes]
     * @throws com.back.omos.global.exception.exceptions.AnalysisException
     *         GITHUB_API_FAIL — API 호출 실패 또는 Rate Limit 초과 시
     */
    fun searchCode(keyword: String, owner: String, repo: String): GitHubCodeSearchRes

    /**
     * 특정 파일의 raw 내용을 조회합니다.
     *
     * 파일이 존재하지 않거나 조회에 실패하면 null을 반환합니다.
     * 호출부는 null을 정상 케이스(파일 없음)로 처리해야 합니다.
     *
     * @param owner 레포지토리 소유자
     * @param repo 레포지토리명
     * @param path 조회할 파일 경로 (예: `"src/main/kotlin/Example.kt"`)
     * @return 디코딩된 파일 내용 문자열. 파일이 없거나 조회 실패 시 null
     * @throws com.back.omos.global.exception.exceptions.AnalysisException
     *         GITHUB_API_FAIL — 네트워크 오류 등 복구 불가능한 API 실패 시
     */
    fun fetchFileContent(owner: String, repo: String, path: String): String?

    /**
     * 레포지토리의 전체 파일 경로 목록을 조회합니다.
     *
     * 디렉토리는 제외하고 파일 경로만 반환합니다.
     * 규모가 큰 레포지토리의 경우 응답에 시간이 걸릴 수 있습니다.
     *
     * @param owner 레포지토리 소유자
     * @param repo 레포지토리명
     * @return 레포지토리 내 전체 파일 경로 목록 (디렉토리 제외)
     * @throws com.back.omos.global.exception.exceptions.AnalysisException
     *         GITHUB_API_FAIL — API 호출 실패 또는 Rate Limit 초과 시
     */
    fun fetchTree(owner: String, repo: String): List<String>
}