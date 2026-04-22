package com.back.omos.domain.analysis.entity

import com.back.omos.domain.issue.entity.Issue
import com.back.omos.global.jpa.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

/**
 * AI가 생성한 코드 수정 가이드를 저장하는 엔티티입니다.
 *
 * 사용자가 이슈를 선택하면 Context Analyzer가 관련 코드를 분석하여
 * 수정 가이드를 생성하고, 동일 이슈 재요청 시 캐싱된 결과를 반환합니다.
 *
 * [BaseEntity]를 상속받아 다음 필드들을 자동으로 관리합니다:
 * - `id`: 시스템 내부 식별자 (Long, PK)
 * - `createdAt`: 엔티티 생성 일시
 * - `updatedAt`: 엔티티 수정 일시
 *
 * @author Jaewon Ryu
 * @since 2026-04-22
 * @see
 */
@Entity
@Table(name = "analysis_results")
class AnalysisResult(

    /**
     * 분석 대상 이슈입니다.
     *
     * 하나의 이슈에 대해 하나의 분석 결과가 생성되며,
     * 동일 이슈 재요청 시 이 관계를 통해 캐시 히트 여부를 판단합니다.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", unique = true, nullable = false)
    var issue: Issue,

    /**
     * AI가 분석한 수정 후보 파일 경로 목록입니다.
     *
     * JSON 배열 형태의 문자열로 저장됩니다.
     * 예: ["src/main/java/.../GpxConversions.java", "src/test/.../GpxConversionsTest.java"]
     *
     * 가이드 생성 시점에 함께 채워지며,
     * 프론트엔드에서 파싱하여 파일 트리 형태로 표시합니다.
     */
    @Column(name = "file_paths", nullable = false, columnDefinition = "TEXT")
    var filePaths: String,

    /**
     * AI가 생성한 코드 수정 가이드라인입니다.
     *
     * 이슈의 핵심 원인, 수정 방향, 주의사항 등을 포함하는 텍스트로,
     * 사용자가 이슈를 선택하면 기본적으로 제공되는 첫 번째 응답입니다.
     *
     * 예: "findAll() 호출 시 연관 엔티티가 Lazy Loading으로 인해
     *      N+1 쿼리가 발생합니다. @EntityGraph를 적용하여 해결하세요."
     */
    @Column(name = "guideline", nullable = false, columnDefinition = "TEXT")
    var guideline: String,

    /**
     * AI가 생성한 구체적인 의사 코드(Pseudo-code)입니다.
     *
     * guideline보다 한 단계 더 구체적인 코드 수준의 수정 제안으로,
     * 최초 분석 시 guideline과 함께 한 번의 GLM 호출로 생성됩니다.
     * 프론트엔드에서는 사용자가 "코드도 보여줘" 버튼을 눌렀을 때
     * 별도 DTO(PseudoCodeResponseDto)로 제공됩니다.
     */
    @Column(name = "pseudo_code", nullable = false, columnDefinition = "TEXT")
    var pseudoCode: String,

    /**
     * AI가 분석한 수정 시 발생 가능한 부작용(Side-effect)입니다.
     *
     * 코드 변경이 다른 모듈이나 기능에 미칠 수 있는 영향을 기술합니다.
     * guideline과 함께 제공되어 사용자가 수정 범위를 판단하는 데 도움을 줍니다.
     *
     * 예: "기존 Lazy Loading에 의존하는 다른 호출부가 있는지 확인 필요"
     */
    @Column(name = "side_effects", nullable = false, columnDefinition = "TEXT")
    var sideEffects: String

) : BaseEntity()