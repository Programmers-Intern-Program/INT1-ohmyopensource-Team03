package com.back.omos.domain.prdraft.service

import com.back.omos.domain.issue.entity.Issue
import com.back.omos.domain.issue.repository.IssueRepository
import com.back.omos.domain.prdraft.ai.AiClient
import com.back.omos.domain.prdraft.ai.AiPrResult
import com.back.omos.domain.prdraft.dto.CreatePrReq
import com.back.omos.domain.prdraft.dto.UpdatePrReq
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
    private val req = CreatePrReq(upstreamRepo = "owner/repo", githubIssueNumber = 1L, baseBranch = "main", headBranch = "fix/issue-123")
    private val user = User(githubId = githubId)
    private val issue = Issue(repoFullName = "owner/repo", issueNumber = 1L, title = "test issue")
    private val diffContent = "@@ -1 +1 @@\n-old\n+new"

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
            given(issueRepository.findByRepoFullNameAndIssueNumber("owner/repo", 1L)).willReturn(issue)
            given(gitHubClient.fetchDiff("owner/repo", "main", githubId, "fix/issue-123")).willReturn(diffContent)
            given(gitHubClient.fetchContributing("owner/repo")).willReturn("contributing content")
            given(prDraftPromptBuilder.build(req, diffContent, "contributing content", emptyList())).willReturn("prompt")
            given(aiClient.generatePrDraft("prompt")).willReturn(AiPrResult("feat: title", "body"))
            given(prDraftRepository.save(any(PrDraft::class.java))).willAnswer {
                val prDraft = it.arguments[0] as PrDraft
                ReflectionTestUtils.setField(prDraft, "id", 1L)
                prDraft
            }

            val result = service.create(githubId, req)

            assertThat(result.id).isEqualTo(1L)
            assertThat(result.title).isEqualTo("feat: title")
            assertThat(result.body).isEqualTo("body")
            verify(prDraftRepository).save(any(PrDraft::class.java))
        }

        @Test
        fun `contributing 없을 때 기존 PR 목록을 가져와 프롬프트를 빌드한다`() {
            val prs = listOf(GitHubPrRes("pr title", "pr body", "2026-01-01"))
            given(userRepository.findByGithubId(githubId)).willReturn(Optional.of(user))
            given(issueRepository.findByRepoFullNameAndIssueNumber("owner/repo", 1L)).willReturn(issue)
            given(gitHubClient.fetchDiff("owner/repo", "main", githubId, "fix/issue-123")).willReturn(diffContent)
            given(gitHubClient.fetchContributing("owner/repo")).willReturn(null)
            given(gitHubClient.fetchMergedPrs("owner/repo")).willReturn(prs)
            given(prDraftPromptBuilder.build(req, diffContent, null, prs)).willReturn("prompt")
            given(aiClient.generatePrDraft("prompt")).willReturn(AiPrResult("title", "body"))
            given(prDraftRepository.save(any(PrDraft::class.java))).willAnswer {
                val prDraft = it.arguments[0] as PrDraft
                ReflectionTestUtils.setField(prDraft, "id", 1L)
                prDraft
            }

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
    inner class UpdateTest {

        @Test
        fun `title과 body를 모두 전달하면 둘 다 수정된다`() {
            val prDraft = PrDraft(user = user, issue = issue, diffContent = "diff", prTitle = "old title", prBody = "old body")
            ReflectionTestUtils.setField(prDraft, "id", 1L)
            given(prDraftRepository.findByIdWithIssueAndUserGithubId(1L, githubId)).willReturn(prDraft)
            given(prDraftRepository.save(any(PrDraft::class.java))).willAnswer { it.arguments[0] }

            val result = service.update(githubId, 1L, UpdatePrReq("new title", "new body"))

            assertThat(result.title).isEqualTo("new title")
            assertThat(result.body).isEqualTo("new body")
        }

        @Test
        fun `title이 null이면 기존 title을 유지한다`() {
            val prDraft = PrDraft(user = user, issue = issue, diffContent = "diff", prTitle = "old title", prBody = "old body")
            ReflectionTestUtils.setField(prDraft, "id", 1L)
            given(prDraftRepository.findByIdWithIssueAndUserGithubId(1L, githubId)).willReturn(prDraft)
            given(prDraftRepository.save(any(PrDraft::class.java))).willAnswer { it.arguments[0] }

            val result = service.update(githubId, 1L, UpdatePrReq(null, "new body"))

            assertThat(result.title).isEqualTo("old title")
            assertThat(result.body).isEqualTo("new body")
        }

        @Test
        fun `body가 null이면 기존 body를 유지한다`() {
            val prDraft = PrDraft(user = user, issue = issue, diffContent = "diff", prTitle = "old title", prBody = "old body")
            ReflectionTestUtils.setField(prDraft, "id", 1L)
            given(prDraftRepository.findByIdWithIssueAndUserGithubId(1L, githubId)).willReturn(prDraft)
            given(prDraftRepository.save(any(PrDraft::class.java))).willAnswer { it.arguments[0] }

            val result = service.update(githubId, 1L, UpdatePrReq("new title", null))

            assertThat(result.title).isEqualTo("new title")
            assertThat(result.body).isEqualTo("old body")
        }

        @Test
        fun `존재하지 않거나 본인 소유가 아닌 PR 초안이면 PrDraftException을 던진다`() {
            given(prDraftRepository.findByIdWithIssueAndUserGithubId(1L, githubId)).willReturn(null)

            assertThatThrownBy { service.update(githubId, 1L, UpdatePrReq("new title", "new body")) }
                .isInstanceOf(PrDraftException::class.java)
        }
    }

    @Nested
    inner class TranslateTest {

        @Test
        fun `본인 소유 PR 초안이면 번역 결과와 GitHub URL을 반환한다`() {
            val prDraft = PrDraft(user = user, issue = issue, diffContent = "diff", prTitle = "feat: 제목", prBody = "본문")
            ReflectionTestUtils.setField(prDraft, "id", 1L)
            given(prDraftRepository.findByIdWithIssueAndUserGithubId(1L, githubId)).willReturn(prDraft)
            given(aiClient.translate("feat: 제목", "본문")).willReturn(AiPrResult("feat: title", "body"))

            val result = service.translate(githubId, 1L)

            assertThat(result.titleEn).isEqualTo("feat: title")
            assertThat(result.bodyEn).isEqualTo("body")
            assertThat(result.githubUrl).contains("owner/repo")
            assertThat(result.githubUrl).contains("quick_pull=1")
        }

        @Test
        fun `존재하지 않거나 본인 소유가 아닌 PR 초안이면 PrDraftException을 던진다`() {
            given(prDraftRepository.findByIdWithIssueAndUserGithubId(1L, githubId)).willReturn(null)

            assertThatThrownBy { service.translate(githubId, 1L) }
                .isInstanceOf(PrDraftException::class.java)
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
        fun `존재하지 않는 issue면 IssueException을 던진다`() {
            given(userRepository.findByGithubId(githubId)).willReturn(Optional.of(user))
            given(issueRepository.findByRepoFullNameAndIssueNumber("owner/repo", 1L)).willReturn(null)

            assertThatThrownBy { service.create(githubId, req) }
                .isInstanceOf(IssueException::class.java)
        }
    }
}
