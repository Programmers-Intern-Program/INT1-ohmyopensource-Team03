package com.back.omos.global.exception.exceptions

import com.back.omos.global.exception.errorCode.AnalysisErrorCode
import com.plog.global.exception.exceptions.BaseException

/**
 * Context Analyzer 도메인에서 발생하는 예외입니다.
 *
 * <p>{@link AnalysisErrorCode}의 값과 (optional) 내부 로그 메시지를 담습니다.
 *
 * <p><b>상속 정보:</b><br>
 * {@link BaseException}의 구현 클래스입니다.
 *
 * <p><b>주요 생성자:</b><br>
 * {@code AnalysisException(AnalysisErrorCode errorCode)} <br>
 * 내부 로그 메시지를 담지 않는 예외를 생성합니다. <br>
 * {@code AnalysisException(AnalysisErrorCode errorCode, String logMessage)} <br>
 * 내부 로그 메시지를 담는 예외를 생성합니다. <br>
 * {@code AnalysisException(AnalysisErrorCode errorCode, String logMessage, String clientMessage)} <br>
 * 클라이언트 메시지 및 내부 로그 메시지를 담는 예외를 생성합니다. <br>
 *
 * @author Jaewon Ryu
 * @see AnalysisErrorCode
 * @see BaseException
 * @since 2026-04-22
 */
class AnalysisException : BaseException {
    constructor(errorCode: AnalysisErrorCode) : super(errorCode)
    constructor(errorCode: AnalysisErrorCode, logMessage: String) : super(errorCode, logMessage)
    constructor(errorCode: AnalysisErrorCode, logMessage: String, clientMessage: String) : super(errorCode, logMessage, clientMessage)
}