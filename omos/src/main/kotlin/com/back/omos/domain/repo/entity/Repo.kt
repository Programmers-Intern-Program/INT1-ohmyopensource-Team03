package com.back.omos.domain.repo.entity

import com.back.omos.domain.issue.entity.Issue
import com.back.omos.global.jpa.entity.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

/**
 * GitHub 저장소 정보를 관리하는 엔티티 클래스입니다.
 *
 * 이 클래스는 사용자가 추천받거나 분석 대상으로 조회하는
 * 오픈소스 저장소의 기본 정보와 메타데이터를 저장합니다.
 *
 * [BaseEntity]를 상속받아 다음 필드들을 자동으로 관리합니다:
 * - `id`: 시스템 내부 식별자 (Long, PK)
 * - `createdAt`: 엔티티 생성 일시
 * - `updatedAt`: 엔티티 수정 일시
 *
 * @author 5h6vm
 * @since 2026-04-23
 * @see com.back.omos.global.jpa.entity.BaseEntity
 */
@Entity
@Table(name = "repos")
class Repo(
    /**
     * GitHub 저장소의 전체 이름입니다.
     *
     * `owner/repository` 형식으로 저장되며,
     * 예를 들어 `spring-projects/spring-boot` 와 같은 값이 들어갑니다.
     *
     * 중복될 수 없으며, 외부 GitHub 저장소를 식별하는 주요 값으로 사용됩니다.
     */
    @Column(name = "full_name", unique = true, nullable = false)
    var fullName: String,

    /**
     * GitHub 저장소에 대한 설명입니다.
     *
     * 저장소의 개요나 목적을 나타내며,
     * GitHub API에서 조회한 description 값을 저장합니다.
     */
    @Column(name = "description")
    var description: String? = null,

    /**
     * 저장소의 사용 언어 정보입니다.
     *
     * JSON 문자열 형태로 저장되며,
     * 예를 들어 `{"Java": 80, "Kotlin": 20}` 같은 구조를 가질 수 있습니다.
     *
     * 추후 언어 기반 추천, 필터링, 분석에 활용할 수 있습니다.
     */
    @Column(name = "languages", nullable = false, columnDefinition = "TEXT")
    var languages: String = "{}",

    /**
     * GitHub 저장소의 스타 수입니다.
     *
     * 저장소의 인기도나 관심도를 나타내는 지표로 활용됩니다.
     */
    @Column(name = "stars", nullable = false)
    var stars: Int = 0,

    /**
     * GitHub 저장소의 URL입니다.
     *
     * 사용자가 실제 저장소 페이지로 이동하거나,
     * 외부 링크 연결 시 사용됩니다.
     */
    @Column(name = "url", nullable = false)
    var url: String,

    /**
     * 해당 저장소에 속한 이슈 목록입니다.
     *
     * 하나의 저장소는 여러 개의 이슈를 가질 수 있으며,
     * 이 관계를 통해 저장소별 이슈를 관리합니다.
     */
    @OneToMany(mappedBy = "repository", fetch = FetchType.LAZY)
    var issues: MutableList<Issue> = mutableListOf()

) : BaseEntity() {
}