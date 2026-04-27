package com.back.omos.domain.prdraft.service

import com.back.omos.domain.prdraft.dto.CreatePrReq
import com.back.omos.domain.prdraft.dto.PrInfoRes

/**
 * PR 생성 기능을 제공하는 Service 인터페이스입니다.
 *
 * <p>
 * 사용자의 코드 변경 내용(diff)을 기반으로 PR 제목과 본문을 생성하는 기능을 정의합니다.
 *
 * <p><b>상속 정보:</b><br>
 * 별도의 상속 없이 Service 역할을 정의하는 인터페이스입니다.
 *
 * @author 5h6vm
 * @since 2026-04-22
 */
interface PrDraftService {
    fun create(githubId: String, request: CreatePrReq): PrInfoRes
}