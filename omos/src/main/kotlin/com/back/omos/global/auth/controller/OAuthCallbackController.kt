package com.back.omos.global.auth.controller

import com.plog.global.response.CommonResponse
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 개발 환경에서 OAuth2 로그인 흐름을 프론트엔드 없이 테스트하기 위한 임시 콜백 컨트롤러입니다.
 *
 * <p>
 * {@code dev} 프로파일에서만 활성화되며, [OAuth2SuccessHandler]가 리다이렉트하는
 * {@code /oauth/callback} 경로를 처리하여 발급된 JWT를 응답 본문에 그대로 반환합니다.
 *
 * <p>
 * 사용 방법:
 * <ol>
 *   <li>브라우저에서 {@code GET /oauth2/authorization/github}로 접근합니다.</li>
 *   <li>GitHub 로그인을 완료합니다.</li>
 *   <li>이 엔드포인트가 JWT를 JSON으로 반환합니다.</li>
 *   <li>반환된 토큰을 Swagger UI 또는 curl의 {@code Authorization: Bearer} 헤더에 사용합니다.</li>
 * </ol>
 *
 * <p><b>주의:</b> 이 컨트롤러는 {@code dev} 프로파일에서만 동작합니다. 운영 환경에서는 비활성화됩니다.
 *
 * @author MintyU
 * @since 2026-04-23
 * @see com.back.omos.global.auth.handler.OAuth2SuccessHandler
 */
@RestController
@Profile("dev")
class OAuthCallbackController {

    /**
     * OAuth2 로그인 성공 후 리다이렉트된 요청에서 JWT를 추출하여 반환합니다.
     *
     * @param token [OAuth2SuccessHandler]가 쿼리 파라미터로 전달한 JWT 문자열
     * @return 발급된 JWT를 담은 성공 응답
     */
    @GetMapping("/oauth/callback")
    fun callback(@RequestParam token: String): CommonResponse<Map<String, String>> {
        return CommonResponse.success(mapOf("token" to token), "로그인 성공. 아래 토큰을 Authorization 헤더에 사용하세요.")
    }
}
