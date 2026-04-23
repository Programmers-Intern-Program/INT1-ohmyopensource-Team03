package com.back.omos.global.exception.exceptions

import com.back.omos.global.exception.errorCode.IssueErrorCode

/**
 * 이슈 관리 및 처리 과정에서 발생하는 예외입니다.
 *
 * <p>{@link IssueErrorCode} 의 값과 (optional) 내부 로그 메시지를 담습니다.
 *
 * <p><b>상속 정보:</b><br>
 * {@link BaseException} 의 구현 클래스입니다.
 *
 * <p><b>주요 생성자:</b><br>
 * {@code IssueException(IssueErrorCode errorCode)} <br>
 * IssueErrorCode만 매개변수로 받도록 강제합니다. 내부 로그 메시지를 담지 않는 예외를 생성합니다. <br>
 * {@code IssueException(IssueErrorCode errorCode, String logMessage)} <br>
 * IssueErrorCode와 함께 내부 로그 메시지를 담는 예외를 생성합니다. <br>
 * {@code IssueException(IssueErrorCode errorCode, String logMessage, String clientMessage)} <br>
 * IssueErrorCode와 함께 클라이언트로의 반환 메시지 및 내부 로그 메시지를 담는 예외를 생성합니다. <br>
 *
 * @author 유재원
 * @see IssueErrorCode
 * @see BaseException
 * @since 2026-04-22
 */
class IssueException : BaseException {
    constructor(errorCode: IssueErrorCode) : super(errorCode)
    constructor(errorCode: IssueErrorCode, logMessage: String) : super(errorCode, logMessage)
    constructor(errorCode: IssueErrorCode, logMessage: String, clientMessage: String) : super(errorCode, logMessage, clientMessage)
}