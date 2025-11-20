package com.mysite.knitly.domain.payment.repository

import com.mysite.knitly.domain.payment.entity.Payment
import com.mysite.knitly.domain.payment.entity.PaymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface PaymentRepository : JpaRepository<Payment, Long> {

    fun findByOrder_OrderId(orderId: Long?): Payment?

    fun findByTossPaymentKey(tossPaymentKey: String): Payment?

    fun findByPaymentStatusAndRequestedAtBefore(
        status: PaymentStatus,
        threshold: LocalDateTime
    ): List<Payment>
}
