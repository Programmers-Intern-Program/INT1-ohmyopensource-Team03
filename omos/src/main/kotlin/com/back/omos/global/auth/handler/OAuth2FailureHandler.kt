package com.back.omos.global.auth.handler

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component

/**
 * GitHub OAuth2 로그인 실패 시 프론트엔드로 에러를 전달하는 핸들러입니다.
 *
 * <p>
 * [com.back.omos.global.auth.service.CustomOAuth2UserService]에서 예외가 발생하거나,
 * GitHub 인증 자체가 실패한 경우 Spring Security에 의해 호출됩니다.
 *
 * <p>
 * 실패 시 설정된 프론트엔드 URI에 {@code ?error=oauth_failed} 쿼리 파라미터를 붙여 리다이렉트합니다.
 * 프론트엔드는 이 파라미터를 감지하여 사용자에게 로그인 실패 안내를 표시해야 합니다.
 *
 * @property redirectUri OAuth2 로그인 실패 후 이동할 프론트엔드 URI
 *
 * @author MintyU
 * @since 2026-04-23
 * @see OAuth2SuccessHandler
 */
@Component
class OAuth2FailureHandler(
    @Value("\${app.oauth2.redirect-uri}") private val redirectUri: String
) : AuthenticationFailureHandler {

    /**
     * 로그인 실패 시 에러 파라미터를 포함하여 프론트엔드 URI로 리다이렉트합니다.
     *
     * @param request 현재 HTTP 요청
     * @param response 현재 HTTP 응답
     * @param exception 인증 실패 원인 예외
     */
    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ) {
        response.sendRedirect("$redirectUri?error=oauth_failed")
    }
}
