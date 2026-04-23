package com.back.omos.global.config

import com.back.omos.global.auth.handler.OAuth2FailureHandler
import com.back.omos.global.auth.handler.OAuth2SuccessHandler
import com.back.omos.global.auth.jwt.JwtAuthenticationFilter
import com.back.omos.global.auth.jwt.JwtProvider
import com.back.omos.global.auth.service.CustomOAuth2UserService
import com.plog.global.exception.errorCode.AuthErrorCode
import com.plog.global.response.CommonResponse
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import tools.jackson.databind.ObjectMapper

/**
 * 프로젝트의 보안 설정을 담당하는 클래스입니다.
 *
 * <p>
 * GitHub OAuth2 로그인과 JWT 기반의 Stateless 인증을 함께 구성합니다.
 * OAuth2 인증 흐름은 세션을 통해 상태(state 파라미터)를 유지하며,
 * 로그인 성공 후 발급된 JWT를 클라이언트가 이후 요청의 {@code Authorization} 헤더에 포함합니다.
 *
 * <p>
 * 주요 설정:
 * <ul>
 *   <li>CSRF: REST API이므로 비활성화</li>
 *   <li>OAuth2 로그인: [CustomOAuth2UserService], [OAuth2SuccessHandler], [OAuth2FailureHandler] 연동</li>
 *   <li>JWT 필터: [JwtAuthenticationFilter]를 [org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter] 앞에 등록</li>
 *   <li>인증 실패 응답: 표준 [CommonResponse] 형식의 JSON 반환</li>
 * </ul>
 *
 * <p>
 * 인증 없이 접근 가능한 경로: {@code /oauth2/~}, {@code /login/~}, {@code /swagger-ui/~}, {@code /v3/api-docs/~}
 *
 * @property customOAuth2UserService GitHub 사용자 정보 로드 및 DB 동기화 서비스
 * @property oauth2SuccessHandler OAuth2 로그인 성공 시 JWT 발급 핸들러
 * @property oauth2FailureHandler OAuth2 로그인 실패 시 에러 리다이렉트 핸들러
 * @property jwtProvider JWT 생성 및 검증 컴포넌트
 * @property objectMapper JSON 직렬화를 위한 ObjectMapper
 *
 * @author MintyU
 * @since 2026-04-23
 * @see CustomOAuth2UserService
 * @see OAuth2SuccessHandler
 * @see OAuth2FailureHandler
 * @see JwtAuthenticationFilter
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val oauth2SuccessHandler: OAuth2SuccessHandler,
    private val oauth2FailureHandler: OAuth2FailureHandler,
    private val jwtProvider: JwtProvider,
    private val objectMapper: ObjectMapper
) {

    /**
     * Spring Security 필터 체인을 구성하여 빈으로 등록합니다.
     *
     * @param http [HttpSecurity] 빌더
     * @return 구성이 완료된 [SecurityFilterChain]
     */
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/oauth2/**",
                        "/login/**",
                        "/oauth/callback",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2
                    .userInfoEndpoint { it.userService(customOAuth2UserService) }
                    .successHandler(oauth2SuccessHandler)
                    .failureHandler(oauth2FailureHandler)
            }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint { _, response, _ ->
                    response.status = HttpServletResponse.SC_UNAUTHORIZED
                    response.contentType = "application/json;charset=UTF-8"
                    val body = CommonResponse.fail<Any>(AuthErrorCode.LOGIN_REQUIRED.message)
                    response.writer.write(objectMapper.writeValueAsString(body))
                }
            }
            .addFilterBefore(
                JwtAuthenticationFilter(jwtProvider),
                UsernamePasswordAuthenticationFilter::class.java
            )

        return http.build()
    }
}
