package com.back.omos.domain.analysis.github

import com.back.omos.global.exception.errorCode.AnalysisErrorCode
import com.back.omos.global.exception.exceptions.AnalysisException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.util.Base64
import kotlin.jvm.java
import org.slf4j.LoggerFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import java.time.Duration

/**
 * GitHub REST API를 호출하여 이슈 정보 및 소스코드를 가져오는 구현체입니다.
 *
 * <p>
 * RestClient를 사용하여 GitHub API를 호출하며,
 * 파일 내용은 Base64 디코딩하여 반환합니다.
 *
 * <p><b>상속 정보:</b><br>
 * {@link GitHubClient}를 구현합니다.
 *
 * @author Jaewon Ryu
 * @since 2026-04-24
 * @see GitHubClient
 */
@Component("analysisGitHubClientImpl")
class GitHubClientImpl(
    @Value("\${github.token}") private val token: String,
    @Value("\${github.api.base-url:https://api.github.com}") private val baseUrl: String = "https://api.github.com"
) : GitHubClient {
    private val log = LoggerFactory.getLogger(GitHubClientImpl::class.java)

    init {
        require(token.isNotBlank()) { "[GitHubClientImpl#init] github.token이 설정되지 않았습니다." }
    }

    private val restClient = RestClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
        .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
        .requestFactory(SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofSeconds(3))
            setReadTimeout(Duration.ofSeconds(10))
        })
        .build()

    /**
     * GitHub Issues API를 통해 이슈 정보를 가져옵니다.
     *
     * GET /repos/{owner}/{repo}/issues/{issueNumber}
     *
     * @throws AnalysisException GITHUB_API_FAIL - API 호출 실패 시
     */
    override fun fetchIssue(owner: String, repo: String, issueNumber: Int): GitHubIssueRes {
        return try {
            restClient.get()
                .uri("/repos/$owner/$repo/issues/$issueNumber")
                .retrieve()
                .body(GitHubIssueRes::class.java)
                ?: throw AnalysisException(
                    AnalysisErrorCode.GITHUB_API_FAIL,
                    "[GitHubClientImpl#fetchIssue] 응답이 null입니다: $owner/$repo#$issueNumber",
                    "이슈 정보를 가져오는 데 실패했습니다."
                )
        } catch (e: RestClientResponseException) {
            throw AnalysisException(
                AnalysisErrorCode.GITHUB_API_FAIL,
                "[GitHubClientImpl#fetchIssue] HTTP ${e.statusCode}: $owner/$repo#$issueNumber",
                "이슈 정보를 가져오는 데 실패했습니다."
            )
        }
    }

    /**
     * GitHub Code Search API를 통해 관련 소스코드를 검색합니다.
     *
     * GET /search/code?q={keyword}+repo:{owner}/{repo}
     *
     * @throws AnalysisException GITHUB_API_FAIL - API 호출 실패 시
     */
    override fun searchCode(keyword: String, owner: String, repo: String): GitHubCodeSearchRes {
        return try {
            restClient.get()
                .uri { uriBuilder ->
                    uriBuilder.path("/search/code")
                        .queryParam("q", "$keyword repo:$owner/$repo")
                        .build()
                }
                .retrieve()
                .body(GitHubCodeSearchRes::class.java)
                ?: throw AnalysisException(
                    AnalysisErrorCode.GITHUB_API_FAIL,
                    "[GitHubClientImpl#searchCode] 응답이 null입니다: keyword=$keyword, $owner/$repo",
                    "관련 코드 검색에 실패했습니다."
                )
        } catch (e: RestClientResponseException) {
            throw AnalysisException(
                AnalysisErrorCode.GITHUB_API_FAIL,
                "[GitHubClientImpl#searchCode] HTTP ${e.statusCode}: keyword=$keyword, $owner/$repo",
                "관련 코드 검색에 실패했습니다."
            )
        }
    }

    /**
     * GitHub Contents API를 통해 파일 내용을 가져옵니다.
     *
     * GET /repos/{owner}/{repo}/contents/{path}
     *
     * 파일이 존재하지 않거나 API 호출 실패 시 null을 반환합니다.
     */
    override fun fetchFileContent(owner: String, repo: String, path: String): String? {
        return try {
            val response = restClient.get()
                .uri("/repos/$owner/$repo/contents/$path")
                .retrieve()
                .body(GitHubFileContentRes::class.java)
            response?.content
                ?.replace("\n", "")
                ?.let { Base64.getDecoder().decode(it).toString(Charsets.UTF_8) }
        } catch (e: RestClientResponseException) {
            if (e.statusCode.value() != 404) {
                log.warn(
                    "[GitHubClientImpl#fetchFileContent] HTTP ${e.statusCode}: $owner/$repo/$path"
                )
            }
            null
        }
    }
    override fun fetchTree(owner: String, repo: String): List<String> {
        return try {
            val response = restClient.get()
                .uri("/repos/$owner/$repo/git/trees/HEAD?recursive=1")
                .retrieve()
                .body(GitHubTreeRes::class.java)
                ?: throw AnalysisException(
                    AnalysisErrorCode.GITHUB_API_FAIL,
                    "[GitHubClientImpl#fetchTree] 응답이 null입니다: $owner/$repo",
                    "레포지토리 파일 목록을 가져오는 데 실패했습니다."
                )

            // truncated=true면 대형 레포로 판단, 부분 목록으로 분석 진행 시 오탐 가능성 있음
            if (response.truncated) {
                throw AnalysisException(
                    AnalysisErrorCode.GITHUB_API_FAIL,
                    "[GitHubClientImpl#fetchTree] 트리 결과가 잘렸습니다: $owner/$repo",
                    "레포지토리가 너무 커서 전체 파일 목록을 가져올 수 없습니다."
                )
            }

            response.tree
                .filter { it.type == "blob" }
                .map { it.path }
        } catch (e: AnalysisException) {
            throw e
        } catch (e: RestClientResponseException) {
            throw AnalysisException(
                AnalysisErrorCode.GITHUB_API_FAIL,
                "[GitHubClientImpl#fetchTree] HTTP ${e.statusCode}: $owner/$repo",
                "레포지토리 파일 목록을 가져오는 데 실패했습니다."
            )
        }
    }
    override fun fetchFileContents(owner: String, repo: String, paths: List<String>): Map<String, String> {
        if (paths.isEmpty()) return emptyMap()

        val fileQueries = paths.mapIndexed { index, path ->
            """file$index: object(expression: "HEAD:$path") { ... on Blob { text } }"""
        }.joinToString("\n")

        val query = """
        query {
            repository(owner: "$owner", name: "$repo") {
                $fileQueries
            }
        }
    """.trimIndent()

        return try {
            val response = restClient.post()
                .uri("/graphql")
                .header(HttpHeaders.ACCEPT, "application/json")
                .body(mapOf("query" to query))
                .retrieve()
                .body(Map::class.java)
                ?: throw AnalysisException(
                    AnalysisErrorCode.GITHUB_API_FAIL,
                    "[GitHubClientImpl#fetchFileContents] 응답이 null입니다: $owner/$repo",
                    "파일 내용을 가져오는 데 실패했습니다."
                )

            @Suppress("UNCHECKED_CAST")
            val repository = (response["data"] as? Map<String, Any>)
                ?.get("repository") as? Map<String, Any>
                ?: return emptyMap()

            paths.mapIndexed { index, path ->
                val text = (repository["file$index"] as? Map<String, Any>)
                    ?.get("text") as? String
                if (text != null) path to text else null
            }.filterNotNull().toMap()

        } catch (e: AnalysisException) {
            throw e
        } catch (e: RestClientResponseException) {
            throw AnalysisException(
                AnalysisErrorCode.GITHUB_API_FAIL,
                "[GitHubClientImpl#fetchFileContents] HTTP ${e.statusCode}: $owner/$repo",
                "파일 내용을 가져오는 데 실패했습니다."
            )
        }
    }
}
