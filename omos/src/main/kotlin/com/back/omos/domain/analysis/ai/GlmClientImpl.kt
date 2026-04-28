package com.back.omos.domain.analysis.ai

import com.back.omos.global.exception.errorCode.AnalysisErrorCode
import com.back.omos.global.exception.exceptions.AnalysisException
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.model.ChatModel
import org.springframework.stereotype.Component

/**
 * Spring AI [ChatModel]을 통해 GLM API를 호출하는 [GlmClient] 구현체입니다.
 *
 * 이슈 정보와 관련 소스코드를 프롬프트로 구성하여 GLM API에 전달하고,
 * JSON 형태의 응답을 파싱하여 [GlmAnalysisRes]로 반환합니다.
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
     * 이슈 정보와 소스코드를 기반으로 GLM API에 코드 분석을 요청합니다.
     *
     * 프롬프트를 구성하여 [ChatModel]에 전달하고, JSON 응답을 [GlmAnalysisRes]로 파싱합니다.
     * 응답이 null이거나 JSON 파싱에 실패하면 [AnalysisException]을 던집니다.
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
        val prompt = buildPrompt(issueTitle, issueBody, labels, fileContents)

        return try {
            val response = chatModel.call(prompt)
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
     * 이슈 정보와 파일 경로 목록을 기반으로 GLM API에 관련 파일 선별을 요청합니다.
     *
     * 이슈 해결에 수정이 필요한 파일을 최대 5개 선별하여 반환합니다.
     * JSON 파싱에 실패하거나 API 호출이 실패하면 [AnalysisException]을 던집니다.
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
        val prompt = buildSelectFilesPrompt(issueTitle, issueBody, filePaths)

        return try {
            val response = chatModel.call(prompt)
            parseFileList(response)
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
     * 파일 선별 요청을 위한 프롬프트를 구성합니다.
     *
     * GLM이 반드시 `{ "files": [...] }` 형태의 JSON만 반환하도록 지시합니다.
     *
     * @param issueTitle 이슈 제목
     * @param issueBody 이슈 본문 (없으면 null)
     * @param filePaths 선별 대상 파일 경로 목록
     * @return GLM API에 전달할 프롬프트 문자열
     */
    private fun buildSelectFilesPrompt(
        issueTitle: String,
        issueBody: String?,
        filePaths: List<String>
    ): String {
        return """
    아래 이슈를 해결하기 위해 수정이 필요한 파일을 골라줘.
    반드시 아래 JSON 형식으로만 응답해. 설명이나 마크다운 없이 JSON만 출력해.
    최대 5개까지만 선택해.
    
    [이슈 내용]
    제목: $issueTitle
    본문: ${issueBody ?: "내용 없음"}
    
    [파일 목록]
    ${filePaths.joinToString("\n")}
    
    [출력 형식]
    {
        "files": ["파일경로1", "파일경로2"]
    }
    """.trimIndent()
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
            node.get("files").map { it.asText() }
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
     * 코드 분석 요청을 위한 프롬프트를 구성합니다.
     *
     * 이슈 제목·본문·라벨과 관련 소스코드를 포함하며,
     * GLM이 반드시 `guideline`, `pseudoCode`, `sideEffects` 필드를 가진
     * JSON만 반환하도록 지시합니다.
     *
     * @param issueTitle 이슈 제목
     * @param issueBody 이슈 본문 (없으면 null)
     * @param labels 이슈 라벨 목록
     * @param fileContents 관련 파일 경로와 내용의 맵 (key: 파일 경로, value: 파일 내용)
     * @return GLM API에 전달할 프롬프트 문자열
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
            "pseudoCode": "Before:\\n[수정 전 코드]\\n\\nAfter:\\n[수정 후 코드] 형태의 문자열로 줘",
            "sideEffects": "수정 시 발생할 수 있는 부작용 (한국어)"
        }
        """.trimIndent()
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
}