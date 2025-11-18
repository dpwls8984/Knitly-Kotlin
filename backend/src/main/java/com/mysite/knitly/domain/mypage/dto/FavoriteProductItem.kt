package com.mysite.knitly.domain.mypage.dto

// 마이페이지 - 내가 찜한 상품 조회
data class FavoriteProductItem(
    val productId: Long,
    val productTitle: String,
    val sellerName: String,
    val thumbnailUrl: String?
)
