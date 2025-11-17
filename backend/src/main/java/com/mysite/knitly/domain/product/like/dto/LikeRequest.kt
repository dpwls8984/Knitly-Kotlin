package com.mysite.knitly.domain.product.like.dto

import jakarta.validation.constraints.NotNull

data class LikeRequest(
    @field:NotNull(message = "상품 ID는 필수입니다.")
    val productId: Long?
)