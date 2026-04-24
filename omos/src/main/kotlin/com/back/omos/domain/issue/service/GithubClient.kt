package com.back.omos.domain.issue.service

import com.back.omos.domain.issue.dto.GithubIssueResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import kotlin.jvm.java

/**
 * GitHub REST API와 직접 통신하여 리포지토리 데이터를 수집하는 클라이언트 클래스입니다.
 * <p>
 * 비차단(Non-blocking) I/O 모델인 [WebClient]를 사용하여 GitHub 서버에 HTTP 요청을 전송하며,
 * 전달받은 JSON 응답을 [GithubIssueResponse] DTO 리스트로 변환하는 역할을 수행합니다.
 *
 * <p><b>상속 정보:</b><br>
 * 해당 사항 없음
 *
 * <p><b>주요 생성자:</b><br>
 * {@code GithubClient(webClient, token)} <br>
 * HTTP 요청을 위한 [WebClient]와 GitHub API 호출 시 인증에 필요한 Personal Access Token을 주입받습니다.
 *
 * <p><b>빈 관리:</b><br>
 * Spring Container에 의해 Singleton 빈으로 관리됩니다. (@Component)
 *
 * <p><b>외부 모듈:</b><br>
 * Spring WebFlux의 WebClient를 사용하여 비동기 통신 아키텍처를 구성합니다.
 *
 * @author 유재원
 * @since 2026-04-24
 * @see <a href="https://docs.github.com/en/rest">GitHub REST API Documentation</a>
 */
@Component
class GithubClient(
    private val webClient: WebClient,
    @Value("\${github.token}") private val token: String
) {
    fun fetchIssues(owner: String, repo: String): List<GithubIssueResponse> {
        return webClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/repos/$owner/$repo/issues")
                    .queryParam("state", "open")
                    .queryParam("per_page", 1) // 현재 테스트용으로 1개 설정
                    .build()
            }
            .header("Authorization", "Bearer $token")
            .retrieve()
            .bodyToFlux(GithubIssueResponse::class.java)
            .collectList()
            .block() ?: emptyList()
    }
}