package com.mysite.knitly.domain.payment.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.mysite.knitly.domain.payment.entity.PaymentStatus
import lombok.Builder
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PaymentCancelResponse(
    val paymentId: Long,
    val paymentKey: String,
    val orderId: String,
    val status: PaymentStatus,
    val cancelAmount: Long,
    val cancelReason: String,
    val canceledAt: LocalDateTime
) 