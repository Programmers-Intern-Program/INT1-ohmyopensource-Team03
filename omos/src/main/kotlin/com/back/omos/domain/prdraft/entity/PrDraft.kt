package com.back.omos.domain.prdraft.entity

import com.back.omos.global.jpa.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * 사용자가 특정 Issue를 해결하기 위해 작성한 PR 초안을 저장하는 엔티티입니다.
 * <p>
 * 사용자와 Issue 간의 관계를 기반으로 코드 변경 내용(diff)과 PR 본문을 관리합니다.
 *
 * <p><b>상속 정보:</b><br>
 * {@link BaseEntity}를 상속받아 공통 필드(id, createdAt, updatedAt)를 사용합니다.
 *
 * @author 5h6vm
 * @since 2026-04-21
 * @see BaseEntity
 */
@Entity
@Table(name = "pr_draft")
class PrDraft(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    var issue: Issue,

    @Lob
    @Column(name = "diff_content", nullable = false, columnDefinition = "TEXT")
    var diffContent: String,

    @Lob
    @Column(name = "pr_body", nullable = false, columnDefinition = "TEXT")
    var prBody: String

) : BaseEntity() {
    protected constructor() : this(
        user = User(),
        issue = Issue(),
        diffContent = "",
        prBody = ""
    )
}