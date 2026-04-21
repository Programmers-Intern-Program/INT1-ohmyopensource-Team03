package com.plog.global.exception.exceptionHandler

/**
 * exception handler 의 처리 순서에 대한 상수 값을 정의하고 관리하는 객체입니다.
 *
 * <p>public 한 static final 내부 필드를 통해 값을 관리합니다.
 *
 * @author jack8
 * @since 2026-01-15
 */
object ExceptionHandlerOrder {
    const val GLOBAL_EXCEPTION_HANDLER = 1
    const val DEFAULT_EXCEPTION_HANDLER = 2
}
