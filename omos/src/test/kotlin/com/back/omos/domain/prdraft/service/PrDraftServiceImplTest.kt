package com.back.omos.domain.prdraft.service

import com.back.omos.domain.issue.entity.Issue
import com.back.omos.domain.issue.repository.IssueRepository
import com.back.omos.domain.prdraft.ai.AiClient
import com.back.omos.domain.prdraft.ai.AiPrResult
import com.back.omos.domain.prdraft.dto.CreatePrReq
import com.back.omos.domain.prdraft.entity.PrDraft
import com.back.omos.domain.prdraft.github.GitHubClient
import com.back.omos.domain.prdraft.github.GitHubPrRes
import com.back.omos.domain.prdraft.repository.PrDraftRepository
import com.back.omos.domain.user.entity.User
import com.back.omos.domain.user.repository.UserRepository
import com.back.omos.global.exception.exceptions.AuthException
import com.back.omos.global.exception.exceptions.IssueException
import com.back.omos.global.exception.exceptions.PrDraftException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.any
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.verify
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class PrDraftServiceImplTest {

    @Mock private lateinit var prDraftRepository: PrDraftRepository
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var issueRepository: IssueRepository
    @Mock private lateinit var prDraftPromptBuilder: PrDraftPromptBuilder
    @Mock private lateinit var aiClient: AiClient
    @Mock private lateinit var gitHubClient: GitHubClient

    private lateinit var service: PrDraftServiceImpl

    private val githubId = "testUser"
    private val req = CreatePrReq(issueId = 1L, diffContent = "@@ -1 +1 @@\n-old\n+new")
    private val user = User(githubId = githubId)
    private val issue = Issue(repoFullName = "owner/repo", issueNumber = 1L, title = "test issue")

    @BeforeEach
    fun setUp() {
        service = PrDraftServiceImpl(
            prDraftRepository, userRepository, issueRepository,
            prDraftPromptBuilder, aiClient, gitHubClient
        )
    }

    @Nested
    inner class CreateTest {

        @Test
        fun `contributing 있을 때 AI 호출 후 결과를 저장하고 반환한다`() {
            given(userRepository.findByGithubId(githubId)).willReturn(Optional.of(user))
            given(issueRepository.findById(1L)).willReturn(Optional.of(issue))
            given(gitHubClient.fetchContributing("owner/repo")).willReturn("contributing content")
            given(prDraftPromptBuilder.build(req, "contributing content", emptyList())).willReturn("prompt")
            given(aiClient.generatePrDraft("prompt")).willReturn(AiPrResult("feat: title", "body"))
            given(prDraftRepository.save(any(PrDraft::class.java))).willAnswer { it.arguments[0] }

            val result = service.create(githubId, req)

            assertThat(result.title).isEqualTo("feat: title")
            assertThat(result.body).isEqualTo("body")
            assertThat(result.githubUrl).contains("owner/repo")
            verify(prDraftRepository).save(any(PrDraft::class.java))
        }

        @Test
        fun `contributing 없을 때 기존 PR 목록을 가져와 프롬프트를 빌드한다`() {
            val prs = listOf(GitHubPrRes("pr title", "pr body", "2026-01-01"))
            given(userRepository.findByGithubId(githubId)).willReturn(Optional.of(user))
            given(issueRepository.findById(1L)).willReturn(Optional.of(issue))
            given(gitHubClient.fetchContributing("owner/repo")).willReturn(null)
            given(gitHubClient.fetchMergedPrs("owner/repo")).willReturn(prs)
            given(prDraftPromptBuilder.build(req, null, prs)).willReturn("prompt")
            given(aiClient.generatePrDraft("prompt")).willReturn(AiPrResult("title", "body"))
            given(prDraftRepository.save(any(PrDraft::class.java))).willAnswer { it.arguments[0] }

            service.create(githubId, req)

            verify(gitHubClient).fetchMergedPrs("owner/repo")
        }
    }

    @Nested
    inner class GetOneTest {

        @Test
        fun `본인 소유 PR 초안이면 상세 정보를 반환한다`() {
            val prDraft = PrDraft(user = user, issue = issue, diffContent = "diff", prTitle = "feat: title", prBody = "body")
            ReflectionTestUtils.setField(prDraft, "id", 1L)
            given(prDraftRepository.findByIdWithIssueAndUserGithubId(1L, githubId)).willReturn(prDraft)

            val result = service.getOne(githubId, 1L)

            assertThat(result.id).isEqualTo(1L)
            assertThat(result.repoFullName).isEqualTo("owner/repo")
            assertThat(result.issueTitle).isEqualTo("test issue")
            assertThat(result.title).isEqualTo("feat: title")
            assertThat(result.body).isEqualTo("body")
            assertThat(result.diffContent).isEqualTo("diff")
        }

        @Test
        fun `존재하지 않거나 본인 소유가 아닌 PR 초안이면 PrDraftException을 던진다`() {
            given(prDraftRepository.findByIdWithIssueAndUserGithubId(1L, githubId)).willReturn(null)

            assertThatThrownBy { service.getOne(githubId, 1L) }
                .isInstanceOf(PrDraftException::class.java)
        }
    }

    @Nested
    inner class GetHistoryTest {

        @Test
        fun `PR 초안 목록을 최신순으로 반환한다`() {
            val prDraft = PrDraft(user = user, issue = issue, diffContent = "diff", prTitle = "feat: title", prBody = "body")
            ReflectionTestUtils.setField(prDraft, "id", 1L)
            given(prDraftRepository.findAllWithIssueByUserGithubId(githubId)).willReturn(listOf(prDraft))

            val result = service.getHistory(githubId)

            assertThat(result).hasSize(1)
            assertThat(result[0].id).isEqualTo(1L)
            assertThat(result[0].repoFullName).isEqualTo("owner/repo")
            assertThat(result[0].issueTitle).isEqualTo("test issue")
            assertThat(result[0].title).isEqualTo("feat: title")
        }

        @Test
        fun `PR 초안이 없으면 빈 목록을 반환한다`() {
            given(prDraftRepository.findAllWithIssueByUserGithubId(githubId)).willReturn(emptyList())

            val result = service.getHistory(githubId)

            assertThat(result).isEmpty()
        }
    }

    @Nested
    inner class DeleteTest {

        @Test
        fun `본인 소유 PR 초안이면 삭제한다`() {
            val prDraft = PrDraft(user = user, issue = issue, diffContent = "diff", prTitle = "feat: title", prBody = "body")
            ReflectionTestUtils.setField(prDraft, "id", 1L)
            given(prDraftRepository.findByIdAndUserGithubId(1L, githubId)).willReturn(prDraft)

            service.delete(githubId, 1L)

            verify(prDraftRepository).delete(prDraft)
        }

        @Test
        fun `존재하지 않거나 본인 소유가 아닌 PR 초안이면 PrDraftException을 던진다`() {
            given(prDraftRepository.findByIdAndUserGithubId(1L, githubId)).willReturn(null)

            assertThatThrownBy { service.delete(githubId, 1L) }
                .isInstanceOf(PrDraftException::class.java)
        }
    }

    @Nested
    inner class ExceptionTest {

        @Test
        fun `존재하지 않는 githubId면 AuthException을 던진다`() {
            given(userRepository.findByGithubId(githubId)).willReturn(Optional.empty())

            assertThatThrownBy { service.create(githubId, req) }
                .isInstanceOf(AuthException::class.java)
        }

        @Test
        fun `존재하지 않는 issueId면 IssueException을 던진다`() {
            given(userRepository.findByGithubId(githubId)).willReturn(Optional.of(user))
            given(issueRepository.findById(1L)).willReturn(Optional.empty())

            assertThatThrownBy { service.create(githubId, req) }
                .isInstanceOf(IssueException::class.java)
        }
    }
}
