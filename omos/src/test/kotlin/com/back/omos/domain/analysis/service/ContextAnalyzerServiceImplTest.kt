package com.back.omos.domain.analysis.service

import com.back.omos.domain.analysis.ai.GlmAnalysisRes
import com.back.omos.domain.analysis.ai.GlmClient
import com.back.omos.domain.analysis.entity.AnalysisResult
import com.back.omos.domain.analysis.entity.UserAnalysisRequest
import com.back.omos.domain.analysis.github.GitHubClient
import com.back.omos.domain.analysis.github.GitHubIssueRes
import com.back.omos.domain.analysis.repository.AnalysisResultRepository
import com.back.omos.domain.analysis.repository.UserAnalysisRequestRepository
import com.back.omos.domain.issue.entity.Issue
import com.back.omos.domain.issue.repository.IssueRepository
import com.back.omos.domain.user.entity.User
import com.back.omos.domain.user.repository.UserRepository
import com.back.omos.global.exception.exceptions.AnalysisException
import com.back.omos.global.exception.exceptions.AuthException
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mockito.lenient
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.springframework.test.context.ActiveProfiles
import java.util.Optional

/**
 * ContextAnalyzerServiceImpl 단위 테스트
 *
 * 외부 의존성(GitHub API, DB)을 Mock으로 대체하여
 * 서비스 로직만 독립적으로 검증합니다.
 *
 * @author Jaewon Ryu
 * @since 2026-04-24
 */
@ExtendWith(MockitoExtension::class)
@ActiveProfiles("test")
class ContextAnalyzerServiceImplTest {

    @Mock
    private lateinit var analysisResultRepository: AnalysisResultRepository

    @Mock
    private lateinit var userAnalysisRequestRepository: UserAnalysisRequestRepository

    @Mock
    private lateinit var issueRepository: IssueRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var gitHubClient: GitHubClient

    @Mock
    private lateinit var glmClient: GlmClient

    @Mock
    private lateinit var mockUser: User

    private lateinit var contextAnalyzerService: ContextAnalyzerServiceImpl

    @BeforeEach
    fun setUp() {
        lenient().`when`(mockUser.id).thenReturn(USER_ID)
        contextAnalyzerService = ContextAnalyzerServiceImpl(
            analysisResultRepository = analysisResultRepository,
            userAnalysisRequestRepository = userAnalysisRequestRepository,
            issueRepository = issueRepository,
            userRepository = userRepository,
            gitHubClient = gitHubClient,
            glmClient = glmClient,
            objectMapper = ObjectMapper()
        )
    }

    companion object {
        private const val GITHUB_ID = "test-user"
        private const val USER_ID = 1L
        private const val ISSUE_ID = 1L
    }

    // ──────────────────────────────────────────
    // 테스트용 픽스처 (공통으로 쓰는 더미 데이터)
    // ──────────────────────────────────────────

    private val mockIssue = Issue(
        repoFullName = "spring-projects/spring-boot",
        issueNumber = 42L,
        title = "Fix NullPointerException",
        content = "이슈 본문",
        status = Issue.IssueStatus.OPEN
    )

    private val mockIssueInfo = GitHubIssueRes(
        number = 42,
        title = "Fix NullPointerException",
        body = "이슈 본문",
        labels = emptyList()
    )

    private val mockAnalysisResult = AnalysisResult(
        issue = mockIssue,
        filePaths = """["src/main/kotlin/com/example/UserService.kt"]""",
        guideline = "TODO: GLM 연동 후 실제 가이드 생성",
        pseudoCode = "TODO: GLM 연동 후 실제 의사 코드 생성",
        sideEffects = "TODO: GLM 연동 후 실제 부작용 분석"
    )

    // ──────────────────────────────────────────
    // getGuide() 테스트
    // ──────────────────────────────────────────

    @Nested
    @DisplayName("getGuide()")
    inner class GetGuide {

        @Test
        @DisplayName("사용자 캐시 HIT - 동일 이슈에 재요청하면 GitHub API 호출 없이 즉시 반환한다")
        fun `사용자 캐시 HIT시 즉시 반환`() {
            // given
            val completedRequest = UserAnalysisRequest(user = mockUser).apply { complete(mockAnalysisResult) }
            given(issueRepository.findById(ISSUE_ID)).willReturn(Optional.of(mockIssue))
            given(userRepository.findByGithubId(GITHUB_ID)).willReturn(Optional.of(mockUser))
            given(userAnalysisRequestRepository.findByUserIdAndAnalysisResultIssueId(USER_ID, ISSUE_ID))
                .willReturn(completedRequest)

            // when
            val result = contextAnalyzerService.getGuide(ISSUE_ID, GITHUB_ID)

            // then
            assertNotNull(result)
            assertEquals("TODO: GLM 연동 후 실제 가이드 생성", result.guideline)
            then(gitHubClient).shouldHaveNoInteractions()
            then(userAnalysisRequestRepository).should().findByUserIdAndAnalysisResultIssueId(USER_ID, ISSUE_ID)
        }

        @Test
        @DisplayName("이슈 캐시 HIT - 다른 사용자의 분석 결과가 있으면 재사용하고 요청 이력을 저장한다")
        fun `이슈 캐시 HIT시 즉시 반환`() {
            // given
            given(issueRepository.findById(ISSUE_ID)).willReturn(Optional.of(mockIssue))
            given(userRepository.findByGithubId(GITHUB_ID)).willReturn(Optional.of(mockUser))
            given(userAnalysisRequestRepository.findByUserIdAndAnalysisResultIssueId(USER_ID, ISSUE_ID))
                .willReturn(null)
            given(userAnalysisRequestRepository.countByUserIdAndCreatedAtBetween(any(), any(), any()))
                .willReturn(0L)
            given(analysisResultRepository.findByIssueId(ISSUE_ID)).willReturn(mockAnalysisResult)

            // when
            val result = contextAnalyzerService.getGuide(ISSUE_ID, GITHUB_ID)

            // then
            assertNotNull(result)
            assertEquals(listOf("src/main/kotlin/com/example/UserService.kt"), result.filePaths)
            assertEquals("TODO: GLM 연동 후 실제 가이드 생성", result.guideline)
            then(gitHubClient).shouldHaveNoInteractions()
            then(userAnalysisRequestRepository).should().save(any())
        }

        @Test
        @DisplayName("캐시 MISS - 분석 결과가 없으면 GitHub API 호출 후 저장한다")
        fun `캐시 MISS시 GitHub API 호출`() {
            // given
            given(issueRepository.findById(ISSUE_ID)).willReturn(Optional.of(mockIssue))
            given(userRepository.findByGithubId(GITHUB_ID)).willReturn(Optional.of(mockUser))
            given(userAnalysisRequestRepository.findByUserIdAndAnalysisResultIssueId(USER_ID, ISSUE_ID))
                .willReturn(null)
            given(userAnalysisRequestRepository.countByUserIdAndCreatedAtBetween(any(), any(), any()))
                .willReturn(0L)
            given(analysisResultRepository.findByIssueId(ISSUE_ID)).willReturn(null)
            given(gitHubClient.fetchIssue("spring-projects", "spring-boot", 42))
                .willReturn(mockIssueInfo)
            given(gitHubClient.fetchTree("spring-projects", "spring-boot"))
                .willReturn(listOf("src/main/kotlin/com/example/UserService.kt"))
            given(glmClient.selectFiles(any(), anyOrNull(), any()))
                .willReturn(listOf("src/main/kotlin/com/example/UserService.kt"))
            given(gitHubClient.fetchFileContent("spring-projects", "spring-boot", "src/main/kotlin/com/example/UserService.kt"))
                .willReturn("fun userService() { ... }")
            given(glmClient.analyze(any(), anyOrNull(), any(), any()))
                .willReturn(GlmAnalysisRes(
                    guideline = "가이드라인",
                    pseudoCode = "의사코드",
                    sideEffects = "부작용"
                ))
            given(analysisResultRepository.save(any())).willReturn(mockAnalysisResult)

            // when
            val result = contextAnalyzerService.getGuide(ISSUE_ID, GITHUB_ID)

            // then
            assertNotNull(result)
            then(gitHubClient).should().fetchIssue("spring-projects", "spring-boot", 42)
            then(gitHubClient).should().fetchTree("spring-projects", "spring-boot")
            then(glmClient).should().selectFiles(any(), anyOrNull(), any())
            then(analysisResultRepository).should().save(any())
            then(userAnalysisRequestRepository).should().save(any())
        }

        @Test
        @DisplayName("이슈가 없으면 AnalysisException을 던진다")
        fun `이슈 없으면 예외 던짐`() {
            // given
            given(issueRepository.findById(999L)).willReturn(Optional.empty())

            // when & then
            assertThrows<AnalysisException> {
                contextAnalyzerService.getGuide(999L, GITHUB_ID)
            }
        }

        @Test
        @DisplayName("사용자가 없으면 AuthException을 던진다")
        fun `사용자 없으면 예외 던짐`() {
            // given
            given(issueRepository.findById(ISSUE_ID)).willReturn(Optional.of(mockIssue))
            given(userRepository.findByGithubId(GITHUB_ID)).willReturn(Optional.empty())

            // when & then
            assertThrows<AuthException> {
                contextAnalyzerService.getGuide(ISSUE_ID, GITHUB_ID)
            }
        }

        @Test
        @DisplayName("일일 분석 요청 횟수 초과 시 AnalysisException을 던진다")
        fun `횟수 초과 시 예외 던짐`() {
            // given
            given(issueRepository.findById(ISSUE_ID)).willReturn(Optional.of(mockIssue))
            given(userRepository.findByGithubId(GITHUB_ID)).willReturn(Optional.of(mockUser))
            given(userAnalysisRequestRepository.findByUserIdAndAnalysisResultIssueId(USER_ID, ISSUE_ID))
                .willReturn(null)
            given(userAnalysisRequestRepository.countByUserIdAndCreatedAtBetween(any(), any(), any()))
                .willReturn(5L)

            // when & then
            assertThrows<AnalysisException> {
                contextAnalyzerService.getGuide(ISSUE_ID, GITHUB_ID)
            }
        }
    }

    // ──────────────────────────────────────────
    // getPseudoCode() 테스트
    // ──────────────────────────────────────────

    @Nested
    @DisplayName("getPseudoCode()")
    inner class GetPseudoCode {

        @Test
        @DisplayName("사용자 캐시 HIT - 동일 이슈에 재요청하면 GitHub API 호출 없이 즉시 반환한다")
        fun `사용자 캐시 HIT시 즉시 반환`() {
            // given
            val completedRequest = UserAnalysisRequest(user = mockUser).apply { complete(mockAnalysisResult) }
            given(issueRepository.findById(ISSUE_ID)).willReturn(Optional.of(mockIssue))
            given(userRepository.findByGithubId(GITHUB_ID)).willReturn(Optional.of(mockUser))
            given(userAnalysisRequestRepository.findByUserIdAndAnalysisResultIssueId(USER_ID, ISSUE_ID))
                .willReturn(completedRequest)

            // when
            val result = contextAnalyzerService.getPseudoCode(ISSUE_ID, GITHUB_ID)

            // then
            assertNotNull(result)
            assertEquals("TODO: GLM 연동 후 실제 의사 코드 생성", result.pseudoCode)
            then(gitHubClient).shouldHaveNoInteractions()
        }

        @Test
        @DisplayName("이슈 캐시 HIT - 다른 사용자의 분석 결과가 있으면 재사용하고 요청 이력을 저장한다")
        fun `이슈 캐시 HIT시 즉시 반환`() {
            // given
            given(issueRepository.findById(ISSUE_ID)).willReturn(Optional.of(mockIssue))
            given(userRepository.findByGithubId(GITHUB_ID)).willReturn(Optional.of(mockUser))
            given(userAnalysisRequestRepository.findByUserIdAndAnalysisResultIssueId(USER_ID, ISSUE_ID))
                .willReturn(null)
            given(userAnalysisRequestRepository.countByUserIdAndCreatedAtBetween(any(), any(), any()))
                .willReturn(0L)
            given(analysisResultRepository.findByIssueId(ISSUE_ID)).willReturn(mockAnalysisResult)

            // when
            val result = contextAnalyzerService.getPseudoCode(ISSUE_ID, GITHUB_ID)

            // then
            assertNotNull(result)
            assertEquals("TODO: GLM 연동 후 실제 의사 코드 생성", result.pseudoCode)
            then(gitHubClient).shouldHaveNoInteractions()
            then(userAnalysisRequestRepository).should().save(any())
        }

        @Test
        @DisplayName("이슈가 없으면 AnalysisException을 던진다")
        fun `이슈 없으면 예외 던짐`() {
            // given
            given(issueRepository.findById(999L)).willReturn(Optional.empty())

            // when & then
            assertThrows<AnalysisException> {
                contextAnalyzerService.getPseudoCode(999L, GITHUB_ID)
            }
        }

        @Test
        @DisplayName("사용자가 없으면 AuthException을 던진다")
        fun `사용자 없으면 예외 던짐`() {
            // given
            given(issueRepository.findById(ISSUE_ID)).willReturn(Optional.of(mockIssue))
            given(userRepository.findByGithubId(GITHUB_ID)).willReturn(Optional.empty())

            // when & then
            assertThrows<AuthException> {
                contextAnalyzerService.getPseudoCode(ISSUE_ID, GITHUB_ID)
            }
        }

        @Test
        @DisplayName("일일 분석 요청 횟수 초과 시 AnalysisException을 던진다")
        fun `횟수 초과 시 예외 던짐`() {
            // given
            given(issueRepository.findById(ISSUE_ID)).willReturn(Optional.of(mockIssue))
            given(userRepository.findByGithubId(GITHUB_ID)).willReturn(Optional.of(mockUser))
            given(userAnalysisRequestRepository.findByUserIdAndAnalysisResultIssueId(USER_ID, ISSUE_ID))
                .willReturn(null)
            given(userAnalysisRequestRepository.countByUserIdAndCreatedAtBetween(any(), any(), any()))
                .willReturn(5L)

            // when & then
            assertThrows<AnalysisException> {
                contextAnalyzerService.getPseudoCode(ISSUE_ID, GITHUB_ID)
            }
        }
    }
}
