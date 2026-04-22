package com.back.omos.domain.analysis.repository

import com.back.omos.domain.analysis.entity.AnalysisResult
import org.springframework.data.jpa.repository.JpaRepository

/**
 * [AnalysisResult] 엔티티에 대한 데이터 액세스 인터페이스입니다.
 *
 * 기본적인 CRUD 연산은 [JpaRepository]에서 제공하며,
 * 캐시 히트 판단을 위한 이슈 기반 조회 메서드를 추가로 정의합니다.
 *
 * @author Jaewon Ryu
 * @since 2026-04-22
 */
interface AnalysisResultRepository : JpaRepository<AnalysisResult, Long> {

    /**
     * 특정 이슈에 대한 분석 결과를 조회합니다.
     *
     * 동일 이슈 재요청 시 캐시된 결과를 반환하기 위해 사용됩니다.
     * 결과가 없으면 null을 반환하며, 이 경우 새로운 분석을 트리거합니다.
     *
     * @param issueId 조회할 이슈의 ID
     * @return 해당 이슈의 분석 결과, 없으면 null
     */
    fun findByIssueId(issueId: Long): AnalysisResult?
}