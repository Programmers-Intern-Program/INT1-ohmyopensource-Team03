package com.back.omos.domain.issue.ai

import com.back.omos.domain.issue.dto.AIRecommendationResult
import com.back.omos.domain.issue.entity.Issue
import com.back.omos.global.exception.errorCode.AiErrorCode
import com.back.omos.global.exception.exceptions.AiException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.core.type.TypeReference
import org.springframework.ai.chat.model.ChatModel
import org.springframework.stereotype.Component

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
    private val objectMapper: ObjectMapper
) : IssueGlmClient {


    override fun generateRecommendationReasons(
        userProfile: String,
        candidateIssues: List<Issue>
    ): List<AIRecommendationResult> {
        val promptText = buildRecommendPrompt(userProfile, candidateIssues)

        val responseContent = try {
            chatModel.call(promptText) ?: throw AiException(AiErrorCode.AI_RESPONSE_EMPTY);
        } catch (e: Exception) {
            throw AiException(AiErrorCode.AI_RESPONSE_PARSE_FAILED);
        }

        return try {
            objectMapper.readValue(responseContent, object : TypeReference<List<AIRecommendationResult>>() {})
        } catch (e: Exception) {
            throw AiException(AiErrorCode.AI_RESPONSE_PARSE_FAILED);
        }
    }

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
            당신은 개발자의 기술 스택과 오픈소스 이슈를 매칭하는 전문 컨설턴트입니다.
            아래 제공된 '개발자 프로필'을 분석하고, '후보 이슈 리스트' 중에서 개발자에게 가장 추천할 만한 이슈 3개를 엄선하여 선정 이유를 작성해주세요.
            
            [개발자 프로필]
            $userProfile
            
            [후보 이슈 리스트]
            $issuesContext
            
            [지시 사항]
            1. 후보 이슈들 중 개발자의 주요 사용 언어 및 관심사와 가장 부합하는 3개의 이슈를 선정하세요.
            2. 선정된 각 이슈에 대해 왜 이 개발자에게 적합한지, 어떤 기술적 성장이 기대되는지 한국어로 상세히 설명하세요.
            3. 결과는 반드시 유사도가 높은 순서대로 3개만 포함해야 합니다.
            4. 반드시 아래 JSON 배열 형식으로만 응답하세요. (마크다운 코드 블록 제외, 순수 JSON만 출력)
            
            [출력 형식]
            [
              {
                "title": "이슈 제목",
                "repoName": "레포지토리 풀네임",
                "reason": "추천 사유 및 기대되는 성장 포인트"
              },
              {
                "title": "이슈 제목",
                "repoName": "레포지토리 풀네임",
                "reason": "추천 사유 및 기대되는 성장 포인트"
              },
              {
                "title": "이슈 제목",
                "repoName": "레포지토리 풀네임",
                "reason": "추천 사유 및 기대되는 성장 포인트"
              }
            ]   
        """.trimIndent()
    }
}