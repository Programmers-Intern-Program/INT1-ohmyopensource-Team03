package com.back.omos.domain.analysis.controller

import com.back.omos.domain.analysis.dto.GuideResponseDto
import com.back.omos.domain.analysis.dto.PseudoCodeResponseDto
import com.back.omos.domain.analysis.service.ContextAnalyzerService
import com.back.omos.global.response.CommonResponse

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Context Analyzer 컨트롤러
 *
 * 이슈에 대한 AI 코드 수정 가이드 및 의사 코드를 제공하는 엔드포인트를 담당합니다.
 * GitHub에서 관련 소스코드를 가져와 GLM API로 생성된 분석 결과를 반환합니다.
 *
 * @property contextAnalyzerService 코드 분석 비즈니스 로직 서비스
 */
@RestController
@RequestMapping("/api/v1/issues")
class ContextAnalyzerController(
    private val contextAnalyzerService: ContextAnalyzerService
) {

    /**
     * 이슈에 대한 코드 수정 가이드 조회
     *
     * 캐시에 분석 결과가 있으면 즉시 반환하고, 없으면 GitHub 소스코드 수집 →
     * GLM API 분석 → DB 저장 순으로 처리한 뒤 반환합니다.
     *
     * - 캐시 HIT: 저장된 분석 결과를 즉시 반환
     * - 캐시 MISS: 신규 분석 생성 후 반환 (시간 소요)
     *
     * @param issueId 분석할 이슈의 고유 식별자
     * @return [GuideResponseDto] 수정 대상 파일 경로, 가이드라인, 사이드 이펙트 포함
     * @throws AnalysisException ISSUE_NOT_FOUND - 이슈가 존재하지 않는 경우
     * @throws AnalysisException GITHUB_API_FAIL - GitHub API 호출 실패 시
     * @throws AnalysisException GLM_API_FAIL - GLM API 호출 실패 시
     * @throws AnalysisException ANALYSIS_GENERATION_FAIL - 분석 생성 중 오류 발생 시
     */
    @GetMapping("/{issueId}/guide")
    fun getGuide(
        @PathVariable issueId: Long
    ): ResponseEntity<CommonResponse<GuideResponseDto>> {
        val result = contextAnalyzerService.getGuide(issueId)
        return ResponseEntity.ok(
            CommonResponse.success(result)
        )
    }

    /**
     * 이슈에 대한 의사 코드(Pseudo Code) 조회
     *
     * 캐시에 분석 결과가 있으면 즉시 반환하고, 없으면 GitHub 소스코드 수집 →
     * GLM API 분석 → DB 저장 순으로 처리한 뒤 반환합니다.
     *
     * - 캐시 HIT: 저장된 분석 결과를 즉시 반환
     * - 캐시 MISS: 신규 분석 생성 후 반환 (시간 소요)
     *
     * @param issueId 분석할 이슈의 고유 식별자
     * @return [PseudoCodeResponseDto] 수정 대상 파일 경로, 의사 코드 포함
     * @throws AnalysisException ISSUE_NOT_FOUND - 이슈가 존재하지 않는 경우
     * @throws AnalysisException GITHUB_API_FAIL - GitHub API 호출 실패 시
     * @throws AnalysisException GLM_API_FAIL - GLM API 호출 실패 시
     * @throws AnalysisException ANALYSIS_GENERATION_FAIL - 분석 생성 중 오류 발생 시
     */
    @GetMapping("/{issueId}/pseudo")
    fun getPseudoCode(
        @PathVariable issueId: Long
    ): ResponseEntity<CommonResponse<PseudoCodeResponseDto>> {
        val result = contextAnalyzerService.getPseudoCode(issueId)
        return ResponseEntity.ok(
            CommonResponse.success(result)
        )
    }
}