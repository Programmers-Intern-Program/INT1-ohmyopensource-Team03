package com.back.omos.domain.analysis.entity

import com.back.omos.domain.user.entity.User
import com.back.omos.global.jpa.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * 사용자의 분석 요청을 저장하는 엔티티입니다.
 *
 * 사용자가 특정 이슈에 대한 분석을 요청할 때 생성되며,
 * 분석 완료 전(PENDING)에는 [analysisResult]가 null이고
 * 분석 완료 후(COMPLETED)에 [AnalysisResult]와 연결됩니다.
 *
 * 동일 이슈에 여러 사용자가 요청할 수 있으며, 이 경우 [AnalysisResult]를 재사용합니다.
 * 같은 사용자가 이미 분석된 이슈에 재요청하면 새로운 레코드를 생성하지 않고
 * 기존 [AnalysisResult]를 반환합니다.
 *
 * [BaseEntity]를 상속받아 다음 필드들을 자동으로 관리합니다:
 * - `id`: 시스템 내부 식별자 (Long, PK)
 * - `createdAt`: 요청 생성 일시 (횟수 제한 계산의 기준이 됩니다)
 * - `updatedAt`: 엔티티 수정 일시
 *
 * @author MintyU
 * @since 2026-04-28
 * @see AnalysisResult
 * @see User
 */
@Entity
@Table(name = "user_analysis_requests")
class UserAnalysisRequest(

    /**
     * 분석을 요청한 사용자입니다.
     *
     * 한 사용자는 여러 이슈에 대해 분석을 요청할 수 있으며,
     * [createdAt]을 기준으로 기간 내 요청 횟수를 집계하여 횟수 제한에 활용합니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    /**
     * 요청에 대응하는 분석 결과입니다.
     *
     * 분석 요청 직후에는 null(PENDING 상태)이며,
     * Context Analyzer가 분석을 완료하면 해당 [AnalysisResult]와 연결됩니다(COMPLETED 상태).
     * 동일 이슈에 대한 기존 [AnalysisResult]가 있으면 새로 생성하지 않고 재사용합니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_result_id", nullable = true)
    var analysisResult: AnalysisResult? = null

) : BaseEntity() {

    /**
     * 분석 결과를 연결하여 요청을 COMPLETED 상태로 전환합니다.
     *
     * @param result 완료된 분석 결과
     */
    fun complete(result: AnalysisResult) {
        this.analysisResult = result
    }
}
