package com.mysite.knitly.global.exception

import org.springframework.http.HttpStatus
import org.springframework.validation.BindingResult

data class ErrorResponse(
    val error: ErrorBody
) {
    data class ErrorBody(
        val code: String,
        val status: String,
        val message: String
    )

    companion object {
        fun errorResponse(errorCode: ErrorCode): ErrorResponse =
            ErrorResponse(
                error = ErrorBody(
                    code = errorCode.name,
                    status = errorCode.status.value().toString(),
                    message = errorCode.message
                )
            )

        fun validationError(bindingResult: BindingResult): ErrorResponse =
            ErrorResponse(
                error = ErrorBody(
                    code = ErrorCode.VALIDATION_ERROR.name,
                    status = HttpStatus.BAD_REQUEST.value().toString(),
                    message = "요청 값이 올바르지 않습니다."
                )
            )
    }
}