package com.mysite.knitly.domain.product.product.dto

data class ProductListPageCache(
    val content: List<ProductListResponse>,
    val totalElements: Long
)
