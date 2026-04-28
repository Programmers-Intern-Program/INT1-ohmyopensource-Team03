package com.back.omos.domain.prdraft.dto

/**
 * PR 초안 번역 결과를 응답으로 전달하는 DTO입니다.
 *
 * <p>
 * 영어로 번역된 PR 제목과 본문, 그리고 해당 내용이 pre-fill된 GitHub PR 작성 창 URL을 포함합니다.
 *
 * @property titleEn 영어로 번역된 PR 제목
 * @property bodyEn 영어로 번역된 PR 본문
 * @property githubUrl 번역된 제목과 본문이 pre-fill된 GitHub PR 생성 URL
 *
 * @author 5h6vm
 * @since 2026-04-28
 */
data class PrTranslateRes(
    val titleEn: String,
    val bodyEn: String,
    val githubUrl: String
)
