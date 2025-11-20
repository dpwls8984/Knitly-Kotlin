package com.mysite.knitly.domain.product.product.dto

import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.entity.ProductCategory
import java.time.LocalDateTime

data class ProductRegisterResponse(
    val productId: Long,
    val title: String,
    val description: String,
    val productCategory: ProductCategory,
    val sizeInfo: String,
    val price: Double,
    val createdAt: LocalDateTime?,
    val stockQuantity: Int,
    val designId: Long,
    val productImageUrls: List<String>
) {
    companion object {
        fun from(product: Product, imageUrls: List<String>): ProductRegisterResponse {
            return ProductRegisterResponse(
                productId = product.productId
                    ?: throw IllegalStateException("Product ID가 null입니다."),
                title = product.title,
                description = product.description,
                productCategory = product.productCategory,
                sizeInfo = product.sizeInfo,
                price = product.price,
                createdAt = product.createdAt,

                // 3. Elvis 연산자
                stockQuantity = product.stockQuantity ?: 0,

                // 2. Null 방어 (Design 엔티티의 ID도 Long? 일 것이므로)
                designId = product.design.designId
                    ?: throw IllegalStateException("Design ID가 null입니다."),
                productImageUrls = imageUrls
            )
        }
    }
}