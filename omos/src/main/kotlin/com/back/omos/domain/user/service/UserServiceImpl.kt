package com.back.omos.domain.user.service

import com.back.omos.domain.user.dto.UserCreateReq
import com.back.omos.domain.user.dto.UserInfoRes
import com.back.omos.domain.user.repository.UserRepository
import com.plog.global.exception.errorCode.AuthErrorCode
import com.plog.global.exception.exceptions.AuthException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * [UserService]의 구현체로, 사용자의 비즈니스 로직을 처리합니다.
 *
 * <p>
 * [UserRepository]를 주입받아 데이터베이스에 접근하며,
 * 사용자가 존재하지 않거나 생성 실패 시 [AuthException]을 발생시킵니다.
 *
 * @property userRepository 사용자 데이터 액세스를 위한 Repository
 *
 * @author MintyU
 * @since 2026-04-22
 * @see UserService
 */
@Service
@Transactional(readOnly = true)
class UserServiceImpl(
    private val userRepository: UserRepository
) : UserService {

    @Transactional
    override fun createUser(request: UserCreateReq): UserInfoRes {
        if (userRepository.existsByGithubId(request.githubId)) {
            throw AuthException(AuthErrorCode.USER_ALREADY_EXIST)
        }
        val user = userRepository.save(request.toEntity())
        return UserInfoRes.from(user)
    }

    override fun getUserByGithubId(githubId: String): UserInfoRes {
        val user = userRepository.findByGithubId(githubId)
            .orElseThrow { AuthException(AuthErrorCode.USER_NOT_FOUND) }
        return UserInfoRes.from(user)
    }

    @Transactional
    override fun updateProfile(githubId: String, name: String?, email: String?): UserInfoRes {
        val user = userRepository.findByGithubId(githubId)
            .orElseThrow { AuthException(AuthErrorCode.USER_NOT_FOUND) }
        
        user.updateProfile(name, email)
        return UserInfoRes.from(user)
    }
}
