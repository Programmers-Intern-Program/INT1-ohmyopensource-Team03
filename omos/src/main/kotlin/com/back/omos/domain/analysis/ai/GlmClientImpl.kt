package com.back.omos.domain.analysis.ai

import com.back.omos.global.exception.errorCode.AnalysisErrorCode
import com.back.omos.global.exception.exceptions.AnalysisException
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.model.ChatModel
import org.springframework.stereotype.Component

/**
 * Spring AI ChatClient를 통해 GLM API를 호출하는 구현체입니다.
 *
 * <p>
 * 이슈 정보와 관련 소스코드를 프롬프트로 구성하여 GLM API에 전달하고,
 * JSON 형태의 응답을 파싱하여 GlmAnalysisRes로 반환합니다.
 *
 * <p><b>상속 정보:</b><br>
 * {@link GlmClient}를 구현합니다.
 *
 * @author Jaewon Ryu
 * @since 2026-04-25
 * @see GlmClient
 */
@Component
class GlmClientImpl(
    private val chatModel: ChatModel,
    private val objectMapper: ObjectMapper
) : GlmClient {


    private val log = LoggerFactory.getLogger(GlmClientImpl::class.java)

    /**
     * 이슈 정보와 소스코드를 기반으로 GLM API에 분석을 요청합니다.
     *
     * @throws AnalysisException GLM_API_FAIL - API 호출 실패 시
     */
    override fun analyze(
        issueTitle: String,
        issueBody: String?,
        labels: List<String>,
        fileContents: Map<String, String>
    ): GlmAnalysisRes {
        val prompt = buildPrompt(issueTitle, issueBody, labels, fileContents)

        return try {
            val response = chatModel.call(prompt)  // 여기 변경
                ?: throw AnalysisException(
                    AnalysisErrorCode.GLM_API_FAIL,
                    "[GlmClientImpl#analyze] GLM 응답이 null입니다.",
                    "코드 분석에 실패했습니다."
                )

            parseResponse(response)

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
     * GLM API에 전달할 프롬프트를 구성합니다.
     *
     * 이슈 제목, 본문, 라벨, 관련 소스코드를 포함하며
     * JSON 형태로만 응답하도록 지시합니다.
     */
    private fun buildPrompt(
        issueTitle: String,
        issueBody: String?,
        labels: List<String>,
        fileContents: Map<String, String>
    ): String {
        val filesSection = fileContents.entries.joinToString("\n\n") { (path, content) ->
            "### $path\n```\n$content\n```"
        }

        return """
        이 이슈를 해결하려면 어떤 파일을 수정해야 하는지 알려주고, 수정 방향을 설명해줘.
        반드시 아래 JSON 형식으로만 응답해줘. 설명이나 마크다운 없이 JSON만 출력해.
        
        [이슈 내용]
        제목: $issueTitle
        라벨: ${labels.joinToString(", ")}
        본문: ${issueBody ?: "내용 없음"}
        
        [관련 코드]
        $filesSection
        
        [출력 형식]
        {
            "guideline": "수정 방향 설명 (한국어)",
            "pseudoCode": "수정 방법을 보여주는 의사 코드 (한국어)",
            "sideEffects": "수정 시 발생할 수 있는 부작용 (한국어)"
        }
        """.trimIndent()
    }

    /**
     * GLM API 응답 JSON을 GlmAnalysisRes로 파싱합니다.
     *
     * @throws AnalysisException GLM_API_FAIL - JSON 파싱 실패 시
     */
    private fun parseResponse(response: String): GlmAnalysisRes {
        return try {
            // ```json ... ``` 형태로 올 경우 제거
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
}