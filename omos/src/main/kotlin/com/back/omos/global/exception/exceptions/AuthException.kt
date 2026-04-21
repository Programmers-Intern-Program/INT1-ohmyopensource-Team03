package com.plog.global.exception.exceptions

import com.plog.global.exception.errorCode.AuthErrorCode

/**
 * 인증 / 인가 및 멤버 관리에서 발생하는 예외입니다.
 *
 * <p>{@link AuthErrorCode} 의 값과 (optional) 내부 로그 메시지를 담습니다.
 *
 * <p><b>상속 정보:</b><br>
 * {@link BaseException} 의 구현 클래스입니다.
 *
 * <p><b>주요 생성자:</b><br>
 * {@code AuthException(AuthErrorCode errorCode)} <br>
 * AuthErrorCode만 매개변수로 받도록 강제합니다. 내부 로그 메시지를 담지 않는 예외를 생성합니다. <br>
 * {@code AuthException(AuthErrorCode errorCode, String clientMessage)} <br>
 * AuthErrorCode만 매개변수로 받도록 강제합니다. 내부 로그 메시지를 담는 예외를 생성합니다. <br>
 * {@code AuthException(AuthErrorCode errorCode, String logMessage, String clientMessage)} <br>
 * AuthErrorCode만 매개변수로 받도록 강제합니다. 클라이언트로의 메시지 및 내부 로그 메시지를 담는 예외를 생성합니다. <br>
 *
 * @author jack8
 * @see AuthErrorCode
 * @see BaseException
 * @since 2026-01-15
 */
class AuthException : BaseException {
    constructor(errorCode: AuthErrorCode) : super(errorCode)
    constructor(errorCode: AuthErrorCode, logMessage: String) : super(errorCode, logMessage)
    constructor(errorCode: AuthErrorCode, logMessage: String, clientMessage: String) : super(errorCode, logMessage, clientMessage)
}
