package com.back.omos.domain.prdraft.github

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File

class GitHubClientImplTest {

    private val token = File(".env").takeIf { it.exists() }?.readLines()
        ?.firstOrNull { it.startsWith("GITHUB_TOKEN=") }
        ?.removePrefix("GITHUB_TOKEN=")
        ?.trim()
        ?: System.getenv("GITHUB_TOKEN")
        ?: ""

    private val gitHubClient = GitHubClientImpl(token)

    @Nested
    inner class ContributingTest {
        @Test
        fun `CONTRIBUTING_md가 있는 레포에서 내용을 가져온다`() {
            val result = gitHubClient.fetchContributing("facebook/react")

            assertThat(result).isNotNull()
            println("=== CONTRIBUTING.md 내용 ===")
            println(result)
        }

        @Test
        fun `github_폴더에 있는 CONTRIBUTING_md도 가져온다`() {
            val result = gitHubClient.fetchContributing("kubernetes/kubernetes")

            assertThat(result).isNotNull()
            println("=== .github/CONTRIBUTING.md 내용 ===")
            println(result)
        }

        @Test
        fun `CONTRIBUTING_adoc인 레포에서도 내용을 가져온다`() {
            val result = gitHubClient.fetchContributing("spring-projects/spring-framework")

            assertThat(result).isNotNull()
            println("=== CONTRIBUTING.adoc 내용 ===")
            println(result)
        }

        @Test
        fun `CONTRIBUTING_md가 없는 레포에서 null을 반환한다`() {
            val result = gitHubClient.fetchContributing("torvalds/linux")

            assertThat(result).isNull()
        }
    }

    @Nested
    inner class PullRequestTest {
        @Test
        fun `merged PR 목록을 가져온다`() {
            val result = gitHubClient.fetchMergedPrs("facebook/react")

            assertThat(result).isNotEmpty()
            assertThat(result.size).isLessThanOrEqualTo(10)
            println("=== merged PR 목록 ===")
            result.forEach {
                println("제목: ${it.title}")
                //println("내용: ${it.body}")
                //println("========================================")
            }
        }

        @Test
        fun `존재하지 않는 레포에서 빈 리스트를 반환한다`() {
            val result = gitHubClient.fetchMergedPrs("omos-nonexistent/repo")

            assertThat(result).isEmpty()
        }
    }
}
