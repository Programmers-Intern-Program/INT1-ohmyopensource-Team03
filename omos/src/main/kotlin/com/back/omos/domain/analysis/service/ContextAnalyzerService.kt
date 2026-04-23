package com.back.omos.domain.analysis.service

import com.back.omos.domain.analysis.dto.GuideResponseDto
import com.back.omos.domain.analysis.dto.PseudoCodeResponseDto

/**
 * 코드에 대한 전체적인 역할을 적습니다.
 * <p>
 * 코드에 대한 작동 원리 등을 적습니다.
 *
 * <p><b>상속 정보:</b><br>
 * 상속 정보를 적습니다.
 *
 * <p><b>주요 생성자:</b><br>
 * {@code ContextAnalyzerService(String example)} <br>
 * 주요 생성자와 그 매개변수에 대한 설명을 적습니다. <br>
 *
 * <p><b>빈 관리:</b><br>
 * 필요 시 빈 관리에 대한 내용을 적습니다.
 *
 * <p><b>외부 모듈:</b><br>
 * 필요 시 외부 모듈에 대한 내용을 적습니다.
 *
 * @author Jaewon Ryu
 * @since 2026-04-22
 * @see
 */
interface ContextAnalyzerService {

    /**
     * 이슈에 대한 코드 수정 가이드를 조회합니다.
     * 캐시된 결과가 있고 이슈가 수정되지 않았으면 캐시를 반환하고,
     * 이슈가 수정됐거나 캐시가 없으면 새로 분석합니다.
     */
    fun getGuide(issueId: Long): GuideResponseDto

    /**
     * 이슈에 대한 의사 코드를 조회합니다.
     */
    fun getPseudoCode(issueId: Long): PseudoCodeResponseDto
}