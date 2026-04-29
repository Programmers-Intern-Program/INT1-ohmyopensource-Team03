package com.back.omos.domain.issue.controller


import com.back.omos.domain.issue.dto.RecommendIssueRes
import com.back.omos.domain.issue.entity.Issue
import com.back.omos.domain.issue.repository.IssueRepository
import com.back.omos.domain.issue.service.IssueService
import com.back.omos.domain.issue.service.RecommendService
import com.back.omos.global.auth.principal.OAuthPrincipal
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.LocalDateTime

/**
 * 이슈(Issue) 관련 API의 엔드포인트 동작을 검증하는 컨트롤러 테스트 클래스입니다.
 * <p>
 * MockMvc를 사용하여 실제 서블릿 컨테이너를 구동하지 않고 HTTP 요청 및 응답을 시뮬레이션하며,
 * 서비스 계층을 Mocking 하여 컨트롤러의 요청 파싱, 권한 처리, 응답 포맷팅 로직을 집중적으로 테스트합니다.
 *
 * <p><b>상속 정보:</b><br>
 * - {@code WebMvcTest}: IssueController와 관련된 웹 레이어 빈들만 로드하여 가벼운 테스트 환경을 구축합니다.
 *
 * <p><b>주요 생성자:</b><br>
 * {@code IssueControllerTest()} <br>
 * Spring Boot의 테스트 컨텍스트 프레임워크에 의해 필요한 Mock 객체들이 자동으로 주입됩니다.
 *
 * <p><b>외부 모듈:</b><br>
 * - MockMvc: REST API 호출 및 검증 <br>
 * - Mockito: 서비스 및 리포지토리 레이어의 의존성 제거 <br>
 * - Spring Security Test: 인증된 사용자(OAuthPrincipal) 및 CSRF 토큰 처리
 *
 * @author 유재원
 * @since 2026-04-29
 */
@WebMvcTest(IssueController::class)
class IssueControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var issueService: IssueService

    @MockitoBean
    private lateinit var issueRepository: IssueRepository

    @MockitoBean
    private lateinit var recommendService: RecommendService

    @Test
    @DisplayName("전체 이슈 조회 성공 테스트")
    @WithMockUser
    fun getAllIssuesTest() {
        // given
        val mockIssue = Issue(
            repoFullName = "back-omos/omos-backend",
            issueNumber = 101L,
            title = "Test Issue Title"
        ).apply {
            content = "이슈 테스트 본문입니다."
            labels = listOf("bug", "good-first-issue")
        }

        val mockIssues = listOf(mockIssue)
        given(issueRepository.findAll()).willReturn(mockIssues)

        // when & then
        mockMvc.get("/api/v1/issues") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data[0].title") { value("Test Issue Title") }
            jsonPath("$.data[0].repoFullName") { value("back-omos/omos-backend") }
        }
    }

    @Test
    @DisplayName("깃허브 쿼리로 이슈 크롤링 및 저장 테스트")
    @WithMockUser
    fun crawlBySearchTest() {
        // given
        val query = "language:kotlin"

        val mockIssues = listOf(
            Issue(
                repoFullName = "back-omos/omos-backend",
                issueNumber = 42L,
                title = "Crawled Issue"
            ).apply {
                content = "크롤링된 이슈 본문입니다."
            }
        )

        given(issueService.crawlAndSaveByQuery(query)).willReturn(mockIssues)

        // when & then
        mockMvc.post("/api/v1/issues/crawl/search") {
            param("q", query)
            with(csrf()) // Security POST 요청 시 필수
        }.andExpect {
            status { isOk() }
            jsonPath("$.data[0].title") { value("Crawled Issue") }
        }
    }

    @Test
    @DisplayName("사용자 맞춤 이슈 추천 테스트")
    fun getRecommendationTest() {
        // given
        val githubId = "jaewon-user"

        val mockPrincipal = OAuthPrincipal(
            githubId = githubId,
            attributes = mapOf("login" to githubId)
        )

        val authentication = UsernamePasswordAuthenticationToken(
            mockPrincipal,
            null,
            mockPrincipal.authorities
        )

        val mockResponses = listOf(
            RecommendIssueRes(
                id = 1L,
                repoFullName = "back-omos/omos-backend",
                issueNumber = 123L,
                title = "AI Recommended Issue",
                summary = "AI가 분석한 추천 사유 요약입니다.",
                score = 0.95f,
                labels = listOf("help wanted"),
                status = "OPEN"
            )
        )

        given(recommendService.getPersonalizedRecommendation(githubId)).willReturn(mockResponses)

        // when & then
        mockMvc.get("/api/v1/issues/recommend") {
            with(authentication(authentication))
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data[0].title") { value("AI Recommended Issue") }
            jsonPath("$.data[0].score") { value(0.95) }
        }
    }
}