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
               - **반드시 모든 추천 사유('reason' 필드)는 한국어로만 작성하세요.**
               - 후보 이슈의 제목이나 요약이 영어로 되어 있더라도, 이를 분석하여 **한국어로 번역 및 설명**해야 합니다.
               - 전문 용어(예: Spring Boot, JWT, Redis)는 그대로 사용하되, 문장은 자연스러운 한국어로 구성하세요.
            4. **형식 준수**: 아래 JSON 배열 형식으로만 응답하며, 마크다운 코드 블록(```json)이나 다른 인사말은 절대 포함하지 마세요.
            
            ### [출력 형식]
            [
              {
                "title": "이슈 제목",
                "repoName": "레포지토리 풀네임",
                "reason": "반드시 한국어로 작성된 맞춤형 추천 사유 및 성장 포인트"
              }
            ]
        """.trimIndent()
    }
}