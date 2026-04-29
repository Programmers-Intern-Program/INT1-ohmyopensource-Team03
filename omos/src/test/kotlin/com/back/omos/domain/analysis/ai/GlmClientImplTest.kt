package com.back.omos.domain.analysis.ai

import com.back.omos.global.exception.exceptions.AnalysisException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.BDDMockito.given
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.springframework.ai.chat.model.ChatModel

/**
 * GlmClientImpl 단위 테스트
 *
 * chatModel은 Mock으로 대체하고, ObjectMapper는 실제 인스턴스를 주입하여
 * JSON 파싱 로직을 포함한 모든 분기를 검증합니다.
 *
 * @author Jaewon Ryu
 * @since 2026-04-29
 */
@ExtendWith(MockitoExtension::class)
@DisplayName("GlmClientImpl 단위 테스트")
class GlmClientImplTest {

    @Mock
    private lateinit var chatModel: ChatModel

    // Kotlin data class 역직렬화를 위해 Kotlin 모듈이 등록된 ObjectMapper 사용
    private val objectMapper = jacksonObjectMapper()

    private lateinit var glmClientImpl: GlmClientImpl

    @BeforeEach
    fun setUp() {
        glmClientImpl = GlmClientImpl(chatModel, objectMapper)
    }

    // ──────────────────────────────────────────
    // parseFileList() — selectFiles()를 통해 간접 검증
    // ──────────────────────────────────────────

    @Nested
    @DisplayName("parseFileList()")
    inner class ParseFileList {

        @Test
        @DisplayName("정상 JSON 응답을 파일 경로 목록으로 파싱한다")
        fun `정상 JSON 파싱 성공`() {
            // given
            given(chatModel.call(any<String>()))
                .willReturn("""{"files": ["src/a.kt", "src/b.kt"]}""")

            // when
            val result = glmClientImpl.selectFiles("Fix bug", null, listOf("src/a.kt", "src/b.kt"))

            // then
            assertEquals(listOf("src/a.kt", "src/b.kt"), result)
        }

        @Test
        @DisplayName("마크다운 코드블록이 포함된 응답도 정상 파싱한다")
        fun `마크다운 코드블록 응답 파싱 성공`() {
            // given: ```json ... ``` 래핑된 응답
            val response = "```json\n{\"files\": [\"src/a.kt\"]}\n```"
            given(chatModel.call(any<String>())).willReturn(response)

            // when
            val result = glmClientImpl.selectFiles("Fix bug", null, listOf("src/a.kt"))

            // then
            assertEquals(listOf("src/a.kt"), result)
        }

        @Test
        @DisplayName("'files' 키가 없는 JSON 응답이면 AnalysisException을 던진다")
        fun `files 키 없으면 예외`() {
            // given: files 키 없는 올바른 JSON
            given(chatModel.call(any<String>())).willReturn("""{"other": []}""")

            // when & then
            assertThrows(AnalysisException::class.java) {
                glmClientImpl.selectFiles("Fix bug", null, emptyList())
            }
        }

        @Test
        @DisplayName("JSON 형식이 깨진 응답이면 AnalysisException을 던진다")
        fun `JSON 형식 오류시 예외`() {
            // given: JSON이 아닌 문자열
            given(chatModel.call(any<String>())).willReturn("not json at all")

            // when & then
            assertThrows(AnalysisException::class.java) {
                glmClientImpl.selectFiles("Fix bug", null, emptyList())
            }
        }
    }

    // ──────────────────────────────────────────
    // selectFiles() 테스트
    // ──────────────────────────────────────────

    @Nested
    @DisplayName("selectFiles()")
    inner class SelectFiles {

        @Test
        @DisplayName("chatModel.call()이 null을 반환하면 AnalysisException을 던진다")
        fun `chatModel null 반환시 예외`() {
            // given
            given(chatModel.call(any<String>())).willReturn(null)

            // when & then
            assertThrows(AnalysisException::class.java) {
                glmClientImpl.selectFiles("Fix bug", null, emptyList())
            }
        }

        @Test
        @DisplayName("chatModel.call()이 RuntimeException을 던지면 AnalysisException으로 래핑한다")
        fun `chatModel RuntimeException시 AnalysisException 래핑`() {
            // given: 네트워크 오류 등 런타임 예외 시뮬레이션
            given(chatModel.call(any<String>())).willThrow(RuntimeException("API connection error"))

            // when & then
            assertThrows(AnalysisException::class.java) {
                glmClientImpl.selectFiles("Fix bug", null, emptyList())
            }
        }
    }

    // ──────────────────────────────────────────
    // analyze() 테스트
    // ──────────────────────────────────────────

    @Nested
    @DisplayName("analyze()")
    inner class Analyze {

        @Test
        @DisplayName("chatModel.call()이 null을 반환하면 AnalysisException을 던진다")
        fun `chatModel null 반환시 예외`() {
            // given
            given(chatModel.call(any<String>())).willReturn(null)

            // when & then
            assertThrows(AnalysisException::class.java) {
                glmClientImpl.analyze("Fix bug", null, emptyList(), emptyMap())
            }
        }

        @Test
        @DisplayName("chatModel.call()이 RuntimeException을 던지면 AnalysisException으로 래핑한다")
        fun `chatModel RuntimeException시 AnalysisException 래핑`() {
            // given
            given(chatModel.call(any<String>())).willThrow(RuntimeException("API connection error"))

            // when & then
            assertThrows(AnalysisException::class.java) {
                glmClientImpl.analyze("Fix bug", null, emptyList(), emptyMap())
            }
        }

        @Test
        @DisplayName("유효한 JSON 응답을 GlmAnalysisRes로 파싱하여 반환한다")
        fun `유효한 JSON 응답 파싱 성공`() {
            // given
            val json = """
                {
                    "guideline": "서비스 계층에서 null 체크 추가",
                    "pseudoCode": "UserService.findById() → Optional.orElseThrow()",
                    "sideEffects": "기존 null 반환에 의존하던 코드 영향 가능"
                }
            """.trimIndent()
            given(chatModel.call(any<String>())).willReturn(json)

            // when
            val result = glmClientImpl.analyze("Fix NullPointerException", "본문", listOf("bug"), emptyMap())

            // then
            assertEquals("서비스 계층에서 null 체크 추가", result.guideline)
            assertEquals("UserService.findById() → Optional.orElseThrow()", result.pseudoCode)
            assertEquals("기존 null 반환에 의존하던 코드 영향 가능", result.sideEffects)
        }

        @Test
        @DisplayName("GlmAnalysisRes로 파싱할 수 없는 JSON 응답이면 AnalysisException을 던진다")
        fun `파싱 불가 JSON 응답시 예외`() {
            // given: 필수 필드가 누락된 JSON (GlmAnalysisRes 역직렬화 실패)
            given(chatModel.call(any<String>())).willReturn("broken json")

            // when & then
            assertThrows(AnalysisException::class.java) {
                glmClientImpl.analyze("Fix bug", null, emptyList(), emptyMap())
            }
        }
    }
}
