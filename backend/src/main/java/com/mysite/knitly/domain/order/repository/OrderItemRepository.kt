package com.mysite.knitly.domain.order.repository

import com.mysite.knitly.domain.order.entity.OrderItem
import org.springframework.data.jpa.repository.JpaRepository

interface OrderItemRepository : JpaRepository<OrderItem, Long>
