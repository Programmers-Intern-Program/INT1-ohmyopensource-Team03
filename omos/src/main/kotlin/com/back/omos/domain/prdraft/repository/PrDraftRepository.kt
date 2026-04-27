package com.back.omos.domain.prdraft.repository

import com.back.omos.domain.prdraft.entity.PrDraft
import org.springframework.data.jpa.repository.JpaRepository

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
     * 특정 사용자의 PR 초안 목록을 생성일 내림차순으로 조회합니다.
     *
     * @param githubId 조회할 사용자의 GitHub ID
     * @return PR 초안 목록 (최신순)
     */
    fun findAllByUserGithubIdOrderByCreatedAtDesc(githubId: String): List<PrDraft>
}