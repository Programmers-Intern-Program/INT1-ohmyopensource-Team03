package com.plog.global.exception.errorCode

import org.springframework.http.HttpStatus

/**
 * 인증/인가 및 유저 조회에서 발생하는 예외에 대한 상수 값을 정의합니다.
 *
 * <p>상황에 대한 코드, 클라이언트로의 응답 코드 및 메시지를 가지며, 그 명명 규칙은 문서를 참조해야 합니다. 해당 {@code AuthErrorCode} 는 {@link
 * com.plog.global.exception.exceptions.AuthException AuthException}에서 사용되며, <br>
 * {@code NAME(HttpStatus.STATUS, "some message")}로 저장됩니다.
 *
 * <p><b>상속 정보:</b><br>
 * {@link ErrorCode}의 구현체입니다.
 *
 * @author jack8
 * @see ErrorCode
 * @see com.plog.global.exception.exceptions.AuthException AuthException
 * @since 2026-01-15
 */
enum class AuthErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String
) : ErrorCode {
    // 조회 및 기본 관리
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."),
    USER_CREATE_FAIL(HttpStatus.BAD_REQUEST, "사용자 생성에 실패하였습니다."),
    USER_UPDATE_FAIL(HttpStatus.BAD_REQUEST, "사용자 정보 수정에 실패하였습니다."),
    USER_DELETE_FAIL(HttpStatus.BAD_REQUEST, "사용자 삭제에 실패하였습니다."),

    // 회원가입 및 로그인
    SIGNUP_FAIL(HttpStatus.BAD_REQUEST, "회원가입에 실패하였습니다."),
    LOGIN_FAIL(HttpStatus.UNAUTHORIZED, "로그인에 실패하였습니다."),
    USER_ALREADY_EXIST(HttpStatus.CONFLICT, "이미 존재하는 사용자입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."),

    // 소셜 로그인
    SOCIAL_LOGIN_FAIL(HttpStatus.UNAUTHORIZED, "소셜 로그인에 실패하였습니다."),
    OAUTH_DATA_ACCESS_FAIL(HttpStatus.UNAUTHORIZED, "소셜 계정 정보를 불러오는 데 실패했습니다."),

    // 인증 및 권한
    USER_AUTH_FAIL(HttpStatus.FORBIDDEN, "사용자 권한 인증에 실패하였습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요한 서비스입니다.");
}