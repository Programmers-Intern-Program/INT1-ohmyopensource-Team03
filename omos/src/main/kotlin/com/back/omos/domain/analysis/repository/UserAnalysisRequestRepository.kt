package com.back.omos.domain.analysis.repository

import com.back.omos.domain.analysis.entity.UserAnalysisRequest
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

/**
 * [UserAnalysisRequest] 엔티티에 대한 데이터 액세스 인터페이스입니다.
 *
 * 기본적인 CRUD 연산은 [JpaRepository]에서 제공하며,
 * 횟수 제한 검사, 중복 요청 탐지, 이슈 목록 분석 여부 조회를 위한 메서드를 추가로 정의합니다.
 *
 * @author MintyU
 * @since 2026-04-28
 */
interface UserAnalysisRequestRepository : JpaRepository<UserAnalysisRequest, Long> {

    /**
     * 특정 사용자의 기간 내 분석 요청 횟수를 조회합니다.
     *
     * [from] 이상 [to] 미만의 [UserAnalysisRequest.createdAt]을 가진 레코드를 집계하며,
     * 서비스 레이어에서 허용 횟수 초과 여부를 판단하는 데 사용됩니다.
     *
     * @param userId 조회할 사용자의 ID
     * @param from 집계 시작 일시 (포함)
     * @param to 집계 종료 일시 (포함)
     * @return 해당 기간의 요청 횟수
     */
    fun countByUserIdAndCreatedAtBetween(userId: Long, from: LocalDateTime, to: LocalDateTime): Long

    /**
     * 특정 사용자가 특정 이슈에 대해 완료된 분석 요청을 조회합니다.
     *
     * 같은 사용자가 이미 분석된 이슈에 재요청할 때 기존 레코드를 반환하기 위해 사용됩니다.
     * [UserAnalysisRequest.analysisResult]가 null인(PENDING) 레코드는 조회되지 않습니다.
     *
     * @param userId 조회할 사용자의 ID
     * @param issueId 조회할 이슈의 ID
     * @return 완료된 분석 요청, 없으면 null
     */
    fun findFirstByUserIdAndAnalysisResultIssueId(userId: Long, issueId: Long): UserAnalysisRequest?

    /**
     * 특정 사용자가 분석을 요청한 이슈 ID 목록을 기반으로 완료된 요청 목록을 조회합니다.
     *
     * 이슈 목록 응답에 `isAnalyzed`, `analysisResultId` 필드를 채우기 위해 사용됩니다.
     * [UserAnalysisRequest.analysisResult]가 null인(PENDING) 레코드는 조회되지 않습니다.
     *
     * @param userId 조회할 사용자의 ID
     * @param issueIds 조회 대상 이슈 ID 목록
     * @return 해당 이슈들에 대한 완료된 분석 요청 목록
     */
    fun findAllByUserIdAndAnalysisResultIssueIdIn(userId: Long, issueIds: List<Long>): List<UserAnalysisRequest>
}
