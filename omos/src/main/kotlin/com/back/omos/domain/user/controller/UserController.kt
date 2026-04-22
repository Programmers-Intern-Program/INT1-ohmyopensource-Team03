package com.back.omos.domain.user.controller

import com.back.omos.domain.user.dto.UserCreateReq
import com.back.omos.domain.user.dto.UserInfoRes
import com.back.omos.domain.user.service.UserService
import com.plog.global.response.CommonResponse
import org.springframework.web.bind.annotation.*

/**
 * 사용자 정보와 관련된 HTTP 요청을 처리하는 컨트롤러입니다.
 *
 * <p>
 * 본 컨트롤러는 `/api/v1/users` 경로를 베이스로 하며,
 * 사용자 가입, 정보 조회, 프로필 수정 등의 API를 제공합니다.
 *
 * @property userService 사용자 비즈니스 로직을 처리하는 서비스
 *
 * @author MintyU
 * @since 2026-04-22
 */
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService
) {

    /**
     * 새로운 사용자를 등록(가입)합니다.
     *
     * <p>
     * GitHub 로그인 성공 후 전달받은 고유 ID와 정보를 기반으로 계정을 생성합니다.
     *
     * @param request 신규 사용자 생성 요청 데이터
     * @return 생성된 사용자 정보와 함께 성공 응답 반환
     */
    @PostMapping("/")
    fun createUser(@RequestBody request: UserCreateReq): CommonResponse<UserInfoRes> {
        val userInfo = userService.createUser(request)
        return CommonResponse.success(userInfo, "사용자 등록에 성공하였습니다.")
    }

    /**
     * 현재 로그인한 사용자의 정보를 조회합니다.
     *
     * <p>
     * 향후 SecurityContext의 인증 정보([org.springframework.security.core.annotation.AuthenticationPrincipal])를 활용하여
     * 현재 접속 중인 사용자의 상세 정보를 반환합니다.
     *
     * @return 현재 로그인한 사용자 정보와 함께 성공 응답 반환
     */
    @GetMapping("/me")
    fun getMyInfo(): CommonResponse<UserInfoRes> {
        // TODO: SecurityContext에서 현재 사용자의 githubId를 가져오는 로직 추가 필요
        // 예: val githubId = authentication.name
        val tempGithubId = "temp-id" 
        val userInfo = userService.getUserByGithubId(tempGithubId)
        return CommonResponse.success(userInfo)
    }

    /**
     * 특정 GitHub ID를 가진 사용자의 정보를 조회합니다.
     *
     * @param githubId 조회할 대상의 GitHub 고유 ID
     * @return 해당 사용자 정보와 함께 성공 응답 반환
     */
    @GetMapping("/{githubId}")
    fun getUserInfo(@PathVariable githubId: String): CommonResponse<UserInfoRes> {
        val userInfo = userService.getUserByGithubId(githubId)
        return CommonResponse.success(userInfo)
    }

    /**
     * 현재 로그인한 사용자의 프로필 정보를 수정합니다.
     *
     * <p>
     * 이름과 이메일 정보를 변경할 수 있습니다.
     *
     * @param request 수정할 필드 정보를 담은 맵 또는 DTO (여기서는 간단히 파라미터로 처리)
     * @return 수정된 사용자 정보와 함께 성공 응답 반환
     */
    @PatchMapping("/me")
    fun updateMyProfile(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) email: String?
    ): CommonResponse<UserInfoRes> {
        // TODO: SecurityContext에서 현재 사용자의 githubId를 가져오는 로직 추가 필요
        val tempGithubId = "temp-id"
        val updatedInfo = userService.updateProfile(tempGithubId, name, email)
        return CommonResponse.success(updatedInfo, "프로필이 성공적으로 수정되었습니다.")
    }
}
