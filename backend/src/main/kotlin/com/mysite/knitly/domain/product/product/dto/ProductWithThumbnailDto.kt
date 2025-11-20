package com.mysite.knitly.domain.product.product.dto

import com.mysite.knitly.domain.product.product.entity.ProductCategory
import java.time.LocalDateTime

data class ProductWithThumbnailDto(
    val productId: Long,
    val title: String,
    val productCategory: ProductCategory,
    val price: Double,
    val purchaseCount: Int,
    val likeCount: Int,
    val stockQuantity: Int?,
    val avgReviewRating: Double?,
    val createdAt: LocalDateTime,
    val thumbnailUrl: String?,
    val userId: Long
) {
    fun toResponse(isLikedByUser: Boolean, sellerName: String?): ProductListResponse {
        return ProductListResponse(
            this.productId,
            this.title,
            this.productCategory,
            this.price,
            this.purchaseCount,
            this.likeCount,
            isLikedByUser,
            this.stockQuantity,
            this.avgReviewRating ?: 0.0,
            this.createdAt,
            this.thumbnailUrl ?: "",
            sellerName ?: "판매자 정보 없음",
            this.price == 0.0,
            this.stockQuantity != null,
            this.stockQuantity != null && this.stockQuantity == 0,
            this.userId
        )
    }
}