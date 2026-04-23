package com.back.omos.domain.issue.repository

import com.back.omos.domain.issue.entity.Issue
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 이슈(Issue) 엔티티의 데이터 접근을 담당하는 Repository입니다.
 *
 * <p>
 * Spring Data JPA를 통해 기본 CRUD 및 조회 기능을 제공합니다.
 *
 * @author 유재원
 * @since 2026-04-22
 * @see
 */
interface IssueRepository : JpaRepository<Issue, Long> {
    /**
     * repositoryId와 issueNumber로 존재하는지 확인
     */
    fun existsByRepositoryIdAndIssueNumber(repositoryId : Long, issueNumber: Long) : Boolean
}