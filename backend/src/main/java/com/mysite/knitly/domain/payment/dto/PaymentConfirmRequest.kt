package com.mysite.knitly.domain.payment.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class PaymentConfirmRequest(
    @field:NotBlank(message = "paymentKey는 필수입니다.")
    val paymentKey: String,

    @field:NotBlank(message = "orderId는 필수입니다.")
    val orderId: String,

    @field:NotNull(message = "amount는 필수입니다.")
    val amount: Long
)