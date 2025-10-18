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

    // Order 3000

    // Post 4000

    // Comment 4000

    // Review 5000

    // Design 6000
    DESIGN_NOT_FOUND("2001", HttpStatus.NOT_FOUND, "도안을 찾을 수 없습니다."),
    DESIGN_DELETION_NOT_ALLOWED("2002", HttpStatus.BAD_REQUEST, "판매 전 상태의 도안만 삭제할 수 있습니다."),
    DESIGN_UNAUTHORIZED("2003", HttpStatus.FORBIDDEN, "본인의 도안만 접근할 수 있습니다."),
    DESIGN_INVALID_GRID_SIZE("2004", HttpStatus.BAD_REQUEST, "도안은 10x10 크기여야 합니다."),
    DESIGN_PDF_GENERATION_FAILED("2005", HttpStatus.INTERNAL_SERVER_ERROR, "PDF 생성에 실패했습니다."),
    DESIGN_FILE_SAVE_FAILED("2006", HttpStatus.INTERNAL_SERVER_ERROR, "PDF 파일 저장에 실패했습니다."),   // Image 7000
    DESIGN_UNAUTHORIZED_DELETE("2007", HttpStatus.FORBIDDEN, "본인의 도안만 삭제할 수 있습니다."),
    DESIGN_NOT_DELETABLE("2008", HttpStatus.BAD_REQUEST, "해당 상태의 도안은 삭제할 수 없습니다.");

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
