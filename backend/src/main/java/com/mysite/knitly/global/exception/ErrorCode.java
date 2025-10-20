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

    // Design 6000
    DESIGN_NOT_FOUND("2001", HttpStatus.NOT_FOUND, "도안을 찾을 수 없습니다."),
    DESIGN_DELETION_NOT_ALLOWED("2002", HttpStatus.BAD_REQUEST, "판매 전 상태의 도안만 삭제할 수 있습니다."),
    DESIGN_UNAUTHORIZED("2003", HttpStatus.FORBIDDEN, "본인의 도안만 접근할 수 있습니다."),
    DESIGN_INVALID_GRID_SIZE("2004", HttpStatus.BAD_REQUEST, "도안은 10x10 크기여야 합니다."),
    DESIGN_PDF_GENERATION_FAILED("2005", HttpStatus.INTERNAL_SERVER_ERROR, "PDF 생성에 실패했습니다."),
    DESIGN_FILE_SAVE_FAILED("2006", HttpStatus.INTERNAL_SERVER_ERROR, "PDF 파일 저장에 실패했습니다."),   // Image 7000
    DESIGN_UNAUTHORIZED_DELETE("2007", HttpStatus.FORBIDDEN, "본인의 도안만 삭제할 수 있습니다."),
    DESIGN_NOT_DELETABLE("2008", HttpStatus.BAD_REQUEST, "해당 상태의 도안은 삭제할 수 없습니다.");

  // Event 6000

    // Image 7000
    IMAGE_FORMAT_NOT_SUPPORTED("7501", HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다. JPG, JPEG, PNG만 가능합니다."),
    REVIEW_IMAGE_SAVE_FAILED("7502", HttpStatus.INTERNAL_SERVER_ERROR, "리뷰 이미지 저장에 실패했습니다."),
    REVIEW_IMAGES_TOO_MANY("7503", HttpStatus.BAD_REQUEST, "리뷰 이미지는 최대 10개까지 등록할 수 있습니다.");

    // File 7000

    // event 8000

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
