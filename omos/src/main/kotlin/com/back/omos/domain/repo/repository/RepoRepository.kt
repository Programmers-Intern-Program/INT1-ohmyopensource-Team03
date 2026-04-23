package com.back.omos.domain.repo.repository

import com.back.omos.domain.repo.entity.Repo
import org.springframework.data.jpa.repository.JpaRepository

/**
 * Repository 엔티티에 대한 데이터 접근을 담당하는 인터페이스입니다.
 *
 * <p>
 * Spring Data JPA를 기반으로 Repository 엔티티의 CRUD 및 조회 기능을 제공합니다.
 * </p>
 *
 * <p><b>상속 정보:</b><br>
 * {@link JpaRepository}를 상속받아 기본 CRUD 기능을 제공합니다.
 *
 * <p><b>주요 메서드:</b><br>
 * 함수 추가시 작성
 *
 * @author 5h6vm
 * @since 2026-04-23
 */
interface RepoRepository : JpaRepository<Repo, Long> {

}