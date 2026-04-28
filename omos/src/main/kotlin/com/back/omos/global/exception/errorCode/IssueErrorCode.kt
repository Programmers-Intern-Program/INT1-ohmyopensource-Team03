package com.back.omos.global.exception.errorCode

import org.springframework.http.HttpStatus

/**
 * 코드에 대한 전체적인 역할을 적습니다.
 * <p>
 * 코드에 대한 작동 원리 등을 적습니다.
 *
 * <p><b>상속 정보:</b><br>
 * 상속 정보를 적습니다.
 *
 * <p><b>주요 생성자:</b><br>
 * {@code ExampleClass(String example)}  <br>
 * 주요 생성자와 그 매개변수에 대한 설명을 적습니다. <br>
 *
 * <p><b>빈 관리:</b><br>
 * 필요 시 빈 관리에 대한 내용을 적습니다.
 *
 * <p><b>외부 모듈:</b><br>
 * 필요 시 외부 모듈에 대한 내용을 적습니다.
 *
 * @author 유재원
 * @since 2026-04-23
 * @see
 */
enum class IssueErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String
) : ErrorCode {
    ISSUE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 이슈를 찾을 수 없습니다."),
    ISSUE_ALREADY_EXIST(HttpStatus.CONFLICT, "이미 존재하는 이슈입니다."),
    ISSUE_CRAWLING_FAIL(HttpStatus.CONFLICT, "이슈 크롤링을 실패했습니다."),
}