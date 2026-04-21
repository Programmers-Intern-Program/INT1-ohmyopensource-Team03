package com.plog.global.response

/**
 * 응답 형식에 대한 템플릿입니다. 도메인 및 기능 코드/상태/데이터/메시지가 포함되어 있습니다.
 *
 * factory method 패턴을 사용하여 다음과 같이 작성할 수 있습니다.
 * `CommonResponse<Example> response = CommonResponse.success(data, "message");`
 * 혹은
 * `CommonResponse<Example> response = CommonResponse.fail("message");`
 *
 * **상속 정보:**
 * [Response] 의 구현 클래스입니다.
 *
 * **주요 생성자:**
 * 생성자는 protected로 선언되었으며, static 메서드를 통하여 인스턴스가 생성됩니다.
 *
 * @author jack8
 * @since 2026-01-15
 */
open class CommonResponse<T> protected constructor(
    override val status: String,
    override val data: T?,
    override val message: String
) : Response<T> {

    companion object {
        @JvmStatic
        fun <T> success(data: T, message: String): CommonResponse<T> {
            return CommonResponse("success", data, message)
        }

        @JvmStatic
        fun <T> success(data: T): CommonResponse<T> {
            return CommonResponse("success", data, "success to response")
        }

        @JvmStatic
        fun <T> fail(message: String?): CommonResponse<T> {
            return CommonResponse("fail", null, message ?: "fail")
        }
    }
}
