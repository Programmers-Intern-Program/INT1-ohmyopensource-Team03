package com.back.omos.global.exception.errorCode

import org.springframework.http.HttpStatus

/**
 * PR 초안 관련 예외에 대한 상수 값을 정의합니다.
 *
 * <p>
 * {@code PrDraftErrorCode}는 {@link com.back.omos.global.exception.exceptions.PrDraftException PrDraftException}에서 사용됩니다.
 *
 * <p><b>상속 정보:</b><br>
 * {@link ErrorCode}의 구현체입니다.
 *
 * @author 5h6vm
 * @see ErrorCode
 * @see com.back.omos.global.exception.exceptions.PrDraftException PrDraftException
 * @since 2026-04-27
 */
enum class PrDraftErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String
) : ErrorCode {
    PR_DRAFT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 PR 초안을 찾을 수 없습니다."),
}
