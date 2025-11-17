package com.mysite.knitly.domain.product.product.controller

import com.mysite.knitly.domain.product.product.dto.ProductDetailResponse
import com.mysite.knitly.domain.product.product.dto.ProductListResponse
import com.mysite.knitly.domain.product.product.entity.ProductCategory
import com.mysite.knitly.domain.product.product.entity.ProductFilterType
import com.mysite.knitly.domain.product.product.entity.ProductSortType
import com.mysite.knitly.domain.product.product.service.ProductService
import com.mysite.knitly.domain.user.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/products")
class ProductListController(
    private val productService: ProductService
) {

    @GetMapping
    fun getProducts(
        @AuthenticationPrincipal user: User?,
        @RequestParam(required = false) category: ProductCategory?,
        @RequestParam(required = false, defaultValue = "ALL") filter: ProductFilterType,
        @RequestParam(required = false, defaultValue = "LATEST") sort: ProductSortType,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<ProductListResponse>> {
        val response = productService.getProducts(user, category, filter, sort, pageable)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{productId}")
    fun getProductDetail(
        @AuthenticationPrincipal user: User?,
        @PathVariable productId: Long
    ): ResponseEntity<ProductDetailResponse> {
        val response = productService.getProductDetail(user, productId)
        return ResponseEntity.ok(response)
    }
}