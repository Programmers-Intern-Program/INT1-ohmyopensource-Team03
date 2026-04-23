package com.back.omos.domain.prdraft.github

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GitHubClientImplTest {

    private val gitHubClient = GitHubClientImpl(System.getenv("GITHUB_TOKEN") ?: "")

    @Test
    fun `CONTRIBUTING_md가 있는 레포에서 내용을 가져온다`() {
        val result = gitHubClient.fetchContributing("angular/angular")

        assertThat(result).isNotNull()
        println("=== CONTRIBUTING.md 내용 ===")
        println(result)
    }

    @Test
    fun `CONTRIBUTING_md가 없는 레포에서 null을 반환한다`() {
        val result = gitHubClient.fetchContributing("torvalds/linux")

        assertThat(result).isNull()
    }
}
