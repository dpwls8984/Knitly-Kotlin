package com.mysite.knitly.domain.payment.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.mysite.knitly.domain.order.entity.Order
import com.mysite.knitly.domain.payment.entity.Payment
import com.mysite.knitly.domain.payment.entity.PaymentMethod
import com.mysite.knitly.domain.payment.entity.PaymentStatus
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PaymentDetailResponse(
    val orderName: String,          // 주문 상품명
    val orderId: String,            // 주문번호 (tossOrderId)
    val method: PaymentMethod,      // 결제 수단
    val totalAmount: Long,          // 결제 금액
    val status: PaymentStatus,      // 결제 상태
    val requestedAt: LocalDateTime?, // 결제 요청 시간
    val approvedAt: LocalDateTime? = null,  // 결제 승인 시간
    val canceledAt: LocalDateTime? = null,  // 취소 시간
    val cancelReason: String? = null,       // 취소 사유
    val buyerName: String           // 구매자 이름
) {

    companion object {
        /**
         * Payment 엔티티로부터 기본 정보 생성
         */
        @JvmStatic
        fun from(payment: Payment): PaymentDetailResponse {
            val order = payment.order
            val orderName = generateOrderName(order)

            return PaymentDetailResponse(
                orderName = orderName,
                orderId = payment.tossOrderId,
                method = payment.paymentMethod,
                totalAmount = payment.totalAmount,
                status = payment.paymentStatus,
                requestedAt = payment.requestedAt,
                approvedAt = payment.approvedAt,
                canceledAt = payment.canceledAt,
                cancelReason = payment.cancelReason,
                buyerName = payment.buyer.name
            )
        }

        private fun generateOrderName(order: Order): String {
            val orderItems = order.orderItems

            if (orderItems.isNullOrEmpty()) {
                return "상품 정보 없음"
            }

            val firstProductTitle = orderItems[0].product.title
            val remainingCount = orderItems.size - 1

            return if (remainingCount > 0) {
                "$firstProductTitle 외 ${remainingCount}개"
            } else {
                firstProductTitle
            }
        }
    }
}
