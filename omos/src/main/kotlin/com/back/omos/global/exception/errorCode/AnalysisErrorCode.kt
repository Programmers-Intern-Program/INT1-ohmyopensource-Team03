package com.back.omos.global.exception.errorCode

import com.back.omos.global.exception.errorCode.ErrorCode
import org.springframework.http.HttpStatus

/**
 * Context Analyzer 도메인에서 발생하는 예외에 대한 상수 값을 정의합니다.
 *
 * <p>{@code AnalysisErrorCode}는 {@link
 * com.back.omos.global.exception.exceptions.AnalysisException AnalysisException}에서 사용되며,
 * {@code NAME(HttpStatus.STATUS, "some message")}로 저장됩니다.
 *
 * <p><b>상속 정보:</b><br>
 * {@link ErrorCode}의 구현체입니다.
 *
 * @author Jaewon Ryu
 * @see ErrorCode
 * @see com.back.omos.global.exception.exceptions.AnalysisException AnalysisException
 * @since 2026-04-22
 */
enum class AnalysisErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String
) : ErrorCode {
    ISSUE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 이슈를 찾을 수 없습니다."),
    ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 이슈에 대한 분석 결과가 존재하지 않습니다."),
    ANALYSIS_GENERATION_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "코드 분석 가이드 생성에 실패하였습니다."),
    GITHUB_API_FAIL(HttpStatus.BAD_GATEWAY, "GitHub API 호출에 실패하였습니다."),
    GLM_API_FAIL(HttpStatus.BAD_GATEWAY, "GLM API 호출에 실패하였습니다.");
}