package com.back.omos.domain.issue.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * GitHub API로부터 수집한 이슈 상세 정보를 담는 데이터 전송 객체(DTO)입니다.
 * <p>
 * GitHub REST API의 응답 JSON 데이터를 코틀린 객체로 역직렬화하며,
 * 분석에 필요한 핵심 필드(제목, 본문, 라벨 등)만을 선택적으로 유지합니다.
 *
 * <p><b>상속 정보:</b><br>
 * 해당 사항 없음 (Data Class)
 *
 * <p><b>주요 생성자:</b><br>
 * {@code GithubIssueResponse(id, number, title, body, htmlUrl, labels)} <br>
 * GitHub 이슈의 식별자, 번호, 텍스트 데이터 및 메타데이터를 초기화합니다.
 *
 * <p><b>빈 관리:</b><br>
 * 해당 객체는 빈으로 관리되지 않으며, 외부 API 호출 시 동적으로 생성됩니다.
 *
 * <p><b>외부 모듈:</b><br>
 * Jackson 라이브러리를 사용하여 JSON 필드 매핑 및 무시 설정을 처리합니다.
 *
 * @author 유재원
 * @since 2026-04-24
 * @see <a href="https://docs.github.com/en/rest/issues/issues">GitHub Issues API</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubIssueResponse(
    val id: Long,
    val number: Long,
    val title: String,
    val body: String?,

    @JsonProperty("html_url")
    val htmlUrl: String,

    /** * 이슈가 속한 레포지토리의 API URL입니다.
     * 예: "https://api.github.com/repos/naver/fixture-monkey"
     * 여기서 "naver/fixture-monkey"를 파싱하여 repoFullName으로 사용합니다.
     */
    @JsonProperty("repository_url")
    val repositoryUrl: String,

    val labels: List<LabelResponse> = emptyList()
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class LabelResponse(
        val name: String,
        val color: String
    )
}