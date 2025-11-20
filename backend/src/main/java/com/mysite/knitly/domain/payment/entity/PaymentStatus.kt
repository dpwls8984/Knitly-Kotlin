package com.mysite.knitly.domain.payment.entity

enum class PaymentStatus {
    // 결제 준비(승인 전), 결제 승인 요청 중, 입금 대기 중(가상계좌), 결제 완료, 결제 취소, 결제 실패
    READY, IN_PROGRESS, WAITING_FOR_DEPOSIT, DONE, CANCELED, FAILED;

    val isCompleted: Boolean
        // 결제가 완료된 상태인지 확인
        get() = this == DONE

    val isCancelable: Boolean
        // 결제를 취소할 수 있는 상태인지 확인
        get() = this == DONE || this == READY

    val isInProgress: Boolean
        // 결제가 진행 중인 상태인지 확인
        get() = this == IN_PROGRESS || this == WAITING_FOR_DEPOSIT

    companion object {
        // 토스페이먼츠 API 응답의 status 값으로부터 enum 변환
        @JvmStatic
        fun fromString(status: String): PaymentStatus {
            requireNotNull(status) { "결제 상태 정보가 없습니다." }

            return when (status.uppercase()) {
                "READY" -> READY
                "IN_PROGRESS" -> IN_PROGRESS
                "WAITING_FOR_DEPOSIT" -> WAITING_FOR_DEPOSIT
                "DONE" -> DONE
                "CANCELED", "PARTIAL_CANCELED" -> CANCELED // 부분취소도 취소로 통합
                "ABORTED", "EXPIRED" -> FAILED // 중단/만료도 실패로 통합
                "FAILED" -> FAILED
                else -> throw IllegalArgumentException("지원하지 않는 결제 상태입니다: $status")
            }
        }
    }
}
