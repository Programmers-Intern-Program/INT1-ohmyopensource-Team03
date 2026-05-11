package com.back.omos.domain.prdraft.ai

import com.back.omos.global.ai.LangfuseClient
import com.back.omos.global.exception.errorCode.AiErrorCode
import com.back.omos.global.exception.exceptions.AiException
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.time.Instant
import java.util.concurrent.Executors

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
 * <p><b>성능 기록:</b><br>
 * 각 AI 호출의 프롬프트·응답·응답시간을 {@link LangfuseClient}를 통해 비동기로 기록합니다.
 * Langfuse 미설정 시 기록을 건너뛰므로 기능에는 영향을 주지 않습니다.
 *
 * @author 5h6vm
 * @since 2026-04-24
 * @see AiClient
 * @see LangfuseClient
 */
@Component
class SpringAiClient(
    private val chatModel: ChatModel,
    private val objectMapper: ObjectMapper,
    private val langfuseClient: LangfuseClient
) : AiClient {

    private val logger = KotlinLogging.logger {}

    companion object {
        // 프롬프트 내용을 변경할 때 버전을 올려야 Langfuse에서 버전별 성능 비교가 가능합니다.
        private const val GENERATION_PR_DRAFT = "pr-draft-v7.0"
        private const val GENERATION_TRANSLATE = "pr-translate-v1"

        // LLM judge 채점 전용 풀 — 동시 채점 수를 제한해 스레드 고갈 방지
        private val judgeExecutor = Executors.newFixedThreadPool(4)
    }

    /**
     * 생성된 PR 초안의 품질을 LLM-as-judge 방식으로 채점합니다.
     *
     * <p>
     * 원본 프롬프트(diff 포함)와 생성 결과를 AI에게 다시 전달하여
     * 0~10점 점수와 채점 근거를 JSON으로 반환받습니다.
     * 채점 결과는 Langfuse의 해당 trace에 score로 기록됩니다.
     *
     * <p><b>비동기 처리:</b><br>
     * AI 호출은 별도 스레드에서 실행되어 PR 생성 응답 시간에 영향을 주지 않습니다.
     *
     * @param traceId 점수를 붙일 Langfuse trace ID
     * @param originalPrompt PR 생성에 사용된 원본 프롬프트
     * @param generatedTitle AI가 생성한 PR 제목
     * @param generatedBody AI가 생성한 PR 본문
     */
    private fun evaluateAsync(
        traceId: String,
        originalPrompt: String,
        generatedTitle: String,
        generatedBody: String
    ) {
        judgeExecutor.submit {
            try {
                val judgePrompt = """
                    아래 PR 초안을 diff 기준으로 엄격하게 평가해줘. 관대하게 채점하지 마.

                    [원본 프롬프트 (diff 포함)]
                    $originalPrompt

                    [생성된 PR 제목]
                    $generatedTitle

                    [생성된 PR 본문]
                    $generatedBody

                    [사전 확인] diff가 공백·포맷팅·주석만 변경한 trivial diff인지 먼저 판단해.
                    trivial diff라면 제목에 refactor:/chore: 컨벤션이 맞고 본문에 "포맷팅 정리" 수준의 설명이 있으면 만점 처리해.
                    변경 이유를 장황하게 서술하지 않아도 감점하지 마.

                    [채점 기준] 각 항목을 꼼꼼히 확인하고 부족하면 감점해.
                    - 제목 (0~3점): feat:/fix: 등 컨벤션 준수 (1점) + diff 핵심 파일·동작을 정확히 한 줄로 요약 (2점). 두루뭉술하거나 컨벤션 없으면 감점.
                    - 변경 내용 (0~5점): 변경 이유와 맥락을 충분히 설명했는가 (0~2점, diff에서 추론 불가능해 '(작성 필요)'로 표시된 경우 감점 제외) + 타입 변경·메서드 시그니처·어노테이션 등 기술적 세부사항을 명시했는가 (0~3점). 고수준 요약만 있으면 감점.
                    - 정확성 (0~2점): diff에 없는 내용을 지어내거나 기술적으로 틀린 설명이 있으면 감점.

                    [채점 기준점] 변경 내용(5점)이 핵심 배점이므로 여기서 충분히 감점해야 전체 점수가 의미 있다. 평범한 PR은 5~6점, 훌륭한 PR만 8점 이상. 10점은 모든 항목 만점일 때만.

                    반드시 아래 JSON 형식으로만 응답해. 항목별 점수를 반드시 포함해.
                    {"score": 6.0, "title": 2, "content": 3, "accuracy": 1, "reason": "감점 항목과 이유를 항목별로 간략히"}
                """.trimIndent()

                val judgeResponse = chatModel.call(judgePrompt) ?: return@submit

                // JSON에서 항목별 점수와 reason 추출
                val json = extractJson(judgeResponse) ?: return@submit
                val node = objectMapper.readTree(json)
                val score = node.get("score")?.asDouble() ?: return@submit
                val title = node.get("title")?.asInt()
                val content = node.get("content")?.asInt()
                val accuracy = node.get("accuracy")?.asInt()
                val reason = node.get("reason")?.asText() ?: ""

                // 항목별 점수를 reason에 포함해서 Langfuse에 기록
                val fullReason = "[제목:$title / 변경내용:$content / 정확성:$accuracy] $reason"
                langfuseClient.recordScore(traceId, score, fullReason)
                logger.debug { "LLM judge 채점 완료: score=$score (title=$title, content=$content, accuracy=$accuracy)" }
            } catch (e: Exception) {
                // 채점 실패는 메인 흐름에 영향을 주지 않음
                logger.warn { "LLM judge 채점 실패 (무시됨): ${e.message}" }
            }
        }
    }

    /**
     * 전달받은 프롬프트를 GLM에 전달하여 PR 초안을 생성합니다.
     *
     * <p>
     * 호출 전후로 시각을 측정하여 응답시간을 포함한 기록을 Langfuse에 비동기로 전송합니다.
     *
     * @param prompt AI에게 전달할 PR 생성 프롬프트
     * @return AI가 생성한 PR 제목 및 본문
     * @throws AiException AI 응답이 비어 있거나 JSON 파싱에 실패한 경우
     */
    override fun generatePrDraft(prompt: String): AiPrResult {
        val startTime = Instant.now()
        val chatResponse = chatModel.call(Prompt(prompt))
        val endTime = Instant.now()

        val response = chatResponse.result.output.text
            ?.takeIf { it.isNotBlank() }
            ?: throw AiException(AiErrorCode.AI_RESPONSE_EMPTY)

        // 토큰 수는 GLM API 응답에 포함된 usage 메타데이터에서 추출
        val usage = chatResponse.metadata.usage

        // 응답시간·토큰 수·프롬프트·결과를 Langfuse에 비동기 기록 (실패해도 메인 흐름에 영향 없음)
        val traceId = langfuseClient.recordGeneration(
            name = GENERATION_PR_DRAFT,
            input = prompt,
            output = response,
            startTime = startTime,
            endTime = endTime,
            inputTokens = usage?.promptTokens?.toInt(),
            outputTokens = usage?.completionTokens?.toInt()
        )

        val result = parseResponse(response)

        // traceId가 있을 때만 LLM judge 채점 (비동기, 응답 시간에 영향 없음)
        if (traceId != null) {
            evaluateAsync(traceId, prompt, result.title, result.body)
        }

        return result
    }

    /**
     * 한국어 PR 제목과 본문을 영어로 번역합니다.
     *
     * <p>
     * 호출 전후로 시각을 측정하여 응답시간을 포함한 기록을 Langfuse에 비동기로 전송합니다.
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

        val startTime = Instant.now()
        val chatResponse = chatModel.call(Prompt(prompt))
        val endTime = Instant.now()

        val response = chatResponse.result.output.text
            ?.takeIf { it.isNotBlank() }
            ?: throw AiException(AiErrorCode.AI_RESPONSE_EMPTY)

        val usage = chatResponse.metadata.usage

        // 응답시간·토큰 수·프롬프트·결과를 Langfuse에 비동기 기록 (실패해도 메인 흐름에 영향 없음)
        langfuseClient.recordGeneration(
            name = GENERATION_TRANSLATE,
            input = prompt,
            output = response,
            startTime = startTime,
            endTime = endTime,
            inputTokens = usage?.promptTokens?.toInt(),
            outputTokens = usage?.completionTokens?.toInt()
        )

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
        val candidate = Regex("""\{[\s\S]*\}""").find(response)?.value
        if (candidate != null && runCatching { objectMapper.readTree(candidate) }.isSuccess) return candidate
        // max_tokens로 잘린 경우 복구 시도
        return repairTruncated(response)
    }

    /**
     * max_tokens 도달로 JSON이 잘린 경우 복구를 시도합니다.
     * body 필드 문자열이 중간에 끊긴 형태({..."body":"...truncated)를 처리합니다.
     */
    private fun repairTruncated(response: String): String? {
        val start = response.indexOf('{')
        if (start == -1) return null
        var s = response.substring(start).trimEnd()
        if (s.endsWith('}')) return null
        while (s.endsWith('\\')) s = s.dropLast(1)
        val candidate = s + "\"}"
        return runCatching { objectMapper.readTree(candidate); candidate }.getOrNull()
    }
}
