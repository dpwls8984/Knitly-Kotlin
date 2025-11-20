package com.mysite.knitly.domain.mypage.dto

data class OrderLine(
    val orderItemId: Long,
    val productId: Long,
    val productTitle: String,
    val quantity: Int,
    val orderPrice: Double,
    val isReviewed: Boolean
)
