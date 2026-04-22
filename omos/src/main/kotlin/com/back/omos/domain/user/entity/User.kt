package com.back.omos.domain.user.entity

import com.back.omos.global.jpa.converter.DoubleArrayToVectorConverter
import com.back.omos.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 서비스의 핵심 사용자 정보를 관리하는 엔티티 클래스입니다.
 *
 * 이 클래스는 GitHub OAuth를 통해 가입한 사용자의 기본 정보와
 * 추천 시스템 및 유사도 검색을 위한 프로필 벡터 데이터를 저장합니다.
 *
 * [BaseEntity]를 상속받아 다음 필드들을 자동으로 관리합니다:
 * - `id`: 시스템 내부 식별자 (Long, PK)
 * - `createdAt`: 계정 생성 일시
 * - `updatedAt`: 정보 수정 일시
 *
 * @author MintyU
 * @since 2026-04-22
 * @see com.back.omos.global.jpa.entity.BaseEntity
 */
@Entity
@Table(name = "users")
class User(
    /**
     * GitHub에서 제공하는 사용자의 고유 식별값입니다.
     *
     * 중복될 수 없으며, 사용자를 식별하는 주요 외부 키로 사용됩니다.
     * nullable이 아니어야 하므로 가입 시 필수적으로 전달받아야 합니다.
     */
    @Column(name = "github_id", unique = true, nullable = false)
    var githubId: String,

    /**
     * 사용자의 실명 또는 닉네임입니다.
     *
     * GitHub 프로필에 설정된 이름이 기본값으로 사용되며, 설정되지 않은 경우 null일 수 있습니다.
     */
    @Column(name = "name", nullable = true)
    var name: String? = null,

    /**
     * 사용자의 이메일 주소입니다.
     *
     * GitHub 계정에 등록된 공개 이메일 정보를 저장합니다.
     */
    @Column(name = "email", nullable = true)
    var email: String? = null,

    /**
     * 사용자의 관심사나 활동을 수치화한 벡터 데이터입니다.
     *
     * PostgreSQL의 `pgvector` 확장을 사용하여 벡터 유사도 검색(Cosine Similarity 등)에 활용됩니다.
     *
     * - **매핑 타입**: [DoubleArray] (DB의 `vector` 타입과 호환)
     * - **차원 설정**: 현재 `1536`차원으로 설정되어 있으며, 이는 OpenAI `text-embedding-3-small` 등의 모델과 호환됩니다.
     * - **컨버터**: [DoubleArrayToVectorConverter]를 통해 DB 입출력 시 직렬화됩니다.
     */
    @Convert(converter = DoubleArrayToVectorConverter::class)
    @Column(name = "profile_vector", columnDefinition = "vector(1536)")
    var profileVector: DoubleArray? = null,

    /**
     * 프로필 벡터 데이터가 마지막으로 갱신된 일시입니다.
     *
     * [updateVector] 메서드가 호출될 때마다 자동으로 현재 시점으로 갱신됩니다.
     */
    @Column(name = "vector_updated_at")
    var vectorUpdatedAt: LocalDateTime? = null

) : BaseEntity() {

    /**
     * 사용자의 프로필 정보를 업데이트합니다.
     *
     * @param name 새 사용자 이름
     * @param email 새 이메일 주소
     * @return 업데이트된 User 객체 (메서드 체이닝 가능)
     */
    fun updateProfile(name: String?, email: String?): User {
        this.name = name
        this.email = email
        return this
    }

    /**
     * 사용자의 프로필 벡터를 갱신하고 갱신 시점을 기록합니다.
     *
     * @param newVector 갱신할 새로운 벡터 데이터
     */
    fun updateVector(newVector: DoubleArray?) {
        this.profileVector = newVector
        this.vectorUpdatedAt = LocalDateTime.now()
    }
}
