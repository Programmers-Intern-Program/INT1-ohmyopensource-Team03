package com.back.omos.domain.issue.github

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

/**
 * GitHub API와 통신하는 GithubClient의 동작을 검증하는 테스트 클래스입니다.
 * <p>
 * 실제 외부 API를 호출하는 대신 {@code MockWebServer}를 사용하여 로컬 환경에서
 * HTTP 가상 서버를 구동하고, 요청 URL, 헤더, 쿼리 파라미터 및 응답 역직렬화 로직을 검증합니다.
 *
 * <p><b>주요 생성자:</b><br>
 * 각 테스트 실행 전 {@code @BeforeEach}를 통해 독립적인 MockWebServer 인스턴스를 생성하고 클라이언트에 주입합니다.
 *
 * <p><b>외부 모듈:</b><br>
 * - OkHttp MockWebServer: 실제 HTTP 호출을 가로채고 가짜 응답(MockResponse)을 반환 <br>
 * - Spring WebFlux (WebClient): 비동기/논블로킹 기반의 HTTP 클라이언트 테스트
 *
 * @author 유재원
 * @since 2026-05-04
 */
class GithubClientTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var githubClient: GithubClient
    private val testToken = "test-github-token"

    @BeforeEach
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        // MockWebServer의 URL을 베이스로 하는 WebClient 생성
        val webClient = WebClient.builder()
            .baseUrl(mockWebServer.url("/").toString())
            .build()

        githubClient = GithubClient(webClient, testToken)
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `searchIssues - 성공 시 결과 리스트를 반환한다`() {
        // Given: 가짜 응답 설정 (JSON)
        val mockResponseBody = """
        {
            "items": [
                { "id": 1, "title": "Issue 1", "html_url": "url1", "repository_url": "url" },
                { "id": 2, "title": "Issue 2", "html_url": "url2", "repository_url": "url" }
            ]
        }
        """.trimIndent()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(mockResponseBody)
        )

        // When
        val result = githubClient.searchIssues("kotlin")

        // Then
        assertThat(result).hasSize(2)
        assertThat(result[0].title).isEqualTo("Issue 1")

        // 요청 쿼리 파라미터 검증
        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.path).contains("q=kotlin%20is:issue%20is:open")
        assertThat(recordedRequest.path).contains("per_page=3")
        assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Bearer $testToken")
    }

    @Test
    fun `searchIssues - 결과가 없을 경우 빈 리스트를 반환한다`() {
        // Given
        val emptyResponse = "{\"items\": []}"
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(emptyResponse)
        )

        // When
        val result = githubClient.searchIssues("unknown")

        // Then
        assertThat(result).isEmpty()
    }
}