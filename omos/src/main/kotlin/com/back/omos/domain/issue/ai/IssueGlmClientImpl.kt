package com.back.omos.domain.issue.ai

import com.back.omos.domain.issue.dto.AIRecommendationResult
import com.back.omos.domain.issue.entity.Issue
import com.back.omos.global.ai.LangfuseClient
import com.back.omos.global.exception.errorCode.AiErrorCode
import com.back.omos.global.exception.exceptions.AiException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.core.type.TypeReference
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

/**
 * Generative AI 모델과 통신하여 실제 추천 로직을 수행하는 클라이언트 구현체입니다.
 * <p>
 * Spring AI의 {@code ChatClient}를 통해 AI 모델에 정제된 프롬프트를 전달하며,
 * 모델로부터 받은 JSON 형식의 응답을 {@code ObjectMapper}를 사용하여 데이터 객체 리스트로 변환합니다.
 *
 * <p><b>상속 정보:</b><br>
 * {@link IssueGlmClient} 인터페이스를 구현합니다.
 *
 * <p><b>주요 생성자:</b><br>
 * {@code IssueGlmClientImpl(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper)}  <br>
 * AI 통신을 위한 클라이언트 빌더와 JSON 파싱을 위한 매퍼를 주입받습니다. <br>
 *
 * <p><b>빈 관리:</b><br>
 * {@code @Component} 어노테이션을 통해 스프링 컨테이너에 의해 싱글톤 빈으로 관리됩니다.
 *
 * <p><b>외부 모듈:</b><br>
 * Spring AI (ChatClient) 및 Jackson (ObjectMapper) 라이브러리를 활용합니다.
 *
 * @author 유재원
 * @since 2026-04-27
 * @see com.back.omos.domain.recommend.client.IssueGlmClient
 */
@Component
class IssueGlmClientImpl(
    private val chatModel: ChatModel,
    private val objectMapper: ObjectMapper,
    private val langfuseClient: LangfuseClient // 추가
) : IssueGlmClient {

    companion object {
        // 프롬프트를 수정할 때마다 버전을 올려야 Langfuse에서 버전별 성능 비교가 가능합니다.
        private const val GENERATION_NAME = "issue-recommendation-GLM-4.5-v5"
    }

    override fun generateRecommendationReasons(
        userProfile: String,
        candidateIssues: List<Issue>
    ): List<AIRecommendationResult> {
        val promptText = buildRecommendPrompt(userProfile, candidateIssues)

        // 측정 시작 (전체 프로세스 시작)
        val startTime = Instant.now()

        // 1. 추천 로직 실행
        val chatResponse = try {
            chatModel.call(Prompt(promptText))
        } catch (e: Exception) {
            throw AiException(AiErrorCode.AI_RESPONSE_PARSE_FAILED)
        }

        val recEndTime = Instant.now()
        val recDuration = Duration.between(startTime, recEndTime).toMillis() / 1000.0

        val responseContent = chatResponse.result.output.text ?: throw AiException(AiErrorCode.AI_RESPONSE_EMPTY)
        val usage = chatResponse.metadata.usage

        return try {
            val results =
                objectMapper.readValue(responseContent, object : TypeReference<List<AIRecommendationResult>>() {})

            // 2. 평가 로직 실행 (평가 결과와 걸린 시간을 리스트로 받아옴)
            val evalStartTime = Instant.now()
            val evaluationResults = evaluateAllRecommendations(userProfile, results) // 일괄 평가 호출
            val totalEndTime = Instant.now()
            val evalDuration = Duration.between(evalStartTime, totalEndTime).toMillis() / 1000.0

            // 3. Langfuse Generation 기록 (전체 시간을 포함)
            val traceId = langfuseClient.recordGeneration(
                name = GENERATION_NAME,
                input = promptText,
                output = responseContent,
                startTime = startTime,
                endTime = totalEndTime,
                inputTokens = usage?.promptTokens?.toInt(),
                outputTokens = usage?.completionTokens?.toInt()
            )

            // 4. 생성된 traceId에 개별 점수 매칭 기록
            if (traceId != null) {
                evaluationResults.forEachIndexed { index, eval ->
                    // 일괄 평가이므로 evalDuration은 전체 호출에 걸린 시간입니다.
                    val timeInfo = "[Time - 추천:${recDuration}s / 평가(Batch):${evalDuration}s]"

                    langfuseClient.recordScore(
                        traceId = traceId,
                        score = eval.score,
                        reason = "$timeInfo ${eval.fullReason}"
                    )
                }
            }

            results
        } catch (e: Exception) {
            // 실패 시에도 일단 현재까지의 시간으로 기록
            val errorEndTime = Instant.now()
            val traceId = langfuseClient.recordGeneration(
                name = GENERATION_NAME,
                input = promptText,
                output = responseContent,
                startTime = startTime,
                endTime = errorEndTime
            )
            if (traceId != null) {
                langfuseClient.recordScore(traceId, 0.0, "JSON Parsing Failed: ${e.message}")
            }
            throw AiException(AiErrorCode.AI_RESPONSE_PARSE_FAILED);
        }
    }

    /**
     * 개별 추천 결과를 평가하고 결과 객체를 반환합니다.
     */
    private fun evaluateAllRecommendations(
        userProfile: String,
        results: List<AIRecommendationResult>
    ): List<EvalResult> {
        val recommendationsJson = results.mapIndexed { index, res ->
            mapOf("id" to index, "title" to res.title, "reason" to res.reason)
        }.let { objectMapper.writeValueAsString(it) }

        val batchEvalPrompt = """
        너는 깐깐한 시니어 개발자다. 아래 [추천 리스트]의 적절성을 [사용자 프로필] 기준으로 한 번에 평가하라.
        
        [사용자 프로필]: $userProfile
        [추천 리스트]: $recommendationsJson

        [채점 기준 (총 10점)]
        1. 스택(3점): 기술 스택 일치 여부
        2. 도메인(4점): 관심 분야 매칭
        3. 성장(3점): 사유의 구체성 및 학습 가치
        
        [규칙]
        - 관대함 금지. 평범하면 5점, 스택 불일치 시 해당 항목 0점.
        - 응답은 반드시 아래 JSON 배열 형식만 허용 (설명 생략):
        [{"id": 0, "score": 8.0, "stack": 3, "domain": 3, "growth": 2, "reason": "한 문장 근거"}, ...]
    """.trimIndent()

        return try {
            val response = chatModel.call(batchEvalPrompt) ?: return emptyList()
            val cleanJson = response.replace("```json", "").replace("```", "").trim()
            val nodes = objectMapper.readTree(cleanJson)

            nodes.map { node ->
                val score = node.get("score").asDouble()
                val stack = node.get("stack").asInt()
                val domain = node.get("domain").asInt()
                val growth = node.get("growth").asInt()
                val reason = node.get("reason").asText()
                EvalResult(score, "[스택:$stack/도메인:$domain/성장:$growth] $reason")
            }
        } catch (e: Exception) {
            results.map { EvalResult(0.0, "일괄 평가 실패: ${e.message}") }
        }
    }

    // 평가 결과를 임시 저장하기 위한 내부 클래스
    private data class EvalResult(val score: Double, val fullReason: String)

    /**
     * 유저의 기술 스택과 후보 이슈 데이터를 하나로 묶는 프롬프트 엔지니어링
     */
    private fun buildRecommendPrompt(userProfile: String, candidateIssues: List<Issue>): String {
        val issuesContext = candidateIssues.mapIndexed { index, issue ->
            """
            [후보 ${index + 1}]
            - 제목: ${issue.title}
            - 레포지토리: ${issue.repoFullName}
            - 주요 라벨: ${issue.labels?.joinToString(", ") ?: "없음"}
            - 이슈 요약: ${issue.content?.take(400) ?: "내용 없음"}
            """.trimIndent()
        }.joinToString("\n\n")

        return """
            당신은 개발자의 기술 이력과 오픈소스 프로젝트를 연결하는 전문 매칭 시스템입니다.
            제공된 [개발자 프로필]을 분석하여, [후보 이슈 리스트] 중 가장 적합한 이슈 3개를 선정하세요.
            
            ### [개발자 프로필]
            $userProfile
            
            ### [후보 이슈 리스트]
            $issuesContext
            
            ### [지시 사항]
            1. **프로필 기반 키워드 추출**: 프로필에서 개발자의 주력 기술과 관심 도메인을 스스로 파악하세요.
            2. **기술적 매칭**: 파악한 키워드와 연관성이 높은 이슈를 3개 선정합니다.
            3. **언어 및 내용 제약 (CRITICAL)**: 
               - **모든 추천 사유('reason' 필드)는 한국어로 작성하세요.**
               - **[핵심] 추천 사유는 2~3문장 내외로 매우 간결하게 작성하세요.** (장황한 설명은 지양하고 매칭된 기술 스택과 이유만 명확히 제시)
               - 영어 이슈도 한국어로 요약하되, 전문 용어(예: Spring Boot, JWT)는 원문을 유지하세요.
            4. **형식 준수**: 아래 JSON 배열 형식으로만 응답하며, 마크다운 코드 블록(```json)이나 다른 인사말은 절대 포함하지 마세요.
            
            ### [출력 형식]
            [
              {
                "title": "이슈 제목",
                "repoName": "레포지토리 풀네임",
                "reason": "핵심 기술 매칭과 성장 포인트 중심의 간결한 한국어 사유"
              }
            ]
        """.trimIndent()
    }
}