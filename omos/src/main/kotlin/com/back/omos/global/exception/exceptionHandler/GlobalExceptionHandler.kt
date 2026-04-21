package com.plog.global.exception.exceptionHandler

import com.plog.global.response.CommonResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.ConversionNotSupportedException
import org.springframework.beans.TypeMismatchException
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.http.converter.HttpMessageNotWritableException
import org.springframework.validation.method.MethodValidationException
import org.springframework.web.ErrorResponseException
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingPathVariableException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.ServletRequestBindingException
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.context.request.async.AsyncRequestNotUsableException
import org.springframework.web.context.request.async.AsyncRequestTimeoutException
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import org.springframework.web.servlet.resource.NoResourceFoundException

/**
 * 스프링에서 request 시 발생하는 예외에 대한 handler 입니다.
 *
 * <p>여러 예외에 대하여, 이를 적절한 방식으로 가공하여 반환합니다. 다음과 같은 예외를 처리합니다.
 *
 * <pre>{@code
 * HttpRequestMethodNotSupportedException
 * HttpMediaTypeNotSupportedException
 * HttpMediaTypeNotAcceptableException
 * MissingPathVariableException
 * MissingServletRequestParameterException
 * MissingServletRequestPartException
 * ServletRequestBindingException
 * NoHandlerFoundException
 * NoResourceFoundException
 * HttpMessageNotReadableException
 * TypeMismatchException
 * MethodArgumentNotValidException
 * HandlerMethodValidationException
 * MethodValidationException
 * HttpMessageNotWritableException
 * ConversionNotSupportedException
 * ErrorResponseException
 * AsyncRequestNotUsableException
 * AsyncRequestTimeoutException
 * MaxUploadSizeExceededException
 * }</pre>
 *
 * <p><b>상속 정보:</b><br>
 * ResponseEntityExceptionHandler 추상 클래스의 구체 클래스입니다.
 *
 * @author jack8
 * @see ResponseEntityExceptionHandler
 * @see ExceptionHandlerOrder
 * @since 2026-01-15
 */
@RestControllerAdvice
@Order(ExceptionHandlerOrder.GLOBAL_EXCEPTION_HANDLER)
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {
    private val log: Logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    private fun fail(message: String?, status: HttpStatusCode): ResponseEntity<Any> {
        return ResponseEntity.status(status).body(CommonResponse.fail<Any>(message))
    }

    // ERE - 요청 형식 오류
    override fun handleHttpRequestMethodNotSupported(
        ex: HttpRequestMethodNotSupportedException, headers: HttpHeaders, status: HttpStatusCode, request: WebRequest
    ): ResponseEntity<Any>? {
        return fail("Unsupported HTTP method", HttpStatus.METHOD_NOT_ALLOWED)
    }

    override fun handleHttpMediaTypeNotSupported(
        ex: HttpMediaTypeNotSupportedException, headers: HttpHeaders, status: HttpStatusCode, request: WebRequest
    ): ResponseEntity<Any>? {
        return fail("Unsupported media studyType", HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    }

    override fun handleHttpMediaTypeNotAcceptable(
        ex: HttpMediaTypeNotAcceptableException, headers: HttpHeaders, status: HttpStatusCode, request: WebRequest
    ): ResponseEntity<Any>? {
        return fail("Not acceptable media studyType", HttpStatus.NOT_ACCEPTABLE)
    }

    override fun handleMissingPathVariable(
        ex: MissingPathVariableException, headers: HttpHeaders, status: HttpStatusCode, request: WebRequest
    ): ResponseEntity<Any>? {
        return fail("Missing path variable", HttpStatus.BAD_REQUEST)
    }

    override fun handleMissingServletRequestParameter(
        ex: MissingServletRequestParameterException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        return fail("Missing request parameter", HttpStatus.BAD_REQUEST)
    }

    override fun handleMissingServletRequestPart(
        ex: MissingServletRequestPartException, headers: HttpHeaders, status: HttpStatusCode, request: WebRequest
    ): ResponseEntity<Any>? {
        return fail("Missing request part", HttpStatus.BAD_REQUEST)
    }

    override fun handleServletRequestBindingException(
        ex: ServletRequestBindingException, headers: HttpHeaders, status: HttpStatusCode, request: WebRequest
    ): ResponseEntity<Any>? {
        return fail("Request binding failed", HttpStatus.BAD_REQUEST)
    }

    override fun handleNoHandlerFoundException(
        ex: NoHandlerFoundException, headers: HttpHeaders, status: HttpStatusCode, request: WebRequest
    ): ResponseEntity<Any>? {
        return fail("No handler found for URL", HttpStatus.NOT_FOUND)
    }

    override fun handleNoResourceFoundException(
        ex: NoResourceFoundException, headers: HttpHeaders, status: HttpStatusCode, request: WebRequest
    ): ResponseEntity<Any>? {
        return fail("Resource not found", HttpStatus.NOT_FOUND)
    }

    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException, headers: HttpHeaders, status: HttpStatusCode, request: WebRequest
    ): ResponseEntity<Any>? {
        return fail("Malformed JSON request", HttpStatus.BAD_REQUEST)
    }

    override fun handleTypeMismatch(
        ex: TypeMismatchException, headers: HttpHeaders, status: HttpStatusCode, request: WebRequest
    ): ResponseEntity<Any>? {
        return fail("Parameter studyType mismatch", HttpStatus.BAD_REQUEST)
    }

    // EVD - 검증 실패
    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException, headers: HttpHeaders, status: HttpStatusCode, request: WebRequest
    ): ResponseEntity<Any>? {
        val message = ex.bindingResult.fieldErrors.stream()
            .map { err -> err.field + ": " + err.defaultMessage }
            .findFirst()
            .orElse("Validation failed")
        return fail(message, HttpStatus.BAD_REQUEST)
    }

    override fun handleHandlerMethodValidationException(
        ex: HandlerMethodValidationException, headers: HttpHeaders, status: HttpStatusCode, request: WebRequest
    ): ResponseEntity<Any>? {
        return fail("Method parameter validation failed", HttpStatus.BAD_REQUEST)
    }

    override fun handleMethodValidationException(
        ex: MethodValidationException, headers: HttpHeaders, status: HttpStatus, request: WebRequest
    ): ResponseEntity<Any>? {
        return fail("Method-level validation failed", HttpStatus.BAD_REQUEST)
    }

    // EIS - 서버 내부 오류
    override fun handleHttpMessageNotWritable(
        ex: HttpMessageNotWritableException, headers: HttpHeaders, status: HttpStatusCode, request: WebRequest
    ): ResponseEntity<Any>? {
        return fail("Failed to write JSON response", HttpStatus.INTERNAL_SERVER_ERROR)
    }

    override fun handleConversionNotSupported(
        ex: ConversionNotSupportedException, headers: HttpHeaders, status: HttpStatusCode, request: WebRequest
    ): ResponseEntity<Any>? {
        return fail("Conversion not supported", HttpStatus.INTERNAL_SERVER_ERROR)
    }

    override fun handleErrorResponseException(
        ex: ErrorResponseException, headers: HttpHeaders, status: HttpStatusCode, request: WebRequest
    ): ResponseEntity<Any>? {
        return fail("Unhandled error occurred", HttpStatus.INTERNAL_SERVER_ERROR)
    }

    // EAS - 비동기 오류
    override fun handleAsyncRequestNotUsableException(
        ex: AsyncRequestNotUsableException, request: WebRequest
    ): ResponseEntity<Any>? {
        return fail("Async request not usable", HttpStatus.BAD_REQUEST)
    }

    override fun handleAsyncRequestTimeoutException(
        ex: AsyncRequestTimeoutException, headers: HttpHeaders, status: HttpStatusCode, request: WebRequest
    ): ResponseEntity<Any>? {
        return fail("Async request timeout", HttpStatus.SERVICE_UNAVAILABLE)
    }

    // EFU - 파일 업로드 실패
    override fun handleMaxUploadSizeExceededException(
        ex: MaxUploadSizeExceededException, headers: HttpHeaders, status: HttpStatusCode, request: WebRequest
    ): ResponseEntity<Any>? {
        return fail("File size exceeds limit", HttpStatus.CONTENT_TOO_LARGE)
    }
}
