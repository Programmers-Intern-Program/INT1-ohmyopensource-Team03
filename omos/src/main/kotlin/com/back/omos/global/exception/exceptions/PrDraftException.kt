package com.back.omos.global.exception.exceptions

import com.back.omos.global.exception.errorCode.PrDraftErrorCode

/**
 * PR 초안 관련 처리 과정에서 발생하는 예외입니다.
 *
 * <p>{@link PrDraftErrorCode}의 값을 담습니다.
 *
 * <p><b>상속 정보:</b><br>
 * {@link BaseException}의 구현 클래스입니다.
 *
 * @author 5h6vm
 * @see PrDraftErrorCode
 * @see BaseException
 * @since 2026-04-27
 */
class PrDraftException : BaseException {
    constructor(errorCode: PrDraftErrorCode) : super(errorCode)
    constructor(errorCode: PrDraftErrorCode, logMessage: String) : super(errorCode, logMessage)
}
