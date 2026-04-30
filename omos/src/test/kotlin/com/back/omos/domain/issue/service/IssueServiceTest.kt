package com.back.omos.domain.issue.service

import com.back.omos.domain.issue.dto.CreateIssueReq
import com.back.omos.domain.issue.dto.GithubIssueResponse
import com.back.omos.domain.issue.entity.Issue
import com.back.omos.domain.issue.github.GithubClient
import com.back.omos.domain.issue.repository.IssueRepository
import com.back.omos.global.ai.GeminiEmbeddingModel
import com.back.omos.global.exception.errorCode.IssueErrorCode
import com.back.omos.global.exception.exceptions.IssueException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.util.ReflectionTestUtils
import java.util.*
import kotlin.jvm.java

/**
 * 이슈(Issue) 비즈니스 로직의 핵심 기능을 검증하는 서비스 레이어 테스트 클래스입니다.
 * <p>
 * 외부 API 호출, AI 임베딩 모델 연동, DB 접근 로직이 유기적으로 작동하는지 확인하며,
 * 특히 신규 이슈 크롤링 시의 데이터 변환(Float to Double) 및 중복 체크 로직을 집중적으로 검증합니다.
 *
 * <p><b>상속 정보:</b><br>
 * - {@code MockitoExtension}: Mockito를 사용하여 외부 의존성(Repository, Client, AI Model)을 격리한 단위 테스트를 수행합니다.
 *
 * <p><b>주요 생성자:</b><br>
 * {@code IssueServiceTest()} <br>
 * {@code @BeforeEach} 단계에서 Mock 객체들을 주입하여 테스트 대상인 {@code IssueServiceImpl} 인스턴스를 직접 생성합니다.
 *
 * <p><b>외부 모듈:</b><br>
 * - Mockito: 외부 API(GitHub) 및 AI 모델(Gemini)의 응답 모킹 <br>
 * - ReflectionTestUtils: JPA 엔티티의 불변 필드(ID 등)에 테스트용 데이터를 강제 주입 <br>
 * - Spring AI: Document 및 Embedding 관련 인터페이스 타입 지원
 *
 * @author 유재원
 * @since 2026-04-29
 */

@ExtendWith(MockitoExtension::class)
class IssueServiceTest {

    @Mock
    private lateinit var issueRepository: IssueRepository

    @Mock
    private lateinit var githubClient: GithubClient

    @Mock
    private lateinit var embeddingModel: GeminiEmbeddingModel

    private lateinit var issueService: IssueServiceImpl

    @BeforeEach
    fun setUp() {
        issueService = IssueServiceImpl(issueRepository, githubClient, embeddingModel)
    }


    @Test
    @DisplayName("이슈 생성 성공")
    fun createIssueSuccess() {
        // given
        val request = CreateIssueReq(
            repoFullName = "naver/fixture-monkey",
            issueNumber = 1L,
            title = "New Issue",
            status = Issue.IssueStatus.OPEN
        )

        val mockIssue = request.toEntity()
        ReflectionTestUtils.setField(mockIssue, "id", 1L)

        given(issueRepository.existsByRepoFullNameAndIssueNumber(any(), any())).willReturn(false)
        given(issueRepository.save(any<Issue>())).willReturn(mockIssue)

        // when
        val result = issueService.createIssue(request)

        // then
        assertNotNull(result)
        assertEquals(1L, result.id) // DTO에 ID가 잘 담겼는지 확인
        verify(issueRepository).save(any<Issue>())
    }

    @Test
    @DisplayName("이슈 생성 실패 - 중복된 이슈 번호가 존재할 경우 예외 발생")
    fun createIssueFailAlreadyExist() {
        // given
        val request = CreateIssueReq(
            repoFullName = "org/repo",
            issueNumber = 1L,
            title = "Title",
            status = Issue.IssueStatus.OPEN
        )
        given(issueRepository.existsByRepoFullNameAndIssueNumber(any(), any())).willReturn(true)

        // when & then
        val exception = assertThrows(IssueException::class.java) {
            issueService.createIssue(request)
        }
        assertEquals(IssueErrorCode.ISSUE_ALREADY_EXIST, exception.errorCode)
    }

    @Test
    @DisplayName("크롤링 및 저장 - 신규 이슈일 경우 URL 파싱 및 AI 임베딩(Vector 변환) 후 저장")
    fun crawlAndSaveNewIssue() {
        // given
        val query = "fixture-monkey"

        val mockGithubResponse = GithubIssueResponse(
            id = 12345L,
            number = 99L,
            title = "Parsing Test Issue",
            body = "This is body content",
            htmlUrl = "https://github.com/naver/fixture-monkey/issues/99",
            repositoryUrl = "https://api.github.com/repos/naver/fixture-monkey",
            labels = listOf(GithubIssueResponse.LabelResponse(name = "enhancement", color = "blue"))
        )

        given(githubClient.searchIssues(query)).willReturn(listOf(mockGithubResponse))
        given(issueRepository.findByRepoFullNameAndIssueNumber(any(), any())).willReturn(null)

        // AI 임베딩 모델 모킹 (FloatArray 반환)
        val mockFloatVector = FloatArray(3072) { 0.5f }
        given(embeddingModel.embed(any<Document>())).willReturn(mockFloatVector)

        // 저장 시 전달받은 객체를 그대로 반환하도록 설정
        given(issueRepository.save(any<Issue>())).willAnswer { invocation ->
            invocation.getArgument<Issue>(0)
        }
        // when
        val result = issueService.crawlAndSaveByQuery(query)

        // then
        assertEquals(1, result.size)
        val savedIssue = result[0]

        assertEquals("naver/fixture-monkey", savedIssue.repoFullName) // URL 파싱 결과 확인
        assertEquals(99L, savedIssue.issueNumber)
        assertEquals("Parsing Test Issue", savedIssue.title)

        // FloatArray -> DoubleArray 변환 및 값 검증
        assertNotNull(savedIssue.issueVector)
        assertEquals(3072, savedIssue.issueVector?.size)
        assertEquals(0.5, savedIssue.issueVector!![0], 0.0001)

        verify(embeddingModel).embed(any<Document>())
        verify(issueRepository).save(any())
    }

    @Test
    @DisplayName("크롤링 및 저장 - 이미 존재하는 이슈는 추가 저장 없이 기존 객체 반환")
    fun crawlAndSaveExistingIssue() {
        // given
        val query = "existing"
        val mockGithubResponse = GithubIssueResponse(
            id = 111L,
            number = 1L,
            title = "Existing",
            body = "...",
            htmlUrl = "...",
            repositoryUrl = "https://api.github.com/repos/org/repo",
            labels = emptyList()
        )
        val existingIssue = Issue(repoFullName = "org/repo", issueNumber = 1L, title = "Already In DB")

        given(githubClient.searchIssues(query)).willReturn(listOf(mockGithubResponse))
        given(issueRepository.findByRepoFullNameAndIssueNumber("org/repo", 1L)).willReturn(existingIssue)

        // when
        val result = issueService.crawlAndSaveByQuery(query)

        // then
        assertEquals(1, result.size)
        assertEquals("Already In DB", result[0].title)

        verify(embeddingModel, never()).embed(any<Document>()) // AI 호출 안 함
        verify(issueRepository, never()).save(any()) // 저장 안 함
    }

    @Test
    @DisplayName("이슈 상세 조회 성공")
    fun getIssueSuccess() {
        // given
        val issueId = 1L
        val mockIssue = Issue(repoFullName = "org/repo", issueNumber = 1L, title = "Found")
        ReflectionTestUtils.setField(mockIssue, "id", issueId)

        given(issueRepository.findById(issueId)).willReturn(Optional.of(mockIssue))

        // when
        val result = issueService.getIssue(issueId)

        // then
        assertNotNull(result)
        assertEquals("Found", result.title)
    }

    @Test
    @DisplayName("이슈 상세 조회 실패 - 존재하지 않는 ID")
    fun getIssueFailNotFound() {
        // given
        given(issueRepository.findById(any<Long>())).willReturn(Optional.empty())

        // when & then
        val exception = assertThrows(IssueException::class.java) {
            issueService.getIssue(999L)
        }

        assertEquals(IssueErrorCode.ISSUE_NOT_FOUND, exception.errorCode)
    }
}