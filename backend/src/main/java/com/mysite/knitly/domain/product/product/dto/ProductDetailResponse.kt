package com.mysite.knitly.domain.product.product.dto

import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.entity.ProductCategory
import java.time.LocalDateTime
data class ProductDetailResponse(
    val productId: Long,
    val title: String,
    val description: String,
    val productCategory: ProductCategory,
    val sizeInfo: String,
    val price: Double,
    val createdAt: LocalDateTime?, // 3. String -> LocalDateTime
    val stockQuantity: Int,
    val likeCount: Int,
    val isLikedByUser: Boolean,
    val avgReviewRating: Double,
    val productImageUrls: List<String>,
    val reviewCount: Int
) {
    companion object {
        fun from(product: Product, imageUrls: List<String>, isLikedByUser: Boolean): ProductDetailResponse {
            return ProductDetailResponse(
                productId = product.productId
                    ?: throw IllegalStateException("Product ID가 null입니다. 응답을 생성할 수 없습니다."),
                title = product.title,
                description = product.description,
                productCategory = product.productCategory,
                sizeInfo = product.sizeInfo,
                price = product.price,
                createdAt = product.createdAt,

                stockQuantity = product.stockQuantity ?: 0,

                likeCount = product.likeCount,
                isLikedByUser = isLikedByUser,

                avgReviewRating = product.avgReviewRating ?: 0.0,

                productImageUrls = imageUrls,

                reviewCount = product.reviewCount ?: 0
            )
        }
    }
}