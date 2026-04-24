package com.back.omos.domain.analysis.github

/**
 * GitHub API 호출 기능을 추상화한 인터페이스입니다.
 *
 * <p>
 * 서비스 계층은 이 인터페이스를 통해 GitHub API를 호출하여
 * 이슈 정보 및 관련 소스코드를 가져옵니다.
 *
 * <p><b>상속 정보:</b><br>
 * 별도의 상속 없이 GitHub API 호출 역할을 정의하는 인터페이스입니다.
 *
 * @author Jaewon Ryu
 * @since 2026-04-24
 */
interface GitHubClient {

    /**
     * 이슈 정보를 fetch합니다.
     *
     * @param owner 레포지토리 소유자 (e.g. "spring-projects")
     * @param repo 레포지토리명 (e.g. "spring-boot")
     * @param issueNumber 이슈 번호
     * @return 이슈 제목, 본문, 라벨 포함한 응답
     */
    fun fetchIssue(owner: String, repo: String, issueNumber: Int): GitHubIssueRes

    /**
     * 이슈 키워드로 관련 소스코드를 검색합니다.
     *
     * @param keyword 검색 키워드 (이슈 제목 기반)
     * @param owner 레포지토리 소유자
     * @param repo 레포지토리명
     * @return 검색된 파일 목록
     */
    fun searchCode(keyword: String, owner: String, repo: String): GitHubCodeSearchRes

    /**
     * 파일 raw content를 fetch합니다.
     *
     * @param owner 레포지토리 소유자
     * @param repo 레포지토리명
     * @param path 파일 경로 (e.g. "src/main/kotlin/Example.kt")
     * @return 디코딩된 파일 내용, 없으면 null
     */
    fun fetchFileContent(owner: String, repo: String, path: String): String?
}