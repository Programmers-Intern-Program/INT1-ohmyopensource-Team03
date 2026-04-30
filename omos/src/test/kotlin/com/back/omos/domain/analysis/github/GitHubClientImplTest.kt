package com.back.omos.domain.analysis.github

import com.back.omos.global.exception.exceptions.AnalysisException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import java.util.Base64

/**
 * GitHubClientImpl 단위 테스트
 *
 * MockWebServer를 사용하여 실제 HTTP 서버를 로컬에 띄우고
 * 응답 시나리오별로 GitHubClientImpl 동작을 검증합니다.
 *
 * @author Jaewon Ryu
 * @since 2026-04-29
 */
@ActiveProfiles("test")
@DisplayName("GitHubClientImpl 단위 테스트")
class GitHubClientImplTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var gitHubClient: GitHubClientImpl

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        gitHubClient = GitHubClientImpl(
            token = "test-token",
            baseUrl = "http://localhost:${mockWebServer.port}"
        )
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // ──────────────────────────────────────────
    // fetchTree() 테스트
    // ──────────────────────────────────────────

    @Nested
    @DisplayName("fetchTree()")
    inner class FetchTree {

        @Test
        @DisplayName("truncated=true 응답 시 AnalysisException을 던진다")
        fun `truncated true 응답시 예외`() {
            // given: 대형 레포에서 GitHub가 결과를 잘라낸 경우
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .addHeader("Content-Type", "application/json")
                    .setBody("""{"tree": [{"path": "src/Foo.kt", "type": "blob"}], "truncated": true}""")
            )

            // when & then
            assertThrows(AnalysisException::class.java) {
                gitHubClient.fetchTree("owner", "repo")
            }
        }

        @Test
        @DisplayName("정상 응답 시 blob 타입 파일 경로만 반환한다")
        fun `정상 응답시 blob 경로 목록 반환`() {
            // given: blob(파일)과 tree(디렉토리)가 혼재한 응답
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .addHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                            "tree": [
                                {"path": "src/main/Foo.kt", "type": "blob"},
                                {"path": "src/main", "type": "tree"},
                                {"path": "src/main/Bar.kt", "type": "blob"}
                            ],
                            "truncated": false
                        }
                        """.trimIndent()
                    )
            )

            // when
            val result = gitHubClient.fetchTree("owner", "repo")

            // then: tree 타입은 제외하고 blob만 반환
            assertEquals(listOf("src/main/Foo.kt", "src/main/Bar.kt"), result)
        }

        @Test
        @DisplayName("HTTP 오류 응답 시 AnalysisException을 던진다")
        fun `HTTP 오류 응답시 예외`() {
            // given: 403 Forbidden (Rate Limit 초과 등)
            mockWebServer.enqueue(MockResponse().setResponseCode(403))

            // when & then
            assertThrows(AnalysisException::class.java) {
                gitHubClient.fetchTree("owner", "repo")
            }
        }
    }

    // ──────────────────────────────────────────
    // fetchFileContent() 테스트
    // ──────────────────────────────────────────

    @Nested
    @DisplayName("fetchFileContent()")
    inner class FetchFileContent {

        @Test
        @DisplayName("404 응답 시 null을 반환한다")
        fun `404 응답시 null 반환`() {
            // given: 파일이 존재하지 않는 경우
            mockWebServer.enqueue(MockResponse().setResponseCode(404))

            // when
            val result = gitHubClient.fetchFileContent("owner", "repo", "src/NotExist.kt")

            // then
            assertNull(result)
        }

        @Test
        @DisplayName("404 외 HTTP 오류(500 등) 시 로그를 남기고 null을 반환한다")
        fun `500 응답시 null 반환`() {
            // given: 서버 내부 오류
            mockWebServer.enqueue(MockResponse().setResponseCode(500))

            // when
            val result = gitHubClient.fetchFileContent("owner", "repo", "src/Foo.kt")

            // then: 예외가 전파되지 않고 null 반환
            assertNull(result)
        }

        @Test
        @DisplayName("정상 응답 시 Base64 디코딩된 파일 내용을 반환한다")
        fun `정상 응답시 디코딩된 파일 내용 반환`() {
            // given: GitHub API는 파일 내용을 Base64로 인코딩하여 반환
            val originalContent = "fun main() { println(\"Hello, World!\") }"
            val encoded = Base64.getEncoder().encodeToString(originalContent.toByteArray())
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .addHeader("Content-Type", "application/json")
                    .setBody("""{"content": "$encoded"}""")
            )

            // when
            val result = gitHubClient.fetchFileContent("owner", "repo", "src/Main.kt")

            // then
            assertEquals(originalContent, result)
        }

        @Test
        @DisplayName("content 필드가 null인 응답 시 null을 반환한다")
        fun `content null 응답시 null 반환`() {
            // given: 빈 파일이나 바이너리 파일의 경우 content가 null
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .addHeader("Content-Type", "application/json")
                    .setBody("""{"content": null}""")
            )

            // when
            val result = gitHubClient.fetchFileContent("owner", "repo", "src/Empty.kt")

            // then
            assertNull(result)
        }
    }

    // ──────────────────────────────────────────
    // fetchIssue() 테스트
    // ──────────────────────────────────────────

    @Nested
    @DisplayName("fetchIssue()")
    inner class FetchIssue {

        @Test
        @DisplayName("정상 응답 시 GitHubIssueRes를 반환한다")
        fun `정상 응답시 이슈 정보 반환`() {
            // given
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .addHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                            "number": 42,
                            "title": "Fix NullPointerException",
                            "body": "이슈 본문",
                            "labels": [{"name": "bug"}, {"name": "good first issue"}]
                        }
                        """.trimIndent()
                    )
            )

            // when
            val result = gitHubClient.fetchIssue("owner", "repo", 42)

            // then
            assertEquals(42, result.number)
            assertEquals("Fix NullPointerException", result.title)
            assertEquals("이슈 본문", result.body)
            assertEquals(listOf("bug", "good first issue"), result.labels.map { it.name })
        }

        @Test
        @DisplayName("HTTP 오류 응답 시 AnalysisException을 던진다")
        fun `HTTP 오류 응답시 예외`() {
            // given: 404 (이슈 없음) 또는 기타 오류
            mockWebServer.enqueue(MockResponse().setResponseCode(404))

            // when & then
            assertThrows(AnalysisException::class.java) {
                gitHubClient.fetchIssue("owner", "repo", 999)
            }
        }
    }
}
