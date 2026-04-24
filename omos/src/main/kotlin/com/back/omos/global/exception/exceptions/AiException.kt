package com.back.omos.global.exception.exceptions

import com.back.omos.global.exception.errorCode.AiErrorCode

/**
 * AI 호출 및 응답 처리 과정에서 발생하는 예외입니다.
 *
 * <p>{@link AiErrorCode} 의 값과 (optional) 내부 로그 메시지를 담습니다.
 *
 * <p><b>상속 정보:</b><br>
 * {@link BaseException} 의 구현 클래스입니다.
 *
 * @author 5h6vm
 * @see AiErrorCode
 * @see BaseException
 * @since 2026-04-24
 */
class AiException : BaseException {
    constructor(errorCode: AiErrorCode) : super(errorCode)
    constructor(errorCode: AiErrorCode, logMessage: String) : super(errorCode, logMessage)
}
