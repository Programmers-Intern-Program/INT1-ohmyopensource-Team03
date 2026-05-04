package com.back.omos.domain.issue.entity

import com.back.omos.domain.user.entity.User
import com.back.omos.global.jpa.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 사용자에게 추천된 이슈 이력을 저장하는 엔티티입니다.
 *
 * 사용자가 추천을 받을 때마다 추천된 이슈와 AI가 생성한 추천 사유를 기록합니다.
 * 동일 사용자-이슈 조합은 유니크 제약으로 하나의 레코드만 유지되며,
 * 재추천 시에는 [summary]가 최신 내용으로 갱신됩니다.
 * [BaseEntity.updatedAt]이 자동 갱신되므로 가장 최근 추천 순으로 정렬할 수 있습니다.
 *
 * [BaseEntity]를 상속받아 다음 필드들을 자동으로 관리합니다:
 * - `id`: 시스템 내부 식별자 (Long, PK)
 * - `createdAt`: 최초 추천 일시
 * - `updatedAt`: 마지막 추천 일시 (재추천 시 갱신)
 *
 * @author MintyU
 * @since 2026-04-29
 * @see Issue
 * @see User
 */
@Entity
@Table(
    name = "user_recommended_issues",
    uniqueConstraints = [UniqueConstraint(
        name = "uk_user_recommended_issue_user_issue",
        columnNames = ["user_id", "issue_id"]
    )]
)
class UserRecommendedIssue(

    /**
     * 추천을 받은 사용자입니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    /**
     * 추천된 이슈입니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    val issue: Issue,

    /**
     * AI가 생성한 이슈 추천 사유입니다.
     *
     * 재추천 시 최신 사유로 갱신됩니다.
     */
    @Column(columnDefinition = "TEXT")
    var summary: String? = null

) : BaseEntity()
