package com.back.omos.domain.prdraft.entity

import com.back.omos.domain.issue.entity.Issue
import com.back.omos.domain.user.entity.User
import com.back.omos.global.jpa.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
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

    /**
     * PR 초안을 작성한 사용자입니다.
     *
     * 하나의 사용자는 여러 개의 PR 초안을 생성할 수 있습니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    /**
     * PR 초안이 연결된 Issue입니다.
     *
     * 해당 Issue를 해결하기 위한 변경 내용을 기반으로 PR이 생성됩니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    var issue: Issue,

    /**
     * 사용자가 작성한 코드 변경 내용(diff)입니다.
     *
     * 파일 변경 내역, 추가/삭제된 코드 등을 포함하며 PR 본문 생성의 입력 데이터로 사용됩니다.
     */
    @Column(name = "diff_content", nullable = false, columnDefinition = "TEXT")
    var diffContent: String,

    /**
     * 생성된 PR 제목입니다.
     *
     * diff 내용을 기반으로 AI가 생성한 제목을 포함합니다.
     */
    @Column(name = "pr_title", nullable = false, length = 255)
    var prTitle: String,

    /**
     * 생성된 PR 본문입니다.
     *
     * diff 내용을 기반으로 AI가 생성한 설명(변경 사항, 테스트 방법 등)을 포함합니다.
     */
    @Column(name = "pr_body", nullable = false, columnDefinition = "TEXT")
    var prBody: String,

    /**
     * GitHub PR 생성 시 기준이 되는 upstream 브랜치입니다. (예: main)
     */
    @Column(name = "base_branch", nullable = false)
    var baseBranch: String,

    /**
     * 포크한 사용자의 작업 브랜치입니다. (예: fix/issue-123)
     */
    @Column(name = "head_branch", nullable = false)
    var headBranch: String,

    /**
     * 포크한 사용자의 GitHub 로그인명입니다.
     *
     * GitHub Compare URL 생성 시 {forkOwner}:{headBranch} 형식으로 사용됩니다.
     */
    @Column(name = "fork_owner", nullable = false)
    var forkOwner: String

) : BaseEntity() {

}