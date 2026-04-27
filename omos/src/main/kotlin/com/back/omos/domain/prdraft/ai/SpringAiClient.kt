package com.back.omos.domain.prdraft.ai

import com.back.omos.global.exception.errorCode.AiErrorCode
import com.back.omos.global.exception.exceptions.AiException
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.ai.chat.model.ChatModel
import org.springframework.stereotype.Component
import java.security.MessageDigest

/**
 * Spring AI를 사용하여 GLM 모델을 호출하는 실제 AI 구현체입니다.
 *
 * <p>
 * 프롬프트를 GLM에 전달하고, JSON 형식으로 반환된 응답을 파싱하여
 * PR 제목과 본문을 추출합니다.
 *
 * <p><b>응답 파싱:</b><br>
 * AI 응답에 마크다운 코드 블록이 포함될 수 있어,
 * 정규식으로 JSON 객체를 추출한 뒤 역직렬화합니다.
 *
 * @author 5h6vm
 * @since 2026-04-24
 * @see AiClient
 */
@Component
class SpringAiClient(
    private val chatModel: ChatModel,
    private val objectMapper: ObjectMapper
) : AiClient {

    private val logger = KotlinLogging.logger {}

    override fun generatePrDraft(prompt: String): AiPrResult {
        val response = chatModel.call(prompt)
            .takeIf { it.isNotBlank() }
            ?: throw AiException(AiErrorCode.AI_RESPONSE_EMPTY)

        return parseResponse(response)
    }

    private fun parseResponse(response: String): AiPrResult {
        val json = extractJson(response)
            ?: run {
                val safe = safeLog(response)
                logger.warn { "AI 응답에서 JSON을 찾을 수 없습니다 ($safe)" }
                throw AiException(AiErrorCode.AI_RESPONSE_PARSE_FAILED, safe)
            }

        return try {
            objectMapper.readValue(json, AiPrResult::class.java)
        } catch (e: Exception) {
            val safe = safeLog(json)
            logger.warn { "AI 응답 JSON 파싱 실패 ($safe)" }
            throw AiException(AiErrorCode.AI_RESPONSE_PARSE_FAILED, safe)
        }
    }

    private fun safeLog(value: String): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)
        return "len=${value.length}, sha256=$hash"
    }

    private fun extractJson(response: String): String? {
        val fenceMatch = Regex("""```(?:json)?\s*([\s\S]*?)```""").find(response)
        if (fenceMatch != null) return fenceMatch.groupValues[1].trim()
        return Regex("""\{[\s\S]*\}""").find(response)?.value
    }
}
