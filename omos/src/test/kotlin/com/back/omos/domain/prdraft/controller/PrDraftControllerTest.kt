package com.back.omos.domain.prdraft.controller

import com.back.omos.domain.prdraft.dto.PrInfoRes
import com.back.omos.domain.prdraft.service.PrDraftService
import com.back.omos.global.auth.handler.OAuth2FailureHandler
import com.back.omos.global.auth.handler.OAuth2SuccessHandler
import com.back.omos.global.auth.jwt.JwtProvider
import com.back.omos.global.auth.principal.OAuthPrincipal
import com.back.omos.global.auth.service.CustomOAuth2UserService
import com.back.omos.global.config.SecurityConfig
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(PrDraftController::class)
@Import(SecurityConfig::class)
@ActiveProfiles("test")
class PrDraftControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc

    @MockitoBean private lateinit var prDraftService: PrDraftService
    @MockitoBean private lateinit var customOAuth2UserService: CustomOAuth2UserService
    @MockitoBean private lateinit var oauth2SuccessHandler: OAuth2SuccessHandler
    @MockitoBean private lateinit var oauth2FailureHandler: OAuth2FailureHandler
    @MockitoBean private lateinit var jwtProvider: JwtProvider

    private val validBody = """{"issueId": 1, "diffContent": "@@ -1 +1 @@\n-old\n+new"}"""

    private fun mockAuth() = UsernamePasswordAuthenticationToken(
        OAuthPrincipal("testUser", emptyMap()), null,
        OAuthPrincipal("testUser", emptyMap()).authorities
    )

    @Nested
    inner class SuccessTest {

        @Test
        fun `정상 요청이면 200과 success 응답을 반환한다`() {
            given(prDraftService.create(any(), any())).willReturn(
                PrInfoRes("feat: title", "body", "https://github.com/owner/repo/compare")
            )

            mockMvc.perform(
                post("/api/v1/pr")
                    .with(authentication(mockAuth()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBody)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.title").value("feat: title"))
        }
    }

    @Nested
    inner class ValidationTest {

        @Test
        fun `issueId가 0이면 400을 반환한다`() {
            mockMvc.perform(
                post("/api/v1/pr")
                    .with(authentication(mockAuth()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"issueId": 0, "diffContent": "@@ -1 +1 @@"}""")
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        fun `diffContent가 비어있으면 400을 반환한다`() {
            mockMvc.perform(
                post("/api/v1/pr")
                    .with(authentication(mockAuth()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"issueId": 1, "diffContent": ""}""")
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    inner class AuthTest {

        @Test
        fun `인증 없이 요청하면 401을 반환한다`() {
            mockMvc.perform(
                post("/api/v1/pr")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBody)
            )
                .andExpect(status().isUnauthorized)
        }
    }
}
