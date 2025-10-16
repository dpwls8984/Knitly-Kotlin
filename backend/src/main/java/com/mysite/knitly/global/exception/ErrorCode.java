package com.mysite.knitly.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // Common 0
    BAD_REQUEST("001", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."), // 에시, 삭제가능

    // User 1000
    USER_NOT_FOUND("1001", HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    // Product 2000
    PRODUCT_NOT_FOUND("2001", HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    // Order 3000

    // Post 4000

    // Comment 4000

    // Review 5000
    REVIEW_NOT_FOUND("5001", HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    REVIEW_NOT_AUTHORIZED("5002", HttpStatus.FORBIDDEN, "리뷰 삭제 권한이 없습니다."),
    REVIEW_CANNOT_BE_EMPTY("5003", HttpStatus.BAD_REQUEST, "리뷰 내용은 필수입니다."),
    REVIEW_RATING_INVALID("5004", HttpStatus.BAD_REQUEST, "리뷰 평점은 1~5 사이여야 합니다."),

    // Event 6000

    // Image 7000
    IMAGE_FORMAT_NOT_SUPPORTED("7501", HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다. JPG, JPEG, PNG만 가능합니다."),
    REVIEW_IMAGE_SAVE_FAILED("7502", HttpStatus.INTERNAL_SERVER_ERROR, "리뷰 이미지 저장에 실패했습니다.");
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
