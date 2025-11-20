package com.mysite.knitly.domain.order.dto

data class EmailNotificationDto(
    val orderId: Long?,
    val userId: Long,
    val userEmail: String
)
