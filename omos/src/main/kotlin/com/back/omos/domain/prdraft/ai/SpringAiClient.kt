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

    /**
     * 전달받은 프롬프트를 GLM에 전달하여 PR 초안을 생성합니다.
     *
     * @param prompt AI에게 전달할 PR 생성 프롬프트
     * @return AI가 생성한 PR 제목 및 본문
     * @throws AiException AI 응답이 비어 있거나 JSON 파싱에 실패한 경우
     */
    override fun generatePrDraft(prompt: String): AiPrResult {
        val response = chatModel.call(prompt)
            .takeIf { it.isNotBlank() }
            ?: throw AiException(AiErrorCode.AI_RESPONSE_EMPTY)

        return parseResponse(response)
    }

    /**
     * 한국어 PR 제목과 본문을 영어로 번역합니다.
     *
     * @param title 번역할 PR 제목 (한국어)
     * @param body 번역할 PR 본문 (한국어)
     * @return 영어로 번역된 PR 제목 및 본문
     * @throws AiException AI 응답이 비어 있거나 JSON 파싱에 실패한 경우
     */
    override fun translate(title: String, body: String): AiPrResult {
        val prompt = """
            Translate the following Korean PR title and body into natural English.
            Return only the JSON below with no extra text.
            {
              "title": "translated title",
              "body": "translated body"
            }

            [Korean Title]
            $title

            [Korean Body]
            $body
        """.trimIndent()

        val response = chatModel.call(prompt)
            .takeIf { it.isNotBlank() }
            ?: throw AiException(AiErrorCode.AI_RESPONSE_EMPTY)

        return parseResponse(response)
    }

    /**
     * AI 응답 문자열에서 JSON을 추출하고 [AiPrResult]로 역직렬화합니다.
     *
     * @param response AI 원본 응답 문자열
     * @return 파싱된 PR 제목 및 본문
     * @throws AiException JSON 추출 또는 역직렬화에 실패한 경우
     */
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

    /**
     * 로그에 민감한 AI 응답 원문 대신 길이와 SHA-256 해시 앞 16자리를 반환합니다.
     *
     * @param value 로깅할 문자열
     * @return 길이와 해시 정보를 담은 안전한 로그 문자열
     */
    private fun safeLog(value: String): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)
        return "len=${value.length}, sha256=$hash"
    }

    /**
     * AI 응답에서 JSON 객체를 추출합니다.
     *
     * <p>
     * 마크다운 코드 블록(```json ... ```)이 있으면 그 안의 내용을 우선 추출하고,
     * 없으면 중괄호 패턴으로 JSON 객체를 탐색합니다.
     *
     * @param response AI 원본 응답 문자열
     * @return 추출된 JSON 문자열, 찾지 못한 경우 null
     */
    private fun extractJson(response: String): String? {
        val fenceMatch = Regex("""```(?:json)?\s*([\s\S]*?)```""").find(response)
        if (fenceMatch != null) return fenceMatch.groupValues[1].trim()
        val candidate = Regex("""\{[\s\S]*?\}""").find(response)?.value ?: return null
        return runCatching { objectMapper.readTree(candidate); candidate }.getOrNull()
    }
}
