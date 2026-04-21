package com.plog.global.exception.errorCode

import org.springframework.http.HttpStatus

/**
 * 모든 errorCode enum class 의 인터페이스입니다. 해당 enum class 가 가져야 할 필드를 정의합니다.
 *
 * <p>상태에 대한 코드 및 http status, message에 대한 getter 메서드를 정의합니다.
 *
 * <p><b>상속 정보:</b><br>
 * 추후 모든 ErrorCode 종류의 구현체 클래스의 부모 인터페이스입니다.
 *
 * @author jack8
 * @see RuntimeException
 * @since 2026-01-15
 */
interface ErrorCode {
    val httpStatus: HttpStatus
    val message: String
}
