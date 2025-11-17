package com.mysite.knitly.domain.product.product.dto

import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.entity.ProductCategory

import java.time.LocalDateTime

data class ProductListResponse(
        val productId: Long,
        val title: String,
        val productCategory: ProductCategory,
        val price: Double,
        val purchaseCount: Int,
        val likeCount: Int,
        val isLikedByUser: Boolean,
        val stockQuantity: Int?,
        val avgReviewRating: Double,
        val createdAt: LocalDateTime,
        val thumbnailUrl: String?,  // 대표 이미지 URL (sortOrder = 1)
        val sellerName: String,
        val isFree: Boolean,        // 무료 여부
        val isLimited: Boolean,     // 한정판매 여부
        val isSoldOut: Boolean,     // 품절 여부 (stockQuantity = 0)
        val userId: Long
) {
    companion object {
        fun from(product: Product, isLikedByUser: Boolean): ProductListResponse {
            // Product의 첫 번째 이미지를 thumbnailUrl로 사용
            val thumbnailUrl = product.productImages
                    ?.takeIf { it.isNotEmpty() }
                ?.let { images ->
                    // sortOrder가 1인 이미지를 찾거나, 없으면 첫 번째 이미지 사용
                    images.firstOrNull { it.sortOrder == 1L }?.productImageUrl
                    ?: images.first().productImageUrl
            }

            // data class는 생성자를 통해 필드를 초기화합니다.
            return ProductListResponse(
                    productId = product.productId!!,
                    title = product.title!!,
                    productCategory = product.productCategory!!,
                    price = product.price!!,
                    purchaseCount = product.purchaseCount!!,
                    likeCount = product.likeCount!!,
                    isLikedByUser = isLikedByUser,
                    stockQuantity = product.stockQuantity,
                    avgReviewRating = product.avgReviewRating!!,
                    createdAt = product.createdAt!!,
                    thumbnailUrl = thumbnailUrl, // 🔥 수정: Product의 첫 번째 이미지 URL
                    sellerName = product.user?.name ?: "알 수 없음",
                    isFree = product.price == 0.0,
                    isLimited = product.stockQuantity != null,
                    isSoldOut = product.stockQuantity != null && product.stockQuantity == 0,
                    userId = product.user!!.userId
            )
        }
    }
}