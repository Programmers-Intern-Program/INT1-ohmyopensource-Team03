package com.back.omos.global.auth.service

import com.back.omos.domain.user.entity.User
import com.back.omos.domain.user.repository.UserRepository
import com.back.omos.global.auth.principal.OAuthPrincipal
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * GitHub OAuth2 인증 과정에서 사용자 정보를 로드하고 DB와 동기화하는 서비스입니다.
 *
 * <p>
 * Spring Security OAuth2 Client가 GitHub로부터 액세스 토큰을 발급받은 후,
 * GitHub User API({@code /user})를 호출하여 사용자 정보를 가져오는 과정에서 이 클래스가 사용됩니다.
 *
 * <p>
 * 처리 흐름:
 * <ol>
 *   <li>부모 클래스 [DefaultOAuth2UserService.loadUser]를 통해 GitHub로부터 사용자 정보를 수신합니다.</li>
 *   <li>응답 속성({@code id}, {@code login}, {@code email})에서 필요한 정보를 추출합니다.</li>
 *   <li>DB에 해당 {@code githubId}를 가진 사용자가 없으면 신규 생성하고, 있으면 프로필을 최신 상태로 업데이트합니다.</li>
 *   <li>[OAuthPrincipal]을 반환하여 이후 [com.back.omos.global.auth.handler.OAuth2SuccessHandler]에서 JWT 발급에 활용됩니다.</li>
 * </ol>
 *
 * <p><b>상속 정보:</b><br>
 * [DefaultOAuth2UserService]의 구현 클래스입니다.
 *
 * @property userRepository 사용자 데이터 액세스를 위한 Repository
 *
 * @author MintyU
 * @since 2026-04-23
 * @see OAuthPrincipal
 * @see com.back.omos.global.auth.handler.OAuth2SuccessHandler
 */
@Service
class CustomOAuth2UserService(
    private val userRepository: UserRepository
) : DefaultOAuth2UserService() {

    /**
     * GitHub OAuth2 인증 완료 후 사용자 정보를 로드하고 DB와 동기화합니다.
     *
     * <p>
     * GitHub 응답 속성 중 {@code id}(숫자 형태의 고유 ID), {@code login}(GitHub 사용자명),
     * {@code email}(공개 이메일)을 사용합니다. {@code id}가 없는 경우 [OAuth2AuthenticationException]을 발생시킵니다.
     *
     * @param userRequest OAuth2 클라이언트 등록 정보와 액세스 토큰을 포함한 요청 객체
     * @return GitHub ID와 속성 정보를 담은 [OAuthPrincipal]
     * @throws OAuth2AuthenticationException GitHub 응답에서 사용자 ID를 가져오지 못한 경우
     */
    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val attributes = oAuth2User.attributes

        val githubId = attributes["id"]?.toString()
            ?: throw OAuth2AuthenticationException(
                OAuth2Error("invalid_user_info", "GitHub ID를 가져올 수 없습니다.", null)
            )
        val name = attributes["login"] as? String
        val email = attributes["email"] as? String

        val user = userRepository.findByGithubId(githubId).orElse(null)
            ?: userRepository.save(User(githubId = githubId, name = name, email = email))

        if (user.name != name || user.email != email) {
            user.updateProfile(name, email)
        }

        return OAuthPrincipal(githubId, attributes)
    }
}
