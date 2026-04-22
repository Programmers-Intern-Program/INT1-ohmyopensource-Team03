package com.back.domain.issue.entity

import com.back.omos.global.jpa.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

/**
 * 레포지토리의 이슈(Issue) 정보를 관리하는 핵심 엔티티 클래스입니다.
 *
 * 이 클래스는 이슈의 기본 정보(번호, 제목, 본문, 상태)를 비롯하여,
 * 확장성을 고려한 라벨 데이터(JSON)와 AI 기반의 유사도 검색 및 추천을 위한
 * 이슈 컨텍스트 벡터(Vector) 데이터를 함께 저장합니다.
 *
 * [BaseEntity]를 상속받아 다음 공통 필드들을 자동으로 관리합니다:
 * - `id`: 시스템 내부 식별자 (Long, PK)
 * - `createdAt`: 엔티티 생성 일시
 * - `updatedAt`: 엔티티 수정 일시
 *
 * @author 유재원
 * @since 2026-04-22
 */
@Entity
@Table(name = "issues")
class Issue(

    /**
     * 이 이슈가 속한 레포지토리의 고유 식별자(ID)입니다.
     *
     * 데이터베이스 상에서 외래 키(FK) 역할을 하며, 객체 참조 대신 ID 값만 유지하여
     * 도메인 간의 결합도를 낮추는 구조로 설계되었습니다.
     */
    @Column(name = "repository_id", nullable = false)
    var repositoryId: Long,

    /**
     * 레포지토리 내에서 해당 이슈를 식별하는 고유 번호입니다.
     *
     * 예: GitHub의 이슈 번호(#1, #42 등)와 동일한 역할을 하며, URL 경로나 API에서 주로 사용됩니다.
     */
    @Column(name = "issue_number", nullable = false)
    var issueNumber: Int,

    /**
     * 이슈의 제목입니다.
     *
     * 목록에서 가장 먼저 노출되는 필수 텍스트 정보입니다.
     */
    @Column(nullable = false)
    var title: String,

    /**
     * 이슈의 본문(상세 내용)입니다.
     *
     * 사용자가 작성하는 긴 글이 들어갈 수 있으므로, 길이 제한이 없는 데이터베이스의 TEXT 타입으로 매핑됩니다.
     */
    @Column(columnDefinition = "TEXT")
    var content: String? = null,

    /**
     * 이슈에 부여된 라벨들의 목록입니다. (예: ["bug", "good-first-issue"])
     *
     * PostgreSQL의 강력한 jsonb 타입을 사용하여 저장하며, 별도의 연관관계 테이블 없이도
     * 효율적인 검색과 인덱싱이 가능하도록 설계되었습니다.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var labels: List<String>? = null,

    /**
     * 이슈 본문/제목 등의 의미(Context)를 임베딩한 벡터 데이터입니다.
     *
     * AI 기반의 유사도 검색이나 추천 기능 등에 사용됩니다.
     * PostgreSQL의 pgvector 확장을 사용하며, 1536 차원의 벡터 공간을 가집니다.
     * (참고: 1536은 OpenAI의 text-embedding 모델 등에서 주로 사용하는 차원 수입니다.)
     */
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(name = "issue_vector", columnDefinition = "vector(1536)")
    var issueVector: FloatArray? = null,

    /**
     * 이슈의 현재 진행 상태입니다.
     *
     * 기본값은 OPEN이며, 데이터베이스에는 문자열 형태('OPEN', 'CLOSED')로 저장되어 가독성을 높입니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: IssueStatus = IssueStatus.OPEN

) : BaseEntity() {

    /**
     * 이슈의 생명주기를 나타내는 상태값
     */
    enum class IssueStatus {
        OPEN,
        CLOSED
    }
}