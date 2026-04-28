package com.back.omos.domain.analysis.ai

/**
 * GLM API 호출 기능을 추상화한 인터페이스입니다.
 *
 * 서비스 계층은 이 인터페이스를 통해 GLM API와 통신하며,
 * 이슈에 대한 코드 수정 가이드 생성([analyze])과
 * 관련 파일 선별([selectFiles]) 두 가지 기능을 제공합니다.
 *
 * @author Jaewon Ryu
 * @since 2026-04-25
 * @see GlmClientImpl
 */
interface GlmClient {

    /**
     * 이슈 정보와 관련 소스코드를 기반으로 코드 수정 가이드를 생성합니다.
     *
     * @param issueTitle 이슈 제목
     * @param issueBody 이슈 본문. 본문이 없는 이슈의 경우 null을 전달하며,
     *                  구현체는 null 시 "내용 없음"으로 대체하여 처리합니다.
     * @param labels 이슈에 붙은 라벨 목록. 없으면 빈 리스트를 전달합니다.
     * @param fileContents 관련 파일 경로와 내용의 맵 (key: 파일 경로, value: 파일 내용)
     * @return GLM이 생성한 가이드라인·수도 코드·사이드 이펙트를 담은 [GlmAnalysisRes]
     * @throws com.back.omos.global.exception.exceptions.AnalysisException
     *         GLM_API_FAIL — API 호출 실패, 응답이 null이거나 JSON 파싱에 실패한 경우
     */
    fun analyze(
        issueTitle: String,
        issueBody: String?,
        labels: List<String>,
        fileContents: Map<String, String>
    ): GlmAnalysisRes

    /**
     * 레포지토리 파일 트리와 이슈 내용을 기반으로 수정이 필요한 파일 경로를 선별합니다.
     *
     * @param issueTitle 이슈 제목
     * @param issueBody 이슈 본문. 본문이 없는 이슈의 경우 null을 전달하며,
     *                  구현체는 null 시 "내용 없음"으로 대체하여 처리합니다.
     * @param filePaths 선별 대상 파일 경로 목록 (레포지토리 전체 파일 트리)
     * @return GLM이 선별한 관련 파일 경로 목록
     * @throws com.back.omos.global.exception.exceptions.AnalysisException
     *         GLM_API_FAIL — API 호출 실패 또는 응답 JSON 파싱에 실패한 경우
     */
    fun selectFiles(
        issueTitle: String,
        issueBody: String?,
        filePaths: List<String>
    ): List<String>
}