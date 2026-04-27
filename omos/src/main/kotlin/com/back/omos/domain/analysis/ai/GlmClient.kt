package com.back.omos.domain.analysis.ai

/**
 * GLM API 호출 기능을 추상화한 인터페이스입니다.
 *
 * <p>
 * 서비스 계층은 이 인터페이스를 통해 GLM API를 호출하여
 * 이슈에 대한 코드 수정 가이드를 생성합니다.
 *
 * @author Jaewon Ryu
 * @since 2026-04-25
 */
interface GlmClient {

    /**
     * 이슈 정보와 관련 소스코드를 기반으로 코드 수정 가이드를 생성합니다.
     *
     * @param issueTitle 이슈 제목
     * @param issueBody 이슈 본문
     * @param labels 이슈 라벨 목록
     * @param fileContents 관련 파일 경로 → 파일 내용 Map
     * @return GLM이 생성한 분석 결과
     */
    fun analyze(
        issueTitle: String,
        issueBody: String?,
        labels: List<String>,
        fileContents: Map<String, String>
    ): GlmAnalysisRes
}