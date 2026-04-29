package com.back.omos.domain.prdraft.github

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import java.time.Duration
import java.util.Base64

/**
 * GitHub Contents API를 호출하여 파일 내용을 가져오는 구현체입니다.
 *
 * <p>
 * RestClient를 사용하여 GitHub API를 호출하며,
 * 응답으로 받은 base64 인코딩된 파일 내용을 디코딩하여 반환합니다.
 *
 * <p><b>탐색 순서:</b><br>
 * CONTRIBUTING 파일은 레포지토리마다 위치와 확장자가 다를 수 있어,
 * 아래 순서로 순차 탐색하며 가장 먼저 발견된 파일을 반환합니다.
 * <ol>
 *   <li>CONTRIBUTING.md (루트)</li>
 *   <li>.github/CONTRIBUTING.md</li>
 *   <li>CONTRIBUTING.adoc (루트)</li>
 *   <li>.github/CONTRIBUTING.adoc</li>
 * </ol>
 *
 * <p><b>타임아웃:</b><br>
 * 연결 타임아웃 3초, 읽기 타임아웃 5초로 설정되어 있습니다.
 * 타임아웃 초과 시 예외를 잡아 null을 반환하며, 서비스 장애로 이어지지 않습니다.
 *
 * <p><b>상속 정보:</b><br>
 * {@link GitHubClient}를 구현합니다.
 *
 * @author 5h6vm
 * @since 2026-04-23
 * @see GitHubClient
 */
@Component
class GitHubClientImpl(
    @Value("\${github.token}") private val token: String
) : GitHubClient {

    private val logger = KotlinLogging.logger {}

    init {
        require(token.isNotBlank()) { "github.token이 설정되지 않았습니다." }
    }

    private val restClient = RestClient.builder()
        .baseUrl("https://api.github.com")
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
        .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
        .requestFactory(SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofSeconds(3))
            setReadTimeout(Duration.ofSeconds(5))
        })
        .build()

    /**
     * CONTRIBUTING 파일 내용을 가져옵니다.
     *
     * <p>
     * 루트 및 .github 폴더 하위에서 .md, .adoc 확장자 순으로 탐색합니다.
     * 파일이 존재하지 않거나 API 호출에 실패한 경우 null을 반환합니다.
     *
     * <p>
     * CONTRIBUTING은 AI 프롬프트의 선택적 컨텍스트이므로,
     * 파일이 없거나 GitHub API 장애가 발생해도 null을 반환하여 PR 생성은 계속 진행됩니다.
     *
     * @param fullName owner/repo 형식의 레포지토리 이름
     * @return CONTRIBUTING 파일 내용, 없으면 null
     */
    override fun fetchContributing(fullName: String): String? {
        val paths = listOf("CONTRIBUTING.md", ".github/CONTRIBUTING.md", "CONTRIBUTING.adoc", ".github/CONTRIBUTING.adoc")
        for (path in paths) {
            val result = fetchFile(fullName, path)
            if (result != null) return result
        }
        return null
    }

    /**
     * 레포지토리의 merged된 PR 목록을 최대 10개 가져옵니다.
     *
     * <p>
     * closed 상태의 PR 20개를 가져온 뒤, merged되고 본문이 있는 것만 필터링하여 최대 10개를 반환합니다.
     * API 호출 실패 시 빈 리스트를 반환하며, 서비스 장애로 이어지지 않습니다.
     *
     * @param fullName owner/repo 형식의 레포지토리 이름
     * @return merged PR 목록, 없거나 실패 시 빈 리스트
     */
    override fun fetchMergedPrs(fullName: String): List<GitHubPrRes> {
        return try {
            val response = restClient.get()
                .uri("/repos/$fullName/pulls?state=closed&per_page=20")
                .retrieve()
                .body(Array<GitHubPrRes>::class.java)

            response
                ?.filter { !it.mergedAt.isNullOrBlank() && !it.body.isNullOrBlank() }
                ?.take(10)
                ?: emptyList()
        } catch (e: RestClientResponseException) {
            if (e.statusCode.value() != 404) {
                logger.warn { "GitHub PR 조회 오류: $fullName - ${e.statusCode}" }
            }
            emptyList()
        } catch (e: RestClientException) {
            logger.warn { "GitHub PR 네트워크 오류: $fullName - ${e.message}" }
            emptyList()
        }
    }

    /**
     * GitHub Compare API로 두 브랜치 간의 diff를 가져옵니다.
     *
     * <p>
     * upstream 레포지토리의 baseBranch와 포크 사용자의 headBranch를 비교합니다.
     * 변경된 파일의 patch를 filename 헤더와 함께 합쳐 하나의 문자열로 반환합니다.
     * patch가 없는 파일(바이너리 등)은 건너뜁니다.
     *
     * @param upstreamRepo owner/repo 형식의 upstream 레포지토리
     * @param baseBranch 기준 브랜치 (예: main)
     * @param forkOwner 포크한 사용자의 GitHub ID
     * @param headBranch 작업 브랜치 (예: fix/issue-123)
     * @return 변경된 파일들의 patch를 합친 diff 문자열
     */
    override fun fetchDiff(upstreamRepo: String, baseBranch: String, forkOwner: String, headBranch: String): String {
        val response = restClient.get()
            .uri("/repos/$upstreamRepo/compare/$baseBranch...$forkOwner:$headBranch")
            .retrieve()
            .body(GitHubCompareRes::class.java)

        return response?.files
            ?.filter { !it.patch.isNullOrBlank() }
            ?.joinToString("\n") { "--- ${it.filename}\n${it.patch}" }
            ?: ""
    }

    /**
     * GitHub Contents API로 단일 파일의 내용을 가져옵니다.
     *
     * <p>
     * GitHub Contents API는 파일 내용을 base64로 인코딩하여 반환합니다.
     * 줄바꿈 문자(\n)를 제거한 뒤 디코딩하여 원본 텍스트로 복원합니다.
     *
     * <p>
     * HTTP 오류(4xx/5xx), 네트워크 오류, 타임아웃 등 모든 API 호출 실패는
     * {@link RestClientException}으로 잡아 null을 반환합니다.
     *
     * @param fullName owner/repo 형식의 레포지토리 이름
     * @param path 조회할 파일 경로 (예: CONTRIBUTING.md, .github/CONTRIBUTING.md)
     * @return 디코딩된 파일 내용, 실패 시 null
     */
    private fun fetchFile(fullName: String, path: String): String? {
        return try {
            val response = restClient.get()
                .uri("/repos/$fullName/contents/$path")
                .retrieve()
                .body(GitHubContentsRes::class.java)

            response?.content
                ?.replace("\n", "")
                ?.let { Base64.getDecoder().decode(it).toString(Charsets.UTF_8) }
        } catch (e: RestClientResponseException) {
            if (e.statusCode.value() == 404) {
                null
            } else {
                logger.warn { "GitHub API 오류: $fullName/$path - ${e.statusCode}" }
                null
            }
        } catch (e: RestClientException) {
            logger.warn { "GitHub API 네트워크 오류: $fullName/$path - ${e.message}" }
            null
        }
    }
}
