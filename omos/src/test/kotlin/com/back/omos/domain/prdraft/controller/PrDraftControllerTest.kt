package com.back.omos.domain.prdraft.controller

import com.back.omos.domain.prdraft.dto.PrDetailRes
import com.back.omos.domain.prdraft.dto.PrHistoryRes
import com.back.omos.domain.prdraft.dto.PrInfoRes
import com.back.omos.domain.prdraft.dto.PrPageRes
import com.back.omos.domain.prdraft.dto.PrTranslateRes
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

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

    private val validBody = """{"upstreamRepo": "owner/repo", "githubIssueNumber": 1, "baseBranch": "main", "headBranch": "fix/issue-123"}"""

    private fun mockAuth() = UsernamePasswordAuthenticationToken(
        OAuthPrincipal("testUser", emptyMap()), null,
        OAuthPrincipal("testUser", emptyMap()).authorities
    )

    @Nested
    inner class SuccessTest {

        @Test
        fun `정상 요청이면 200과 success 응답을 반환한다`() {
            given(prDraftService.create(any(), any())).willReturn(
                PrInfoRes(1L, "feat: title", "body")
            )

            mockMvc.perform(
                post("/api/v1/pr")
                    .with(authentication(mockAuth()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBody)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.title").value("feat: title"))
        }
    }

    @Nested
    inner class GetOneTest {

        @Test
        fun `단건 조회 정상 요청이면 200과 상세 정보를 반환한다`() {
            given(prDraftService.getOne(any(), any())).willReturn(
                PrDetailRes(1L, "owner/repo", "test issue", "feat: title", "body", "diff content", LocalDateTime.now())
            )

            mockMvc.perform(
                get("/api/v1/pr/1")
                    .with(authentication(mockAuth()))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.repoFullName").value("owner/repo"))
                .andExpect(jsonPath("$.data.issueTitle").value("test issue"))
                .andExpect(jsonPath("$.data.title").value("feat: title"))
                .andExpect(jsonPath("$.data.diffContent").value("diff content"))
        }

        @Test
        fun `인증 없이 단건 조회하면 401을 반환한다`() {
            mockMvc.perform(get("/api/v1/pr/1"))
                .andExpect(status().isUnauthorized)
        }
    }

    @Nested
    inner class GetHistoryTest {

        @Test
        fun `목록 조회 정상 요청이면 200과 페이징 목록을 반환한다`() {
            given(prDraftService.getHistory(any(), any())).willReturn(
                PrPageRes(
                    content = listOf(PrHistoryRes(1L, "owner/repo", "test issue", "feat: title", LocalDateTime.now())),
                    totalElements = 1L,
                    totalPages = 1,
                    page = 0,
                    size = 10
                )
            )

            mockMvc.perform(
                get("/api/v1/pr/history")
                    .with(authentication(mockAuth()))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.content[0].title").value("feat: title"))
                .andExpect(jsonPath("$.data.content[0].repoFullName").value("owner/repo"))
                .andExpect(jsonPath("$.data.content[0].issueTitle").value("test issue"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
        }

        @Test
        fun `인증 없이 목록 조회하면 401을 반환한다`() {
            mockMvc.perform(get("/api/v1/pr/history"))
                .andExpect(status().isUnauthorized)
        }
    }

    @Nested
    inner class UpdateTest {

        @Test
        fun `수정 정상 요청이면 200과 수정된 데이터를 반환한다`() {
            given(prDraftService.update(any(), any(), any())).willReturn(
                PrDetailRes(1L, "owner/repo", "test issue", "updated title", "updated body", "diff content", LocalDateTime.now())
            )

            mockMvc.perform(
                patch("/api/v1/pr/1")
                    .with(authentication(mockAuth()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"title": "updated title", "body": "updated body"}""")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.title").value("updated title"))
                .andExpect(jsonPath("$.data.body").value("updated body"))
        }

        @Test
        fun `인증 없이 수정 요청하면 401을 반환한다`() {
            mockMvc.perform(
                patch("/api/v1/pr/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"title": "updated title", "body": "updated body"}""")
            )
                .andExpect(status().isUnauthorized)
        }
    }

    @Nested
    inner class TranslateTest {

        @Test
        fun `번역 정상 요청이면 200과 번역 결과를 반환한다`() {
            given(prDraftService.translate(any(), any())).willReturn(
                PrTranslateRes("fix: mock PR title", "## Changes\n- Generated PR draft.", "https://github.com/owner/repo/compare?quick_pull=1&title=fix%3A%20mock%20PR%20title&body=...")
            )

            mockMvc.perform(
                post("/api/v1/pr/1/translate")
                    .with(authentication(mockAuth()))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.titleEn").value("fix: mock PR title"))
                .andExpect(jsonPath("$.data.bodyEn").exists())
                .andExpect(jsonPath("$.data.githubUrl").exists())
        }

        @Test
        fun `인증 없이 번역 요청하면 401을 반환한다`() {
            mockMvc.perform(post("/api/v1/pr/1/translate"))
                .andExpect(status().isUnauthorized)
        }
    }

    @Nested
    inner class DeleteTest {

        @Test
        fun `삭제 정상 요청이면 200을 반환한다`() {
            mockMvc.perform(
                delete("/api/v1/pr/1")
                    .with(authentication(mockAuth()))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").doesNotExist())
        }

        @Test
        fun `인증 없이 삭제 요청하면 401을 반환한다`() {
            mockMvc.perform(delete("/api/v1/pr/1"))
                .andExpect(status().isUnauthorized)
        }
    }

    @Nested
    inner class ValidationTest {

        @Test
        fun `githubIssueNumber가 0이면 400을 반환한다`() {
            mockMvc.perform(
                post("/api/v1/pr")
                    .with(authentication(mockAuth()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"upstreamRepo": "owner/repo", "githubIssueNumber": 0, "baseBranch": "main", "headBranch": "fix/issue-123"}""")
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        fun `upstreamRepo가 비어있으면 400을 반환한다`() {
            mockMvc.perform(
                post("/api/v1/pr")
                    .with(authentication(mockAuth()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"upstreamRepo": "", "githubIssueNumber": 1, "baseBranch": "main", "headBranch": "fix/issue-123"}""")
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
