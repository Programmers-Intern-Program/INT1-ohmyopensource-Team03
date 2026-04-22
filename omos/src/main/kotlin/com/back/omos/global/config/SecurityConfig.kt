package com.back.omos.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 * 프로젝트의 보안 설정을 담당하는 클래스입니다.
 * 
 * <p>
 * 개발 초기 단계에서는 테스트 편의를 위해 모든 API 경로([/api/~])에 대한
 * 인증을 해제하고 CSRF 보호를 비활성화합니다.
 * 
 * @author MintyU
 * @since 2026-04-22
 **/
@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() } // REST API이므로 CSRF 비활성화
            .headers { headers -> 
                headers.frameOptions { it.disable() } // H2 Console 등을 사용할 경우 대비
            }
            .authorizeHttpRequests { auth ->
                auth.anyRequest().permitAll() // 모든 요청 임시 허용
            }
        
        return http.build()
    }
}
