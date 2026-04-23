package com.back.omos.global.exception.errorCode

import com.plog.global.exception.errorCode.ErrorCode
import org.springframework.http.HttpStatus

/**
 * 이슈 관리 및 처리 과정에서 발생하는 예외에 대한 상수 값을 정의합니다.
 *
 * <p>상황에 대한 코드, 클라이언트로의 응답 코드 및 메시지를 가지며, 그 명명 규칙은 문서를 참조해야 합니다. 해당 {@code IssueErrorCode} 는 {@link
 * IssueException IssueException}에서 사용되며, <br>
 * {@code NAME(HttpStatus.STATUS, "some message")}로 저장됩니다.
 *
 * <p><b>상속 정보:</b><br>
 * {@link ErrorCode}의 구현체입니다.
 *
 * @author 유재원
 * @see ErrorCode
 * @see IssueException
 * @since 2026-04-22
 */
enum class IssueErrorCode (
    override val httpStatus: HttpStatus,
   override val message: String
) : ErrorCode {
    ISSUE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 이슈를 찾을 수 없습니다."),
    ISSUE_ALREADY_EXIST(HttpStatus.CONFLICT, "이미 존재하는 이슈입니다."),
}