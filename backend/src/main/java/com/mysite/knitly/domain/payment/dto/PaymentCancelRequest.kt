package com.mysite.knitly.domain.payment.dto

import jakarta.validation.constraints.NotBlank

data class PaymentCancelRequest(
    @field:NotBlank(message = "취소 사유는 필수입니다.")
    val cancelReason: String
)