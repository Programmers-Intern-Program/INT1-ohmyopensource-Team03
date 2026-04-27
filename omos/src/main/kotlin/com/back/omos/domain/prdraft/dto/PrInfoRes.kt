package com.back.omos.domain.prdraft.dto

/**
 * 생성된 PR 정보를 응답으로 전달하는 DTO입니다.
 *
 * <p>
 * AI를 통해 생성된 PR 제목과 본문, 그리고 GitHub PR 생성 페이지 URL을 포함합니다.
 * githubUrl을 통해 제목과 본문이 미리 채워진 GitHub PR 작성 창으로 바로 이동할 수 있습니다.
 *
 * @property title AI가 생성한 PR 제목
 * @property body AI가 생성한 PR 본문
 * @property githubUrl 제목과 본문이 pre-fill된 GitHub PR 생성 URL
 *
 * @author 5h6vm
 * @since 2026-04-22
 */
data class PrInfoRes(
    val title: String,
    val body: String,
    val githubUrl: String
)
