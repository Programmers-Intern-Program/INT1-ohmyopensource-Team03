package com.back.omos.domain.issue.repository

import com.back.omos.domain.issue.entity.UserRecommendedIssue
import org.springframework.data.jpa.repository.JpaRepository

/**
 * [UserRecommendedIssue] 엔티티에 대한 데이터 액세스 인터페이스입니다.
 *
 * 기본적인 CRUD 연산은 [JpaRepository]에서 제공하며,
 * 추천 이력 조회 및 upsert를 위한 배치 조회 메서드를 추가로 정의합니다.
 *
 * @author MintyU
 * @since 2026-04-29
 */
interface UserRecommendedIssueRepository : JpaRepository<UserRecommendedIssue, Long> {

    /**
     * 특정 사용자의 추천 이력을 최근 추천 순으로 조회합니다.
     *
     * [UserRecommendedIssue.updatedAt]을 기준으로 내림차순 정렬하므로
     * 재추천된 이슈는 갱신된 일시 기준으로 상위에 노출됩니다.
     *
     * @param userId 조회할 사용자의 ID
     * @return 해당 사용자의 전체 추천 이력 (최근 추천 순)
     */
    fun findAllByUserIdOrderByUpdatedAtDesc(userId: Long): List<UserRecommendedIssue>

    /**
     * 특정 사용자의 추천 이력 중 지정한 이슈 ID 목록에 해당하는 레코드를 조회합니다.
     *
     * 추천 결과 저장 시 기존 레코드 유무를 한 번의 쿼리로 확인하기 위해 사용됩니다.
     *
     * @param userId 조회할 사용자의 ID
     * @param issueIds 조회 대상 이슈 ID 목록
     * @return 해당 이슈들에 대한 기존 추천 이력 목록
     */
    fun findAllByUserIdAndIssueIdIn(userId: Long, issueIds: List<Long>): List<UserRecommendedIssue>
}
