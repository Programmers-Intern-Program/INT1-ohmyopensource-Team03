package com.back.omos.global.ai

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant
import java.util.Base64
import java.util.UUID

/**
 * Langfuse에 AI 호출 결과를 비동기로 기록하는 클라이언트입니다.
 *
 * <p>
 * AI 호출마다 프롬프트·응답·응답시간·메타데이터를 Langfuse 인제스션 API로 전송하여
 * 프롬프트 버전별 성능 변화를 대시보드에서 추적할 수 있도록 합니다.
 *
 * <p><b>설정:</b><br>
 * {@code langfuse.host}, {@code langfuse.public-key}, {@code langfuse.secret-key} 중
 * 하나라도 비어 있으면 WebClient를 생성하지 않고 기록을 전부 건너뜁니다.
 * 덕분에 Langfuse 미설정 환경에서도 애플리케이션이 정상 동작합니다.
 *
 * <p><b>장애 격리:</b><br>
 * 전송은 fire-and-forget 방식으로 처리됩니다.
 * Langfuse 서버가 다운되거나 네트워크 오류가 발생해도 경고 로그만 남기고
 * 메인 요청 흐름에는 영향을 주지 않습니다.
 *
 * @author 5h6vm
 * @since 2026-04-30
 */
@Component
class LangfuseClient(
    @Value("\${langfuse.host:}") private val host: String,
    @Value("\${langfuse.public-key:}") private val publicKey: String,
    @Value("\${langfuse.secret-key:}") private val secretKey: String
) {

    private val logger = KotlinLogging.logger {}

    // host·publicKey·secretKey가 모두 설정된 경우에만 WebClient를 초기화
    private val webClient: WebClient? by lazy {
        if (host.isBlank() || publicKey.isBlank() || secretKey.isBlank()) {
            logger.info { "Langfuse 설정이 없어 AI 호출 기록을 건너뜁니다." }
            null
        } else {
            WebClient.builder()
                .baseUrl(host)
                .defaultHeader(
                    "Authorization",
                    // Langfuse는 Basic Auth 방식으로 인증 (publicKey:secretKey를 Base64 인코딩)
                    "Basic " + Base64.getEncoder().encodeToString("$publicKey:$secretKey".toByteArray())
                )
                .build()
        }
    }

    /**
     * AI 호출 한 건을 Langfuse generation으로 기록합니다.
     *
     * <p>
     * Langfuse 인제스션 API({@code POST /api/public/ingestion})의 batch 형식으로
     * generation-create 이벤트를 전송합니다.
     * WebClient가 초기화되지 않은 경우(미설정) 즉시 반환합니다.
     *
     * <p><b>비동기 처리:</b><br>
     * {@code subscribe()}로 fire-and-forget 방식으로 전송하므로 호출 스레드를 블로킹하지 않습니다.
     * 전송 실패 시 에러 콜백에서 경고 로그를 남기고 종료합니다.
     *
     * @param name generation 이름 (예: "pr-draft-v1", "pr-translate-v1")
     * @param input AI에 전달한 프롬프트 원문
     * @param output AI가 반환한 응답 원문
     * @param startTime AI 호출 시작 시각
     * @param endTime AI 호출 완료 시각
     * @param inputTokens 프롬프트 토큰 수 (GLM API 응답에서 추출, null이면 기록 생략)
     * @param outputTokens 응답 토큰 수 (GLM API 응답에서 추출, null이면 기록 생략)
     * @param metadata 프롬프트 버전·컨텍스트 타입 등 추가 기록 정보
     */
    fun recordGeneration(
        name: String,
        input: String,
        output: String,
        startTime: Instant,
        endTime: Instant,
        inputTokens: Int? = null,
        outputTokens: Int? = null,
        metadata: Map<String, Any> = emptyMap()
    ): String? {
        // Langfuse가 설정되지 않은 환경에서는 기록 생략
        val client = webClient ?: return null

        // traceId를 미리 생성해 반환함으로써 호출부에서 score를 붙일 수 있게 함
        val traceId = UUID.randomUUID().toString()
        val body = buildIngestionBody(traceId, name, input, output, startTime, endTime, inputTokens, outputTokens, metadata)

        // fire-and-forget: 전송 실패가 메인 요청에 영향을 주지 않도록 subscribe만 걸고 반환
        client.post()
            .uri("/api/public/ingestion")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(String::class.java)
            .subscribe(
                { res -> logger.debug { "Langfuse 기록 성공: $res" } },
                { e -> logger.warn { "Langfuse 기록 실패 (무시됨): ${e.message}" } }
            )

        return traceId
    }

    /**
     * 특정 trace에 품질 점수를 기록합니다.
     *
     * <p>
     * LLM-as-judge가 채점한 결과를 Langfuse scores API로 전송합니다.
     * Langfuse UI의 해당 trace에 점수와 채점 근거가 표시됩니다.
     *
     * <p><b>비동기 처리:</b><br>
     * fire-and-forget 방식으로 처리되어 메인 흐름에 영향을 주지 않습니다.
     *
     * @param traceId 점수를 붙일 trace ID ([recordGeneration]의 반환값)
     * @param score 품질 점수 (0.0 ~ 10.0)
     * @param reason 채점 근거
     */
    fun recordScore(traceId: String, score: Double, reason: String) {
        val client = webClient ?: return

        val body = mapOf(
            "traceId" to traceId,
            "name" to "quality",
            "value" to score,
            "comment" to reason
        )

        // fire-and-forget: 점수 전송 실패가 메인 요청에 영향을 주지 않도록 함
        client.post()
            .uri("/api/public/scores")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(String::class.java)
            .subscribe(
                { res -> logger.debug { "Langfuse 점수 기록 성공: $res" } },
                { e -> logger.warn { "Langfuse 점수 기록 실패 (무시됨): ${e.message}" } }
            )
    }

    /**
     * Langfuse 인제스션 API에 전송할 요청 바디를 구성합니다.
     *
     * <p>
     * trace-create와 generation-create 두 이벤트를 하나의 batch로 묶어 반환합니다.
     * Langfuse UI에서 generation은 trace 하위에 속해야 표시되므로
     * 두 이벤트가 같은 traceId를 공유합니다.
     *
     * @param name generation 이름
     * @param input 프롬프트 원문
     * @param output AI 응답 원문
     * @param startTime 호출 시작 시각
     * @param endTime 호출 완료 시각
     * @param inputTokens 프롬프트 토큰 수 (null이면 usage 필드 생략)
     * @param outputTokens 응답 토큰 수 (null이면 usage 필드 생략)
     * @param metadata 추가 기록 정보
     * @return Langfuse 인제스션 API 요청 바디
     */
    private fun buildIngestionBody(
        traceId: String,
        name: String,
        input: String,
        output: String,
        startTime: Instant,
        endTime: Instant,
        inputTokens: Int?,
        outputTokens: Int?,
        metadata: Map<String, Any>
    ): Map<String, Any> {

        return mapOf("batch" to listOf(
                // trace를 먼저 생성해야 generation이 그 아래에 표시됨
                mapOf(
                    "id" to UUID.randomUUID().toString(),
                    "type" to "trace-create",
                    "timestamp" to startTime.toString(),
                    "body" to mapOf(
                        "id" to traceId,
                        "name" to name,
                        "timestamp" to startTime.toString(),
                        "metadata" to metadata
                    )
                ),
                mapOf(
                    "id" to UUID.randomUUID().toString(),
                    "type" to "generation-create",
                    "timestamp" to startTime.toString(),
                    "body" to buildGenerationBody(traceId, name, input, output, startTime, endTime, inputTokens, outputTokens, metadata)
                )
            )
        )
    }

    /**
     * generation-create 이벤트의 body를 구성합니다.
     *
     * <p>
     * inputTokens·outputTokens가 null이 아닌 경우에만 usage 필드를 포함합니다.
     * GLM API가 토큰 정보를 반환하지 않는 경우 usage 없이 기록합니다.
     *
     * @return generation body 맵
     */
    private fun buildGenerationBody(
        traceId: String,
        name: String,
        input: String,
        output: String,
        startTime: Instant,
        endTime: Instant,
        inputTokens: Int?,
        outputTokens: Int?,
        metadata: Map<String, Any>
    ): Map<String, Any> {
        val body = mutableMapOf<String, Any>(
            "id" to UUID.randomUUID().toString(),
            "traceId" to traceId,
            "name" to name,
            "startTime" to startTime.toString(),
            "endTime" to endTime.toString(),
            "model" to "glm-4.5",
            "input" to input,
            "output" to output,
            "metadata" to metadata
        )

        // 토큰 정보가 있을 때만 usage 필드 추가 (GLM API가 미제공 시 생략)
        if (inputTokens != null || outputTokens != null) {
            body["usage"] = mapOf(
                "input" to (inputTokens ?: 0),
                "output" to (outputTokens ?: 0),
                "total" to ((inputTokens ?: 0) + (outputTokens ?: 0)),
                "unit" to "TOKENS"
            )
        }

        return body
    }
}
