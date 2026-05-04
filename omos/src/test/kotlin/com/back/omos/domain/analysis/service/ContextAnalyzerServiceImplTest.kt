package com.back.omos.domain.analysis.service

import com.back.omos.domain.analysis.ai.GlmAnalysisRes
import com.back.omos.domain.analysis.ai.GlmClient
import com.back.omos.domain.analysis.entity.AnalysisResult
import com.back.omos.domain.analysis.entity.UserAnalysisRequest
import com.back.omos.domain.analysis.github.GitHubClient
import com.back.omos.domain.analysis.github.GitHubIssueRes
import com.back.omos.domain.analysis.repository.AnalysisResultRepository
import com.back.omos.domain.analysis.repository.UserAnalysisRequestRepository
import org.springframework.test.util.ReflectionTestUtils
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
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.springframework.test.context.ActiveProfiles
import java.util.Optional
import java.time.LocalDateTime

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

    companion object {
        private const val GITHUB_ID = "test-user"
        private const val USER_ID = 1L
        private const val ISSUE_ID = 1L
    }

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
        labels = emptyList(),
        updatedAt = null
    )

    private val mockAnalysisResult = AnalysisResult(
        issue = mockIssue,
        filePaths = """["src/main/kotlin/com/example/UserService.kt"]""",
        guideline = "TODO: GLM 연동 후 실제 가이드 생성",
        pseudoCode = "TODO: GLM 연동 후 실제 의사 코드 생성",
        sideEffects = "TODO: GLM 연동 후 실제 부작용 분석"
    )

    @Nested
    @DisplayName("getGuide()")
    inner class GetGuide {

        @Test
        @DisplayName("사용자 캐시 HIT - 동일 이슈에 재요청하면 GitHub API 호출 없이 즉시 반환한다")
        fun `사용자 캐시 HIT시 즉시 반환`() {
            val completedRequest = UserAnalysisRequest(user = mockUser).apply { complete(mockAnalysisResult) }
            given(issueRepository.findById(ISSUE_ID)).willReturn(Optional.of(mockIssue))
            given(userRepository.findByGithubIdWithLock(GITHUB_ID)).willReturn(Optional.of(mockUser))
            given(userAnalysisRequestRepository.findFirstByUserIdAndAnalysisResultIssueId(eq(USER_ID), eq(ISSUE_ID)))
                .willReturn(completedRequest)

            val result = contextAnalyzerService.getGuide(ISSUE_ID, GITHUB_ID)

            assertNotNull(result)
            assertEquals("TODO: GLM 연동 후 실제 가이드 생성", result.guideline)
            then(gitHubClient).shouldHaveNoInteractions()
        }

        @Test
        @DisplayName("이슈 캐시 HIT - 다른 사용자의 분석 결과가 있으면 재사용하고 요청 이력을 저장한다")
        fun `이슈 캐시 HIT시 즉시 반환`() {
            given(issueRepository.findById(ISSUE_ID)).willReturn(Optional.of(mockIssue))
            given(userRepository.findByGithubIdWithLock(GITHUB_ID)).willReturn(Optional.of(mockUser))
            given(userAnalysisRequestRepository.findFirstByUserIdAndAnalysisResultIssueId(eq(USER_ID), eq(ISSUE_ID)))
                .willReturn(null)
            given(userAnalysisRequestRepository.countByUserIdAndCreatedAtBetween(any(), any(), any()))
                .willReturn(0L)
            given(analysisResultRepository.findByIssueId(ISSUE_ID)).willReturn(mockAnalysisResult)

            val result = contextAnalyzerService.getGuide(ISSUE_ID, GITHUB_ID)

            assertNotNull(result)
            assertEquals("TODO: GLM 연동 후 실제 가이드 생성", result.guideline)
            then(gitHubClient).shouldHaveNoInteractions()
            then(userAnalysisRequestRepository).should().save(any())
        }

        @Test
        @DisplayName("캐시 MISS - 분석 결과가 없으면 GitHub API 호출 후 저장한다")
        fun `캐시 MISS시 GitHub API 호출`() {
            given(issueRepository.findById(ISSUE_ID)).willReturn(Optional.of(mockIssue))
            given(userRepository.findByGithubIdWithLock(GITHUB_ID)).willReturn(Optional.of(mockUser))
            given(userAnalysisRequestRepository.findFirstByUserIdAndAnalysisResultIssueId(eq(USER_ID), eq(ISSUE_ID)))
                .willReturn(null)
            given(userAnalysisRequestRepository.countByUserIdAndCreatedAtBetween(any(), any(), any()))
                .willReturn(0L)
            given(analysisResultRepository.findByIssueId(ISSUE_ID)).willReturn(null)
            given(gitHubClient.fetchIssue("spring-projects", "spring-boot", 42)).willReturn(mockIssueInfo)
            given(gitHubClient.fetchTree("spring-projects", "spring-boot"))
                .willReturn(listOf("src/main/kotlin/com/example/UserService.kt"))
            given(glmClient.selectFiles(any(), anyOrNull(), any()))
                .willReturn(listOf("src/main/kotlin/com/example/UserService.kt"))
            given(gitHubClient.fetchFileContents(eq("spring-projects"), eq("spring-boot"), any()))
                .willReturn(mapOf("src/main/kotlin/com/example/UserService.kt" to "fun userService() { ... }"))
            given(glmClient.analyze(any(), anyOrNull(), any(), any()))
                .willReturn(GlmAnalysisRes(guideline = "가이드라인", pseudoCode = "의사코드", sideEffects = "부작용"))
            given(analysisResultRepository.save(any())).willReturn(mockAnalysisResult)

            val result = contextAnalyzerService.getGuide(ISSUE_ID, GITHUB_ID)

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
            given(issueRepository.findById(999L)).willReturn(Optional.empty())

            assertThrows<AnalysisException> {
                contextAnalyzerService.getGuide(999L, GITHUB_ID)
            }
        }

        @Test
        @DisplayName("사용자가 없으면 AuthException을 던진다")
        fun `사용자 없으면 예외 던짐`() {
            given(issueRepository.findById(ISSUE_ID)).willReturn(Optional.of(mockIssue))
            given(userRepository.findByGithubIdWithLock(GITHUB_ID)).willReturn(Optional.empty())

            assertThrows<AuthException> {
                contextAnalyzerService.getGuide(ISSUE_ID, GITHUB_ID)
            }
        }

        @Test
        @DisplayName("일일 분석 요청 횟수 초과 시 AnalysisException을 던진다")
        fun `횟수 초과 시 예외 던짐`() {
            given(issueRepository.findById(ISSUE_ID)).willReturn(Optional.of(mockIssue))
            given(userRepository.findByGithubIdWithLock(GITHUB_ID)).willReturn(Optional.of(mockUser))
            given(userAnalysisRequestRepository.findFirstByUserIdAndAnalysisResultIssueId(eq(USER_ID), eq(ISSUE_ID)))
                .willReturn(null)
            given(userAnalysisRequestRepository.countByUserIdAndCreatedAtBetween(any(), any(), any()))
                .willReturn(5L)

            assertThrows<AnalysisException> {
                contextAnalyzerService.getGuide(ISSUE_ID, GITHUB_ID)
            }
        }

        @Test
        @DisplayName("repoFullName이 'owner/repo' 형식이 아니면 AnalysisException을 던진다")
        fun `repoFullName 형식 오류시 예외`() {
            val badIssue = Issue(
                repoFullName = "invalid-format",
                issueNumber = 42L,
                title = "Fix NullPointerException",
                content = "이슈 본문",
                status = Issue.IssueStatus.OPEN
            )
            given(issueRepository.findById(ISSUE_ID)).willReturn(Optional.of(badIssue))
            // 나머지 stub 다 제거!

            assertThrows<AnalysisException> {
                contextAnalyzerService.getGuide(ISSUE_ID, GITHUB_ID)
            }
            then(gitHubClient).shouldHaveNoInteractions()
        }

        @Test
        @DisplayName("GLM이 모든 배치에서 빈 파일 목록을 반환하면 AnalysisException을 던진다")
        fun `GLM 빈 파일 목록 반환시 예외 던짐`() {
            given(issueRepository.findById(ISSUE_ID)).willReturn(Optional.of(mockIssue))
            given(userRepository.findByGithubIdWithLock(GITHUB_ID)).willReturn(Optional.of(mockUser))
            given(userAnalysisRequestRepository.findFirstByUserIdAndAnalysisResultIssueId(eq(USER_ID), eq(ISSUE_ID)))
                .willReturn(null)
            given(userAnalysisRequestRepository.countByUserIdAndCreatedAtBetween(any(), any(), any()))
                .willReturn(0L)
            given(analysisResultRepository.findByIssueId(ISSUE_ID)).willReturn(null)
            given(gitHubClient.fetchIssue("spring-projects", "spring-boot", 42)).willReturn(mockIssueInfo)
            given(gitHubClient.fetchTree("spring-projects", "spring-boot"))
                .willReturn(listOf("src/main/kotlin/com/example/UserService.kt"))
            given(glmClient.selectFiles(any(), anyOrNull(), any())).willReturn(emptyList())

            assertThrows<AnalysisException> {
                contextAnalyzerService.getGuide(ISSUE_ID, GITHUB_ID)
            }
        }

        @Nested
        @DisplayName("getPseudoCode()")
        inner class GetPseudoCode {

            @Test
            @DisplayName("사용자 캐시 HIT - 동일 이슈에 재요청하면 GitHub API 호출 없이 즉시 반환한다")
            fun `사용자 캐시 HIT시 즉시 반환`() {
                val completedRequest = UserAnalysisRequest(user = mockUser).apply { complete(mockAnalysisResult) }
                given(issueRepository.findById(ISSUE_ID)).willReturn(Optional.of(mockIssue))
                given(userRepository.findByGithubIdWithLock(GITHUB_ID)).willReturn(Optional.of(mockUser))
                given(
                    userAnalysisRequestRepository.findFirstByUserIdAndAnalysisResultIssueId(
                        eq(USER_ID),
                        eq(ISSUE_ID)
                    )
                )
                    .willReturn(completedRequest)

                val result = contextAnalyzerService.getPseudoCode(ISSUE_ID, GITHUB_ID)

                assertNotNull(result)
                assertEquals("TODO: GLM 연동 후 실제 의사 코드 생성", result.pseudoCode)
                then(gitHubClient).shouldHaveNoInteractions()
            }

            @Test
            @DisplayName("이슈 캐시 HIT - 다른 사용자의 분석 결과가 있으면 재사용하고 요청 이력을 저장한다")
            fun `이슈 캐시 HIT시 즉시 반환`() {
                given(issueRepository.findById(ISSUE_ID)).willReturn(Optional.of(mockIssue))
                given(userRepository.findByGithubIdWithLock(GITHUB_ID)).willReturn(Optional.of(mockUser))
                given(
                    userAnalysisRequestRepository.findFirstByUserIdAndAnalysisResultIssueId(
                        eq(USER_ID),
                        eq(ISSUE_ID)
                    )
                )
                    .willReturn(null)
                given(userAnalysisRequestRepository.countByUserIdAndCreatedAtBetween(any(), any(), any()))
                    .willReturn(0L)
                given(analysisResultRepository.findByIssueId(ISSUE_ID)).willReturn(mockAnalysisResult)

                val result = contextAnalyzerService.getPseudoCode(ISSUE_ID, GITHUB_ID)

                assertNotNull(result)
                assertEquals("TODO: GLM 연동 후 실제 의사 코드 생성", result.pseudoCode)
                then(gitHubClient).shouldHaveNoInteractions()
                then(userAnalysisRequestRepository).should().save(any())
            }

            @Test
            @DisplayName("이슈가 없으면 AnalysisException을 던진다")
            fun `이슈 없으면 예외 던짐`() {
                given(issueRepository.findById(999L)).willReturn(Optional.empty())

                assertThrows<AnalysisException> {
                    contextAnalyzerService.getPseudoCode(999L, GITHUB_ID)
                }
            }

            @Test
            @DisplayName("사용자가 없으면 AuthException을 던진다")
            fun `사용자 없으면 예외 던짐`() {
                given(issueRepository.findById(ISSUE_ID)).willReturn(Optional.of(mockIssue))
                given(userRepository.findByGithubIdWithLock(GITHUB_ID)).willReturn(Optional.empty())

                assertThrows<AuthException> {
                    contextAnalyzerService.getPseudoCode(ISSUE_ID, GITHUB_ID)
                }
            }

            @Test
            @DisplayName("일일 분석 요청 횟수 초과 시 AnalysisException을 던진다")
            fun `횟수 초과 시 예외 던짐`() {
                given(issueRepository.findById(ISSUE_ID)).willReturn(Optional.of(mockIssue))
                given(userRepository.findByGithubIdWithLock(GITHUB_ID)).willReturn(Optional.of(mockUser))
                given(
                    userAnalysisRequestRepository.findFirstByUserIdAndAnalysisResultIssueId(
                        eq(USER_ID),
                        eq(ISSUE_ID)
                    )
                )
                    .willReturn(null)
                given(userAnalysisRequestRepository.countByUserIdAndCreatedAtBetween(any(), any(), any()))
                    .willReturn(5L)

                assertThrows<AnalysisException> {
                    contextAnalyzerService.getPseudoCode(ISSUE_ID, GITHUB_ID)
                }
            }

            @Test
            @DisplayName("캐시 3일 이내 - GitHub API 호출 없이 기존 캐시 반환한다")
            fun `캐시 3일 이내 기존 캐시 반환`() {
                val recentResult = AnalysisResult(
                    issue = mockIssue,
                    filePaths = """["src/main/kotlin/com/example/UserService.kt"]""",
                    guideline = "최신 가이드",
                    pseudoCode = "최신 코드",
                    sideEffects = "최신 부작용"
                )
                // createdAt 기본값이 현재 시각이라 그대로 두면 3일 이내

                given(issueRepository.findById(ISSUE_ID)).willReturn(Optional.of(mockIssue))
                given(userRepository.findByGithubIdWithLock(GITHUB_ID)).willReturn(Optional.of(mockUser))
                given(
                    userAnalysisRequestRepository.findFirstByUserIdAndAnalysisResultIssueId(
                        eq(USER_ID),
                        eq(ISSUE_ID)
                    )
                )
                    .willReturn(null)
                given(userAnalysisRequestRepository.countByUserIdAndCreatedAtBetween(any(), any(), any()))
                    .willReturn(0L)
                given(analysisResultRepository.findByIssueId(ISSUE_ID)).willReturn(recentResult)
                given(userAnalysisRequestRepository.save(any())).willReturn(any())

                val result = contextAnalyzerService.getGuide(ISSUE_ID, GITHUB_ID)

                assertNotNull(result)
                // fetchIssue 호출 안 됨
                then(gitHubClient).shouldHaveNoInteractions()
            }

            @Test
            @DisplayName("캐시 3일 초과 + 이슈 수정 안 됨 - 기존 캐시 반환한다")
            fun `캐시 3일 초과 이슈 수정 안됨 기존 캐시 반환`() {
                val oldResult = AnalysisResult(
                    issue = mockIssue,
                    filePaths = """["src/main/kotlin/com/example/UserService.kt"]""",
                    guideline = "오래된 가이드",
                    pseudoCode = "오래된 코드",
                    sideEffects = "오래된 부작용"
                )
                ReflectionTestUtils.setField(oldResult, "createdAt", LocalDateTime.now().minusDays(4))

                val latestIssueInfo = GitHubIssueRes(
                    number = 42,
                    title = "Fix NullPointerException",
                    body = "이슈 본문",
                    labels = emptyList(),
                    updatedAt = LocalDateTime.now().minusDays(5)  // createdAt보다 이전
                )

                given(issueRepository.findById(ISSUE_ID)).willReturn(Optional.of(mockIssue))
                given(userRepository.findByGithubIdWithLock(GITHUB_ID)).willReturn(Optional.of(mockUser))
                given(
                    userAnalysisRequestRepository.findFirstByUserIdAndAnalysisResultIssueId(
                        eq(USER_ID),
                        eq(ISSUE_ID)
                    )
                )
                    .willReturn(null)
                given(userAnalysisRequestRepository.countByUserIdAndCreatedAtBetween(any(), any(), any()))
                    .willReturn(0L)
                given(analysisResultRepository.findByIssueId(ISSUE_ID)).willReturn(oldResult)
                given(gitHubClient.fetchIssue("spring-projects", "spring-boot", 42)).willReturn(latestIssueInfo)
                given(userAnalysisRequestRepository.save(any())).willReturn(any())

                val result = contextAnalyzerService.getGuide(ISSUE_ID, GITHUB_ID)

                assertNotNull(result)
                assertEquals("오래된 가이드", result.guideline)
                then(analysisResultRepository).shouldHaveNoMoreInteractions()
            }

            @Test
            @DisplayName("캐시 3일 초과 + 이슈 수정됨 - 캐시 삭제 후 재생성한다")
            fun `캐시 3일 초과 이슈 수정됨 재생성`() {
                val oldResult = AnalysisResult(
                    issue = mockIssue,
                    filePaths = """["src/main/kotlin/com/example/UserService.kt"]""",
                    guideline = "오래된 가이드",
                    pseudoCode = "오래된 코드",
                    sideEffects = "오래된 부작용"
                )
                ReflectionTestUtils.setField(oldResult, "createdAt", LocalDateTime.now().minusDays(4))

                val latestIssueInfo = GitHubIssueRes(
                    number = 42,
                    title = "Fix NullPointerException",
                    body = "이슈 본문",
                    labels = emptyList(),
                    updatedAt = LocalDateTime.now()  // createdAt보다 최신
                )

                given(issueRepository.findById(ISSUE_ID)).willReturn(Optional.of(mockIssue))
                given(userRepository.findByGithubIdWithLock(GITHUB_ID)).willReturn(Optional.of(mockUser))
                given(
                    userAnalysisRequestRepository.findFirstByUserIdAndAnalysisResultIssueId(
                        eq(USER_ID),
                        eq(ISSUE_ID)
                    )
                )
                    .willReturn(null)
                given(userAnalysisRequestRepository.countByUserIdAndCreatedAtBetween(any(), any(), any()))
                    .willReturn(0L)
                given(analysisResultRepository.findByIssueId(ISSUE_ID)).willReturn(oldResult)
                given(gitHubClient.fetchIssue("spring-projects", "spring-boot", 42)).willReturn(latestIssueInfo)
                given(gitHubClient.fetchTree("spring-projects", "spring-boot"))
                    .willReturn(listOf("src/main/kotlin/com/example/UserService.kt"))
                given(glmClient.selectFiles(any(), anyOrNull(), any()))
                    .willReturn(listOf("src/main/kotlin/com/example/UserService.kt"))
                given(gitHubClient.fetchFileContents(eq("spring-projects"), eq("spring-boot"), any()))
                    .willReturn(mapOf("src/main/kotlin/com/example/UserService.kt" to "fun userService() { ... }"))
                given(glmClient.analyze(any(), anyOrNull(), any(), any()))
                    .willReturn(GlmAnalysisRes(guideline = "새 가이드", pseudoCode = "새 코드", sideEffects = "새 부작용"))
                given(analysisResultRepository.save(any())).willReturn(mockAnalysisResult)

                contextAnalyzerService.getGuide(ISSUE_ID, GITHUB_ID)

                // 캐시 삭제 검증
                then(analysisResultRepository).should().delete(oldResult)
                then(analysisResultRepository).should().flush()
                then(analysisResultRepository).should().save(any())
            }
        }
    }
}