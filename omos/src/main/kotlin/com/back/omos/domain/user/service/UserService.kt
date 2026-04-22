package com.back.omos.domain.user.service

import com.back.omos.domain.user.dto.UserCreateReq
import com.back.omos.domain.user.dto.UserInfoRes

/**
 * 사용자 정보 관리를 담당하는 서비스 인터페이스입니다.
 *
 * <p>
 * 사용자 가입, 조회, 정보 수정 등의 비즈니스 로직을 정의합니다.
 *
 * @author MintyU
 * @since 2026-04-22
 */
interface UserService {
    /**
     * 새로운 사용자를 생성(등록)합니다.
     *
     * @param request 신규 사용자 생성 정보를 담은 DTO
     * @return 생성된 사용자 정보 DTO
     */
    fun createUser(request: UserCreateReq): UserInfoRes

    /**
     * GitHub 고유 ID를 통해 사용자 정보를 조회합니다.
     *
     * @param githubId 조회할 GitHub 고유 ID
     * @return 조회된 사용자 정보 DTO
     */
    fun getUserByGithubId(githubId: String): UserInfoRes

    /**
     * 사용자의 이름과 이메일 정보를 업데이트합니다.
     *
     * @param githubId 업데이트할 사용자의 GitHub 고유 ID
     * @param name 변경할 이름 (null인 경우 변경 없음)
     * @param email 변경할 이메일 (null인 경우 변경 없음)
     * @return 업데이트된 사용자 정보 DTO
     */
    fun updateProfile(githubId: String, name: String?, email: String?): UserInfoRes
}
