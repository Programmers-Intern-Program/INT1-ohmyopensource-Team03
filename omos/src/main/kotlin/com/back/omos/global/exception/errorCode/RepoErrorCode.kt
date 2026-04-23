package com.back.omos.global.exception.errorCode

import org.springframework.http.HttpStatus

/**
 * 레포지토리 관리 및 조회 과정에서 발생하는 예외에 대한 상수 값을 정의합니다.
 *
 * <p>상황에 대한 코드, 클라이언트로의 응답 코드 및 메시지를 가지며, 그 명명 규칙은 문서를 참조해야 합니다. 해당 {@code RepositoryErrorCode} 는 {@link
 * RepositoryException RepositoryException}에서 사용되며, <br>
 * {@code NAME(HttpStatus.STATUS, "some message")}로 저장됩니다.
 *
 * <p><b>상속 정보:</b><br>
 * {@link ErrorCode}의 구현체입니다.
 *
 * @author 유재원
 * @see ErrorCode
 * @see RepoException
 * @since 2026-04-22
 */
enum class RepoErrorCode (
    override val httpStatus: HttpStatus,
    override val message: String
) : ErrorCode {
    /**
     * 밑에 형식에 맞춰서 사용하는 에러코드를 작성해주시면 됩니다.
     */
    REPO_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 레포지토리를 찾을 수 없습니다."),
}