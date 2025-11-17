package com.mysite.knitly.domain.order.dto

import com.mysite.knitly.domain.order.entity.Order
import com.mysite.knitly.domain.order.entity.OrderItem
import java.time.LocalDateTime

data class OrderCreateResponse(
    val orderId: Long?,
    val tossOrderId: String,
    val orderDate: LocalDateTime,
    val totalPrice: Double,
    val orderItems: List<OrderItemInfo>
) {

    companion object {
        fun from(order: Order): OrderCreateResponse {
            return OrderCreateResponse(
                orderId = order.orderId,
                tossOrderId = order.tossOrderId,
                orderDate = order.createdAt,
                totalPrice = order.totalPrice,
                orderItems = order.orderItems.map { OrderItemInfo.from(it) }
            )
        }
    }

    data class OrderItemInfo(
        val productId: Long?,
        val title: String,
        val orderPrice: Double
    ) {
        companion object {
            fun from(item: OrderItem): OrderItemInfo {
                return OrderItemInfo(
                    productId = item.product?.productId,
                    title = item.product?.title ?: "(상품명 없음)",
                    orderPrice = item.orderPrice
                )
            }
        }
    }
}
