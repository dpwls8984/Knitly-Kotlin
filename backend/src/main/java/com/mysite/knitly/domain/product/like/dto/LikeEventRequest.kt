package com.mysite.knitly.domain.product.like.dto

import java.io.Serializable

data class LikeEventRequest(
    val userId: Long,
    val productId: Long
) : Serializable