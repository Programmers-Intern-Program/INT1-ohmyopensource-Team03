package com.back.omos.domain.issue.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.mockito.kotlin.whenever
import org.springframework.ai.chat.model.ChatModel
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.kotlin.mock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
    private val client = IssueGlmClientImpl(chatModel, objectMapper)

    @Test
    fun `AI가 준 JSON 응답이 객체 리스트로 정확히 변환되는지 테스트`() {
        val fakeJsonResponse = """
            [
              { "title": "이슈1", "repoName": "owner/repo1", "reason": "이유1" },
              { "title": "이슈2", "repoName": "owner/repo2", "reason": "이유2" }
            ]
        """.trimIndent()

        whenever(chatModel.call(any<String>())).thenReturn(fakeJsonResponse)

        // when: 클라이언트 호출
        val results = client.generateRecommendationReasons("Kotlin 스택", emptyList())

        // then: 파싱 결과 확인
        assertEquals(2, results.size)
        assertEquals("이슈1", results[0].title)
        assertEquals("owner/repo2", results[1].repoName)
    }
}