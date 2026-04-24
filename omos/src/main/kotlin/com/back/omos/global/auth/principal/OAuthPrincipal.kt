package com.back.omos.global.auth.principal

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User

/**
 * GitHub OAuth2 인증을 통해 로그인한 사용자의 인증 주체(Principal)를 나타내는 클래스입니다.
 *
 * <p>
 * Spring Security의 [OAuth2User]를 구현하여 [org.springframework.security.core.context.SecurityContext]에
 * 등록되며, [org.springframework.security.core.annotation.AuthenticationPrincipal] 어노테이션을 통해
 * 컨트롤러 메서드 파라미터로 주입받을 수 있습니다.
 *
 * <p>
 * 이 클래스는 GitHub OAuth2 인증 완료 후 [com.back.omos.global.auth.service.CustomOAuth2UserService]에서
 * 생성되며, JWT 재발급 시에는 [githubId]만 담긴 빈 attributes로도 생성됩니다.
 *
 * <p><b>상속 정보:</b><br>
 * [OAuth2User]의 구현 클래스입니다.
 *
 * @property githubId GitHub에서 제공하는 사용자의 고유 숫자 ID (문자열로 저장)
 * @property attributes GitHub OAuth2 응답에서 받은 사용자 속성 맵 (login, email, avatar_url 등 포함)
 *
 * @author MintyU
 * @since 2026-04-23
 * @see com.back.omos.global.auth.service.CustomOAuth2UserService
 * @see com.back.omos.global.auth.jwt.JwtAuthenticationFilter
 */
class OAuthPrincipal(
    val githubId: String,
    private val attributes: Map<String, Any>
) : OAuth2User {

    /**
     * Spring Security가 이 Principal을 식별하는 데 사용하는 이름을 반환합니다.
     *
     * @return GitHub 사용자 고유 ID ([githubId])
     */
    override fun getName(): String = githubId

    /**
     * GitHub OAuth2 응답에서 받은 사용자 속성 맵을 반환합니다.
     *
     * <p>
     * JWT 인증 경로에서 생성된 경우 빈 맵이 반환될 수 있습니다.
     *
     * @return 사용자 속성 맵 (login, name, email, avatar_url 등)
     */
    override fun getAttributes(): Map<String, Any> = attributes

    /**
     * 이 사용자에게 부여된 권한 목록을 반환합니다.
     *
     * <p>
     * 현재는 모든 인증된 사용자에게 `ROLE_USER` 권한을 단일하게 부여합니다.
     *
     * @return [SimpleGrantedAuthority]("ROLE_USER")를 포함한 권한 목록
     */
    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_USER"))
}
