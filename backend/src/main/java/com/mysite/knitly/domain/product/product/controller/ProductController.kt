package com.mysite.knitly.domain.product.product.controller

import com.mysite.knitly.domain.product.product.dto.ProductModifyRequest
import com.mysite.knitly.domain.product.product.dto.ProductModifyResponse
import com.mysite.knitly.domain.product.product.dto.ProductRegisterRequest
import com.mysite.knitly.domain.product.product.dto.ProductRegisterResponse
import com.mysite.knitly.domain.product.product.service.ProductService
import com.mysite.knitly.domain.user.entity.User
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/my/products")
class ProductController(
    private val productService: ProductService
) {

//    @PostMapping("/{designId}/sale")
//    fun registerProduct(
//        @AuthenticationPrincipal user: User,
//        @PathVariable designId: Long,
//        @ModelAttribute @Valid request: ProductRegisterRequest
//    ): ResponseEntity<ProductRegisterResponse> {
//        val response = productService.registerProduct(user, designId, request)
//        return ResponseEntity.ok(response)
//    }
//
//    @PatchMapping("/{productId}/modify")
//    fun modifyProduct(
//        @AuthenticationPrincipal user: User,
//        @PathVariable productId: Long,
//        @ModelAttribute @Valid request: ProductModifyRequest
//    ): ResponseEntity<ProductModifyResponse> {
//        val response = productService.modifyProduct(user, productId, request)
//        return ResponseEntity.ok(response)
//    }
//
//    @DeleteMapping("/{productId}")
//    fun deleteProduct(
//        @AuthenticationPrincipal user: User,
//        @PathVariable productId: Long
//    ): ResponseEntity<Void> {
//        productService.deleteProduct(user, productId)
//        return ResponseEntity.noContent().build()
//    }
//
//    @PostMapping("/{productId}/relist")
//    fun relistProduct(
//        @AuthenticationPrincipal user: User,
//        @PathVariable productId: Long
//    ): ResponseEntity<Void> {
//        productService.relistProduct(user, productId)
//        return ResponseEntity.ok().build()
//    }
}