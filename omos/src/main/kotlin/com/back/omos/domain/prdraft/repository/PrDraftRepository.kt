package com.back.omos.domain.prdraft.repository

import com.back.omos.domain.prdraft.entity.PrDraft
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * PR 초안(PrDraft) 엔티티의 데이터 접근을 담당하는 Repository입니다.
 *
 * <p>
 * Spring Data JPA를 통해 기본 CRUD 및 조회 기능을 제공합니다.
 *
 * <p><b>상속 정보:</b><br>
 * JpaRepository를 상속받아 별도의 구현 없이 데이터베이스 연산을 수행합니다.
 *
 * @author 5h6vm
 * @since 2026-04-21
 */

interface PrDraftRepository : JpaRepository<PrDraft, Long> {

    /**
     * 특정 사용자의 PR 초안 목록을 이슈 정보와 함께 생성일 내림차순으로 페이징 조회합니다.
     *
     * <p>
     * fetch join과 페이징을 함께 사용하기 위해 countQuery를 분리합니다.
     *
     * @param githubId 조회할 사용자의 GitHub ID
     * @param pageable 페이지 정보
     * @return PR 초안 페이지 (최신순, issue 포함)
     */
    @Query(
        value = "SELECT pd FROM PrDraft pd JOIN FETCH pd.issue WHERE pd.user.githubId = :githubId ORDER BY pd.createdAt DESC",
        countQuery = "SELECT COUNT(pd) FROM PrDraft pd WHERE pd.user.githubId = :githubId"
    )
    fun findAllWithIssueByUserGithubId(@Param("githubId") githubId: String, pageable: Pageable): Page<PrDraft>

    /**
     * PR 초안 ID와 사용자 GitHub ID로 PR 초안을 조회합니다.
     *
     * <p>
     * 소유자 확인을 DB 쿼리 레벨에서 수행하여 존재 여부 노출을 방지합니다.
     *
     * @param id PR 초안 ID
     * @param githubId 조회할 사용자의 GitHub ID
     * @return PR 초안 (없거나 소유자 불일치 시 null)
     */
    fun findByIdAndUserGithubId(id: Long, githubId: String): PrDraft?

    /**
     * PR 초안 ID와 사용자 GitHub ID로 PR 초안을 이슈 정보와 함께 조회합니다.
     *
     * <p>
     * fetch join을 사용하여 issue를 한 번의 쿼리로 함께 조회하며,
     * 소유자 확인을 DB 쿼리 레벨에서 수행하여 존재 여부 노출을 방지합니다.
     *
     * @param id PR 초안 ID
     * @param githubId 조회할 사용자의 GitHub ID
     * @return PR 초안 (없거나 소유자 불일치 시 null, issue 포함)
     */
    @Query("SELECT pd FROM PrDraft pd JOIN FETCH pd.issue WHERE pd.id = :id AND pd.user.githubId = :githubId")
    fun findByIdWithIssueAndUserGithubId(@Param("id") id: Long, @Param("githubId") githubId: String): PrDraft?
}
