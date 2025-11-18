package com.mysite.knitly.domain.product.product.dto

import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.entity.ProductCategory
import java.time.LocalDateTime
// 2. @JvmRecord와 @JvmField 모두 제거 (팩토리 메서드를 사용하므로 불필요)
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
                // 4. [중요] Null 방어: product.productId는 Long? 입니다.
                productId = product.productId
                    ?: throw IllegalStateException("Product ID가 null입니다. 응답을 생성할 수 없습니다."),
                title = product.title,
                description = product.description,
                productCategory = product.productCategory,
                sizeInfo = product.sizeInfo,
                price = product.price,
                createdAt = product.createdAt, // 3. String.toString() 대신 객체 자체를 전달

                // 5. [중요] Elvis 연산자: stockQuantity가 Int? 이므로, null일 경우 0을 사용
                stockQuantity = product.stockQuantity ?: 0,

                likeCount = product.likeCount,
                isLikedByUser = isLikedByUser,

                // 5. Elvis 연산자
                avgReviewRating = product.avgReviewRating ?: 0.0,

                productImageUrls = imageUrls,

                // 6. [개선] if문 대신 Elvis 연산자 사용 (더 코틀린다움)
                reviewCount = product.reviewCount ?: 0
            )
        }
    }
}