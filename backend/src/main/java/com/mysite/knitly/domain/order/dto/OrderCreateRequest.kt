package com.mysite.knitly.domain.order.dto

data class OrderCreateRequest(
    val productIds: List<Long>
)
