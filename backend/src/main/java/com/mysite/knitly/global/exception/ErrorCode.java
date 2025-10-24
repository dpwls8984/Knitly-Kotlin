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
    LIKE_ALREADY_EXISTS("2401", HttpStatus.CONFLICT, "이미 찜한 상품입니다."),
    LIKE_NOT_FOUND("2402", HttpStatus.NOT_FOUND, "삭제할 찜을 찾을 수 없습니다."),

    // Order 3000
    OUT_OF_STOCK("3001", HttpStatus.BAD_REQUEST, "품절된 상품입니다."),
    ORDER_NOT_FOUND("3002", HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),

    // Post 4000
    POST_NOT_FOUND("4001", HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
    POST_UNAUTHORIZED("4002", HttpStatus.UNAUTHORIZED, "로그인이 필요한 요청입니다."),
    POST_UPDATE_FORBIDDEN("4003", HttpStatus.FORBIDDEN, "게시글 수정 권한이 없습니다."),
    POST_DELETE_FORBIDDEN("4004", HttpStatus.FORBIDDEN, "게시글 삭제 권한이 없습니다."),
    POST_ALREADY_DELETED("4005", HttpStatus.BAD_REQUEST, "이미 삭제된 게시글입니다."),
    POST_CONTENT_TOO_SHORT("4006", HttpStatus.BAD_REQUEST, "게시글 내용은 최소 길이 요건을 충족해야 합니다."),
    POST_IMAGE_EXTENSION_INVALID("4007", HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다. JPG, JPEG, PNG만 가능합니다."),
    POST_TITLE_LENGTH_INVALID("4008", HttpStatus.BAD_REQUEST, "게시글 제목은 1자 이상 100자 이하로 작성해야 합니다."),
    POST_IMAGE_COUNT_EXCEEDED("4009", HttpStatus.BAD_REQUEST, "이미지는 최대 5개까지만 업로드할 수 있습니다."),
    VALIDATION_ERROR("4010", HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),

    // Comment 4050
    COMMENT_NOT_FOUND("4051", HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    COMMENT_UNAUTHORIZED("4052", HttpStatus.UNAUTHORIZED, "로그인이 필요한 요청입니다."),
    COMMENT_UPDATE_FORBIDDEN("4053", HttpStatus.FORBIDDEN, "댓글 수정 권한이 없습니다."),
    COMMENT_DELETE_FORBIDDEN("4054", HttpStatus.FORBIDDEN, "댓글 삭제 권한이 없습니다."),
    COMMENT_ALREADY_DELETED("4055", HttpStatus.BAD_REQUEST, "이미 삭제된 댓글입니다."),
    COMMENT_CONTENT_TOO_SHORT("4056", HttpStatus.BAD_REQUEST, "댓글은 1자 이상 300자 이하로 입력해 주세요."),

    // MyPage 4100
    MP_UNAUTHORIZED("4101", HttpStatus.UNAUTHORIZED, "로그인이 필요한 요청입니다."),
    MP_FORBIDDEN("4102", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    MP_PROFILE_NOT_FOUND("4103", HttpStatus.NOT_FOUND, "프로필 정보를 찾을 수 없습니다."),
    MP_ORDER_NOT_FOUND("4104", HttpStatus.NOT_FOUND, "주문 내역을 찾을 수 없습니다."),
    MP_ORDER_LIST_EMPTY("4105", HttpStatus.NO_CONTENT, "주문 내역이 비어 있습니다."),
    MP_MY_POSTS_EMPTY("4106", HttpStatus.NO_CONTENT, "작성한 게시글이 없습니다."),
    MP_MY_POSTS_QUERY_TOO_SHORT("4107", HttpStatus.BAD_REQUEST, "검색어가 너무 짧습니다."),
    MP_MY_COMMENTS_EMPTY("4108", HttpStatus.NO_CONTENT, "작성한 댓글이 없습니다."),
    MP_MY_COMMENTS_QUERY_TOO_SHORT("4109", HttpStatus.BAD_REQUEST, "검색어가 너무 짧습니다."),
    MP_FAVORITES_EMPTY("4110", HttpStatus.NO_CONTENT, "찜한 상품이 없습니다."),
    MP_REVIEWS_EMPTY("4111", HttpStatus.NO_CONTENT, "작성한 리뷰가 없습니다."),
    MP_INVALID_PAGE_PARAM("4112", HttpStatus.BAD_REQUEST, "page 파라미터가 올바르지 않습니다."),
    MP_INVALID_SIZE_PARAM("4113", HttpStatus.BAD_REQUEST, "size 파라미터가 올바르지 않습니다."),
    MP_INVALID_SORT_PARAM("4114", HttpStatus.BAD_REQUEST, "정렬 파라미터가 올바르지 않습니다."),


    // Review 5000

    REVIEW_NOT_FOUND("5001", HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    REVIEW_NOT_AUTHORIZED("5002", HttpStatus.FORBIDDEN, "리뷰 삭제 권한이 없습니다."),

    // Design 6000
    DESIGN_NOT_FOUND("2001", HttpStatus.NOT_FOUND, "도안을 찾을 수 없습니다."),
    DESIGN_DELETION_NOT_ALLOWED("2002", HttpStatus.BAD_REQUEST, "판매 전 상태의 도안만 삭제할 수 있습니다."),
    DESIGN_UNAUTHORIZED("2003", HttpStatus.FORBIDDEN, "본인의 도안만 접근할 수 있습니다."),
    DESIGN_INVALID_GRID_SIZE("2004", HttpStatus.BAD_REQUEST, "도안은 10x10 크기여야 합니다."),
    DESIGN_PDF_GENERATION_FAILED("2005", HttpStatus.INTERNAL_SERVER_ERROR, "PDF 생성에 실패했습니다."),
    DESIGN_FILE_SAVE_FAILED("2006", HttpStatus.INTERNAL_SERVER_ERROR, "PDF 파일 저장에 실패했습니다."),   // Image 7000
    DESIGN_UNAUTHORIZED_DELETE("2007", HttpStatus.FORBIDDEN, "본인의 도안만 삭제할 수 있습니다."),
    DESIGN_NOT_DELETABLE("2008", HttpStatus.BAD_REQUEST, "해당 상태의 도안은 삭제할 수 없습니다."),
    DESIGN_FILE_EMPTY("2009", HttpStatus.BAD_REQUEST, "업로드할 파일이 없습니다."),
    DESIGN_FILE_INVALID_TYPE("2010", HttpStatus.BAD_REQUEST, "PDF 파일만 업로드 가능합니다."),
    DESIGN_FILE_SIZE_EXCEEDED("2011", HttpStatus.BAD_REQUEST, "파일 크기는 10MB를 초과할 수 없습니다."),
    DESIGN_FILE_NAME_INVALID("2012", HttpStatus.BAD_REQUEST, "파일명이 유효하지 않습니다."),
    DESIGN_NOT_ON_SALE("2013", HttpStatus.BAD_REQUEST, "판매중이 아닌 도안입니다."),
    DESIGN_ALREADY_ON_SALE("2014", HttpStatus.BAD_REQUEST, "이미 판매중인 도안입니다."),
    DESIGN_NOT_STOPPED("2015", HttpStatus.BAD_REQUEST, "이미 판매중지된 도안입니다."),
    DESIGN_UNAUTHORIZED_ACCESS("2016", HttpStatus.FORBIDDEN, "본인의 도안만 접근할 수 있습니다."),
    DESIGN_FILE_NOT_FOUND("2017", HttpStatus.NOT_FOUND, "도안 파일을 찾을 수 없습니다."),

    // Event 6000

    // Image 7000
    IMAGE_FORMAT_NOT_SUPPORTED("7501", HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다. JPG, JPEG, PNG만 가능합니다."),
    REVIEW_IMAGE_SAVE_FAILED("7502", HttpStatus.INTERNAL_SERVER_ERROR, "리뷰 이미지 저장에 실패했습니다."),
    PRODUCT_IMAGE_SAVE_FAILED("7503", HttpStatus.INTERNAL_SERVER_ERROR, "상품 이미지 저장에 실패했습니다."),


    // File 7000

    // File 7000
    FILE_STORAGE_FAILED("7601", HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장에 실패했습니다."),

    // Payment 8000
    PAYMENT_NOT_FOUND("8001", HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."),
    PAYMENT_AMOUNT_MISMATCH("8002", HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다."),
    PAYMENT_API_CALL_FAILED("8003", HttpStatus.INTERNAL_SERVER_ERROR, "결제 API 호출에 실패했습니다."),
    PAYMENT_CONFIRM_FAILED("8004", HttpStatus.INTERNAL_SERVER_ERROR, "결제 승인에 실패했습니다."),
    PAYMENT_ALREADY_EXISTS("8005", HttpStatus.CONFLICT, "이미 결제가 완료된 주문입니다."),
    PAYMENT_NOT_CANCELABLE("8006", HttpStatus.BAD_REQUEST, "취소할 수 없는 결제 상태입니다."),
    PAYMENT_CANCEL_API_FAILED("8007", HttpStatus.INTERNAL_SERVER_ERROR, "결제 취소 API 호출에 실패했습니다."),
    PAYMENT_CANCEL_FAILED("8008", HttpStatus.INTERNAL_SERVER_ERROR, "결제 취소에 실패했습니다."),
    PAYMENT_UNAUTHORIZED_ACCESS("8009", HttpStatus.FORBIDDEN, "결제 정보에 접근할 권한이 없습니다."),
    // System 9000

    LOCK_ACQUISITION_FAILED("9001", HttpStatus.INTERNAL_SERVER_ERROR, "락 획득에 실패했습니다. 잠시 후 다시 시도해주세요.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
