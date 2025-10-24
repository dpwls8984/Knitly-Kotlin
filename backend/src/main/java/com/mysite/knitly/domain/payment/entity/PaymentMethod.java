package com.mysite.knitly.domain.payment.entity;

public enum PaymentMethod {
    // 카드, 가상계좌, 간편결제
    CARD, VIRTUAL_ACCOUNT, EASY_PAY;

    // 토스페이먼츠 API 응답의 method 값으로부터 enum 변환
    public static PaymentMethod fromString(String method) {
        if (method == null) {
            throw new IllegalArgumentException("결제수단 정보가 없습니다.");
        }

        return switch (method.toUpperCase()) {
            case "CARD", "카드" -> CARD;
            case "VIRTUALACCOUNT", "VIRTUAL_ACCOUNT", "가상계좌" -> VIRTUAL_ACCOUNT;
            case "EASYPAY", "EASY_PAY", "간편결제" -> EASY_PAY;
            default -> throw new IllegalArgumentException("지원하지 않는 결제수단입니다: " + method);
        };
    }
}
