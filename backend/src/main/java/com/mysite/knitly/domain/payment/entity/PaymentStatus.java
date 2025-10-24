package com.mysite.knitly.domain.payment.entity;

public enum PaymentStatus {
    // 결제 준비(승인 전), 결제 진행 중(인증 후 승인 API 호출 전), 입금 대기 중(가상계좌), 결제 완료, 결제 취소, 결제 승인 실패
    READY, IN_PROGRESS, WAITING_FOR_DEPOSIT, DONE, CANCELED, FAILED;

    // 토스페이먼츠 API 응답의 status 값으로부터 enum 변환
    public static PaymentStatus fromString(String status) {
        if (status == null) {
            throw new IllegalArgumentException("결제 상태 정보가 없습니다.");
        }

        return switch (status.toUpperCase()) {
            case "READY" -> READY;
            case "IN_PROGRESS" -> IN_PROGRESS;
            case "WAITING_FOR_DEPOSIT" -> WAITING_FOR_DEPOSIT;
            case "DONE" -> DONE;
            case "CANCELED", "PARTIAL_CANCELED" -> CANCELED; // 부분취소도 취소로 통합
            case "ABORTED", "EXPIRED" -> FAILED; // 중단/만료도 실패로 통합
            case "FAILED" -> FAILED;
            default -> throw new IllegalArgumentException("지원하지 않는 결제 상태입니다: " + status);
        };
    }

    // 결제가 완료된 상태인지 확인
    public boolean isCompleted() {
        return this == DONE;
    }

    // 결제를 취소할 수 있는 상태인지 확인
    public boolean isCancelable() {
        return this == DONE;
    }

    // 결제가 진행 중인 상태인지 확인
    public boolean isInProgress() {
        return this == IN_PROGRESS || this == WAITING_FOR_DEPOSIT;
    }
}
