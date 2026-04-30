package com.back.omos.domain.prdraft.ai

import com.back.omos.global.ai.LangfuseClient
import com.back.omos.global.exception.errorCode.AiErrorCode
import com.back.omos.global.exception.exceptions.AiException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SpringAiClientTest {

    @Mock private lateinit var chatModel: ChatModel
    @Mock private lateinit var langfuseClient: LangfuseClient

    private lateinit var client: SpringAiClient

    @BeforeEach
    fun setUp() {
        client = SpringAiClient(chatModel, jacksonObjectMapper(), langfuseClient)
        // Langfuse 미설정 환경처럼 동작 (score 기록 불필요)
        given(langfuseClient.recordGeneration(any(), any(), any(), any(), any(), anyOrNull(), anyOrNull(), any())).willReturn(null)
    }

    private fun mockChatResponse(text: String): ChatResponse {
        return ChatResponse(listOf(Generation(AssistantMessage(text))))
    }

    @Nested
    inner class ParseTest {

        @Test
        fun `정상 JSON 응답을 파싱한다`() {
            given(chatModel.call(any<Prompt>()))
                .willReturn(mockChatResponse("""{"title":"feat: test","body":"test body"}"""))

            val result = client.generatePrDraft("prompt")

            assertThat(result.title).isEqualTo("feat: test")
            assertThat(result.body).isEqualTo("test body")
        }

        @Test
        fun `마크다운 코드펜스로 감싸진 응답을 파싱한다`() {
            given(chatModel.call(any<Prompt>()))
                .willReturn(mockChatResponse("```json\n{\"title\":\"feat: test\",\"body\":\"test body\"}\n```"))

            val result = client.generatePrDraft("prompt")

            assertThat(result.title).isEqualTo("feat: test")
        }
    }

    @Nested
    inner class ExceptionTest {

        @Test
        fun `빈 응답이면 AI_RESPONSE_EMPTY 예외를 던진다`() {
            given(chatModel.call(any<Prompt>()))
                .willReturn(mockChatResponse("   "))

            assertThatThrownBy { client.generatePrDraft("prompt") }
                .isInstanceOf(AiException::class.java)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.AI_RESPONSE_EMPTY)
        }

        @Test
        fun `JSON 파싱 실패 시 AI_RESPONSE_PARSE_FAILED 예외를 던진다`() {
            given(chatModel.call(any<Prompt>()))
                .willReturn(mockChatResponse("not a json response at all"))

            assertThatThrownBy { client.generatePrDraft("prompt") }
                .isInstanceOf(AiException::class.java)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.AI_RESPONSE_PARSE_FAILED)
        }
    }
}
