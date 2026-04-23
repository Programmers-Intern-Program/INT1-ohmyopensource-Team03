package com.back.omos.global.auth.handler

import com.back.omos.global.auth.jwt.JwtProvider
import com.back.omos.global.auth.principal.OAuthPrincipal
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

/**
 * GitHub OAuth2 로그인 성공 시 JWT를 발급하고 프론트엔드로 리다이렉트하는 핸들러입니다.
 *
 * <p>
 * [com.back.omos.global.auth.service.CustomOAuth2UserService]가 사용자 정보를 정상적으로
 * 로드한 뒤 Spring Security에 의해 호출됩니다.
 *
 * <p>
 * 처리 흐름:
 * <ol>
 *   <li>[Authentication]에서 [OAuthPrincipal]을 꺼내 {@code githubId}를 추출합니다.</li>
 *   <li>[JwtProvider.generateToken]으로 JWT를 발급합니다.</li>
 *   <li>설정된 프론트엔드 URI에 {@code ?token=<JWT>} 쿼리 파라미터를 붙여 리다이렉트합니다.</li>
 * </ol>
 *
 * <p>
 * 리다이렉트 대상 URI는 {@code app.oauth2.redirect-uri} 프로퍼티 또는
 * {@code OAUTH2_REDIRECT_URI} 환경변수로 설정합니다.
 *
 * @property jwtProvider JWT 생성 및 검증을 담당하는 컴포넌트
 * @property redirectUri OAuth2 로그인 성공 후 이동할 프론트엔드 URI
 *
 * @author MintyU
 * @since 2026-04-23
 * @see JwtProvider
 * @see OAuthPrincipal
 * @see OAuth2FailureHandler
 */
@Component
class OAuth2SuccessHandler(
    private val jwtProvider: JwtProvider,
    @Value("\${app.oauth2.redirect-uri}") private val redirectUri: String
) : AuthenticationSuccessHandler {

    /**
     * 로그인 성공 시 JWT를 발급하고 프론트엔드 URI로 리다이렉트합니다.
     *
     * @param request 현재 HTTP 요청
     * @param response 현재 HTTP 응답
     * @param authentication 인증된 사용자 정보 ([OAuthPrincipal]을 포함)
     */
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val principal = authentication.principal as OAuthPrincipal
        val token = jwtProvider.generateToken(principal.githubId)
        response.sendRedirect("$redirectUri?token=$token")
    }
}
