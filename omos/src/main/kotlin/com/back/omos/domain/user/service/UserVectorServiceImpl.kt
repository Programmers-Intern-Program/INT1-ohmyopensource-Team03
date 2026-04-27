package com.back.omos.domain.user.service

import com.back.omos.domain.user.dto.UserInfoRes
import com.back.omos.domain.user.repository.UserRepository
import com.back.omos.global.exception.errorCode.AuthErrorCode
import com.back.omos.global.exception.exceptions.AuthException
import com.back.omos.global.github.GitHubApiClient
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * [UserVectorService]의 구현체로, GitHub 공개 정보 기반 사용자 프로필 벡터 임베딩을 처리합니다.
 *
 * <p>
 * 처리 흐름:
 * <ol>
 *   <li>[GitHubApiClient]를 통해 사용자의 공개 프로필과 레포지토리 목록을 수집합니다.</li>
 *   <li>수집된 정보(주요 언어, 레포지토리 설명, 기술 토픽, 소개)를 구조화된 텍스트로 변환합니다.</li>
 *   <li>[EmbeddingModel]을 통해 텍스트를 3072차원 벡터로 임베딩합니다.</li>
 *   <li>User 엔티티의 [com.back.omos.domain.user.entity.User.profileVector]를 갱신하고 저장합니다.</li>
 * </ol>
 *
 * <p>
 * 생성된 벡터는 추후 Good First Issue 추천 시 이슈 벡터와의 코사인 유사도 비교에 활용됩니다.
 *
 * @property userRepository 사용자 데이터 액세스 Repository
 * @property gitHubApiClient GitHub 공개 API 호출 클라이언트
 * @property embeddingModel Gemini 임베딩 모델
 *
 * @author MintyU
 * @since 2026-04-27
 * @see UserVectorService
 * @see GitHubApiClient
 */
@Service
@Transactional
class UserVectorServiceImpl(
    private val userRepository: UserRepository,
    private val gitHubApiClient: GitHubApiClient,
    private val embeddingModel: EmbeddingModel
) : UserVectorService {

    /**
     * 사용자의 GitHub 프로필 정보를 기반으로 프로필 벡터를 생성하고 저장합니다.
     *
     * <p>
     * [com.back.omos.domain.user.entity.User.name] 필드에 저장된 GitHub 로그인 이름을 이용하여
     * GitHub API를 호출합니다. 로그인 이름이 없는 경우 예외를 발생시킵니다.
     *
     * @param githubId 벡터를 갱신할 사용자의 GitHub 고유 ID
     * @return 벡터 업데이트 후 사용자 정보 DTO
     * @throws AuthException 사용자가 없거나 username이 없거나 임베딩이 빈 경우
     */
    override fun updateUserVector(githubId: String): UserInfoRes {
        val user = userRepository.findByGithubId(githubId)
            .orElseThrow { AuthException(AuthErrorCode.USER_NOT_FOUND) }

        val username = user.name
            ?: throw AuthException(
                AuthErrorCode.USER_VECTOR_UPDATE_FAIL,
                "GitHub 사용자명(login)이 저장되어 있지 않습니다. githubId=$githubId"
            )

        val profile = gitHubApiClient.getUserProfile(username)
        val repos = gitHubApiClient.getUserRepos(username)
        val languages = gitHubApiClient.aggregateLanguages(repos)

        val text = buildProfileText(profile, repos, languages)

        val floatArray = embeddingModel.embed(text)
        if (floatArray.isEmpty()) {
            throw AuthException(
                AuthErrorCode.USER_VECTOR_UPDATE_FAIL,
                "임베딩 모델이 빈 벡터를 반환했습니다. username=$username"
            )
        }

        val vector = floatArray.map { it.toDouble() }.toDoubleArray()
        user.updateVector(vector, languages.keys.take(5).toList())

        return UserInfoRes.from(user)
    }

    /**
     * GitHub 프로필 데이터를 Gemini 임베딩에 적합한 구조화된 텍스트로 변환합니다.
     *
     * <p>
     * 생성된 텍스트는 이슈 임베딩 시 사용하는 텍스트(이슈 제목, 설명, 레포 언어 및 토픽)와
     * 동일한 임베딩 공간에 투영되어 코사인 유사도 비교에 활용됩니다.
     *
     * @param profile GitHub 사용자 프로필 (nullable)
     * @param repos 공개 레포지토리 목록
     * @param languages 언어별 레포지토리 수 맵 (빈도 내림차순)
     * @return 임베딩할 텍스트
     */
    private fun buildProfileText(
        profile: GitHubApiClient.GitHubUserProfile?,
        repos: List<GitHubApiClient.GitHubRepo>,
        languages: Map<String, Int>
    ): String = buildString {
        appendLine("Developer GitHub Profile")
        appendLine()

        if (languages.isNotEmpty()) {
            appendLine("Primary Programming Languages: ${languages.keys.joinToString(", ")}")
        }

        profile?.bio?.takeIf { it.isNotBlank() }?.let {
            appendLine("Bio: $it")
        }

        val reposWithContent = repos.filter { !it.description.isNullOrBlank() || it.topics.isNotEmpty() }
        if (reposWithContent.isNotEmpty()) {
            appendLine()
            appendLine("Recent Projects:")
            reposWithContent.take(10).forEach { repo ->
                val detail = buildList {
                    repo.language?.let { add(it) }
                    repo.description?.let { add(it) }
                }.joinToString(", ")
                val topicStr = if (repo.topics.isNotEmpty()) " [${repo.topics.joinToString(", ")}]" else ""
                appendLine("- ${repo.name} ($detail)$topicStr")
            }
        }

        val allTopics = repos.flatMap { it.topics }.distinct().take(20)
        if (allTopics.isNotEmpty()) {
            appendLine()
            appendLine("Technical Interests: ${allTopics.joinToString(", ")}")
        }
    }
}
