package com.back.omos.domain.analysis.service

import com.back.omos.domain.analysis.dto.GuideResponseDto
import com.back.omos.domain.analysis.dto.PseudoCodeResponseDto

/**
 * 이슈에 대한 코드 수정 가이드 및 수도 코드 조회 기능을 정의하는 서비스 인터페이스입니다.
 *
 * 서비스 계층은 이 인터페이스를 통해 GitHub 이슈를 분석하고,
 * 분석 결과([GuideResponseDto], [PseudoCodeResponseDto])를 반환합니다.
 *
 * **캐시 정책:**
 * 분석 결과가 이미 존재하면 즉시 반환하고, 없으면 GitHub API와 GLM API를
 * 통해 새로운 분석 결과를 생성합니다.
 * 이슈 수정 여부에 따른 캐시 갱신은 현재 미구현 상태입니다. ([TODO])
 *
 * @author Jaewon Ryu
 * @since 2026-04-22
 * @see ContextAnalyzerServiceImpl
 */
interface ContextAnalyzerService {

    /**
     * 이슈에 대한 코드 수정 가이드를 조회합니다.
     *
     * 사용자별 캐시 확인 → 일일 횟수 제한 검사 → 이슈별 캐시 확인 → 신규 분석 생성 순으로 처리합니다.
     * 같은 사용자가 동일 이슈에 재요청하면 기존 결과를 즉시 반환합니다.
     *
     * @param issueId 조회할 이슈의 식별자
     * @param githubId 요청한 사용자의 GitHub ID
     * @return 파일 경로 목록·가이드라인·사이드 이펙트를 담은 [GuideResponseDto]
     * @throws com.back.omos.global.exception.exceptions.AnalysisException
     *         ISSUE_NOT_FOUND — 이슈가 존재하지 않는 경우
     * @throws com.back.omos.global.exception.exceptions.AnalysisException
     *         ANALYSIS_RATE_LIMIT_EXCEEDED — 일일 분석 요청 횟수를 초과한 경우
     * @throws com.back.omos.global.exception.exceptions.AnalysisException
     *         GITHUB_API_FAIL — GitHub API 호출에 실패한 경우
     * @throws com.back.omos.global.exception.exceptions.AnalysisException
     *         GLM_API_FAIL — GLM API 호출 또는 응답 파싱에 실패한 경우
     * @throws com.back.omos.global.exception.exceptions.AuthException
     *         USER_NOT_FOUND — 사용자가 존재하지 않는 경우
     */
    fun getGuide(issueId: Long, githubId: String): GuideResponseDto

    /**
     * 이슈에 대한 수도 코드를 조회합니다.
     *
     * 사용자별 캐시 확인 → 일일 횟수 제한 검사 → 이슈별 캐시 확인 → 신규 분석 생성 순으로 처리합니다.
     * 같은 사용자가 동일 이슈에 재요청하면 기존 결과를 즉시 반환합니다.
     *
     * @param issueId 조회할 이슈의 식별자
     * @param githubId 요청한 사용자의 GitHub ID
     * @return 파일 경로 목록·수도 코드를 담은 [PseudoCodeResponseDto]
     * @throws com.back.omos.global.exception.exceptions.AnalysisException
     *         ISSUE_NOT_FOUND — 이슈가 존재하지 않는 경우
     * @throws com.back.omos.global.exception.exceptions.AnalysisException
     *         ANALYSIS_RATE_LIMIT_EXCEEDED — 일일 분석 요청 횟수를 초과한 경우
     * @throws com.back.omos.global.exception.exceptions.AnalysisException
     *         GITHUB_API_FAIL — GitHub API 호출에 실패한 경우
     * @throws com.back.omos.global.exception.exceptions.AnalysisException
     *         GLM_API_FAIL — GLM API 호출 또는 응답 파싱에 실패한 경우
     * @throws com.back.omos.global.exception.exceptions.AuthException
     *         USER_NOT_FOUND — 사용자가 존재하지 않는 경우
     */
    fun getPseudoCode(issueId: Long, githubId: String): PseudoCodeResponseDto
}