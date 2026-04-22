package com.back.omos.domain.user.dto

import com.back.omos.domain.user.entity.User
import java.time.LocalDateTime

/**
 * 사용자 정보를 응답으로 전달하는 DTO입니다.
 *
 * <p>
 * 사용자의 고유 식별자, GitHub ID, 이름, 이메일 등의 기본 정보를 포함합니다.
 *
 * @property id 시스템 내부 식별자
 * @property githubId GitHub 고유 ID
 * @property name 사용자 이름
 * @property email 사용자 이메일
 * @property vectorUpdatedAt 벡터 데이터 마지막 갱신 일시
 *
 * @author MintyU
 * @since 2026-04-22
 */
data class UserInfoRes(
    val id: Long?,
    val githubId: String,
    val name: String?,
    val email: String?,
    val vectorUpdatedAt: LocalDateTime?
) {
    companion object {
        /**
         * [User] 엔티티를 [UserInfoRes] DTO로 변환합니다.
         *
         * @param user 변환할 User 엔티티
         * @return 변환된 UserInfoRes DTO
         */
        fun from(user: User): UserInfoRes {
            return UserInfoRes(
                id = user.id,
                githubId = user.githubId,
                name = user.name,
                email = user.email,
                vectorUpdatedAt = user.vectorUpdatedAt
            )
        }
    }
}
