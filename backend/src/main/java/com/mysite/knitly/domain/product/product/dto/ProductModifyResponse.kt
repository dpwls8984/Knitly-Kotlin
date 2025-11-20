package com.mysite.knitly.domain.product.product.dto

import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.entity.ProductCategory

data class ProductModifyResponse(
    val productId: Long,
    val title: String,
    val description: String,
    val productCategory: ProductCategory,
    val sizeInfo: String,
    val stockQuantity: Int,
    val productImageUrls: List<String>
) {
    companion object {
        fun from(product: Product, imageUrls: List<String>): ProductModifyResponse {
            return ProductModifyResponse(
                productId = product.productId
                    ?: throw IllegalStateException("Product ID가 null입니다."),
                title = product.title,
                description = product.description,
                productCategory = product.productCategory,
                sizeInfo = product.sizeInfo,

                stockQuantity = product.stockQuantity ?: 0,

                productImageUrls = imageUrls
            )
        }
    }
}