package com.back.omos.domain.prdraft.github

/**
 * GitHub API 호출 기능을 추상화한 인터페이스입니다.
 *
 * <p>
 * 서비스 계층은 이 인터페이스를 통해 GitHub API를 호출하여
 * 레포지토리의 파일 내용을 가져옵니다.
 *
 * <p><b>상속 정보:</b><br>
 * 별도의 상속 없이 GitHub API 호출 역할을 정의하는 인터페이스입니다.
 *
 * @author 5h6vm
 * @since 2026-04-23
 */
interface GitHubClient {

    /**
     * 레포지토리의 CONTRIBUTING.md 내용을 가져옵니다.
     *
     * <p>
     * CONTRIBUTING.md가 존재하지 않는 레포지토리의 경우 null을 반환합니다.
     *
     * @param fullName owner/repo 형식의 레포지토리 이름
     * @return CONTRIBUTING.md 내용, 없으면 null
     */
    fun fetchContributing(fullName: String): String?

    /**
     * 레포지토리의 merged된 PR 목록을 최대 10개 가져옵니다.
     *
     * <p>
     * CONTRIBUTING.md가 없는 경우 기존 PR의 톤앤매너를 분석하기 위해 사용됩니다.
     * 본문이 있는 merged PR만 반환하며, API 호출 실패 시 빈 리스트를 반환합니다.
     *
     * @param fullName owner/repo 형식의 레포지토리 이름
     * @return merged PR 목록, 없거나 실패 시 빈 리스트
     */
    fun fetchMergedPrs(fullName: String): List<GitHubPrRes>
}
