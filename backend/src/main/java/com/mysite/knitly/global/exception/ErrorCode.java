package com.mysite.knitly.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // Common 0
    BAD_REQUEST("001", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."), // 에시, 삭제가능

    // User 1000

    // Product 2000

    // Order 3000

    // Post 4000

    // Comment 4000

    POST_NOT_FOUND("4000", HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),

    // 로그인 없이 작성/ 수정 /삭제 시도
    POST_UNAUTHORIZED("4001", HttpStatus.UNAUTHORIZED, "로그인이 필요한 요청입니다."),

    // 작성자가 아닌 사용자의 수정 시도
    POST_UPDATE_FORBIDDEN("4002", HttpStatus.FORBIDDEN, "게시글 수정 권한이 없습니다."),

    // 작성자가 아닌 사용자의 삭제 시도
    POST_DELETE_FORBIDDEN("4003", HttpStatus.FORBIDDEN, "게시글 삭제 권한이 없습니다."),

    // 이미 소프트삭제된 게시글에 대한 수정/삭제 시도
    POST_ALREADY_DELETED("4004", HttpStatus.BAD_REQUEST, "이미 삭제된 게시글입니다."),

    // 내용 길이/형식 검증 (ex: 최소 10자, 정하기)
    POST_CONTENT_TOO_SHORT("4005", HttpStatus.BAD_REQUEST, "게시글 내용은 최소 길이 요건을 충족해야 합니다."),

    // 이미지 확장자 검증 실패 (png/jpg/jpeg 외)
    POST_IMAGE_EXTENSION_INVALID("4006", HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다. JPG, JPEG, PNG만 가능합니다."),


    // 게시글 없음
    COMMENT_NOT_FOUND("4007", HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),

    // 로그인 안 한 경우
    COMMENT_UNAUTHORIZED("4008", HttpStatus.UNAUTHORIZED, "로그인이 필요한 요청입니다."),

    // 작성자가 아님
    COMMENT_UPDATE_FORBIDDEN("4009", HttpStatus.FORBIDDEN, "댓글 수정 권한이 없습니다."),

    // 작성자가 아님
    COMMENT_DELETE_FORBIDDEN("4010", HttpStatus.FORBIDDEN, "댓글 삭제 권한이 없습니다."),

    COMMENT_ALREADY_DELETED("4011", HttpStatus.BAD_REQUEST, "이미 삭제된 댓글입니다."),

    // 댓글 길이 제한
    COMMENT_CONTENT_TOO_SHORT("4012", HttpStatus.BAD_REQUEST, "댓글은 1자 이상 300자 이하로 입력해 주세요.");


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
