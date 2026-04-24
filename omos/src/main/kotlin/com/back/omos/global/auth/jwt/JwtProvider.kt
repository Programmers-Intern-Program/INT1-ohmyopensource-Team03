package com.back.omos.global.auth.jwt

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

/**
 * JWT(JSON Web Token)의 생성, 파싱, 유효성 검증을 담당하는 컴포넌트입니다.
 *
 * <p>
 * JJWT 0.12.x 라이브러리를 사용하며, HMAC-SHA 알고리즘으로 서명합니다.
 * 서명 키와 만료 시간은 애플리케이션 설정 파일의 {@code jwt.secret}, {@code jwt.expiration}
 * 프로퍼티에서 주입받습니다.
 *
 * <p>
 * 발급된 토큰은 사용자의 {@code githubId}를 subject 클레임에 담으며,
 * [JwtAuthenticationFilter]에서 요청마다 검증됩니다.
 *
 * @property expiration 토큰 만료 시간 (밀리초 단위, 기본값 86400000ms = 24시간)
 *
 * @author MintyU
 * @since 2026-04-23
 * @see JwtAuthenticationFilter
 * @see com.back.omos.global.auth.handler.OAuth2SuccessHandler
 */
@Component
class JwtProvider(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.expiration}") private val expiration: Long
) {

    /**
     * 설정에서 주입받은 시크릿 문자열로 초기화된 HMAC-SHA 서명 키입니다.
     *
     * <p>
     * 키 길이는 최소 32바이트(256비트) 이상이어야 하며, {@code JWT_SECRET} 환경변수로 관리합니다.
     */
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8))

    /**
     * 주어진 GitHub ID를 subject로 담은 JWT를 생성합니다.
     *
     * <p>
     * 발급 시각은 현재 시각이며, 만료 시각은 현재 시각에 {@link #expiration}을 더한 값입니다.
     *
     * @param githubId JWT subject에 담을 GitHub 사용자 고유 ID
     * @return 서명된 JWT 문자열
     */
    fun generateToken(githubId: String): String =
        Jwts.builder()
            .subject(githubId)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expiration))
            .signWith(key)
            .compact()

    /**
     * JWT에서 GitHub ID(subject 클레임)를 추출합니다.
     *
     * <p>
     * 서명 검증에 실패하거나 토큰이 만료된 경우 JJWT 예외가 발생합니다.
     * 외부에서 호출 전 [isValid]로 유효성을 먼저 확인하는 것을 권장합니다.
     *
     * @param token 파싱할 JWT 문자열
     * @return 토큰의 subject에 저장된 GitHub 사용자 고유 ID
     */
    fun getGithubId(token: String): String =
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
            .subject

    /**
     * JWT의 서명과 만료 여부를 검증합니다.
     *
     * <p>
     * 서명 불일치, 만료, 형식 오류 등 JJWT 예외가 발생하는 모든 경우에 {@code false}를 반환하며,
     * 예외를 외부로 전파하지 않습니다.
     *
     * @param token 검증할 JWT 문자열
     * @return 유효한 토큰이면 {@code true}, 그렇지 않으면 {@code false}
     */
    fun isValid(token: String): Boolean = runCatching {
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
        true
    }.getOrDefault(false)
}
