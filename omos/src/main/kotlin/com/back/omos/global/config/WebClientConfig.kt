package com.back.omos.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

/**
 * WebClient 설정을 담당하는 클래스입니다.
 *  @author 유재원
 *  @since 2026-04-24
 *  @see
 */
@Configuration
class WebClientConfig {

    /**
     * 공통으로 사용할 WebClient 빈을 등록합니다.
     */
    @Bean
    fun webClient(): WebClient {
        return WebClient.builder()
            .baseUrl("https://api.github.com") // 기본 주소 설정
            .defaultHeader("Accept", "application/vnd.github.v3+json") // 깃허브 권장 헤더
            .build()
    }
}