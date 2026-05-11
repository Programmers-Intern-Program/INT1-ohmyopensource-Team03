package com.back.omos.domain.issue.ai

import com.back.omos.domain.issue.entity.Issue
import com.back.omos.global.ai.LangfuseClient
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.mockito.kotlin.whenever
import org.springframework.ai.chat.model.ChatModel
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.Mockito.mock
import org.mockito.kotlin.*
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.metadata.ChatResponseMetadata
import org.springframework.ai.chat.metadata.Usage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt
import kotlin.jvm.java

/**
 * AI 모델(GLM)을 이용한 이슈 추천 로직의 단위 테스트 클래스입니다.
 * <p>
 * Mock 된 ChatModel이 반환하는 JSON 형태의 문자열 응답이
 * 서비스 내부에서 사용하는 객체 리스트로 정확히 역직렬화(Parsing)되는지 검증합니다.
 *
 * <p><b>주요 생성자:</b><br>
 * {@code IssueGlmClientTest()} <br>
 * Mockito를 통해 가짜 ChatModel을 주입하고, KotlinModule이 등록된 ObjectMapper를 초기화하여 테스트 환경을 구성합니다.
 *
 * <p><b>외부 모듈:</b><br>
 * - Mockito: AI 모델 및 인터페이스 모킹<br>
 * - Jackson (KotlinModule): JSON 데이터 파싱 검증
 *
 * @author 유재원
 * @since 2026-04-28
 */
class IssueGlmClientTest {
    private val chatModel: ChatModel = mock()
    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    private val langfuseClient: LangfuseClient = mock()
    private val client = IssueGlmClientImpl(chatModel, objectMapper, langfuseClient)

    @Test
    fun `AI가 준 JSON 응답이 객체 리스트로 정확히 변환되는지 테스트`() {
        // 1. Given: 가짜 JSON 응답 준비
        val fakeJsonResponse = """
        [
          { "title": "이슈1", "repoName": "owner/repo1", "reason": "이유1" },
          { "title": "이슈2", "repoName": "owner/repo2", "reason": "이유2" }
        ]
    """.trimIndent()

        // [수정 포인트] ChatResponse 구조를 한 번에 모킹합니다.
        val mockResponse = mock(ChatResponse::class.java)
        val mockGeneration = mock(Generation::class.java)
        val mockMetadata = mock(ChatResponseMetadata::class.java)
        val mockUsage = mock(Usage::class.java)
        val mockAssistantMessage = AssistantMessage(fakeJsonResponse)

        // 계층 구조 연결 (NPE 방지 세트)
        whenever(mockResponse.result).thenReturn(mockGeneration)
        whenever(mockGeneration.output).thenReturn(mockAssistantMessage)
        whenever(mockResponse.metadata).thenReturn(mockMetadata)
        whenever(mockMetadata.usage).thenReturn(mockUsage)

        // [수정 포인트] any<String>() 대신 any<Prompt>() 사용
        whenever(chatModel.call(any<Prompt>())).thenReturn(mockResponse)

        // 2. When: 클라이언트 호출
        val results = client.generateRecommendationReasons("Kotlin 스택", emptyList())

        // 3. Then: 파싱 결과 확인
        assertEquals(2, results.size)
        assertEquals("이슈1", results[0].title)
        assertEquals("owner/repo1", results[0].repoName) // 인덱스 및 필드명 확인
        assertEquals("owner/repo2", results[1].repoName)
    }

    @Test
    fun `이슈 리스트의 모든 분기점(Nullable 체크 및 루프)을 실행하여 프롬프트를 생성한다`() {
        // 1. Given
        val candidateIssues = listOf(
            // Case 1: 모든 데이터가 꽉 찬 경우
            Issue(
                repoFullName = "google/guava",
                issueNumber = 101,
                title = "정상 이슈 제목",
                content = "A".repeat(500),
                labels = listOf("bug", "p0"),
                status = Issue.IssueStatus.OPEN
            ),
            // Case 2: Nullable 필드들이 null인 경우
            Issue(
                repoFullName = "apache/spark",
                issueNumber = 202,
                title = "데이터 미비 이슈",
                content = null,
                labels = null,
                status = Issue.IssueStatus.OPEN
            )
        )

        val fakeJsonResponse = """
            [
              { "title": "정상 이슈 제목", "repoName": "google/guava", "reason": "라벨이 적절함" },
              { "title": "데이터 미비 이슈", "repoName": "apache/spark", "reason": "내용은 없지만 제목이 관련됨" }
            ]
        """.trimIndent()

        // 1. 필요한 추가 Mock 객체들
        val mockResponse = mock(ChatResponse::class.java)
        val mockGeneration = mock(Generation::class.java)
        val mockMetadata = mock(ChatResponseMetadata::class.java) // 메타데이터 추가
        val mockUsage = mock(Usage::class.java) // 토큰 사용량 추가

        // 2. 가짜 응답 본문 설정
        val mockAssistantMessage = AssistantMessage(fakeJsonResponse)
        whenever(mockGeneration.output).thenReturn(mockAssistantMessage)
        whenever(mockResponse.result).thenReturn(mockGeneration)

        // 3. [핵심] 메타데이터와 Usage 연결 (NPE 방지)
        whenever(mockResponse.metadata).thenReturn(mockMetadata)
        whenever(mockMetadata.usage).thenReturn(mockUsage)

        // (선택사항) 만약 코드에서 토큰 숫자를 직접 읽는다면 아래도 추가
        whenever(mockUsage.promptTokens).thenReturn(100L.toInt())
        whenever(mockUsage.completionTokens).thenReturn(100L.toInt())

        // 4. Mocking 완료
        whenever(chatModel.call(any<Prompt>())).thenReturn(mockResponse)

        // 2. When
        val results = client.generateRecommendationReasons("Java/Kotlin Backend", candidateIssues)

        // 3. Then
        assertEquals(2, results.size)

        // [핵심 수정] ArgumentCaptor도 Prompt 타입을 캡처하도록 변경
        val promptCaptor = argumentCaptor<Prompt>()
        verify(chatModel).call(promptCaptor.capture())

        // 캡처된 Prompt 객체 내부의 텍스트 내용을 가져옴
        // instructions[0]을 거치지 않고 바로 뽑기
        val generatedPrompt = promptCaptor.firstValue.contents

        assertTrue(generatedPrompt.contains("google/guava"))
        assertTrue(generatedPrompt.contains("bug, p0"))
        assertTrue(generatedPrompt.contains("데이터 미비 이슈")) // "없음" 대신 구체적인 제목 확인 추천
    }
}