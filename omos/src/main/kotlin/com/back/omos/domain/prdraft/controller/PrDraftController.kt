package com.back.omos.domain.prdraft.controller

import com.back.omos.domain.prdraft.dto.CreatePrReq
import com.back.omos.domain.prdraft.dto.PrInfoRes
import com.back.omos.domain.prdraft.service.PrDraftService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * PR 생성 요청을 처리하는 Controller입니다.
 *
 * <p>
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
    fun create(@RequestBody req: CreatePrReq): PrInfoRes {
        return prDraftService.create(req)
    }

}
