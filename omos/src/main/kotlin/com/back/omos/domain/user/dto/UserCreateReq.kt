package com.back.omos.domain.user.dto

import com.back.omos.domain.user.entity.User

/**
 * 신규 사용자 생성을 위한 요청 DTO입니다.
 *
 * <p>
 * GitHub 로그인 성공 후 전달받은 정보를 기반으로 사용자를 등록할 때 사용됩니다.
 *
 * @property githubId GitHub 고유 ID (필수)
 * @property name 사용자 이름
 * @property email 사용자 이메일
 *
 * @author MintyU
 * @since 2026-04-22
 */
data class UserCreateReq(
    val githubId: String,
    val name: String?,
    val email: String?
) {
    /**
     * [UserCreateReq] DTO를 [User] 엔티티로 변환합니다.
     *
     * @return 생성된 User 엔티티
     */
    fun toEntity(): User {
        return User(
            githubId = githubId,
            name = name,
            email = email
        )
    }
}
