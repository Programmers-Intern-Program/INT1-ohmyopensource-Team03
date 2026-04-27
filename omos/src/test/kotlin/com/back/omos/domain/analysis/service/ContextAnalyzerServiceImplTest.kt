package com.back.omos.domain.analysis.service

import com.back.omos.domain.analysis.ai.GlmClient
import com.back.omos.domain.analysis.ai.GlmAnalysisRes
import com.back.omos.domain.analysis.dto.GuideResponseDto
import com.back.omos.domain.analysis.dto.PseudoCodeResponseDto
import com.back.omos.domain.analysis.entity.AnalysisResult
import com.back.omos.domain.analysis.github.GitHubClient
import com.back.omos.domain.analysis.github.GitHubCodeSearchItem
import com.back.omos.domain.analysis.github.GitHubCodeSearchRes
import com.back.omos.domain.analysis.github.GitHubIssueRes
import com.back.omos.domain.analysis.repository.AnalysisResultRepository
import com.back.omos.domain.issue.entity.Issue
import com.back.omos.domain.issue.repository.IssueRepository
import com.back.omos.domain.repo.entity.Repo
import com.back.omos.domain.repo.repository.RepoRepository
import com.back.omos.global.exception.exceptions.AnalysisException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import com.fasterxml.jackson.databind.ObjectMapper
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
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
    private lateinit var issueRepository: IssueRepository

    @Mock
    private lateinit var gitHubClient: GitHubClient

    @Mock
    private lateinit var glmClient: GlmClient

    private lateinit var contextAnalyzerService: ContextAnalyzerServiceImpl

    @BeforeEach
    fun setUp() {
        contextAnalyzerService = ContextAnalyzerServiceImpl(
            analysisResultRepository = analysisResultRepository,
            issueRepository = issueRepository,
            gitHubClient = gitHubClient,
            glmClient = glmClient,
            objectMapper = ObjectMapper()
        )
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

    private val mockSearchResult = GitHubCodeSearchRes(
        totalCount = 1,
        items = listOf(
            GitHubCodeSearchItem(
                name = "UserService.kt",
                path = "src/main/kotlin/com/example/UserService.kt",
                htmlUrl = "https://github.com/..."
            )
        )
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
        @DisplayName("캐시 HIT - 분석 결과가 이미 있으면 GitHub API 호출 없이 즉시 반환한다")
        fun `캐시 HIT시 즉시 반환`() {
            // given
            given(issueRepository.findById(1L)).willReturn(Optional.of(mockIssue))
            given(analysisResultRepository.findByIssueId(1L)).willReturn(mockAnalysisResult)

            // when
            val result = contextAnalyzerService.getGuide(1L)

            // then
            assertNotNull(result)
            assertEquals(listOf("src/main/kotlin/com/example/UserService.kt"), result.filePaths)
            assertEquals("TODO: GLM 연동 후 실제 가이드 생성", result.guideline)

            // GitHub API 호출 안 됐는지 검증
            then(gitHubClient).shouldHaveNoInteractions()
        }

        @Test
        @DisplayName("캐시 MISS - 분석 결과가 없으면 GitHub API 호출 후 저장한다")
        fun `캐시 MISS시 GitHub API 호출`() {
            // given
            given(issueRepository.findById(1L)).willReturn(Optional.of(mockIssue))
            given(analysisResultRepository.findByIssueId(1L)).willReturn(null)
            given(gitHubClient.fetchIssue("spring-projects", "spring-boot", 42))
                .willReturn(mockIssueInfo)
            given(gitHubClient.searchCode("Fix NullPointerException", "spring-projects", "spring-boot"))
                .willReturn(mockSearchResult)
            given(gitHubClient.fetchFileContent("spring-projects", "spring-boot", "src/main/kotlin/com/example/UserService.kt"))
                .willReturn("fun userService() { ... }")
            given(analysisResultRepository.save(any())).willReturn(mockAnalysisResult)
            given(glmClient.analyze(any(), anyOrNull(), any(), any()))
                .willReturn(GlmAnalysisRes(
                    guideline = "가이드라인",
                    pseudoCode = "의사코드",
                    sideEffects = "부작용"
                ))

            // when
            val result = contextAnalyzerService.getGuide(1L)

            // then
            assertNotNull(result)
            // GitHub API 호출됐는지 검증
            then(gitHubClient).should().fetchIssue("spring-projects", "spring-boot", 42)
            then(gitHubClient).should().searchCode("Fix NullPointerException", "spring-projects", "spring-boot")
            then(analysisResultRepository).should().save(any())
        }

        @Test
        @DisplayName("이슈가 없으면 AnalysisException을 던진다")
        fun `이슈 없으면 예외 던짐`() {
            // given
            given(issueRepository.findById(999L)).willReturn(Optional.empty())

            // when & then
            assertThrows(AnalysisException::class.java) {
                contextAnalyzerService.getGuide(999L)
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
        @DisplayName("캐시 HIT - 분석 결과가 이미 있으면 GitHub API 호출 없이 즉시 반환한다")
        fun `캐시 HIT시 즉시 반환`() {
            // given
            given(issueRepository.findById(1L)).willReturn(Optional.of(mockIssue))
            given(analysisResultRepository.findByIssueId(1L)).willReturn(mockAnalysisResult)  // 추가!

            // when
            val result = contextAnalyzerService.getPseudoCode(1L)

            // then
            assertNotNull(result)
            assertEquals("TODO: GLM 연동 후 실제 의사 코드 생성", result.pseudoCode)
            then(gitHubClient).shouldHaveNoInteractions()
        }

        @Test
        @DisplayName("이슈가 없으면 AnalysisException을 던진다")
        fun `이슈 없으면 예외 던짐`() {
            // given
            given(issueRepository.findById(999L)).willReturn(Optional.empty())

            // when & then
            assertThrows(AnalysisException::class.java) {
                contextAnalyzerService.getPseudoCode(999L)
            }
        }
    }
}