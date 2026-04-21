package com.plog.global.exception.exceptionHandler

import com.plog.global.exception.exceptions.BaseException
import com.plog.global.response.CommonResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * custom exception에 대한 exception handler 입니다.
 *
 * <p>{@link ExceptionHandlerOrder}에 정의된 바에 따라 비교적 후순위로 처리됩니다. 내부 필드 isDebug를 통해 내부 메시지의 표시 여부를
 * 결정합니다. 모든 응답은 {@link com.plog.global.response.CommonResponse CommonResponse} 를 통해 이루어집니다.
 *
 * <p><b>상속 정보:</b><br>
 * 상속 정보를 적습니다.
 *
 * <p><b>외부 모듈:</b><br>
 * 로그 표시를 위한 {@code Slf4j}를 사용합니다.
 *
 * @author jack8
 * @see ExceptionHandlerOrder
 * @see BaseException
 * @since 2026-01-15
 */
@RestControllerAdvice
@Order(ExceptionHandlerOrder.DEFAULT_EXCEPTION_HANDLER)
class DefaultExceptionHandler {
    private val log: Logger = LoggerFactory.getLogger(DefaultExceptionHandler::class.java)

    private fun fail(message: String?, status: HttpStatus): ResponseEntity<Any> {
        return ResponseEntity.status(status).body(CommonResponse.fail<Any>(message))
    }

    @ExceptionHandler(BaseException::class)
    fun handleBaseException(ex: BaseException): ResponseEntity<Any> {
        log.warn("{}", ex.logMessage)
        return fail(ex.message, ex.errorCode.httpStatus)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(ex: Exception): ResponseEntity<Any> {
        log.error("[unexpected] {}", ex.message, ex)
        return fail("unknown internal server error", HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
