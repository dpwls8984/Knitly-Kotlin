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
    PRODUCT_MODIFY_UNAUTHORIZED("2002", HttpStatus.FORBIDDEN, "상품 수정 권한이 없습니다."),
    PRODUCT_DELETE_UNAUTHORIZED("2003", HttpStatus.FORBIDDEN, "상품 삭제 권한이 없습니다."),
    PRODUCT_ALREADY_DELETED("2004", HttpStatus.BAD_REQUEST, "이미 삭제된 상품입니다."),
    PRODUCT_STOCK_INSUFFICIENT("2005", HttpStatus.BAD_REQUEST, "상품 재고보다 많은 수량을 주문할 수 없습니다. 남은 재고를 확인해주세요."),

    // Order 3000
    OUT_OF_STOCK("3001", HttpStatus.BAD_REQUEST, "품절된 상품입니다."),

    // Post 4000

    // Comment 4000

    // Review 5000

    // Design 6000
    DESIGN_NOT_FOUND("6001", HttpStatus.NOT_FOUND, "상품으로 등록할 도안을 찾을 수 없습니다.");

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
