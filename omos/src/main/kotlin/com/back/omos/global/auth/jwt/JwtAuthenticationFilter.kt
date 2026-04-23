package com.back.omos.global.auth.jwt

import com.back.omos.global.auth.principal.OAuthPrincipal
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * HTTP 요청의 {@code Authorization} 헤더에서 JWT를 추출하여 인증을 처리하는 필터입니다.
 *
 * <p>
 * 요청당 한 번만 실행되도록 [OncePerRequestFilter]를 상속받으며,
 * [com.back.omos.global.config.SecurityConfig]에서 [org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter]
 * 앞에 등록됩니다.
 *
 * <p>
 * 처리 흐름:
 * <ol>
 *   <li>{@code Authorization: Bearer <token>} 헤더에서 토큰을 추출합니다.</li>
 *   <li>[JwtProvider.isValid]로 유효성을 검증합니다.</li>
 *   <li>유효한 경우 [JwtProvider.getGithubId]로 사용자 ID를 추출하고
 *       [OAuthPrincipal]을 생성하여 [SecurityContextHolder]에 등록합니다.</li>
 *   <li>유효하지 않은 경우 인증 정보를 등록하지 않고 다음 필터로 넘깁니다.</li>
 * </ol>
 *
 * <p><b>상속 정보:</b><br>
 * [OncePerRequestFilter]의 구현 클래스입니다.
 *
 * @property jwtProvider JWT 생성 및 검증을 담당하는 컴포넌트
 *
 * @author MintyU
 * @since 2026-04-23
 * @see JwtProvider
 * @see OAuthPrincipal
 * @see com.back.omos.global.config.SecurityConfig
 */
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider
) : OncePerRequestFilter() {

    /**
     * 요청 헤더에서 JWT를 추출하고 유효성을 검증하여 [SecurityContextHolder]에 인증 정보를 설정합니다.
     *
     * @param request 현재 HTTP 요청
     * @param response 현재 HTTP 응답
     * @param filterChain 다음 필터 체인
     */
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = extractToken(request)
        if (token != null && jwtProvider.isValid(token)) {
            val githubId = jwtProvider.getGithubId(token)
            val principal = OAuthPrincipal(githubId, emptyMap())
            val auth = UsernamePasswordAuthenticationToken(
                principal, null, listOf(SimpleGrantedAuthority("ROLE_USER"))
            )
            SecurityContextHolder.getContext().authentication = auth
        }
        filterChain.doFilter(request, response)
    }

    /**
     * HTTP 요청 헤더에서 Bearer 토큰을 추출합니다.
     *
     * <p>
     * {@code Authorization} 헤더가 없거나 {@code Bearer }로 시작하지 않는 경우 {@code null}을 반환합니다.
     *
     * @param request Bearer 토큰을 추출할 HTTP 요청
     * @return 추출된 JWT 문자열, 헤더가 없거나 형식이 올바르지 않으면 {@code null}
     */
    private fun extractToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        return if (header.startsWith("Bearer ")) header.substring(7) else null
    }
}
