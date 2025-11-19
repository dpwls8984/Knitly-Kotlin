package com.mysite.knitly.domain.product.like.entity

import java.io.Serializable

data class ProductLikeId(
    val userId: Long,
    val productId: Long
) : Serializable {
    constructor() : this(0L, 0L)
}