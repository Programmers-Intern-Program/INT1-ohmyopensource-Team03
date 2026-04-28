package com.back.omos.domain.issue.repository

import com.back.omos.domain.issue.entity.Issue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

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


    /**
     * 유저의 프로필 벡터와 유사한 이슈를 지정된 개수만큼 검색합니다.
     *
     * PostgreSQL의 `pgvector` 확장을 사용하여 유저 벡터와 이슈 벡터 간의
     * 코사인 거리(Cosine Distance)를 기반으로 가장 유사도가 높은 이슈들을 추출합니다.
     * * @param userVector 사용자의 기술 스택 및 관심사가 반영된 3072차원 임베딩 벡터
     * @return 유사도가 높은 순서대로 정렬된 상위 5개의 [Issue] 리스트
     */
    @Query(
        value = """
            SELECT * FROM issues 
            ORDER BY issue_vector <=> CAST(:userVector AS vector) 
            LIMIT :limit
        """, nativeQuery = true
    )
    fun findBySimilarity(
        @Param("userVector") userVector: DoubleArray,
        @Param("limit") limit: Int
    ): List<Issue>
}