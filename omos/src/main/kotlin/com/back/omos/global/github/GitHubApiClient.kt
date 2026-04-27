package com.back.omos.global.github

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

/**
 * GitHub REST API v3를 호출하여 사용자의 공개 프로필 및 레포지토리 정보를 조회하는 클라이언트입니다.
 *
 * <p>
 * 인증 없이 공개 프로필 정보를 조회합니다(시간당 60회 제한).
 * 주로 사용자의 주요 사용 언어, 레포지토리 설명, 기술 토픽을 수집하여
 * 프로필 벡터 임베딩에 활용합니다.
 *
 * <p>
 * 호출 패턴:
 * <ol>
 *   <li>{@code GET /users/{username}} - 사용자 프로필 (bio, 공개 레포 수)</li>
 *   <li>{@code GET /users/{username}/repos} - 레포지토리 목록 (언어, 토픽, 설명)</li>
 * </ol>
 *
 * @author MintyU
 * @since 2026-04-27
 */
@Component
class GitHubApiClient {

    private val restClient = RestClient.builder()
        .baseUrl("https://api.github.com")
        .defaultHeader("Accept", "application/vnd.github+json")
        .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
        .build()

    /**
     * GitHub 사용자의 공개 프로필 정보를 조회합니다.
     *
     * @param username GitHub 로그인 이름 (ex. "MintyU")
     * @return 사용자 프로필 정보, API 호출 실패 시 null 반환
     */
    fun getUserProfile(username: String): GitHubUserProfile? {
        return try {
            restClient.get()
                .uri("/users/{username}", username)
                .retrieve()
                .body(GitHubUserProfileResponse::class.java)
                ?.toDomain()
        } catch (e: RestClientException) {
            null
        }
    }

    /**
     * GitHub 사용자의 공개 레포지토리 목록을 조회합니다.
     *
     * <p>
     * 포크된 레포지토리는 제외하고, 스타 수 기준 내림차순으로 최대 [limit]개를 반환합니다.
     * API 호출 실패 시 빈 리스트를 반환합니다.
     *
     * @param username GitHub 로그인 이름
     * @param limit 반환할 레포지토리 최대 수 (기본값: 20)
     * @return 레포지토리 목록
     */
    fun getUserRepos(username: String, limit: Int = 20): List<GitHubRepo> {
        return try {
            restClient.get()
                .uri("/users/{username}/repos?per_page=100&sort=updated", username)
                .retrieve()
                .body(Array<GitHubRepoResponse>::class.java)
                ?.filter { !it.fork }
                ?.sortedByDescending { it.stargazersCount }
                ?.take(limit)
                ?.map { it.toDomain() }
                ?: emptyList()
        } catch (e: RestClientException) {
            emptyList()
        }
    }

    /**
     * 레포지토리 목록에서 사용 언어를 집계합니다.
     *
     * <p>
     * 각 언어의 등장 빈도를 카운트하여, 빈도 내림차순으로 정렬된 맵을 반환합니다.
     *
     * @param repos 레포지토리 목록
     * @return 언어명 → 사용 레포지토리 수 맵 (빈도 내림차순)
     */
    fun aggregateLanguages(repos: List<GitHubRepo>): Map<String, Int> {
        return repos
            .mapNotNull { it.language }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .associate { it.key to it.value }
    }

    // ── 도메인 모델 ──────────────────────────────────────────────────────────────

    /**
     * GitHub 사용자 프로필 정보를 담는 도메인 모델입니다.
     *
     * @property login GitHub 로그인 이름
     * @property bio 프로필 소개 (nullable)
     * @property publicRepos 공개 레포지토리 수
     */
    data class GitHubUserProfile(
        val login: String,
        val bio: String?,
        val publicRepos: Int
    )

    /**
     * GitHub 레포지토리 정보를 담는 도메인 모델입니다.
     *
     * @property name 레포지토리 이름
     * @property description 레포지토리 설명 (nullable)
     * @property language 주 사용 언어 (nullable)
     * @property topics 레포지토리 토픽 목록
     * @property stars 스타 수
     */
    data class GitHubRepo(
        val name: String,
        val description: String?,
        val language: String?,
        val topics: List<String>,
        val stars: Int
    )

    // ── GitHub API 응답 DTO ──────────────────────────────────────────────────────

    private data class GitHubUserProfileResponse(
        val login: String,
        val bio: String?,
        @JsonProperty("public_repos") val publicRepos: Int
    ) {
        fun toDomain() = GitHubUserProfile(login, bio, publicRepos)
    }

    private data class GitHubRepoResponse(
        val name: String,
        val description: String?,
        val language: String?,
        val topics: List<String> = emptyList(),
        @JsonProperty("stargazers_count") val stargazersCount: Int,
        val fork: Boolean
    ) {
        fun toDomain() = GitHubRepo(name, description, language, topics, stargazersCount)
    }
}
