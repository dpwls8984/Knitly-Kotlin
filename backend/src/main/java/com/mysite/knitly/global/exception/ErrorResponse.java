package com.mysite.knitly.global.exception;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;

@Getter
@Builder
public class ErrorResponse {
    private final ErrorBody error;

    @Getter
    @Builder
    public static class ErrorBody{
        private final String code;
        private final String status;
        private final String message;
    }

    public static ErrorResponse errorResponse(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .error(ErrorBody.builder()
                        .code(errorCode.name())
                        .status(String.valueOf(errorCode.getStatus().value()))
                        .message(errorCode.getMessage())
                        .build())
                .build();
    }
    public static ErrorResponse validationError(BindingResult bindingResult) {
        return ErrorResponse.builder()
                .error(ErrorBody.builder()
                        .code(ErrorCode.VALIDATION_ERROR.name())
                        .status(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                        .message("요청 값이 올바르지 않습니다.")
                        .build())
                .build();
    }
}
