package com.back.omos.domain.issue.service

import com.back.omos.domain.analysis.repository.UserAnalysisRequestRepository
import com.back.omos.domain.issue.ai.IssueGlmClient
import com.back.omos.domain.issue.entity.Issue
import com.back.omos.domain.issue.repository.IssueRepository
import com.back.omos.domain.issue.repository.UserRecommendedIssueRepository
import com.back.omos.domain.issue.dto.AIRecommendationResult
import com.back.omos.domain.user.entity.User
import com.back.omos.domain.user.repository.UserRepository
import com.back.omos.global.exception.exceptions.AuthException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.*

/**
 * 사용자 맞춤형 이슈 추천 비즈니스 로직을 검증하는 서비스 레이어 테스트 클래스입니다.
 * <p>
 * 사용자의 기술 스택 및 프로필 벡터를 기반으로 DB에서 유사 이슈 후보군을 추출하고,
 * 이를 AI(GLM) 모델에 전달하여 최종적인 추천 사유와 함께 정제된 데이터를 반환하는지 확인합니다.
 * 특히 데이터 불일치 시의 필터링 로직과 사용자 정보 부재 시의 예외 처리를 집중적으로 테스트합니다.
 *
 * <p><b>주요 생성자:</b><br>
 * {@code RecommendServiceTest()} <br>
 * Mockito를 통해 Mocking된 {@code IssueRepository}, {@code UserRepository}, {@code IssueGlmClient}를
 * {@code RecommendServiceImpl}에 주입하여 테스트 환경을 구성합니다.
 *
 * <p><b>외부 모듈:</b><br>
 * - Mockito (mockito-kotlin): {@code doReturn}, {@code whenever}를 활용한 고차원적인 객체 모킹 <br>
 * - JUnit 5: {@code assertThrows}, {@code assertEquals} 등을 활용한 로직 결과 및 예외 검증
 *
 * @author 유재원
 * @since 2026-04-29
 */
class RecommendServiceTest {

    private val issueRepository: IssueRepository = mock()
    private val userRepository: UserRepository = mock()
    private val issueGlmClient: IssueGlmClient = mock()
    private val userRecommendedIssueRepository: UserRecommendedIssueRepository = mock()
    private val userAnalysisRequestRepository: UserAnalysisRequestRepository = mock()

    private val recommendService = RecommendServiceImpl(
        issueRepository,
        userRepository,
        issueGlmClient,
        userRecommendedIssueRepository,
        userAnalysisRequestRepository
    )

    @Test
    @DisplayName("성공: AI가 준 제목과 레포 이름이 DB 후보군과 일치하면 정상적으로 리스트를 반환한다")
    fun getRecommendation_Success() {
        // given
        val githubId = "jaewon-test"
        val mockUser = mock<User> {
            on { profileVector } doReturn doubleArrayOf(0.1, 0.2)
            on { primaryLanguages } doReturn listOf("Kotlin", "Java")
        }

        val issue1 = createIssue("Fix bug", "owner/repo1")
        val issue2 = createIssue("Add feature", "owner/repo2")
        val topIssues = listOf(issue1, issue2)

        whenever(userRepository.findByGithubId(githubId)).thenReturn(Optional.of(mockUser))
        whenever(issueRepository.findBySimilarity(any(), any())).thenReturn(topIssues)

        // AI가 정확한 정보를 응답했다고 가정
        val aiResults = listOf(
            AIRecommendationResult("Fix bug", "owner/repo1", "좋은 버그 수정 이슈입니다."),
            AIRecommendationResult("Add feature", "owner/repo2", "배울 점이 많은 기능 구현입니다.")
        )
        whenever(issueGlmClient.generateRecommendationReasons(any(), any())).thenReturn(aiResults)

        // when
        val results = recommendService.getPersonalizedRecommendation(githubId)

        // then
        assertEquals(2, results.size)
        assertEquals("Fix bug", results[0].title)
        assertEquals("좋은 버그 수정 이슈입니다.", results[0].summary)
    }

    @Test
    @DisplayName("검증: 제목은 같지만 레포 이름이 다르거나, 후보군에 없는 이슈는 mapNotNull에 의해 필터링된다")
    fun getRecommendation_FilteringMismatch() {
        // given
        val githubId = "jaewon-test"
        val mockUser = mock<User> {
            on { profileVector } doReturn doubleArrayOf(0.1, 0.2)
        }

        val issue1 = createIssue("Fix bug", "owner/repo1")
        val topIssues = listOf(issue1)

        whenever(userRepository.findByGithubId(githubId)).thenReturn(Optional.of(mockUser))
        whenever(issueRepository.findBySimilarity(any(), any())).thenReturn(topIssues)

        // AI가 엉뚱한 레포 이름을 주거나 아예 없는 제목을 준 경우
        val aiResults = listOf(
            AIRecommendationResult("Fix bug", "wrong/repo", "이건 필터링되어야 함"), // 레포 불일치
            AIRecommendationResult("Unknown Title", "owner/repo1", "이것도 필터링되어야 함") // 제목 불일치
        )
        whenever(issueGlmClient.generateRecommendationReasons(any(), any())).thenReturn(aiResults)

        // when
        val results = recommendService.getPersonalizedRecommendation(githubId)

        // then
        // 모든 결과가 매칭에 실패했으므로 빈 리스트가 반환되어야 함
        assertTrue(results.isEmpty())
    }

    @Test
    @DisplayName("예외: 유저의 프로필 벡터가 없으면 AuthException을 던진다")
    fun getRecommendation_NoVector() {
        // given
        val githubId = "jaewon-test"
        val mockUser = mock<User> {
            on { profileVector } doReturn null // 벡터가 없음
        }
        whenever(userRepository.findByGithubId(githubId)).thenReturn(Optional.of(mockUser))

        // when & then
        assertThrows<AuthException> {
            recommendService.getPersonalizedRecommendation(githubId)
        }
    }

    // 테스트용 Issue 엔티티 생성을 돕는 헬퍼 메서드
    private fun createIssue(title: String, repoFullName: String, issueNumber: Long = 1L): Issue {
        return Issue(
            repoFullName = repoFullName,
            issueNumber = issueNumber,
            title = title,
            content = "테스트용 이슈 본문입니다.",
            labels = listOf("bug", "good-first-issue"),
            issueVector = null,
            status = Issue.IssueStatus.OPEN
        )
    }
}