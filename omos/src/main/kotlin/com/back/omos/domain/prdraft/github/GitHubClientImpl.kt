package com.back.omos.domain.prdraft.github

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.util.Base64

/**
 * GitHub Contents API를 호출하여 파일 내용을 가져오는 구현체입니다.
 *
 * <p>
 * RestClient를 사용하여 GitHub API를 호출하며,
 * 응답으로 받은 base64 인코딩된 파일 내용을 디코딩하여 반환합니다.
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

    private val restClient = RestClient.builder()
        .baseUrl("https://api.github.com")
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
        .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
        .build()

    /**
     * GitHub Contents API를 통해 CONTRIBUTING.md 내용을 가져옵니다.
     *
     * <p>
     * 파일이 존재하지 않거나 API 호출에 실패한 경우 null을 반환합니다.
     *
     * @param fullName owner/repo 형식의 레포지토리 이름
     * @return CONTRIBUTING.md 내용, 없으면 null
     */
    override fun fetchContributing(fullName: String): String? {
        val paths = listOf("CONTRIBUTING.md", ".github/CONTRIBUTING.md")
        for (path in paths) {
            val result = fetchFile(fullName, path)
            if (result != null) return result
        }
        return null
    }

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
            null
        }
    }
}
