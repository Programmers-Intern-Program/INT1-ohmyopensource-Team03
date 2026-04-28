package com.back.omos.domain.prdraft.controller

import com.back.omos.domain.prdraft.dto.CreatePrReq
import com.back.omos.domain.prdraft.dto.PrDetailRes
import com.back.omos.domain.prdraft.dto.PrHistoryRes
import com.back.omos.domain.prdraft.dto.PrInfoRes
import com.back.omos.domain.prdraft.dto.PrTranslateRes
import com.back.omos.domain.prdraft.dto.UpdatePrReq
import com.back.omos.domain.prdraft.service.PrDraftService
import com.back.omos.global.auth.principal.OAuthPrincipal
import com.back.omos.global.response.CommonResponse
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * PR 초안 생성, 조회, 수정, 번역, 삭제 요청을 처리하는 Controller입니다.
 *
 * <p>
 * diff 내용과 이슈 정보를 받아 AI 기반 PR 초안을 생성하고,
 * 생성된 초안의 단건/목록 조회, 수정, 영어 번역 및 삭제 기능을 제공합니다.
 * </p>
 *
 * <p><b>상속 정보:</b><br>
 * 별도의 상속 없이 REST API 엔드포인트를 제공하는 Controller입니다.
 *
 * <p><b>빈 관리:</b><br>
 * Spring의 REST Controller 빈으로 등록되어 HTTP 요청을 처리합니다.
 *
 * @author 5h6vm
 * @since 2026-04-22
 * @see PrDraftService
 */
@RestController
@RequestMapping("/api/v1/pr")
class PrDraftController(
    private val prDraftService: PrDraftService
) {
    @PostMapping
    fun create(
        @AuthenticationPrincipal principal: OAuthPrincipal,
        @Valid @RequestBody req: CreatePrReq
    ): CommonResponse<PrInfoRes> {
        return CommonResponse.success(prDraftService.create(principal.githubId, req))
    }

    @GetMapping("/{id}")
    fun getOne(
        @AuthenticationPrincipal principal: OAuthPrincipal,
        @PathVariable id: Long
    ): CommonResponse<PrDetailRes> {
        return CommonResponse.success(prDraftService.getOne(principal.githubId, id))
    }

    @GetMapping("/history")
    fun getHistory(
        @AuthenticationPrincipal principal: OAuthPrincipal
    ): CommonResponse<List<PrHistoryRes>> {
        return CommonResponse.success(prDraftService.getHistory(principal.githubId))
    }

    @PatchMapping("/{id}")
    fun update(
        @AuthenticationPrincipal principal: OAuthPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody req: UpdatePrReq
    ): CommonResponse<PrDetailRes> {
        return CommonResponse.success(prDraftService.update(principal.githubId, id, req))
    }

    @PostMapping("/{id}/translate")
    fun translate(
        @AuthenticationPrincipal principal: OAuthPrincipal,
        @PathVariable id: Long
    ): CommonResponse<PrTranslateRes> {
        return CommonResponse.success(prDraftService.translate(principal.githubId, id))
    }

    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal principal: OAuthPrincipal,
        @PathVariable id: Long
    ): CommonResponse<Void?> {
        prDraftService.delete(principal.githubId, id)
        return CommonResponse.success(null)
    }

}
