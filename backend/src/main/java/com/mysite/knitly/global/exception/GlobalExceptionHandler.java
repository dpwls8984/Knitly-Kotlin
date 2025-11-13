package com.mysite.knitly.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j   // 팀원 코드에서 쓰던 로그 추가
public class GlobalExceptionHandler {

    // ServiceException은 팀원 코드 그대로 + 로그 유지
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorResponse> handleServiceException(ServiceException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("[ServiceException] code={}, message={}", errorCode, e.getMessage(), e);
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.errorResponse(errorCode));
    }

    // Bean Validation(@Valid) 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.validationError(bindingResult));
    }

    // 1) @RequestParam 누락 (multipart 포함)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException e) {
        // 필요한 경우 로그만 가볍게 남김
        log.warn("[MissingServletRequestParameterException] name={}, message={}", e.getParameterName(), e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.errorResponse(ErrorCode.BAD_REQUEST));
    }

    // 2) 타입 불일치 (예: category=NOT_ENUM, page=abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("[MethodArgumentTypeMismatchException] name={}, value={}, message={}",
                e.getName(), e.getValue(), e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.errorResponse(ErrorCode.BAD_REQUEST));
    }

    // 3) 잘못된 파라미터(직접 던진 IllegalArgumentException)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[IllegalArgumentException] message={}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.errorResponse(ErrorCode.BAD_REQUEST));
    }
}
