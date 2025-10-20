package com.mysite.knitly.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // Common 0
    BAD_REQUEST("001", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."); // 에시, 삭제가능

    // User 1000

    // Product 2000

    // Order 3000

    // Post 4000

    // Comment 4000

    // Review 5000

    // Event 6000

    // Image 7000

    // File 7000

    // System 9000

    private final String code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
