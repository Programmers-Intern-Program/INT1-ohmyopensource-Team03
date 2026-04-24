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
     * 특정 레포지토리와 이슈 번호로 존재 여부 확인
     */
    fun existsByRepoFullNameAndIssueNumber(repoFullName: String, issueNumber: Long): Boolean

    /**
     * 특정 레포지토리와 이슈 번호로 단일 이슈 조회
     * 결과가 없을 수 있으므로 반환 타입을 Issue? (Nullable)로 설정합니다.
     */
    fun findByRepoFullNameAndIssueNumber(repoFullName: String, issueNumber: Long): Issue?
}