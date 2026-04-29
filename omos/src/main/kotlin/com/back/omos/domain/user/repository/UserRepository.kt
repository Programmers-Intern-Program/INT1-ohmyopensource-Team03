package com.back.omos.domain.user.repository

import com.back.omos.domain.user.entity.User
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

/**
 * [User] 엔티티에 대한 데이터 액세스 계층을 담당하는 Repository 인터페이스입니다.
 *
 * Spring 데이터 JPA의 [JpaRepository]를 상속받아 기본적인 CRUD 기능을 제공하며,
 * GitHub 고유 ID를 통한 사용자 조회 등의 추가 기능을 정의합니다.
 *
 * @author MintyU
 * @since 2026-04-22
 * @see com.back.omos.domain.user.entity.User
 */
@Repository
interface UserRepository : JpaRepository<User, Long> {

    /**
     * GitHub 고유 ID([User.githubId])를 사용하여 사용자를 조회합니다.
     *
     * @param githubId 조회할 GitHub 고유 ID
     * @return 해당 ID를 가진 사용자를 포함한 [Optional] 객체
     */
    fun findByGithubId(githubId: String): Optional<User>

    /**
     * 해당 GitHub 고유 ID를 가진 사용자가 존재하는지 확인합니다.
     *
     * @param githubId 존재 여부를 확인할 GitHub 고유 ID
     * @return 존재하면 true, 그렇지 않으면 false
     */
    fun existsByGithubId(githubId: String): Boolean

    /**
     * 사용자 이메일을 통해 사용자를 조회합니다.
     *
     * @param email 조회할 사용자 이메일
     * @return 해당 이메일을 가진 사용자를 포함한 [Optional] 객체
     */
    fun findByEmail(email: String): Optional<User>

    /**
     * GitHub 고유 ID로 사용자를 조회하며 배타적 락(X-lock)을 획득합니다.
     *
     * 일일 분석 요청 횟수 제한의 TOCTOU 경쟁 조건을 방지하기 위해 사용됩니다.
     * 락은 트랜잭션이 커밋되거나 롤백될 때까지 유지되므로,
     * 동일 사용자의 동시 요청이 count 조회 → 저장을 직렬화하여 초과 요청을 방지합니다.
     *
     * @param githubId 조회할 GitHub 고유 ID
     * @return 해당 ID를 가진 사용자를 포함한 [Optional] 객체
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.githubId = :githubId")
    fun findByGithubIdWithLock(githubId: String): Optional<User>
}
