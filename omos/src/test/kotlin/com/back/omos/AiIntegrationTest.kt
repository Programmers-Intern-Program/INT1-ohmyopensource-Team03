package com.back.omos

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.test.context.ActiveProfiles

/**
 * GLM(Chat)과 Gemini(Embedding) 모델의 실제 API 호출을 검증하는 통합 테스트입니다.
 *
 * 테스트 실행 전 환경변수 GLM_API_KEY, GEMINI_API_KEY가 설정되어 있어야 합니다.
 *
 * @author MintyU
 * @since 2026-04-24
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
class AiIntegrationTest {

    @Autowired
    private lateinit var chatModel: ChatModel

    @Autowired
    private lateinit var embeddingModel: EmbeddingModel

    @Test
    fun `GLM 채팅 모델 정상 응답 확인`() {
        val response = chatModel.call("Introduce yourself in one sentence.")

        println("=== GLM Response ===")
        println(response)

        assertThat(response).isNotBlank()
    }

    @Test
    fun `Gemini 임베딩 모델 벡터 생성 확인`() {
        val embedding = embeddingModel.embed("This is a test text for embedding.")

        println("=== Gemini Embedding ===")
        println("Vector dimension: ${embedding.size}")

        assertThat(embedding).isNotEmpty()
    }
}
