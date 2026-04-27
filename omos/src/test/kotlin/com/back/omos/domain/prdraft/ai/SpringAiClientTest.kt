package com.back.omos.domain.prdraft.ai

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
import org.mockito.ArgumentMatchers.anyString
import org.springframework.ai.chat.model.ChatModel

@ExtendWith(MockitoExtension::class)
class SpringAiClientTest {

    @Mock private lateinit var chatModel: ChatModel

    private lateinit var client: SpringAiClient

    @BeforeEach
    fun setUp() {
        client = SpringAiClient(chatModel, jacksonObjectMapper())
    }

    @Nested
    inner class ParseTest {

        @Test
        fun `정상 JSON 응답을 파싱한다`() {
            given(chatModel.call(anyString())).willReturn("""{"title":"feat: test","body":"test body"}""")

            val result = client.generatePrDraft("prompt")

            assertThat(result.title).isEqualTo("feat: test")
            assertThat(result.body).isEqualTo("test body")
        }

        @Test
        fun `마크다운 코드펜스로 감싸진 응답을 파싱한다`() {
            val response = "```json\n{\"title\":\"feat: test\",\"body\":\"test body\"}\n```"
            given(chatModel.call(anyString())).willReturn(response)

            val result = client.generatePrDraft("prompt")

            assertThat(result.title).isEqualTo("feat: test")
        }
    }

    @Nested
    inner class ExceptionTest {

        @Test
        fun `빈 응답이면 AI_RESPONSE_EMPTY 예외를 던진다`() {
            given(chatModel.call(anyString())).willReturn("   ")

            assertThatThrownBy { client.generatePrDraft("prompt") }
                .isInstanceOf(AiException::class.java)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.AI_RESPONSE_EMPTY)
        }

        @Test
        fun `JSON 파싱 실패 시 AI_RESPONSE_PARSE_FAILED 예외를 던진다`() {
            given(chatModel.call(anyString())).willReturn("not a json response at all")

            assertThatThrownBy { client.generatePrDraft("prompt") }
                .isInstanceOf(AiException::class.java)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.AI_RESPONSE_PARSE_FAILED)
        }
    }
}
