package com.mysite.knitly.domain.product.like.controller

import com.mysite.knitly.domain.product.like.service.ProductLikeService
import com.mysite.knitly.domain.user.entity.User
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/products/{productId}/like")
class ProductLikeController(
    private val productLikeService: ProductLikeService
) {

    @PostMapping
    fun addLike(
        @AuthenticationPrincipal user: User,
        @PathVariable productId: Long
    ): ResponseEntity<Unit> {

        val currentUserId = user.userId
        log.info { "[POST /products/$productId/like] 좋아요 추가 - userId=$currentUserId" }

        productLikeService.addLike(currentUserId, productId)

        return ResponseEntity.ok().build()
    }

    @DeleteMapping
    fun deleteLike(
        @AuthenticationPrincipal user: User,
        @PathVariable productId: Long
    ): ResponseEntity<Unit> {

        val currentUserId = user.userId
        log.info { "[DELETE /products/$productId/like] 좋아요 취소 - userId=$currentUserId" }

        productLikeService.deleteLike(currentUserId, productId)

        return ResponseEntity.ok().build()
    }
}