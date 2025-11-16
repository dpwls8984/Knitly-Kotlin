package com.mysite.knitly.domain.mypage.dto

import java.time.LocalDateTime

data class OrderCardResponse(
    val orderId: Long,
    val orderedAt: LocalDateTime,
    val totalPrice: Double,
    val items: List<OrderLine>
) {
    companion object {
        @JvmStatic
        fun of(orderId: Long, orderedAt: LocalDateTime, totalPrice: Double): OrderCardResponse {

            return OrderCardResponse(
                orderId = orderId,
                orderedAt = orderedAt,
                totalPrice = totalPrice,
                items = mutableListOf()
            )
        }
    }
}
