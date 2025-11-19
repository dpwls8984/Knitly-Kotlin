package com.mysite.knitly.domain.payment.entity

enum class PaymentMethod {
    // 카드, 가상계좌, 간편결제, 무료 결제
    CARD, VIRTUAL_ACCOUNT, EASY_PAY, FREE;

    companion object {
        // 토스페이먼츠 API 응답의 method 값으로부터 enum 변환
        @JvmStatic
        fun fromString(method: String): PaymentMethod {
            requireNotNull(method) { "결제수단 정보가 없습니다." }

            return when (method.uppercase()) {
                "CARD", "카드" -> PaymentMethod.CARD
                "VIRTUALACCOUNT", "VIRTUAL_ACCOUNT", "가상계좌" -> PaymentMethod.VIRTUAL_ACCOUNT
                "EASYPAY", "EASY_PAY", "간편결제" -> PaymentMethod.EASY_PAY
                "FREE", "무료" -> PaymentMethod.FREE
                else -> throw IllegalArgumentException("지원하지 않는 결제수단입니다: " + method)
            }
        }
    }
}

