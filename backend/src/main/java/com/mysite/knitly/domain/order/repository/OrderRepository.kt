package com.mysite.knitly.domain.order.repository

import com.mysite.knitly.domain.order.entity.Order
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface OrderRepository : JpaRepository<Order, Long> {
    // tossOrderId로 주문 조회 (결제 승인 시 사용)
    fun findByTossOrderId(tossOrderId: String): Order?
}