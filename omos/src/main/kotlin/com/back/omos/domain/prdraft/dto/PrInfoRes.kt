package com.back.omos.domain.prdraft.dto

/**
 * 생성된 PR 정보를 응답으로 전달하는 DTO입니다.
 *
 * <p>
 * AI를 통해 생성된 PR 제목과 본문을 포함합니다.
 *
 * @property title AI가 생성한 PR 제목
 * @property body AI가 생성한 PR 본문
 *
 * @author 5h6vm
 * @since 2026-04-22
 */
data class PrInfoRes(
    val title: String,
    val body: String
)
