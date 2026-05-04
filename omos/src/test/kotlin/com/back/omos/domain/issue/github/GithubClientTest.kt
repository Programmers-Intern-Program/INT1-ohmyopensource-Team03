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
 * 코드에 대한 전체적인 역할을 적습니다.
 * <p>
 * 코드에 대한 작동 원리 등을 적습니다.
 *
 * <p><b>상속 정보:</b><br>
 * 상속 정보를 적습니다.
 *
 * <p><b>주요 생성자:</b><br>
 * {@code ExampleClass(String example)}  <br>
 * 주요 생성자와 그 매개변수에 대한 설명을 적습니다. <br>
 *
 * <p><b>빈 관리:</b><br>
 * 필요 시 빈 관리에 대한 내용을 적습니다.
 *
 * <p><b>외부 모듈:</b><br>
 * 필요 시 외부 모듈에 대한 내용을 적습니다.
 *
 * @author 유재원
 * @since 2026-05-04
 * @see
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
                    { 
                        "id": 1, 
                        "title": "Issue 1", 
                        "html_url": "url1",
                        "repository_url": "https://api.github.com/repos/test/repo" 
                    }
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
        assertThat(recordedRequest.path).contains("q=kotlin+is%3Aissue+is%3Aopen")
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