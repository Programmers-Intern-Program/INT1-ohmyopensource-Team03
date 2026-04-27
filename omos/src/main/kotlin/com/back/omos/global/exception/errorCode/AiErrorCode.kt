package com.back.omos.global.exception.errorCode

import org.springframework.http.HttpStatus

/**
 * AI 호출 및 응답 처리 과정에서 발생하는 에러 코드입니다.
 *
 * @author 5h6vm
 * @since 2026-04-24
 */
enum class AiErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String
) : ErrorCode {
    AI_RESPONSE_EMPTY(HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답이 비어 있습니다."),
    AI_RESPONSE_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답을 파싱하는 데 실패했습니다."),
}
