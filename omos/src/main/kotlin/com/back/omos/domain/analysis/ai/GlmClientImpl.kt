package com.back.omos.domain.analysis.ai

import com.back.omos.global.ai.LangfuseClient
import com.back.omos.global.exception.errorCode.AnalysisErrorCode
import com.back.omos.global.exception.exceptions.AnalysisException
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.model.ChatModel
import org.springframework.stereotype.Component
import org.springframework.ai.chat.prompt.Prompt
import java.time.Instant
import java.util.concurrent.Executors

/**
 * Spring AI [ChatModel]을 통해 GLM API를 호출하는 [GlmClient] 구현체입니다.
 *
 * 이슈 정보와 관련 소스코드를 프롬프트로 구성하여 GLM API에 전달하고,
 * JSON 형태의 응답을 파싱하여 [GlmAnalysisRes]로 반환합니다.
 *
 * 각 AI 호출의 프롬프트·응답·응답시간은 [LangfuseClient]를 통해 비동기로 기록됩니다.
 * Langfuse 미설정 시 기록을 건너뛰므로 기능에는 영향을 주지 않습니다.
 * @author Jaewon Ryu
 * @since 2026-04-25
 * @see GlmClient
 */
@Component
class GlmClientImpl(
    private val chatModel: ChatModel,
    private val objectMapper: ObjectMapper,
    private val langfuseClient: LangfuseClient,
    private val promptBuilder: GlmPromptBuilder
) : GlmClient {

    companion object {
        private const val GENERATION_SELECT_FILES = "select-files-${GlmPromptBuilder.PROMPT_VERSION_SELECT_FILES}"
        private const val GENERATION_ANALYZE = "analyze-${GlmPromptBuilder.PROMPT_VERSION_ANALYZE}"
        private val judgeExecutor = Executors.newFixedThreadPool(2)
    }

    private val log = LoggerFactory.getLogger(GlmClientImpl::class.java)

    /**
     * 이슈 정보와 소스코드를 기반으로 GLM API에 코드 분석을 요청합니다.
     *
     * 프롬프트를 구성하여 [ChatModel]에 전달하고, JSON 응답을 [GlmAnalysisRes]로 파싱합니다.
     * 응답이 null이거나 JSON 파싱에 실패하면 [AnalysisException]을 던집니다.
     * 호출 전후로 시각을 측정하여 응답시간·토큰 수를 [LangfuseClient]에 비동기로 기록합니다.
     *
     * @param issueTitle 분석 대상 이슈 제목
     * @param issueBody 분석 대상 이슈 본문 (없으면 null)
     * @param labels 이슈에 붙은 라벨 목록
     * @param fileContents 관련 파일 경로와 내용의 맵 (key: 파일 경로, value: 파일 내용)
     * @return GLM API가 생성한 가이드라인·수도 코드·사이드 이펙트를 담은 [GlmAnalysisRes]
     * @throws AnalysisException GLM_API_FAIL — API 응답이 null이거나 호출/파싱에 실패한 경우
     */
    override fun analyze(
        issueTitle: String,
        issueBody: String?,
        labels: List<String>,
        fileContents: Map<String, String>
    ): GlmAnalysisRes {
        val prompt = promptBuilder.buildAnalyzePrompt(issueTitle, issueBody, labels, fileContents)

        return try {
            val startTime = Instant.now()
            val chatResponse = chatModel.call(Prompt(prompt))
            val endTime = Instant.now()

            val response = chatResponse.result.output.text
                ?: throw AnalysisException(
                    AnalysisErrorCode.GLM_API_FAIL,
                    "[GlmClientImpl#analyze] GLM 응답이 null입니다.",
                    "코드 분석에 실패했습니다."
                )
            val usage = chatResponse.metadata.usage
            val traceId = langfuseClient.recordGeneration(
                name = GENERATION_ANALYZE,
                input = prompt,
                output = response,
                startTime = startTime,
                endTime = endTime,
                inputTokens = usage?.promptTokens?.toInt(),
                outputTokens = usage?.completionTokens?.toInt()
            )

            val result = parseResponse(response)
            if (traceId != null) {
                evaluateAnalyzeAsync(traceId, prompt, result)
            }
            return result

        } catch (e: AnalysisException) {
            throw e
        } catch (e: Exception) {
            log.error("[GlmClientImpl#analyze] GLM API 호출 실패: ${e.message}")
            throw AnalysisException(
                AnalysisErrorCode.GLM_API_FAIL,
                "[GlmClientImpl#analyze] GLM API 호출 실패: ${e.message}",
                "코드 분석에 실패했습니다."
            )
        }
    }

    /**
     * 이슈 정보와 파일 경로 목록을 기반으로 GLM API에 관련 파일 선별을 요청합니다.
     *
     * 이슈 해결에 수정이 필요한 파일을 최대 5개 선별하여 반환합니다.
     * JSON 파싱에 실패하거나 API 호출이 실패하면 [AnalysisException]을 던집니다.
     * 호출 전후로 시각을 측정하여 응답시간·토큰 수를 [LangfuseClient]에 비동기로 기록합니다.
     *
     * @param issueTitle 분석 대상 이슈 제목
     * @param issueBody 분석 대상 이슈 본문 (없으면 null)
     * @param filePaths 후보 파일 경로 목록
     * @return GLM API가 선별한 관련 파일 경로 목록 (최대 5개)
     * @throws AnalysisException GLM_API_FAIL — API 호출 또는 응답 파싱에 실패한 경우
     */
    override fun selectFiles(
        issueTitle: String,
        issueBody: String?,
        filePaths: List<String>
    ): List<String> {
        val prompt = promptBuilder.buildSelectFilesPrompt(issueTitle, issueBody, filePaths)

        return try {
            val startTime = Instant.now()
            val chatResponse = chatModel.call(Prompt(prompt))
            val endTime = Instant.now()

            val response = chatResponse.result.output.text
                ?: throw AnalysisException(
                    AnalysisErrorCode.GLM_API_FAIL,
                    "[GlmClientImpl#selectFiles] GLM 응답이 null입니다.",
                    "관련 파일 탐색에 실패했습니다."
                )
            val usage = chatResponse.metadata.usage
            val traceId = langfuseClient.recordGeneration(
                name = GENERATION_SELECT_FILES,
                input = prompt,
                output = response,
                startTime = startTime,
                endTime = endTime,
                inputTokens = usage?.promptTokens?.toInt(),
                outputTokens = usage?.completionTokens?.toInt()
            )

            val result = parseFileList(response)

            if (traceId != null) {
                evaluateSelectFilesAsync(traceId, issueTitle, issueBody, filePaths, result)
            }

            return result
        } catch (e: AnalysisException) {
            throw e
        } catch (e: Exception) {
            log.error("[GlmClientImpl#selectFiles] GLM API 호출 실패: ${e.message}")
            throw AnalysisException(
                AnalysisErrorCode.GLM_API_FAIL,
                "[GlmClientImpl#selectFiles] GLM API 호출 실패: ${e.message}",
                "관련 파일 탐색에 실패했습니다."
            )
        }
    }

    /**
     * GLM API의 파일 선별 응답 JSON을 파일 경로 목록으로 파싱합니다.
     *
     * 응답에 마크다운 코드 블록(` ```json `)이 포함된 경우 제거 후 파싱합니다.
     * JSON에 `"files"` 키가 없거나 파싱에 실패하면 [AnalysisException]을 던집니다.
     *
     * @param response GLM API의 원본 응답 문자열
     * @return 파싱된 파일 경로 목록
     * @throws AnalysisException GLM_API_FAIL — JSON 파싱 실패 또는 `"files"` 키가 없는 경우
     */
    private fun parseFileList(response: String): List<String> {
        return try {
            val cleaned = response
                .replace(Regex("```json\\s*"), "")
                .replace(Regex("```\\s*"), "")
                .trim()
            val node = objectMapper.readTree(cleaned)
            val filesNode = node.get("files")
                ?: throw AnalysisException(
                    AnalysisErrorCode.GLM_API_FAIL,
                    "[GlmClientImpl#parseFileList] 'files' 키가 없습니다.",
                    "관련 파일 탐색 결과를 처리하는 데 실패했습니다."
                )
            filesNode.map { it.asText() }
        } catch (e: AnalysisException) {  // 추가!
            throw e
        } catch (e: Exception) {
            log.error("[GlmClientImpl#parseFileList] JSON 파싱 실패: $response")
            throw AnalysisException(
                AnalysisErrorCode.GLM_API_FAIL,
                "[GlmClientImpl#parseFileList] JSON 파싱 실패: ${e.message}",
                "관련 파일 탐색 결과를 처리하는 데 실패했습니다."
            )
        }
    }


    /**
     * GLM API 응답 JSON을 [GlmAnalysisRes]로 파싱합니다.
     *
     * 응답에 마크다운 코드 블록(` ```json `)이 포함된 경우 제거 후 파싱합니다.
     * Jackson 역직렬화에 실패하면 [AnalysisException]을 던집니다.
     *
     * @param response GLM API의 원본 응답 문자열
     * @return 파싱된 [GlmAnalysisRes]
     * @throws AnalysisException GLM_API_FAIL — JSON 파싱 실패 시
     */
    private fun parseResponse(response: String): GlmAnalysisRes {
        return try {
            val cleaned = response
                .replace(Regex("```json\\s*"), "")
                .replace(Regex("```\\s*"), "")
                .trim()

            objectMapper.readValue(cleaned, GlmAnalysisRes::class.java)
        } catch (e: Exception) {
            log.error("[GlmClientImpl#parseResponse] JSON 파싱 실패: $response")
            throw AnalysisException(
                AnalysisErrorCode.GLM_API_FAIL,
                "[GlmClientImpl#parseResponse] JSON 파싱 실패: ${e.message}",
                "코드 분석 결과를 처리하는 데 실패했습니다."
            )
        }
    }

    private fun evaluateSelectFilesAsync(
        traceId: String,
        issueTitle: String,
        issueBody: String?,
        candidates: List<String>,
        selected: List<String>
    ) {
        judgeExecutor.submit {
            try {
                val judgePrompt = """
                아래 파일 선별 결과가 이슈 해결에 얼마나 적절한지 평가해줘.

                [이슈 제목]
                $issueTitle

                [이슈 본문]
                ${issueBody ?: "내용 없음"}

                [전체 후보 파일 수]
                ${candidates.size}개

                [선별된 파일]
                ${selected.joinToString("\n")}

[평가 기준]
- 선별된 파일이 이슈 제목/본문의 핵심 키워드와 직접 관련 있는가?
  (모두 관련: 4점, 절반 이상 관련: 2점, 절반 미만 관련: 0점)
- 이슈 해결에 필요한 핵심 파일이 포함됐는가?
  (핵심 파일 명확히 포함: 3점, 불확실: 1점, 누락 의심: 0점)
- 관련 없는 파일이 포함됐는가?
  (없음: 3점, 1개: 1점, 2개 이상: 0점)

                반드시 아래 JSON 형식으로만 응답해.
                {"score": 7.5, "reason": "채점 근거 한 줄"}
            """.trimIndent()

                val judgeResponse = chatModel.call(judgePrompt) ?: return@submit
                val json = extractJson(judgeResponse) ?: return@submit
                val node = objectMapper.readTree(json)
                val score = node.get("score")?.asDouble() ?: return@submit
                val reason = node.get("reason")?.asText() ?: ""

                langfuseClient.recordScore(traceId, score, reason)
                log.debug("selectFiles judge 채점 완료: score=$score")
            } catch (e: Exception) {
                log.warn("selectFiles judge 채점 실패 (무시됨): ${e.message}")
            }
        }
    }

    private fun evaluateAnalyzeAsync(traceId: String, originalPrompt: String, result: GlmAnalysisRes) {
        judgeExecutor.submit {
            try {
                val judgePrompt = """
                아래 코드 분석 결과가 이슈와 코드 내용을 얼마나 잘 반영했는지 평가해줘.

                [원본 프롬프트 (이슈 + 코드 포함)]
                $originalPrompt

                [생성된 분석 결과]
                guideline: ${result.guideline}
                pseudoCode: ${result.pseudoCode}
                sideEffects: ${result.sideEffects}

[평가 기준]
- guideline이 이슈의 핵심 원인을 정확히 짚는가?
  (정확히 짚음: 3점, 부분적: 1점, 엉뚱함: 0점)
- pseudoCode가 구체적인 파일경로와 함수명을 포함하는가?
  (둘 다 있음: 3점, 하나만: 1점, 없음: 0점)
- sideEffects가 실제 발생 가능한 내용인가?
  (현실적: 2점, 모호함: 1점, 없거나 엉뚱함: 0점)
- 없는 내용을 만들어내지 않았는가?
  (hallucination 없음: 2점, 있음: 0점)

                반드시 아래 JSON 형식으로만 응답해.
                {"score": 7.5, "reason": "채점 근거 한 줄"}
            """.trimIndent()

                val judgeResponse = chatModel.call(judgePrompt) ?: return@submit
                val json = extractJson(judgeResponse) ?: return@submit
                val node = objectMapper.readTree(json)
                val score = node.get("score")?.asDouble() ?: return@submit
                val reason = node.get("reason")?.asText() ?: ""

                langfuseClient.recordScore(traceId, score, reason)
                log.debug("analyze judge 채점 완료: score=$score")
            } catch (e: Exception) {
                log.warn("analyze judge 채점 실패 (무시됨): ${e.message}")
            }
        }
    }

    private fun extractJson(response: String): String? {
        val fenceMatch = Regex("""```(?:json)?\s*([\s\S]*?)```""").find(response)
        if (fenceMatch != null) return fenceMatch.groupValues[1].trim()
        val candidate = Regex("""\{[\s\S]*\}""").find(response)?.value ?: return null
        return runCatching { objectMapper.readTree(candidate); candidate }.getOrNull()
    }
}