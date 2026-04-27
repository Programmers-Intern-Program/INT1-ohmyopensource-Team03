package com.back.omos.global.ai

import org.springframework.ai.document.Document
import org.springframework.ai.embedding.Embedding
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.ai.embedding.EmbeddingResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * Google AI Studio의 OpenAI 호환 엔드포인트를 통해 Gemini 임베딩을 수행하는 [EmbeddingModel] 구현체입니다.
 *
 * Spring AI의 Vertex AI 스타터는 GCP 프로젝트 ID가 필수이므로,
 * API 키만으로 사용 가능한 Google AI Studio의 OpenAI 호환 REST API를 직접 호출합니다.
 *
 * @property apiKey Google AI Studio API 키
 * @property model 사용할 임베딩 모델 ID (기본값: gemini-embedding-2)
 *
 * @author MintyU
 * @since 2026-04-24
 */
@Primary
@Component
class GeminiEmbeddingModel(
    @Value("\${gemini.api-key}") private val apiKey: String,
    @Value("\${gemini.embedding.model:gemini-embedding-2}") private val model: String
) : EmbeddingModel {

    private val restClient = RestClient.builder()
        .baseUrl("https://generativelanguage.googleapis.com/v1beta/openai")
        .defaultHeader("Authorization", "Bearer $apiKey")
        .build()

    override fun call(request: EmbeddingRequest): EmbeddingResponse {
        val embeddings = embedTexts(request.instructions).mapIndexed { index, floatArray ->
            Embedding(floatArray, index)
        }
        return EmbeddingResponse(embeddings)
    }

    override fun embed(document: Document): FloatArray =
        embedTexts(listOfNotNull(document.text)).firstOrNull() ?: floatArrayOf()

    private fun embedTexts(texts: List<String>): List<FloatArray> {
        val response = restClient.post()
            .uri("/embeddings")
            .contentType(MediaType.APPLICATION_JSON)
            .body(OpenAiEmbeddingRequest(model = model, input = texts))
            .retrieve()
            .body(OpenAiEmbeddingResponse::class.java)

        return response?.data
            ?.sortedBy { it.index }
            ?.map { it.embedding.toFloatArray() }
            ?: emptyList()
    }

    private data class OpenAiEmbeddingRequest(val model: String, val input: List<String>)
    private data class OpenAiEmbeddingResponse(val data: List<EmbeddingData>)
    private data class EmbeddingData(val embedding: List<Float>, val index: Int)
}
