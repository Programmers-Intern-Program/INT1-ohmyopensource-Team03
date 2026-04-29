package com.back.omos.domain.prdraft.dto

import org.springframework.data.domain.Page

/**
 * PR 초안 목록 페이징 응답 DTO입니다.
 *
 * @property content 현재 페이지 데이터 목록
 * @property totalElements 전체 데이터 수
 * @property totalPages 전체 페이지 수
 * @property page 현재 페이지 번호 (0부터 시작)
 * @property size 페이지당 데이터 수
 *
 * @author 5h6vm
 * @since 2026-04-29
 */
data class PrPageRes<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int
) {
    companion object {
        fun <T> from(page: Page<T>) = PrPageRes(
            content = page.content,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            page = page.number,
            size = page.size
        )
    }
}
